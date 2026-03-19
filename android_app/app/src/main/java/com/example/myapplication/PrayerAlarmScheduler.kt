package com.example.myapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object PrayerAlarmScheduler {
    fun scheduleAlarms(context: Context, prayerTimes: JakimPrayerTime, notifyBeforeMins: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        
        val prayers = listOf(
            "Subuh" to prayerTimes.fajr,
            "Syuruk" to prayerTimes.syuruk,
            "Zohor" to prayerTimes.dhuhr,
            "Asar" to prayerTimes.asr,
            "Maghrib" to prayerTimes.maghrib,
            "Isyak" to prayerTimes.isha
        )
        
        val now = java.util.Calendar.getInstance()
        
        prayers.forEachIndexed { index, (name, timeStr) ->
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                val calExact = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                    set(java.util.Calendar.MINUTE, parts[1].toInt())
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                
                // 1. EXACT Prayer Time Notification
                if (calExact.after(now)) {
                    val intentExact = Intent(context, PrayerNotificationReceiver::class.java).apply {
                        putExtra("PRAYER_NAME", name)
                        putExtra("MINUTES_BEFORE", 0)
                    }
                    val piExact = PendingIntent.getBroadcast(context, index, intentExact, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calExact.timeInMillis, piExact)
                        } else {
                            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, calExact.timeInMillis, 60000L, piExact)
                        }
                    } catch (e: SecurityException) {
                        try { alarmManager.set(AlarmManager.RTC_WAKEUP, calExact.timeInMillis, piExact) } catch (e2: Exception) {}
                    } catch (e: Exception) {}
                }
                
                // 2. WARNING Before Prayer Notification
                if (notifyBeforeMins > 0) {
                    val calBefore = calExact.clone() as java.util.Calendar
                    calBefore.add(java.util.Calendar.MINUTE, -notifyBeforeMins)
                    
                    if (calBefore.after(now)) {
                        val intentBefore = Intent(context, PrayerNotificationReceiver::class.java).apply {
                            putExtra("PRAYER_NAME", name)
                            putExtra("MINUTES_BEFORE", notifyBeforeMins)
                        }
                        val piBefore = PendingIntent.getBroadcast(context, index + 10, intentBefore, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calBefore.timeInMillis, piBefore)
                            } else {
                                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, calBefore.timeInMillis, 60000L, piBefore)
                            }
                        } catch (e: SecurityException) {
                            try { alarmManager.set(AlarmManager.RTC_WAKEUP, calBefore.timeInMillis, piBefore) } catch (e2: Exception) {}
                        } catch (e: Exception) {}
                    }
                }
            }
        }
    }

    fun scheduleWidgetUpdates(context: Context, prayerTimes: JakimPrayerTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        
        val prayers = listOf(
            prayerTimes.fajr,
            prayerTimes.syuruk,
            prayerTimes.dhuhr,
            prayerTimes.asr,
            prayerTimes.maghrib,
            prayerTimes.isha
        )
        
        val now = java.util.Calendar.getInstance()
        
        prayers.forEachIndexed { index, timeStr ->
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                    set(java.util.Calendar.MINUTE, parts[1].toInt())
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                
                // Schedule EXACTLY at the prayer time to refresh the countdown UI
                if (cal.after(now)) {
                    val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                        action = "com.example.myapplication.ACTION_UPDATE_WIDGET"
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        index + 100, // offset request codes
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                            } else {
                                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, cal.timeInMillis, 60000L, pendingIntent)
                            }
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                        }
                    } catch (e: Exception) { }
                }
            }
        }
        
        // Also schedule a midnight refresh to wrap around the "Isyak -> Subuh" gap properly
        val midnight = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 1)
        }
        val midnightIntent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            action = "com.example.myapplication.ACTION_UPDATE_WIDGET"
        }
        val midnightPi = PendingIntent.getBroadcast(context, 999, midnightIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, midnight.timeInMillis, 60000L, midnightPi)
        } catch (e: Exception) { }
    }
    
    fun cancelAlarms(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        for (i in 0..15) {
            val intent = Intent(context, PrayerNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, i, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
