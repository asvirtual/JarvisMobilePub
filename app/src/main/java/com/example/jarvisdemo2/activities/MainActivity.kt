package com.example.jarvisdemo2.activities

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.jarvisdemo2.R
import com.example.jarvisdemo2.receivers.KeepAliveBroadcast
import com.example.jarvisdemo2.services.LocationTrackerService
import com.example.jarvisdemo2.services.PorcupineService
import com.example.jarvisdemo2.services.SpeechRecognitionService
import com.example.jarvisdemo2.services.WhatsappService
import com.example.jarvisdemo2.utilities.Constants
import com.example.jarvisdemo2.utilities.Constants.makeToast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var alarmManager: AlarmManager
    private lateinit var mTTS: TextToSpeech

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    private fun hasRecordPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            1
        )
    }

    private fun requestRecordPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
            ),
            0
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val recordButton = findViewById<ToggleButton>(R.id.startButton)
        when (requestCode) {
            0 -> {
                if (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    recordButton.toggle()
                } else {
                    startService()
                }
            }
            1 -> {
                if (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "Location permissions denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startService() {
        ContextCompat.startForegroundService(this, Intent(this, PorcupineService::class.java))
    }

    private fun stopService() {
        stopService(Intent(this, PorcupineService::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        /*if (intent.extras?.getBoolean("from_service_hotword") == true || intent.extras?.getBoolean("from_service_speechRec") == true) {
            setTheme(R.style.Theme_Transparent)
        }*/
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        registerReceiver(mMessageReceiver, IntentFilter("closeActivity"))

        /*if (!isAccessibilityOn(this, WhatsappService::class.java)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            // startActivity(intent)
            makeToast(
                this,
                "Attiva i servizi di accessibilità per usare la funzione per mandare messaggi su whatsapp"
            )
        }*//* else {
            val url = "https://api.whatsapp.com/send?phone=+393493226141&text=Test di jarvis${Constants.WHATSAPP_MESSAGE_SUFFIX}"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }*/

        if (!hasLocationPermission()) requestLocationPermission()

        val window: Window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        val recordButton = findViewById<ToggleButton>(R.id.startButton)
        recordButton.setOnClickListener {
            if (recordButton.isChecked) {
                if (hasRecordPermission()) {
                    // startSpeechToText()
                    startService()
                } else {
                    requestRecordPermission()
                }
                setKeepAlive()
            } else {
                stopService()
                val intent = Intent(this, KeepAliveBroadcast::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    12345678,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
                alarmManager.cancel(pendingIntent)
            }
        }

        recordButton.isChecked = isMyServiceRunning(PorcupineService::class.java) || isMyServiceRunning(
            SpeechRecognitionService::class.java
        ) || intent.extras?.getBoolean("from_service") == true

        Log.i(
            "ROUTINES",
            "mainactivity alarm extra: " + intent.getStringArrayListExtra(Constants.ROUTINE_ALARM)
                ?.joinToString(
                    ", "
                ).toString()
        )
        if (!intent.getStringArrayListExtra(Constants.ROUTINE_ALARM).isNullOrEmpty()) {
            stopService()
            val serviceIntent = Intent(this, SpeechRecognitionService::class.java)
            serviceIntent.putExtra(
                Constants.ROUTINE_ALARM,
                intent.getStringArrayListExtra(Constants.ROUTINE_ALARM)
            )
            serviceIntent.putExtra(
                Constants.HOTWORD_RECOGNITION_SERVICE_RUNNING, intent.getBooleanExtra(
                    Constants.HOTWORD_RECOGNITION_SERVICE_RUNNING,
                    false
                )
            )
            startForegroundService(serviceIntent)
            finishAffinity()
            finish()
        } else if (intent.extras?.getBoolean("from_call") == true) {
            window.addFlags(WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY)
            stopService()
            Handler().postDelayed({
                val serviceIntent = Intent(
                    this,
                    SpeechRecognitionService::class.java
                )
                startForegroundService(
                    serviceIntent
                )
            }, 500)
            val number = getSharedPreferences("jarvis", Context.MODE_PRIVATE).getString(
                "incoming_number",
                ""
            )
            // if (number != "") mTTS.speak("Signore, chiamata in arrivo da $number", TextToSpeech.QUEUE_FLUSH, null)
        } else if (intent.extras?.getBoolean("from_service_hotword") == true) {
            val serviceIntent = Intent(
                this,
                SpeechRecognitionService::class.java
            )
            startForegroundService(
                serviceIntent
            )
        } else if (intent.extras?.getBoolean("from_service_speech_rec") == true) {
            val serviceIntent = Intent(
                this,
                PorcupineService::class.java
            )
            startForegroundService(
                serviceIntent
            )
        } else if (intent.extras?.getBoolean("keep_alive") == true) {
            if (!isMyServiceRunning(SpeechRecognitionService::class.java)) {
                stopService()
                Handler().postDelayed({ startService() }, 500)
            }
            setKeepAlive()
            finish()
        } else {
            // val locationTrackerIntent = Intent(this, LocationTrackerService::class.java)
            // locationTrackerIntent.putExtra("destination", "Via Carducci")
            /**
             * TODO: Solve problem with getting current location in LocationTrackerService
             */
            // startForegroundService(locationTrackerIntent)
            /*if (isBluetoothHeadsetConnected() && isMyServiceRunning(PorcupineService::class.java)) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, SpeechRecognitionService::class.java)
                )
            }*/
        }

        /**
         * TESTING AREA
         */

        /*try {
            val database: FirebaseDatabase = FirebaseDatabase.getInstance()
            val myRef: DatabaseReference = database.getReference("mobile")

            myRef.setValue("Hello, World!")
        } catch (e: Exception) {
            Log.e("FIREBASE", "Exception: ${e.printStackTrace()}, ${e.message}, $e")
        }*/

        /*val conversation = arrayListOf<TextMessage>()
        val smartReplyGenerator = SmartReply.getClient()

        val queue = Volley.newRequestQueue(applicationContext)
        val url = "https://libretranslate.com/translate"

        // Request a string response from the provided URL.
        queue.add(@SuppressLint("MissingPermission")
        object : JsonObjectRequest(
            Method.POST, url, null,
            { response ->
                val jsonRes = JSONObject(response.toString()).toMap()
                Log.i("TRANSLATION", jsonRes["translatedText"].toString())
            },
            { err ->
                Log.e("TRANSLATION", "error ${err.printStackTrace()}, $err")
            }) {

            /*override fun getBodyContentType(): String {
                return "application/json"
            }*/

            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["q"] = "Ciao come va?"
                params["source"] = "it"
                params["target"] = "en"

                return JSONObject(params as Map<*, *>).toString().toByteArray()
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        })*/

        /*arrayListOf(
            "Ciao",
            "Cosa fate di bello oggi?",
            "Bene... Buona giornata"
        ).forEach { message ->
            Translator.translate(
                context = this@MainActivity,
                text = message,
                src = "it",
                to = "en",
                callback = object: Translator.TranslationCallback {
                    override fun onSuccess(text: String) {
                        conversation.add(TextMessage.createForRemoteUser(text, System.currentTimeMillis(), "Utente"))
                        Log.i("SMARTREPLY", "message translated: $text")

                        smartReplyGenerator.suggestReplies(conversation)
                            .addOnSuccessListener { result ->
                                if (result.status == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                                    Log.i("SMARTREPLY", "Language not supported")
                                } else if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                                    Log.i("SMARTREPLY", "Suggestion: ${result.suggestions.last().text}")
                                    Translator.translate(
                                        context = this@MainActivity,
                                        text = result.suggestions.last().text,
                                        src = "en",
                                        to = "it",
                                        callback = object: Translator.TranslationCallback {
                                            override fun onSuccess(text: String) {
                                                Log.i("SMARTREPLY", "Suggestion translated: $text")
                                            }

                                            override fun onError(error: String) {
                                                Log.i("SMARTREPLY", "Error translating to it: $error")
                                            }

                                        }
                                    )
                                    /*result.suggestions.forEach { smartReplySuggestion ->

                                    }*/
                                    conversation.add(TextMessage.createForLocalUser(result.suggestions.last().text, System.currentTimeMillis()))
                                }
                            }
                            .addOnFailureListener {
                                Log.i("SMARTREPLY", "Error: ${it.message}")
                            }
                    }

                    override fun onError(error: String) {
                        Log.i("SMARTREPLY", "Error translating to it: $error")
                    }

                }
            )
            SystemClock.sleep(2000)
        }*/

        // conversation.add(TextMessage.createForRemoteUser("Come la va?", System.currentTimeMillis(), "Utente"))
        /*val message = "Buongiorno! Che fate di bello oggi?"
        Translator.translate(
            context = this,
            text = message,
            callback = object : Translator.TranslationCallback {
                override fun onSuccess(text: String) {
                    Log.i("SMARTREPLY", "Translated message: $text")
                    conversation.add(TextMessage.createForRemoteUser(text, System.currentTimeMillis(), "Utente"))

                    smartReplyGenerator.suggestReplies(conversation)
                        .addOnSuccessListener { result ->
                            if (result.status == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                                Log.i("SMART REPLY", "Language not supported")
                            } else if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                                result.suggestions.forEach { smartReplySuggestion ->
                                    Log.i("SMARTREPLY", "Suggestion: ${smartReplySuggestion.text}")
                                    Translator.translate(
                                        context = this@MainActivity,
                                        text = smartReplySuggestion.text,
                                        src = "en",
                                        to = "it",
                                        callback = object: Translator.TranslationCallback {
                                            override fun onSuccess(text: String) {
                                                Log.i("SMARTREPLY", "Suggestion translated: $text")
                                            }

                                            override fun onError(error: String) {
                                                Log.i("SMARTREPLY", "Error translating to it: $error")
                                            }

                                        }
                                    )
                                }
                            }
                        }
                        .addOnFailureListener {
                            Log.i("SMARTREPLY", "Error: ${it.message}")
                        }
                }

                override fun onError(error: String) {
                    Log.i("SMARTREPLY", "Error translating to en: $error")
                }

            })*/

        btnSettings.setOnClickListener {
            startActivity(Intent(this, RoutinesSettingsActivity::class.java))
            finish()
        }
    }

    private fun JSONObject.toMap(): Map<String, *> = keys().asSequence().associateWith { it ->
        when (val value = this[it])
        {
            is JSONArray -> {
                val map = (0 until value.length()).associate { Pair(it.toString(), value[it]) }
                JSONObject(map).toMap().values.toList()
            }
            is JSONObject -> value.toMap()
            JSONObject.NULL -> null
            else            -> value
        }
    }

    private fun setKeepAlive() {
        val alarmIntent = Intent(
            this,
            KeepAliveBroadcast::class.java
        )
        val date = Calendar.getInstance()
        /*date.add(Calendar.SECOND, 10)*/
        date.add(Calendar.MINUTE, 10)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            12345678,
            alarmIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            date.timeInMillis,
            pendingIntent
        )
    }

    private fun isBluetoothHeadsetConnected(): Boolean {
        return (BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled && BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(
            BluetoothHeadset.HEADSET
        ) == BluetoothHeadset.STATE_CONNECTED)
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

    private fun isAccessibilityOn(
        context: Context,
        clazz: Class<out AccessibilityService?>
    ): Boolean {
        var accessibilityEnabled = 0
        val service = context.packageName + "/" + clazz.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (ignored: Settings.SettingNotFoundException) {
        }
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue: String = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            colonSplitter.setString(settingValue)
            while (colonSplitter.hasNext()) {
                val accessibilityService: String = colonSplitter.next()
                if (accessibilityService.equals(service, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mMessageReceiver)
    }
}