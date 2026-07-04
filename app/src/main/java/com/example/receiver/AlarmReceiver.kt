package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val type = intent.getStringExtra("type") ?: "hydration"
        val message = intent.getStringExtra("message") ?: "Time to hydrate!"
        
        Log.d("AlarmReceiver", "Received alarm: type=$type, message=$message")
        
        val id = intent.getIntExtra("id", 0)
        showNotification(context, type, message, id)

        if (type == "hydration") {
            scheduleNextHydrationAlarm(context)
        }
    }

    private fun showNotification(context: Context, type: String, message: String, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "senior_care_alerts"
        val channelName = "SeniorCare Notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders for senior hydration and meditation"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", type)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (type == "meditation") 1001 else 1002,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (type) {
            "meditation" -> "🧘 Daily Meditation Time (ध्यान लगाने का समय)"
            "hydration" -> "💧 Drink Water Reminder (पानी पीने का समय)"
            "medication" -> "💊 Take Medicine Reminder (दवा लेने का समय)"
            else -> "👵 SeniorCare Reminder"
        }

        val iconRes = android.R.drawable.ic_dialog_info

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = when (type) {
            "meditation" -> 101
            "hydration" -> 102
            else -> 103 + id
        }
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        fun scheduleMedicationAlarm(context: Context, reminderId: Int, name: String, dosage: String, time: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("type", "medication")
                putExtra("id", reminderId)
                putExtra("message", "Time to take your medicine: $name ($dosage) - दवा लेने का समय हो गया है।")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                // Parse time format e.g. "08:30 AM"
                val parts = time.split(" ")
                if (parts.size < 2) return
                val hm = parts[0].split(":")
                if (hm.size < 2) return
                var hour = hm[0].toInt()
                val min = hm[1].toInt()
                val amPm = parts[1]

                if (amPm == "PM" && hour < 12) hour += 12
                if (amPm == "AM" && hour == 12) hour = 0

                val calendar = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, min)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
                Log.d("AlarmReceiver", "Scheduled medication alarm successfully for $name at $time")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error scheduling alarm", e)
            }
        }

        fun cancelMedicationAlarm(context: Context, reminderId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        fun scheduleNextHydrationAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("type", "hydration")
                putExtra("message", "कृपया पानी पियें! स्वस्थ रहने के लिए हाइड्रेटेड रहना बहुत जरूरी है। (Please drink water!)")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val triggerTime = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                Log.d("AlarmReceiver", "Scheduled next hydration alarm in 5 minutes successfully")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Failed to schedule hydration alarm", e)
            }
        }
    }
}
