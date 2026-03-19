package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WidgetUpdateReceiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        // Redraw the widget smoothly with a protected coroutine
        GlobalScope.launch {
            try {
                if (intent.action == "com.example.myapplication.ACTION_REFRESH_API") {
                    val prefs = context.getSharedPreferences("waktu_solat_prefs", Context.MODE_PRIVATE)
                    val zone = prefs.getString("zone", "SGR01") ?: "SGR01"
                    try {
                        val retrofit = retrofit2.Retrofit.Builder()
                            .baseUrl("https://www.e-solat.gov.my/")
                            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                            .build()
                        val api = retrofit.create(PrayerApiService::class.java)
                        val response = api.getPrayerTimes(zone = zone)
                        if (response.prayerTime.isNotEmpty()) {
                            val p = response.prayerTime[0]
                            prefs.edit().apply {
                                putString("imsak", p.imsak)
                                putString("subuh", p.fajr)
                                putString("syuruk", p.syuruk)
                                putString("zohor", p.dhuhr)
                                putString("asar", p.asr)
                                putString("maghrib", p.maghrib)
                                putString("isyak", p.isha)
                            }.apply()
                            val pt = JakimPrayerTime("", "", "", p.imsak, p.fajr, p.syuruk, p.dhuhr, p.asr, p.maghrib, p.isha)
                            val notifyEnabled = prefs.getBoolean("notify_enabled", false)
                            val notifyMinutes = prefs.getInt("notify_minutes", 10)
                            if (notifyEnabled) {
                                PrayerAlarmScheduler.scheduleAlarms(context, pt, notifyMinutes)
                            }
                            PrayerAlarmScheduler.scheduleWidgetUpdates(context, pt)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                PrayerWidget().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
        
        // If the user manually changes the device time, dynamically reschedule all alarms instantly
        if (intent.action == Intent.ACTION_TIME_CHANGED || intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            val prefs = context.getSharedPreferences("waktu_solat_prefs", Context.MODE_PRIVATE)
            val notifyEnabled = prefs.getBoolean("notify_enabled", false)
            val notifyMinutes = prefs.getInt("notify_minutes", 10)
            
            val imsak = prefs.getString("imsak", "05:30") ?: "05:30"
            val fajr = prefs.getString("subuh", "05:45") ?: "05:45"
            val syuruk = prefs.getString("syuruk", "07:10") ?: "07:10"
            val dhuhr = prefs.getString("zohor", "13:20") ?: "13:20"
            val asar = prefs.getString("asar", "16:35") ?: "16:35"
            val maghrib = prefs.getString("maghrib", "19:25") ?: "19:25"
            val isha = prefs.getString("isyak", "20:35") ?: "20:35"
            
            val pt = JakimPrayerTime("", "", "", imsak, fajr, syuruk, dhuhr, asar, maghrib, isha)
            
            if (notifyEnabled) {
                PrayerAlarmScheduler.scheduleAlarms(context, pt, notifyMinutes)
            }
            PrayerAlarmScheduler.scheduleWidgetUpdates(context, pt)
        }
    }
}
