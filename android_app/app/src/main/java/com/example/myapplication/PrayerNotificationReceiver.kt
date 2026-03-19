package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class PrayerNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Solat"
        val minsBefore = intent.getIntExtra("MINUTES_BEFORE", 0)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = "prayer_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Peringatan Waktu Solat",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val message = if (minsBefore > 0) {
            "Waktu $prayerName akan masuk dalam masa $minsBefore minit."
        } else {
            "Telah masuk waktu $prayerName."
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            // Use standard Android icon since we don't have custom drawables yet
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Peringatan Solat ($prayerName)")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(prayerName.hashCode(), notification)
    }
}
