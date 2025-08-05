package com.deckerpw.birthdays

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

const val channelId = "daily_channel"

fun scheduleDailyNotifications(context: Context, activity: MainActivity) {
    // Check if the POST_NOTIFICATIONS permission is granted
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            1001
        )
    }
    // Create the notification channel if it doesn't exist
    val channel = NotificationChannel(
        channelId,
        "Birthday Reminders",
        NotificationManager.IMPORTANCE_DEFAULT
    )
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)
    // Set up the alarm to trigger the DailyNotificationReceiver at 12:00 every day
    val intent = Intent(context, DailyNotificationReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    // Get the AlarmManager service
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    // Target the next occurrence of 10:00 AM
    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        if (before(Calendar.getInstance())) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    // Set the repeating alarm
    // Check if the alarm is already set
    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}

class DailyNotificationReceiver : BroadcastReceiver() {

    companion object {
        fun checkSoonBirthdays(context: Context) {
            val people = mutableListOf<String>()
            db.getBirthdays().forEach { birthday ->
                if (birthday.daysUntilNextBirthday().toInt() == 7) {
                    people.add(birthday.name)
                }
            }
            if (people.isNotEmpty()) {
                val text = if (people.size == 1) {
                    "${people[0]} hat in 7 Tagen Geburtstag!"
                } else {
                    "${people.joinToString(", ")} haben in 7 Tagen Geburtstag!"
                }
                val notification = Notification.Builder(context, channelId)
                    .setContentTitle("Bald ist es soweit!")
                    .setContentText(text)
                    .setSmallIcon(R.drawable.cake_icon)
                    .setAutoCancel(true)
                    .setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .build()
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(1001, notification)
            }
        }

        fun checkTodayBirthdays(context: Context) {
            val people = mutableListOf<String>()
            db.getBirthdays().forEach { birthday ->
                if (birthday.daysUntilNextBirthday().toInt() == 0) {
                    people.add(birthday.name)
                }
            }
            if (people.isNotEmpty()) {
                val text = if (people.size == 1) {
                    "${people[0]} hat heute Geburtstag!"
                } else {
                    "${people.joinToString(", ")} haben heute Geburtstag!"
                }
                val notification = Notification.Builder(context, channelId)
                    .setContentTitle("Heute ist es soweit!")
                    .setContentText(text)
                    .setSmallIcon(R.drawable.cake_icon)
                    .setAutoCancel(true)
                    .setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .build()
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(1002, notification)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Thread {
            checkSoonBirthdays(context)
            checkTodayBirthdays(context)
        }.start()
    }

}