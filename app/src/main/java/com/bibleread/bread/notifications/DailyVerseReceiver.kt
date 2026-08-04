package com.bibleread.bread.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyVerseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("DailyVerseReceiver", "Received alarm: ${intent?.action}")
        CoroutineScope(Dispatchers.IO).launch {
            DailyVerseScheduler.showVerseNotification(context.applicationContext)
        }
        DailyVerseScheduler.rescheduleNextAlarm(context.applicationContext, intent?.action)
    }
}
