package com.example.myapplication

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.GlanceTheme
import androidx.glance.background
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider

import android.widget.RemoteViews
import android.os.SystemClock
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.action.clickable
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.action.actionSendBroadcast
import android.content.Intent

class PrayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("waktu_solat_prefs", Context.MODE_PRIVATE)
        val zone = prefs.getString("zone", "SGR01") ?: "SGR01"
        
        // Load the actual cached times, fallback to placeholders if user hasn't opened app yet
        val subuh = prefs.getString("subuh", "05:45")?.take(5) ?: "05:45"
        val syuruk = prefs.getString("syuruk", "07:10")?.take(5) ?: "07:10"
        val zohor = prefs.getString("zohor", "13:20")?.take(5) ?: "13:20"
        val asar = prefs.getString("asar", "16:35")?.take(5) ?: "16:35"
        val maghrib = prefs.getString("maghrib", "19:25")?.take(5) ?: "19:25"
        val isyak = prefs.getString("isyak", "20:35")?.take(5) ?: "20:35"

        // Calculate next prayer at current widget refresh
        val now = java.util.Calendar.getInstance()
        val currentSecs = now.get(java.util.Calendar.HOUR_OF_DAY) * 3600 + now.get(java.util.Calendar.MINUTE) * 60 + now.get(java.util.Calendar.SECOND)
        
        var nextPrayerName = "Subuh"
        var nextPrayerTimeStr = subuh
        var found = false
        var diffMillis = 0L
        val pList = listOf("Subuh" to subuh, "Syuruk" to syuruk, "Zohor" to zohor, "Asar" to asar, "Maghrib" to maghrib, "Isyak" to isyak)
        
        for ((name, timeStr) in pList) {
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                val pSecs = parts[0].toInt() * 3600 + parts[1].toInt() * 60
                if (pSecs > currentSecs) {
                    nextPrayerName = name
                    nextPrayerTimeStr = timeStr
                    diffMillis = (pSecs - currentSecs) * 1000L
                    found = true
                    break
                }
            }
        }
        
        if (!found) {
            val parts = subuh.split(":")
            if (parts.size >= 2) {
                val pSecs = parts[0].toInt() * 3600 + parts[1].toInt() * 60
                val tomorrowSecs = pSecs + 86400
                diffMillis = (tomorrowSecs - currentSecs) * 1000L
            }
            nextPrayerName = "Subuh"
            nextPrayerTimeStr = subuh
        }

        provideContent {
            GlanceTheme {
                val darkSurface = ColorProvider(Color(0xFF0F172A))
                val textPrimary = ColorProvider(Color(0xFFF8FAFC))
                val textSecondary = ColorProvider(Color(0xFF94A3B8))
                val highlightColor = ColorProvider(Color(0xFF38BDF8))
                
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(darkSurface)
                        .padding(bottom = 8.dp, top = 8.dp, start = 8.dp, end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⟳",
                            style = TextStyle(
                                color = highlightColor, 
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            modifier = GlanceModifier.clickable(
                                onClick = actionSendBroadcast(
                                    Intent(context, WidgetUpdateReceiver::class.java).apply {
                                        action = "com.example.myapplication.ACTION_REFRESH_API"
                                    }
                                )
                            ).padding(end = 8.dp)
                        )
                        
                        Text(
                            text = "$zone • Seterusnya: $nextPrayerName ($nextPrayerTimeStr)  ",
                            style = TextStyle(
                                color = highlightColor, 
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        
                        androidx.compose.runtime.key(diffMillis) {
                            val rv = RemoteViews(context.packageName, R.layout.chronometer).apply {
                                setChronometer(R.id.chrono, SystemClock.elapsedRealtime() + diffMillis, "%s", true)
                            }
                            AndroidRemoteViews(remoteViews = rv)
                        }
                    }
                    
                    Spacer(modifier = GlanceModifier.height(16.dp))
                    
                    Row(
                        modifier = GlanceModifier.fillMaxWidth()
                    ) {
                        PrayerItem("Subuh", subuh, if (nextPrayerName == "Subuh") highlightColor else textSecondary, if (nextPrayerName == "Subuh") highlightColor else textPrimary, GlanceModifier.defaultWeight())
                        PrayerItem("Syuruk", syuruk, if (nextPrayerName == "Syuruk") highlightColor else textSecondary, if (nextPrayerName == "Syuruk") highlightColor else textPrimary, GlanceModifier.defaultWeight())
                        PrayerItem("Zohor", zohor, if (nextPrayerName == "Zohor") highlightColor else textSecondary, if (nextPrayerName == "Zohor") highlightColor else textPrimary, GlanceModifier.defaultWeight())
                        PrayerItem("Asar", asar, if (nextPrayerName == "Asar") highlightColor else textSecondary, if (nextPrayerName == "Asar") highlightColor else textPrimary, GlanceModifier.defaultWeight())
                        PrayerItem("Maghrib", maghrib, if (nextPrayerName == "Maghrib") highlightColor else textSecondary, if (nextPrayerName == "Maghrib") highlightColor else textPrimary, GlanceModifier.defaultWeight())
                        PrayerItem("Isyak", isyak, if (nextPrayerName == "Isyak") highlightColor else textSecondary, if (nextPrayerName == "Isyak") highlightColor else textPrimary, GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }

    @Composable
    private fun PrayerItem(
        name: String, 
        time: String, 
        nameColor: ColorProvider, 
        timeColor: ColorProvider,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.padding(horizontal = 2.dp)
        ) {
            Text(
                text = name, 
                style = TextStyle(
                    color = nameColor, 
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = time, 
                style = TextStyle(
                    color = timeColor, 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()
}
