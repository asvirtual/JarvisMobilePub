package com.example.jarvisdemo2.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.jarvisdemo2.activities.MainActivity

class KeepAliveBroadcast: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val activityIntent = Intent(context, MainActivity::class.java)
        activityIntent.putExtra("keep_alive", true)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        context?.startActivity(activityIntent)
    }
}