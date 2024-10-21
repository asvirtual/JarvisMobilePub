package com.example.jarvisdemo2.services

import android.app.Notification
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.*


class NotificationService : NotificationListenerService() {

    var context: Context? = null
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var mTTS: TextToSpeech
    private var lastMessage: Long = Calendar.getInstance().timeInMillis

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        mTTS = TextToSpeech(applicationContext, TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                mTTS.language = Locale.ITALIAN
            }
        })
        mTTS.setSpeechRate(0.8F)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pack = sbn.packageName
        var ticker = ""
        if (sbn.notification.tickerText != null) {
            ticker = sbn.notification.tickerText.toString()
        }
        val extras = sbn.notification.extras
        var title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text").toString()
        val id1 = extras.getInt(Notification.EXTRA_SMALL_ICON)
        val id = sbn.notification.largeIcon
        val msgrcv = Intent("notification")
        msgrcv.putExtra("package", pack)
        msgrcv.putExtra("ticker", ticker)
        msgrcv.putExtra("title", title)
        msgrcv.putExtra("text", text)
        /*if (id != null) {
            val stream = ByteArrayOutputStream()
            id.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray: ByteArray = stream.toByteArray()
            msgrcv.putExtra("icon", byteArray)
        }*/

        // Toast.makeText(applicationContext, "$title $text $pack $ticker", Toast.LENGTH_LONG).show()
        val now = Calendar.getInstance().timeInMillis
        // Constants.makeToast(applicationContext, (now - lastMessage).toString())

        if (isBluetoothHeadsetConnected() && pack == "com.whatsapp" && title?.toLowerCase()?.contains("whatsapp") != true && getSharedPreferences("jarvis", Context.MODE_PRIVATE).getBoolean("read_notifications", false) && now - lastMessage > 500) {
            lastMessage = Calendar.getInstance().timeInMillis
            if (title!!.contains(":")) {
                val from = title.split(": ")[1]
                title = if (title.contains("(")) title.split("(")[0] else title.split(":")[0]
                mTTS.speak("Signore, nuovo messaggio sul gruppo $title da $from: $text", TextToSpeech.QUEUE_ADD, null, null)
            } else {
                    mTTS.speak("Signore, nuovo messaggio da $title: $text", TextToSpeech.QUEUE_ADD, null, null)
            }
            // mTTS.speak("Signore, ha ricevuto un messaggio da ")
        }


    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
    }

    private fun isBluetoothHeadsetConnected(): Boolean {
        return (bluetoothAdapter != null && bluetoothAdapter.isEnabled && bluetoothAdapter.getProfileConnectionState(
            BluetoothHeadset.HEADSET
        ) == BluetoothHeadset.STATE_CONNECTED)
    }
}
