package com.example.jarvisdemo2.receivers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager
import com.example.jarvisdemo2.utilities.Constants
import com.example.jarvisdemo2.activities.MainActivity

class CallReceiver : BroadcastReceiver() {

    private lateinit var audioManager: AudioManager

    private fun isBluetoothHeadsetConnected(): Boolean {
        return (BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled && BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(
            BluetoothHeadset.HEADSET
        ) == BluetoothHeadset.STATE_CONNECTED)
    }

    override fun onReceive(context: Context?, intent: Intent) {

        audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                if (incomingNumber != null) {
                    context.getSharedPreferences("jarvis", Context.MODE_PRIVATE)?.edit()
                        ?.putString("incoming_number", incomingNumber)?.apply()

                    if (isBluetoothHeadsetConnected()) setModeBluetooth(context)
                    android.os.Handler().postDelayed({
                        val activityIntent = Intent(context, MainActivity::class.java)
                        activityIntent.putExtra("from_call", true)
                        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        activityIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        context.startActivity(activityIntent)
                    }, 1500)
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> { // after call ended
                val sharedPref = context.getSharedPreferences("jarvis", Context.MODE_PRIVATE)
                /*Constants.makeToast(context!!, "OFFHOOOK" + sharedPref?.getString("incoming_number", null) + sharedPref?.getInt("ringer_mode", 99999).toString())
                if (sharedPref?.getInt("ringer_mode", 99999) != 99999) {
                    val streams = listOf(AudioManager.STREAM_RING, AudioManager.STREAM_NOTIFICATION,
                        AudioManager.STREAM_DTMF, AudioManager.STREAM_ALARM, AudioManager.STREAM_ACCESSIBILITY,
                        AudioManager.STREAM_SYSTEM
                    )
                    for (stream in streams) {
                        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).adjustStreamVolume(
                            stream,
                            AudioManager.ADJUST_UNMUTE,
                            0
                        )
                    }
                    (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode = sharedPref!!.getInt("ringer_mode", 99999)
                }*/
                sharedPref?.edit()?.putString("incoming_number", null)?.apply()
                setModeNormal(context)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {} // after answering
        }
    }

    private fun setModeBluetooth(context: Context) {
        try {
            //sharedPrefs.edit().putBoolean(Constants.SPEAKER_ON, audioManager.isSpeakerphoneOn).apply()
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
            /*audioManager.isMicrophoneMute = true
            audioManager.isSpeakerphoneOn = false*/
        } catch (e: Exception) {
            Constants.makeToast(context, e.message.toString())
        }
    }

    private fun setModeNormal(context: Context) {
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
        } catch (e: Exception) {
            Constants.makeToast(context, e.message.toString())
        }
    }
}