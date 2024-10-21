package com.example.jarvisdemo2.receivers

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.jarvisdemo2.activities.AlarmActivity
import com.example.jarvisdemo2.activities.MainActivity
import com.example.jarvisdemo2.services.LocationTrackerService
import com.example.jarvisdemo2.services.PorcupineService
import com.example.jarvisdemo2.services.SpeechRecognitionService
import com.example.jarvisdemo2.utilities.Constants


class ReminderBroadcast: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val activityIntent = Intent(context!!, AlarmActivity::class.java)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        Log.i("ROUTINES", "ReminderBroadcast triggered")

        val locationTrackerIntent = Intent(context, LocationTrackerService::class.java)

        if (intent != null) {
            locationTrackerIntent.putExtras(intent)
            activityIntent.putExtras(intent)

            if (!intent.getStringArrayListExtra(Constants.ROUTINE_ALARM).isNullOrEmpty()) {
                val commandsList = intent.getStringArrayListExtra(Constants.ROUTINE_ALARM)!!
                val sharedPrefs = context.getSharedPreferences("jarvis", Context.MODE_PRIVATE)
                sharedPrefs.edit().putBoolean(Constants.HOTWORD_RECOGNITION_SERVICE_RUNNING, isMyServiceRunning(context, PorcupineService::class.java)).apply()

                Log.i("ROUTINES", "Routine alarm found, ${commandsList.joinToString(", ")}")
                val mainActivityIntent = Intent(context, MainActivity::class.java)
                mainActivityIntent.putExtra(Constants.ROUTINE_ALARM, commandsList)
                mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                while (isMyServiceRunning(context, SpeechRecognitionService::class.java)) Thread.sleep(100)
                context.startActivity(mainActivityIntent)
            }
            else if (intent.getStringExtra("tripStarted") == "true") context.startForegroundService(locationTrackerIntent)
            else context.startActivity(activityIntent)
        } else {
            context.startActivity(activityIntent)
        }
    }


    private fun isMyServiceRunning(context: Context?, serviceClass: Class<*>): Boolean {
        val manager: ActivityManager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}