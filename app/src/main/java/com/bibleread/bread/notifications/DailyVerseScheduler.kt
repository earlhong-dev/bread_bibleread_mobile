package com.bibleread.bread.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Date
import androidx.core.app.NotificationCompat
import com.bibleread.bread.R
import com.bibleread.bread.data.BibleDatabase
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.TimeZone

object DailyVerseScheduler {
    private const val TAG = "DailyVerseScheduler"
    private const val PREFS_NAME = "daily_verse_scheduler"
    private const val REQUEST_CODE_MIDNIGHT = 1001
    private const val REQUEST_CODE_NOON = 1002

    fun scheduleDailyVerseAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val timeZone = TimeZone.getDefault()

        scheduleAlarm(context, alarmManager, 0, 0s, REQUEST_CODE_MIDNIGHT, timeZone)
        scheduleAlarm(context, alarmManager, 12, 0, REQUEST_CODE_NOON, timeZone)
    }

    fun rescheduleNextAlarm(context: Context, action: String?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val timeZone = TimeZone.getDefault()
        val requestCode = when (action) {
            "com.bibleread.bread.MIDNIGHT_VERSE" -> REQUEST_CODE_MIDNIGHT
            "com.bibleread.bread.NOON_VERSE" -> REQUEST_CODE_NOON
            else -> return
        }
        val targetHour = if (requestCode == REQUEST_CODE_MIDNIGHT) 0 else 12
        val targetMinute = 0
        scheduleAlarm(context, alarmManager, targetHour, targetMinute, requestCode, timeZone)
    }

    fun cancelDailyVerseAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelAlarm(context, alarmManager, REQUEST_CODE_MIDNIGHT)
        cancelAlarm(context, alarmManager, REQUEST_CODE_NOON)
    }

    fun getNextAlarmTime(now: Long, targetHour: Int, targetMinute: Int, timeZone: TimeZone): Long {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = now
        }

        val candidate = Calendar.getInstance(timeZone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (candidate.timeInMillis <= calendar.timeInMillis) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }

        return candidate.timeInMillis
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        targetHour: Int,
        targetMinute: Int,
        requestCode: Int,
        timeZone: TimeZone
    ) {
        val now = System.currentTimeMillis()
        val triggerAt = getNextAlarmTime(now, targetHour, targetMinute, timeZone)
        val intent = Intent(context, DailyVerseReceiver::class.java).apply {
            action = if (requestCode == REQUEST_CODE_MIDNIGHT) "com.bibleread.bread.MIDNIGHT_VERSE" else "com.bibleread.bread.NOON_VERSE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission not granted; falling back to allow-while-idle")
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to schedule exact alarm: ${e.message}")
        }

        Log.d(TAG, "Scheduled ${if (requestCode == REQUEST_CODE_MIDNIGHT) "midnight" else "noon"} at ${Date(triggerAt)}")
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, DailyVerseReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun showVerseNotification(context: Context) = runBlocking {
        val db = BibleDatabase.getInstance(context)
        val count = db.verseDao().getVerseCountFromBooks(getAllowedBooks())
        val verse = if (count > 0) {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            val todayString = dateFormat.format(java.util.Date())
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(todayString.toByteArray(Charsets.UTF_8))
            val seed = java.nio.ByteBuffer.wrap(digest).long
            val offset = (kotlin.math.abs(seed) % count).toInt()
            db.verseDao().getVerseFromBooksAtOffset(getAllowedBooks(), offset)
        } else {
            db.verseDao().getRandomVerse()
        }

        val bookReference = buildString {
            if (!verse?.book.isNullOrBlank()) append(verse!!.book)
            if (verse?.chapter != null) append(" ${verse!!.chapter}")
            if (verse?.verse != null) append(":${verse!!.verse}")
        }.trim()
        val title = if (bookReference.isNotBlank()) bookReference else "Daily Verse"
        val body = buildString {
            val verseText = verse?.text?.trim() ?: "Open the app to view today’s verse"
            append('"')
            append(verseText)
            append('"')
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return@runBlocking
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "daily_verse_channel",
                "Daily Verse",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily verse reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, com.bibleread.bread.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(context, "daily_verse_channel")
            .setSmallIcon(R.drawable.reminderbread)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1000, notification)
    }

    private fun getAllowedBooks(): List<String> = listOf(
        "Mga Awit", "Awit", "Psalms",
        "Mga Kawikaan", "Kawikaan", "Proverbs",
        "Isaias", "Isaiah",
        "Mga Taga Roma", "Mga Taga-Roma", "Roma", "Romans",
        "Mga Taga Filipos", "Mga Taga-Filipos", "Filipos", "Philippians",
        "Santiago", "James",
        "Mateo", "Matthew",
        "Juan", "John",
        "1 Mga Taga Corinto", "1 Mga Taga-Corinto", "1 Corinto", "1 Corinthians",
        "Mga Taga Efeso", "Mga Taga-Efeso", "Efeso", "Ephesians",
        "Mga Taga Colosas", "Mga Taga-Colosas", "Colosas", "Colossians",
        "Mga Hebreo", "Hebreo", "Hebrews",
        "1 Pedro", "1 Peter"
    )
}
