package com.example.jarvisdemo2.receivers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Intent
import android.media.AudioManager
import com.example.jarvisdemo2.utilities.Constants


class BluetoothReceiver : BroadcastReceiver() {

    private lateinit var audioManager: AudioManager

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val extras = intent.extras
            if (extras != null) { //Do something
                audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
                val action = intent.action
                // Toast.makeText(context, action, Toast.LENGTH_LONG).show()
                val state: Int
                if (action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED) {
                    state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.STATE_DISCONNECTED
                    )
                    // Toast.makeText(context, isBluetoothHeadsetConnected().toString(), Toast.LENGTH_SHORT).show()
                    /*if (isBluetoothHeadsetConnected()) setModeBluetooth(context)
                    else setModeNormal(context)*/
                    // Constants.makeToast(context, "\nAction = $action\nState = $state")
                    /*if (state == BluetoothAdapter.STATE_CONNECTED) {
                        Constants.makeToast(context, "connected")
                    } else if (state == BluetoothAdapter.STATE_DISCONNECTED) {
                        // Calling stopVoiceRecognition always returns false here
                        // as it should since the headset is no longer connected.
                        setModeNormal()
                        Constants.makeToast(context, "disconnected")
                    }*/
                } else  // audio
                {
                    state = intent.getIntExtra(
                        BluetoothHeadset.EXTRA_STATE,
                        BluetoothHeadset.STATE_AUDIO_DISCONNECTED
                    )
                    // Toast.makeText(context, isBluetoothHeadsetConnected().toString(), Toast.LENGTH_SHORT).show()
                    /*if (isBluetoothHeadsetConnected()) setModeBluetooth(context)
                    else setModeNormal(context)*/
                    // Constants.makeToast(context, "\nAction = $action\nState = $state")
                    /*if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                        Constants.makeToast(context, "connected")
                        setModeBluetooth()
                    } else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                        setModeNormal()
                        Constants.makeToast(context, "disconnected")
                    }*/
                }
            }
        } catch (e: Exception) {
            Constants.makeToast(context, e.message.toString())
        }
    }

    private fun isBluetoothHeadsetConnected(): Boolean {
        return (BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled && BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(
            BluetoothHeadset.HEADSET
        ) == BluetoothHeadset.STATE_CONNECTED)
    }
}