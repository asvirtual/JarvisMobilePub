package com.example.jarvisdemo2.services

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.example.jarvisdemo2.activities.MainActivity


class KeyEventService : AccessibilityService() {

    private var lastCommand: Int = 0
    private var lastCommandTime: Long = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "service is connected")
    }

    override fun onAccessibilityEvent(accessibilityEvent: AccessibilityEvent) {
        Log.d(TAG, "onAccessibiltyEvent $accessibilityEvent")
    }

    override fun onInterrupt() {}

    // here you can intercept the keyevent
    override fun onKeyEvent(event: KeyEvent): Boolean {
        return handleKeyEvent(event)
    }

    private fun handleKeyEvent(event: KeyEvent): Boolean {
        val action: Int = event.action
        val keyCode: Int = event.keyCode
        Log.i(TAG, "Event detected: $action")
        if (action == KeyEvent.ACTION_DOWN) {
            Log.i(TAG, "Pressed key: $keyCode")
            // KeyEvent.
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    Log.i(TAG, "Volume down pressed")
                    if (System.currentTimeMillis() - lastCommandTime <= 500 && lastCommand == KeyEvent.KEYCODE_VOLUME_UP ) {
                        val activityIntent = Intent(applicationContext, MainActivity::class.java)
                        activityIntent.putExtra("from_service_hotword", true)

                        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        activityIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        if (!isMyServiceRunning(SpeechRecognitionService::class.java)) {
                            stopService(Intent(applicationContext, PorcupineService::class.java))
                            startActivity(activityIntent)
                        }
                    }
                    lastCommandTime = System.currentTimeMillis()
                    lastCommand = keyCode
                    return super.onKeyEvent(event)
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    Log.i(TAG, "Volume up pressed")
                    if (System.currentTimeMillis() - lastCommandTime <= 500 && lastCommand == KeyEvent.KEYCODE_VOLUME_DOWN ) {
                        val activityIntent = Intent(applicationContext, MainActivity::class.java)
                        activityIntent.putExtra("from_service_hotword", true)

                        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        activityIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        if (!isMyServiceRunning(SpeechRecognitionService::class.java)) {
                            stopService(Intent(applicationContext, PorcupineService::class.java))
                            startActivity(activityIntent)
                        }
                    }
                    lastCommandTime = System.currentTimeMillis()
                    lastCommand = keyCode
                    return super.onKeyEvent(event)
                }
            }
        }
        return false
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager: ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    companion object {
        const val TAG = "KEY_EVENT"
    }
}