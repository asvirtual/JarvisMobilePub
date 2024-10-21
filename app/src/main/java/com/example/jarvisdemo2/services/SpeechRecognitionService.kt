package com.example.jarvisdemo2.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.hardware.camera2.CameraManager
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.provider.ContactsContract
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telecom.TelecomManager
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.ServerError
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.jarvisdemo2.R
import com.example.jarvisdemo2.activities.AlarmActivity
import com.example.jarvisdemo2.activities.MainActivity
import com.example.jarvisdemo2.database.Alarm
import com.example.jarvisdemo2.database.AlarmsDB
import com.example.jarvisdemo2.database.AlarmsDatabaseDAO
import com.example.jarvisdemo2.receivers.ReminderBroadcast
import com.example.jarvisdemo2.utilities.*
import com.example.jarvisdemo2.utilities.Constants.makeToast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Track
import org.json.JSONArray
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import java.util.*
// import androidx.test.platform.app.InstrumentationRegistry

class SpeechRecognitionService: Service() {

    /*private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent*/

    private var stopAfterRoutineExecution = false
    private var startAfterRoutineExecution = false
    private var mTTSReady = false

    private var startIntent: Intent? = null
    private var lastSCOAudioState: Int? = null
    private lateinit var mTTS: TextToSpeech
    private lateinit var audioManager: AudioManager
    private lateinit var alarmManager: AlarmManager
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var notificationManager: NotificationManager
    private lateinit var batteryManager: BatteryManager
    private lateinit var telecomManager: TelecomManager
    private var currentInteraction: String? = null
    private var currentInteractionData: Map<Any, Any>? = null
    private var lastRingerStatus = 999

    private lateinit var alarmsDBDao: AlarmsDatabaseDAO

    private var spotifyAppRemote: SpotifyAppRemote? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                "SpeechRecognition",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(notificationChannel)
        }
    }

    private fun startSpeechRecOrHotwordDetection(
        speechRecognizer: SpeechRecognizer,
        speechRecognizerIntent: Intent
    ) {
        if (currentInteraction == null) {
            // Toast.makeText(applicationContext, "startin hotworddd", Toast.LENGTH_SHORT).show()
            setModeNormal()
            restartHotwordDetection(speechRecognizer)
        }
        else {
            // Toast.makeText(applicationContext, "restarting speech red", Toast.LENGTH_SHORT).show()
            if (lastRingerStatus != 999) {
                lastRingerStatus = audioManager.ringerMode
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
            setModeNormal()
            speechRecognizer.startListening(speechRecognizerIntent)
        }
    }

    @RequiresApi(30)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("ROUTINES", "onStartCommand")
        startIntent = intent

        try {
            audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            batteryManager = applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            telecomManager = applicationContext.getSystemService(TELECOM_SERVICE) as TelecomManager
            sharedPrefs = applicationContext.getSharedPreferences("jarvis", Context.MODE_PRIVATE)
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            mTTS = TextToSpeech(applicationContext) { status ->
                if (status != TextToSpeech.ERROR) {
                    mTTS.language = Locale.ITALIAN
                    mTTS.setSpeechRate(0.8F)
                    mTTSReady = true
                }
            }

            if (isBluetoothHeadsetConnected()) {
                lastRingerStatus = audioManager.ringerMode
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }

            if (JarvisMediaPlayer.isPlaying() == true) JarvisMediaPlayer.stopAudio()
            if (!(AlarmActivity() as Activity).isDestroyed && !(AlarmActivity() as Activity).isFinishing) {
            }
            if (sharedPrefs.getBoolean(Constants.INTENT_EXTRA_ALARM_RUNNING, false)) {
                val alarmTitle = sharedPrefs.getString(Constants.INTENT_EXTRA_ALARM_RUNNING, null)
                if (!alarmTitle.isNullOrEmpty()) speak("Signore, è suonata la sveglia per $alarmTitle")
                sharedPrefs.edit().putBoolean(Constants.INTENT_EXTRA_ALARM_RUNNING, false).apply()
                sharedPrefs.edit().putString(Constants.INTENT_EXTRA_ALARM_RUNNING, null).apply()
                sendBroadcast(Intent("finish_alarm"))
            } else if (sharedPrefs.getString("incoming_number", null) != null) {
                stopService(Intent(applicationContext, PorcupineService::class.java))
                /*speak(
                    "Signore, chiamata in arrivo da ${
                        sharedPrefs.getString(
                            "incoming_number",
                            null
                        )
                    }"
                )*/
            } else if (startIntent?.extras?.get("trip") != null) {
                speak("Signore, dovrebbe cominciare a prepararsi per andare a ${startIntent?.getStringExtra("destination")}")
            }

            createNotificationChannel()

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                0
            )
            val notification =
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Jarvis")
                    .setContentText("In ascolto")
                    .setSmallIcon(R.drawable.ic_mic)
                    .setContentIntent(pendingIntent)
                    .build()

            startForeground(123456, notification, FOREGROUND_SERVICE_TYPE_MICROPHONE)

            Toast.makeText(applicationContext, "Thread started", Toast.LENGTH_SHORT).show()
            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            /*speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.ITALIAN.toString())
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.ITALIAN.toString())
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, Locale.ITALIAN.toString())*/
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)

            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(bundle: Bundle?) {
                    // Toast.makeText(applicationContext, "onReadyForSpeech", Toast.LENGTH_SHORT).show()
                }

                override fun onBeginningOfSpeech() {
                    // Toast.makeText(applicationContext, "onBeginningOfSpeech", Toast.LENGTH_SHORT).show()
                }

                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(bytes: ByteArray?) {
                    // Toast.makeText(applicationContext, "onBufferReceived", Toast.LENGTH_SHORT).show()
                }

                override fun onEndOfSpeech() {
                    // Toast.makeText(applicationContext, "onEndOfSpeech", Toast.LENGTH_SHORT).show()
                }

                override fun onError(errorCode: Int) {
                    // if (errorCode == SpeechRecognizer.ERROR_NO_MATCH) {
                    // }
                    if (lastRingerStatus != 999) audioManager.ringerMode = lastRingerStatus
                    startSpeechRecOrHotwordDetection(speechRecognizer, speechRecognizerIntent)
                    Toast.makeText(applicationContext, getErrorText(errorCode), Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onResults(bundle: Bundle) {
                    if (lastRingerStatus != 999) audioManager.ringerMode = lastRingerStatus
                    val result = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (result != null) {
                        Toast.makeText(applicationContext, result[0], Toast.LENGTH_SHORT).show()
                        witAiRequest(result[0], speechRecognizer = speechRecognizer, speechRecognizerIntent = speechRecognizerIntent)
                    } else {
                        Toast.makeText(applicationContext, "No text", Toast.LENGTH_SHORT).show()
                        startSpeechRecOrHotwordDetection(speechRecognizer, speechRecognizerIntent)
                    }
                }

                override fun onPartialResults(results: Bundle) {
//                  val matches = results.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
//                  var text = ""
//                  for (result in matches!!) text += result + "\n"
//                  Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
//                  Toast.makeText(applicationContext, "onPartialResults", Toast.LENGTH_SHORT).show()
                }

                override fun onEvent(i: Int, bundle: Bundle?) {
                    // Toast.makeText(applicationContext, "onEvent $i", Toast.LENGTH_SHORT).show()
                }

            })

            Log.i("ROUTINES", "SpeechRec intent: $startIntent, extras: ${startIntent?.extras}")
            Log.i("ROUTINES", "SpeechRec routine alarm extra: ${startIntent?.getStringArrayListExtra(Constants.ROUTINE_ALARM)?.joinToString(", ")}")
            if (!startIntent?.getStringArrayListExtra(Constants.ROUTINE_ALARM).isNullOrEmpty()) {
                Log.i("ROUTINES", "Handling routine date actions")
                handleRoutineActions(startIntent?.getStringArrayListExtra(Constants.ROUTINE_ALARM)!!.toMutableList(), ActionResolver(mapOf("" to "")), speechRecognizer = speechRecognizer, speechRecognizerIntent = speechRecognizerIntent)
                startIntent?.putExtra(Constants.ROUTINE_ALARM, arrayListOf<String>())
            } else if (currentInteraction == null) {
                Log.i("ROUTINES", "Currentinteraction is null and no routine alarm extra are found")
                if (isBluetoothHeadsetConnected()) {
                    setModeBluetooth(applicationContext)
                    registerReceiver(object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent) {
                            val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                            /* Constants.makeToast(
                                applicationContext,
                                "Audio SCO state: $state, ${AudioManager.SCO_AUDIO_STATE_CONNECTED}"
                            ) */
                            if (lastSCOAudioState != null && lastSCOAudioState == 0 && state == 0) {
                                restartHotwordDetection(speechRecognizer)
                            } else if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                                unregisterReceiver(this)
                                speechRecognizer.startListening(speechRecognizerIntent)
                            }
                            lastSCOAudioState = state
                        }
                    }, IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED))
                } else {
                    setModeNormal()
                    speechRecognizer.startListening(speechRecognizerIntent)
                }
            } else {
                currentInteraction = null
            }

            sendBroadcast(Intent("closeActivity"))
        } catch (e: Exception) {
            Log.e("ROUTINES", "$e, ${e.message}, ${e.stackTrace}")
            makeToast(applicationContext, e.message.toString())
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresApi(30)
    override fun onCreate() {
        Log.i("ROUTINES", "OnCreate")
        super.onCreate()

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        batteryManager = applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        telecomManager = applicationContext.getSystemService(TELECOM_SERVICE) as TelecomManager
        sharedPrefs = applicationContext.getSharedPreferences("jarvis", Context.MODE_PRIVATE)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        alarmsDBDao = AlarmsDB.getInstance(applicationContext).alarmsDatabaseDao()

        mTTS = TextToSpeech(applicationContext) { status ->
            if (status != TextToSpeech.ERROR) {
                mTTS.language = Locale.ITALIAN
                mTTS.setSpeechRate(0.8F)
                mTTSReady = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun witAiRequest(text: String, fromRoutine: Boolean = false, speechRecognizer: SpeechRecognizer, speechRecognizerIntent: Intent) {
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(applicationContext)
        val url = "https://api.wit.ai/message?v=20210730&q=${text}"

        // Request a string response from the provided URL.
        queue.add(@SuppressLint("MissingPermission")
        object : JsonObjectRequest(
            Method.GET, url, null,
            { response ->
                val jsonRes = JSONObject(response.toString()).toMap()
                val actionResolver = ActionResolver(jsonRes as Map<String, Any>)

                executeIntents(actionResolver, jsonRes["text"].toString(), text, fromRoutine = fromRoutine, speechRecognizer = speechRecognizer, speechRecognizerIntent = speechRecognizerIntent)
            },
            { err ->
                Toast.makeText(
                    applicationContext,
                    "Error $err occured",
                    Toast.LENGTH_SHORT
                ).show()
                startSpeechRecOrHotwordDetection(
                    speechRecognizer,
                    speechRecognizerIntent
                )
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer"
                return headers
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    private fun executeIntents(actionResolver: ActionResolver, text: String, originalText: String, fromRoutine: Boolean = false, speechRecognizer: SpeechRecognizer, speechRecognizerIntent: Intent) {

        when (currentInteraction) {
            Constants.INTERACTION_SET_ALARM -> {
                val notificationDate =
                    actionResolver.getEntity("datetime")?.split(
                        "+"
                    )?.get(0)
                if (notificationDate != null) {
                    currentInteraction = null
                    setAlarm(notificationDate)
                    speak(getAlarmTime(notificationDate))
                } else if (text.toLowerCase().contains(Regex("(annulla|cancella)"))) {
                    speak("Sveglia cancellata, signore")
                    currentInteraction = null
                } else {
                    speak("Non ho capito, a che ora vuole essere svegliato?")
                }
            }
            Constants.INTERACTION_CONFIRM_REMINDER -> {
                if (text.toLowerCase().contains(
                        Regex(
                            "(si|sì|conferm|ok|va bene|sã¬|se)"
                        )
                    )
                ) {
                    pushAppointmentsToCalendar(
                        title = currentInteractionData?.get("title")
                            ?.toString(),
                        addInfo = currentInteractionData?.get("addInfo")
                            ?.toString(),
                        place = currentInteractionData?.get("place")
                            ?.toString(),
                        status = currentInteractionData?.get("status")
                            ?.toString()?.toInt(),
                        startDate = currentInteractionData?.get("startDate")
                            ?.toString()?.toLong(),
                        needReminder = currentInteractionData?.get("needReminder")
                            ?.toString()?.toBoolean(),
                        needMailService = currentInteractionData?.get("needMailService")
                            ?.toString()?.toBoolean(),
                    )
                    speak("Evento creato")
                } else speak("Evento cancellato")
                currentInteraction = null
                currentInteractionData = null
            }
            Constants.INTERACTION_CONFIRM_CALL -> {
                if (text.contains(Regex("(si|sì|conferm|ok|va bene|sã¬|se)"))) startCall(
                    currentInteractionData?.get(
                        "contact"
                    ).toString()
                )
                else speak("Chiamata annullata")
                currentInteraction = null
            }
            Constants.INTERACTION_SET_TRIP_REMINDER -> {
                if (text.contains(Regex("(si|sì|conferm|ok|va bene|sã¬|se)"))) {
                    val format = SimpleDateFormat("yyyy-MM-dd't'HH:mm:ss")
                    val date: Date = format.parse(currentInteractionData!!["tripReminderDate"] as String)
                    pushAppointmentsToCalendar(
                        title = "Percorso da ${currentInteractionData!!["origin"]} a ${currentInteractionData!!["destination"]}",
                        addInfo = currentInteractionData!!["url"] as String?,
                        place = currentInteractionData!!["destination"] as String?,
                        status = 0,
                        startDate = date.toInstant().toEpochMilli(),
                        needReminder = true,
                        needMailService = false
                    )

                    val alarmDate: Calendar = Calendar.getInstance()
                    alarmDate.timeInMillis = date.toInstant().toEpochMilli()
                    alarmDate.add(Calendar.MINUTE, -30)
                    setAlarmLongDate(alarmDate.timeInMillis, intentExtras=mapOf(
                        "trip" to "true",
                        "destination" to currentInteractionData!!["destination"] as String
                    ))

                    setAlarm(currentInteractionData!!["tripReminderDate"] as String, intentExtras=mapOf(
                        "tripStarted" to "true",
                        "desination" to currentInteractionData!!["destination"] as String
                    ))
                } else {
                    speak("Come vuole, signore")
                }
                currentInteraction = null
                currentInteractionData = null
            }
            else -> {
                when (actionResolver.resolve()) {
                    Constants.PHONE_CALL -> {
                        var contact = actionResolver.getEntity("contact")
                        if (contact == null) {
                            try {
                                // Constants.makeToast(applicationContext, text.split("chiama ").toString())
                                contact = text.toLowerCase().split("chiama ")[1]
                            } catch (e: Exception) {
                                // Constants.makeToast(applicationContext, e.message.toString())
                                e.printStackTrace()
                            }
                        }
                        // Constants.makeToast(applicationContext, contact.toString())
                        if (contact != null) {
                            currentInteractionData = mapOf("contact" to contact) as Map<Any, Any>
                            currentInteraction = Constants.INTERACTION_CONFIRM_CALL
                            speak("Signore, chiamo $contact. Conferma?")
                        } else speak("Non ho capito")
                    }
                    Constants.VOLUME_DOWN -> audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Constants.VOLUME_UP -> audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Constants.VOLUME_MUTE -> audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Constants.VOLUME_UNMUTE -> audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Constants.VOICE_CALL_DOWN -> audioManager.adjustStreamVolume(
                        AudioManager.STREAM_VOICE_CALL,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Constants.VOICE_CALL_UP -> audioManager.adjustStreamVolume(
                        AudioManager.STREAM_VOICE_CALL,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Constants.VOICE_CALL_MUTE -> {
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_VOICE_CALL,
                            AudioManager.ADJUST_MUTE,
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                    Constants.VOICE_CALL_UNMUTE -> {
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_VOICE_CALL,
                            AudioManager.ADJUST_UNMUTE,
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                    Constants.RINGER_VIBRATE -> {
                        audioManager.ringerMode =
                            AudioManager.RINGER_MODE_VIBRATE
                        speak("Telefono in vibrazione")
                    }
                    Constants.RINGER_SILENT -> {
                        try {
                            audioManager.ringerMode =
                                AudioManager.RINGER_MODE_SILENT
                            speak("Telefono in silenzioso")
                        } catch (e: Exception) {
                            Toast.makeText(
                                applicationContext,
                                e.message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                            speak("Signore, non ho i permessi per farlo")
                            val intent =
                                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    }
                    Constants.RINGER_NORMAL -> {
                        audioManager.ringerMode =
                            AudioManager.RINGER_MODE_NORMAL
                        speak("Suoneria attivata")
                    }
                    Constants.TELL_TIME -> speak(
                        "Signore, sono le ${
                            Calendar.getInstance().get(
                                Calendar.HOUR_OF_DAY
                            )
                        } e ${
                            Calendar.getInstance().get(Calendar.MINUTE)
                        }"
                    )
                    Constants.TELL_DATE -> speak(
                        "Signore, oggi è il ${
                            Calendar.getInstance().get(
                                Calendar.DAY_OF_MONTH
                            )
                        } ${
                            Constants.MONTHS[Calendar.getInstance()
                                .get(Calendar.MONTH)]
                        } ${
                            Calendar.getInstance().get(
                                Calendar.YEAR
                            )
                        }"
                    )
                    Constants.SET_TIMER -> {
                        val notificationDate =
                            actionResolver.getEntity("datetime")?.split(
                                "+"
                            )?.get(0)
                        var alarmTitle = actionResolver.getEntity("agenda_entry")
                        if (alarmTitle != null && (alarmTitle.contains("sveglia") || alarmTitle.contains("timer"))) alarmTitle = null
                        if (!notificationDate.isNullOrEmpty()) {
                            try {
                                setAlarm(notificationDate, alarmTitle)
                                speak(getAlarmTime(notificationDate) + if (alarmTitle != null) " con titolo $alarmTitle" else "")
                            } catch (e: ParseException) {
                                Toast.makeText(
                                    applicationContext,
                                    e.message.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            speak("Quando devo impostare la sveglia, signore?")
                            currentInteraction = Constants.INTERACTION_SET_ALARM
                        }
                    }
                    Constants.DELETE_TIMER -> {
                        val title = try {
                            actionResolver.getEntities("agenda_entry")?.last()
                                ?.get("value") as String?
                        } catch (e: Exception) {
                            Log.e("ALARMS", "Error retrieving alarm title: ${e.message}")
                            null
                        }
                        deleteAlarm (
                            title = title,
                            strDate = actionResolver.getEntity("datetime")?.split("+")?.get(0)
                        )
                    }
                    Constants.END_CALL -> telecomManager.endCall() // if (telecomManager.isInCall)
                    Constants.TRIP -> {
                        var origin: String? = null
                        var destination: String? = null
                        val locations =
                            actionResolver.getEntities("location")
                        if (locations?.size == 2) {
                            origin = locations[0]["value"] as String?
                            destination = locations[1]["value"] as String?
                            if (destination?.contains(Regex("(piedi|macchina)")) == true) {
                                destination = origin
                                origin = ""
                            }
                        } else if (locations?.size == 1) {
                            origin = ""
                            destination = locations[0]["value"] as String?
                        }
                        if (origin == null || destination == null) {
                            speak("non ho capito")
                        } else {
                            Toast.makeText(
                                applicationContext,
                                "from: $origin to $destination",
                                Toast.LENGTH_LONG
                            ).show()
                            //Toast.makeText(context, origin, Toast.LENGTH_LONG).show()

                            /**
                             * building the Uri and starting the activity
                             */
                            val builder = Uri.Builder()
                            builder.scheme("https")
                                .authority("www.google.com")
                                .appendPath("maps")
                                .appendPath("dir")
                                .appendPath("")
                                .appendQueryParameter("api", "1")
                                .appendQueryParameter("origin", origin)
                                .appendQueryParameter(
                                    "destination",
                                    destination
                                )
                            if (text.contains("a piedi")) {
                                builder.appendQueryParameter(
                                    "dir_action",
                                    "navigate"
                                )
                                builder.appendQueryParameter(
                                    "travelmode",
                                    "walking"
                                )
                            } else if (text.contains(
                                    "macchina"
                                )
                            ) {
                                builder.appendQueryParameter(
                                    "dir_action",
                                    "navigate"
                                )
                                builder.appendQueryParameter(
                                    "travelmode",
                                    "driving"
                                )

                            } else {
                                builder.appendQueryParameter(
                                    "travelmode",
                                    "transit"
                                )
                            }

                            //https://www.google.com/maps/dir/?api=1&origin=Google+Pyrmont+NSW&destination=QVB&destination_place_id=ChIJISz8NjyuEmsRFTQ9Iw7Ear8&travelmode=transit
                            //"https://www.google.com/maps/dir/?api=1&origin=Google+Pyrmont+NSW&destination=QVB&travelmode=transit"

                            val url = builder.build().toString()
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                                )
                                intent.setPackage("com.google.android.apps.maps")
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                                val date = actionResolver.getEntity("datetime")?.split("+")?.get(0)
                                if (date == null) {
                                    speak("Certo signore, apro google maps")
                                    startActivity(intent)
                                }
                                else {
                                    speak("Certo signore, apro google maps. Vuole anche aggiungere un promemoria con le indicazioni per il percorso?")
                                    currentInteraction = Constants.INTERACTION_SET_TRIP_REMINDER
                                    currentInteractionData = mapOf(
                                        "tripReminderDate" to date,
                                        "origin" to origin,
                                        "destination" to destination,
                                        "url" to url
                                    )
                                }
                                /*val activities: List<ResolveInfo> =
                                    packageManager.queryIntentActivities(
                                        intent,
                                        0
                                    )
                                val isIntentSafe: Boolean =
                                    activities.isNotEmpty()
                                if (isIntentSafe) {
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(
                                        applicationContext,
                                        "Intent not safe",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }*/
                            } catch (e: Exception) {
                                Toast.makeText(
                                    applicationContext,
                                    "error:$e",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    Constants.MATH_PROBLEM -> {
                        var query = originalText.toLowerCase()
                            .replace(Regex("grad[a-z]*"), "deg")
                            .replace(Regex("adiant[a-z]*"), "radians")
                            .replace(Regex("logaritmo naturale"), "ln")
                            .replace(Regex(" di "), " ")
                            .replace(Regex("radice quadrata"), "sqrt")
                            .replace("al quadrato", "** 2")
                            .replace(Regex("alla"), "**")
                            .replace("fratto", "/")
                            .replace("diviso", "/")
                            .replace("coseno", "cos")
                            .replace("seno", "sin")
                            .replace("tangente", "tan")
                            .replace("logaritmo", "log")
                            .replace("per", "*")
                            .replace("+", "plus")
                            .replace("più", "plus")
                            .replace("per", "*")
                            .replace("meno", "-")
                            .replace("pi greco", "π")

                        mapOf(
                            "prim" to  1,
                            "second" to 2,
                            "terz" to 3,
                            "quart" to 4,
                            "quint" to 5,
                            "sest" to 6,
                            "settim" to 7,
                            "ottav" to 8,
                            "non" to 9,
                            "decim" to 10,
                            "undicesim" to 11,
                            "dodicesim" to 12,
                            "tredcesim" to 13,
                            "quattordicesim" to 14,
                            "quindicesim" to 15
                        ).forEach { (word, number) ->
                            query = query.replace(Regex("${word}[a-z]"), number.toString())
                        }

                        for (substring in listOf("quanto fa ", "quanto vale ", "quant[a-z]* (e|è) ", "calcola ", "la", "il")) {
                            query = query.replace(Regex(substring), "")
                        }
                        query = query.replace(" ", "+")

                        makeToast(applicationContext, "Math problem: $query, original text: $originalText")

                        fun getResult(text: String) {
                            val url = "https://api.wolframalpha.com/v1/spoken?i=${text}%3F&appid="
                            val queue = Volley.newRequestQueue(
                                applicationContext
                            )
                            queue.add(StringRequest(
                                Request.Method.GET, url,
                                { response ->
                                    Translator.translate(
                                        text = response,
                                        src = "en",
                                        to = "it",
                                        context = applicationContext,
                                        callback = object :
                                            Translator.TranslationCallback {
                                            override fun onError(
                                                error: String
                                            ) {
                                                makeToast(
                                                    applicationContext,
                                                    "$error from english to italian, speaking english res"
                                                )
                                                try {
                                                    speak(response.toLowerCase().split("è ")[1].replace("di ", "").replace(".", ","))
                                                } catch (e: Exception) {
                                                    speak(response)
                                                }
                                            }

                                            override fun onSuccess(
                                                text: String
                                            ) {
                                                try {
                                                    speak(text.toLowerCase().split("è ")[1].replace("di ", "").replace(".", ","))
                                                } catch (e: Exception) {
                                                    speak(text)
                                                }
                                            }
                                        })
                                },
                                { error ->
                                    makeToast(applicationContext, error.message.toString())
                                    speak("Non sono riuscito a calcolare il risultato")
                                }
                            ))
                        }

                        Translator.translate(
                            text = query,
                            src = "it",
                            to = "en",
                            context = applicationContext,
                            callback = object :
                                Translator.TranslationCallback {
                                override fun onError(
                                    error: String
                                ) {
                                    getResult(query)
                                }

                                override fun onSuccess(
                                    text: String
                                ) {
                                    getResult(text)
                                }
                            })
                    }
                    Constants.GENERAL_QUESTION -> {
                        Translator.translate(
                            text = text,
                            context = applicationContext,
                            callback = object :
                                Translator.TranslationCallback {
                                override fun onError(error: String) {
                                    makeToast(
                                        applicationContext,
                                        error
                                    )
                                }

                                override fun onSuccess(text: String) {
                                    val url =
                                        "https://api.wolframalpha.com/v1/spoken?i=${text}%3F&appid="
                                    val queue = Volley.newRequestQueue(
                                        applicationContext
                                    )
                                    queue.add(StringRequest(
                                        Request.Method.GET, url,
                                        { response ->
                                            Translator.translate(
                                                text = response,
                                                src = "en",
                                                to = "it",
                                                context = applicationContext,
                                                callback = object :
                                                    Translator.TranslationCallback {
                                                    override fun onError(
                                                        error: String
                                                    ) {
                                                        makeToast(
                                                            applicationContext,
                                                            error
                                                        )
                                                    }

                                                    override fun onSuccess(
                                                        text: String
                                                    ) {
                                                        speak(text)
                                                    }
                                                })
                                        },
                                        { error ->
                                            makeToast(
                                                applicationContext,
                                                error.message.toString()
                                            )
                                        }
                                    ))
                                }
                            })
                    }
                    Constants.SET_REMINDER -> {
                        val strDate =
                            actionResolver.getEntity("datetime")?.split(
                                "+"
                            )
                                ?.get(0)
                                ?.toLowerCase()
                        val format =
                            SimpleDateFormat("yyyy-MM-dd't'HH:mm:ss")
                        val date: Date = format.parse(strDate)
                        val calendarDate = Calendar.getInstance()
                        calendarDate.timeInMillis =
                            date.toInstant().toEpochMilli()
                        val description =
                            actionResolver.getEntity("agenda_entry")
                        currentInteraction =
                            Constants.INTERACTION_CONFIRM_REMINDER
                        currentInteractionData = mapOf(
                            "title" to description,
                            "addInfo" to "",
                            "place" to sharedPrefs.getString(
                                "city",
                                "Trieste"
                            ),
                            "status" to 0,
                            "startDate" to date.toInstant().toEpochMilli(),
                            "needReminder" to true,
                            "needMailService" to false
                        ) as Map<Any, Any>
                        speak(
                            "Signore, creo un evento con descrizione $description il ${
                                calendarDate.get(
                                    Calendar.DAY_OF_MONTH
                                )
                            } ${
                                Constants.MONTHS[calendarDate.get(
                                    Calendar.MONTH
                                )]
                            } ${
                                calendarDate.get(
                                    Calendar.YEAR
                                )
                            } alle ${calendarDate.get(Calendar.HOUR)} e ${
                                calendarDate.get(
                                    Calendar.MINUTE
                                )
                            }. Conferma?"
                        )
                    }
                    Constants.GET_WEATHER -> {
                        getWeather(actionResolver)
                    }
                    Constants.NO_RESULT -> {
                        val text = text.toLowerCase()
                        val routineTriggered = actionResolver.resolveVoiceRoutine(applicationContext, text)
                        val commandsLeft = sharedPrefs.getString(Constants.ROUTINE_COMMANDS_LEFT, null)?.split(Constants.ROUTINE_COMMANDS_SEPARATOR)?.toMutableList()
                        if (routineTriggered != null && !fromRoutine && routineTriggered.commands != null) {
                            handleRoutineActions(
                                routineTriggered.commands!!,
                                actionResolver,
                                speechRecognizer = speechRecognizer,
                                speechRecognizerIntent = speechRecognizerIntent,
                                voiceTriggered = true
                            )
                            return
                        }
                        /*else if (fromRoutine && !commandsLeft.isNullOrEmpty()) {
                            handleRoutineActions(
                                commandsLeft,
                                actionResolver,
                                speechRecognizer = speechRecognizer,
                                speechRecognizerIntent = speechRecognizerIntent,
                                voiceTriggered = true
                            )
                            // handlingRoutines = true
                            return
                        }*/
                        else if (text.contains("bluetooth")) {
                            if (text.contains(Regex("(accendi|attiva|metti)")) || text.contains(
                                    Regex(
                                        "(spegni|disattiva|togli)"
                                    )
                                )
                            ) {
                                try {
                                    switchBluetooth()
                                } catch (e: Exception) {
                                    makeToast(
                                        applicationContext,
                                        e.message.toString()
                                    )
                                }
                            } else if (text.contains("connetti")) {
                                bluetoothDiscoverable()
                            }
                        } else if (text.contains("rispondi") && sharedPrefs.getString(
                                "incoming_number",
                                null
                            ) != null
                        ) {
                            telecomManager.acceptRingingCall()
                        } else if (text.contains(Regex("(ci sei|sei online|sei sveglio|sei attivo)"))) {
                            speak("Eccomi, signore")
                            if (audioManager.getStreamVolume(
                                    AudioManager.STREAM_MUSIC
                                ) == 0
                            ) {
                                val volume = audioManager.getStreamVolume(
                                    AudioManager.STREAM_MUSIC
                                )
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    audioManager.getStreamMaxVolume(
                                        AudioManager.STREAM_MUSIC
                                    ) / 2,
                                    0
                                )
                                speak("Sono mutato, signore")
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    volume,
                                    0
                                )
                            }
                        } else if (text.contains(Regex("(fatti sentire|non ti sento|parla più forte)"))) {
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                audioManager.getStreamMaxVolume(
                                    AudioManager.STREAM_MUSIC
                                ) / 2,
                                AudioManager.FLAG_SHOW_UI
                            )
                            speak("Eccomi, Signore")
                        } else if (text.contains("non disturbare")) {
                            if (text.contains(Regex("(disattiva|togli)"))) {
                                notificationManager.setInterruptionFilter(
                                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                                )
                                speak("Modalità non disturbare disattivata")
                            } else {
                                speak("Modalità non disturbare attivata")
                                notificationManager.setInterruptionFilter(
                                    NotificationManager.INTERRUPTION_FILTER_NONE
                                )
                            }
                        } else if (text.contains("paus") && audioManager.isMusicActive) {
                            val event = KeyEvent(
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_MEDIA_PAUSE
                            )
                            audioManager.dispatchMediaKeyEvent(event)
                        } else if (text.contains("riprendi")) {
                            val event = KeyEvent(
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_MEDIA_PLAY
                            )
                            audioManager.dispatchMediaKeyEvent(event)
                        } else if (text.contains(Regex("(skip|schip|salta)"))) {
                            val event = KeyEvent(
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_MEDIA_NEXT
                            )
                            audioManager.dispatchMediaKeyEvent(event)
                        } else if (text.contains(Regex("(precedente|scorsa)"))) {
                            val event = KeyEvent(
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_MEDIA_PREVIOUS
                            )
                            audioManager.dispatchMediaKeyEvent(event)
                        } else if (text.contains(Regex("(mutani|mutami)"))) {
                            val event = KeyEvent(
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_MUTE
                            )
                            audioManager.dispatchMediaKeyEvent(event)
                        } else if (text.contains(Regex("(lettur|legg)")) && text.contains(
                                "notific"
                            )
                        ) {
                            sharedPrefs.edit().putBoolean(
                                "read_notifications", !text.contains(
                                    Regex(
                                        "(disattiva|togli|non)"
                                    )
                                )
                            ).apply()
                            speak("Impostazioni aggiornate, signore")
                        } else if (text.contains("batteria")) {
                            speak(
                                "Signore, la batteria è al ${
                                    batteryManager.getIntProperty(
                                        BatteryManager.BATTERY_PROPERTY_CAPACITY
                                    )
                                } percento"
                            )
                        } else if (text.contains(Regex("(torcia|luce|vedo)"))) {
                            val camManager: CameraManager =
                                getSystemService(
                                    CAMERA_SERVICE
                                ) as CameraManager
                            val cameraId: String?
                            try {
                                cameraId = camManager.cameraIdList[0]
                                camManager.setTorchMode(
                                    cameraId,
                                    text.contains(Regex("(accendi|apri|fa[a-z]*|avvia|non vedo)"))
                                )
                            } catch (e: Exception) {
                                makeToast(
                                    applicationContext,
                                    e.message.toString()
                                )
                            }
                        } else if (text.contains(Regex("ripet[a-z]*"))) {
                            if (text.contains(Regex("(rallent[a-z]*|piano)"))) mTTS.setSpeechRate(0.5F)
                            sharedPrefs.getString("lastSaid", null)?.let { speak(it) }
                            mTTS.setSpeechRate(0.8F)
                        } else if (text.contains(Regex("riproduci"))) {
                            var query = text.split("riproduci ")[1]

                            val type =
                                if (text.contains("playlist")) {
                                    query = query.split("playlist ")[1]
                                    "playlist"
                                } else if (text.contains("album")) {
                                    query = query.split("album ")[1]
                                    "album"
                                } else {
                                    "track"
                                }


                            audioManager.dispatchMediaKeyEvent(KeyEvent(
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_MEDIA_PLAY
                            ))
                            Handler().postDelayed({
                                audioManager.dispatchMediaKeyEvent(KeyEvent(
                                    KeyEvent.ACTION_DOWN,
                                    KeyEvent.KEYCODE_MEDIA_PAUSE
                                ))
                            }, 300)

                            val url = "https://api.spotify.com/v1/search?query=$type%3A${query}&type=$type"

                            if (spotifyAppRemote != null) SpotifyAppRemote.disconnect(spotifyAppRemote)

                            val connectionParams = ConnectionParams.Builder(Constants.SPOTIFY_CLIENT_ID)
                                .setRedirectUri(Constants.SPOTIFY_REDIRECT_URI)
                                .showAuthView(true)
                                .build()

                            SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
                                override fun onConnected(appRemote: SpotifyAppRemote) {
                                    spotifyAppRemote = appRemote
                                    Log.d("SPOTIFY", "Connected to spotify remote")
                                    // Now you can start interacting with App Remote
                                    // val playlistURI = "spotify:playlist:37i9dQZF1DX7F6B5noG69s"
                                    // spotifyAppRemote?.playerApi?.play(playlistURI)// ?.await()

                                    Log.i("SPOTIFY", "Title: $query")
                                    var token: String

                                    val queue = Volley.newRequestQueue(applicationContext)

                                    queue.add(
                                    object : StringRequest(
                                        Method.POST, "https://accounts.spotify.com/api/token?grant_type=client_credentials",
                                        { response ->
                                            val jsonRes = JSONObject(response.toString()).toMap()
                                            try {
                                                Log.i("SPOTIFY", "Token request response: $jsonRes")
                                                token = jsonRes["access_token"].toString()
                                                Log.i("SPOTIFY", "Token: $token")

                                                queue.add(
                                                object : JsonObjectRequest(
                                                    Method.GET, url, null,
                                                    { playResponse ->
                                                        val playJsonRes = JSONObject(playResponse.toString()).toMap()
                                                        try {
                                                            val result= ((playJsonRes["${type}s"] as Map<*, *>)["items"] as List<*>)[0] as HashMap<*, *>
                                                            Log.i("SPOTIFY", "result: $result")
                                                            Log.i("SPOTIFY", "id: ${result["id"]}")

                                                            val playURI =
                                                                if (result["id"].toString().contains("spotify:")) result["id"].toString()
                                                                else "spotify:$type:${result["id"]}"

                                                            spotifyAppRemote?.playerApi?.play(playURI)
                                                            Log.i("SPOTIFY", "Sent play command")

                                                            spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
                                                                // val track: Track = playerState.track
                                                                // Log.d("SPOTIFY", track.name + " by " + track.artist.name)

                                                                Handler().postDelayed({
                                                                    Log.i("SPOTIFY", "Disconnecting remote")
                                                                    spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback(null)
                                                                    spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.cancel()
                                                                    SpotifyAppRemote.disconnect(spotifyAppRemote)
                                                                }, 500)
                                                            }

                                                        } catch (playException: Exception) {
                                                            Log.e("SPOTIFY", "Error retrieving playlist: ${playException.message}, ${playException.printStackTrace()}")
                                                            speak("Mi dispiace, non ho trovato niente di nome $query")
                                                        }
                                                    },
                                                    { playError ->
                                                        Toast.makeText(
                                                            applicationContext,
                                                            "Error $playError occured",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }) {
                                                    override fun getHeaders(): MutableMap<String, String> {
                                                        val headers = HashMap<String, String>()
                                                        headers["Authorization"] = "Bearer $token"
                                                        return headers
                                                    }
                                                })
                                            } catch(e: Exception) {
                                                Log.e("SPOTIFY", "Error getting a token: ${e.message}, ${e.printStackTrace()}")
                                            }
                                        },
                                        { err ->
                                            Toast.makeText(
                                                applicationContext,
                                                "Error $err occured",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Log.e("SPOTIFY", "Error requesting token ${err.message}, ${err.printStackTrace()}")
                                        }) {

                                        override fun getHeaders(): MutableMap<String, String> {
                                            val headers = HashMap<String, String>()
                                            headers["Authorization"] = "Basic ${Constants.SPOTIFY_ENCODED_CLIENT_ID_SECRET}"
                                            headers["Accept"] = "application/json"
                                            return headers
                                        }
                                    })

                                    /*response = requests.get(
                                        query,
                                        headers={
                                            'Content-Type': 'application/json',
                                            "Authorization": "Bearer " + self.token
                                        }
                                    )
                                    response_json = response.json()
                                    try:
                                        track = response_json["tracks"]["items"][0]
                                        except IndexError as e:
                                        # song not found
                                        # self.jarvis.speak("Canzone non trovata")
                                        # print("error: ", e)
                                        return None
                                        finally:
                                        return track["id"]*/
                                }

                                override fun onFailure(throwable: Throwable) {
                                    Log.e("SPOTIFY", throwable.message, throwable)
                                    // Something went wrong when attempting to connect! Handle errors here
                                }
                            })

                            /*private fun connected() {
                                spotifyAppRemote.let { spotifyAppRemote ->
                                    // Play a playlist
                                    // https://open.spotify.com/playlist/37i9dQZF1DX7F6B5noG69s?si=822596f6285f4489
                                    val playlistURI = "spotify:playlist:37i9dQZF1DX7F6B5noG69s"
                                    spotifyAppRemote?.playerApi?.play(playlistURI)
                                    // Subscribe to PlayerState
                                    spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
                                        val track: Track = playerState.track
                                        Log.d("SPOTIFY", track.name + " by " + track.artist.name)
                                    }
                                }
                            }*/
                        }
                        /*else if (text.contains("wifi")) {
                            if (text.contains("accendi")) {
                                setWifiOn()
                            } else if (text.contains("spegni")) {
                                setWifiOff()
                            }
                        }*/
                    }
                }
            }
        }
        if (!sharedPrefs.getString(Constants.ROUTINE_COMMANDS_LEFT, null).isNullOrEmpty() && currentInteraction == null) {
            handleRoutineActions(sharedPrefs.getString(Constants.ROUTINE_COMMANDS_LEFT, null)!!.split(Constants.ROUTINE_COMMANDS_SEPARATOR).toMutableList(), actionResolver, speechRecognizer, speechRecognizerIntent)
        } else {
            startSpeechRecOrHotwordDetection(
                speechRecognizer,
                speechRecognizerIntent
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun handleRoutineActions (
        commandsList: MutableList<String>,
        actionResolver: ActionResolver,
        speechRecognizer: SpeechRecognizer,
        speechRecognizerIntent: Intent,
        voiceTriggered: Boolean = false
    ) {
        commandsList.forEachIndexed { index, command ->
            Log.i("ROUTINES", "handling action: $command")
            when (command) {
                Constants.ROUTINE_ACTION_TELL_WEATHER -> {
                    Weather.getWeather(
                        cityName = actionResolver.getEntity("location"),
                        context = applicationContext,
                        callback = object : Weather.WeatherCallback {
                            override fun onResponse(res: String) {
                                speak(res, wait = true)
                                handleRoutineActions(commandsList.subList(index + 1, commandsList.size), actionResolver, speechRecognizer, speechRecognizerIntent, voiceTriggered = voiceTriggered)
                            }

                            override fun onError(err: VolleyError) {
                                if (err is ServerError) {
                                    Weather.getWeather(
                                        cityName = "Trieste",
                                        context = applicationContext,
                                        callback = object : Weather.WeatherCallback {
                                            override fun onResponse(res: String) {
                                                speak(res, wait = true)
                                                handleRoutineActions(commandsList.subList(index + 1, commandsList.size), actionResolver, speechRecognizer, speechRecognizerIntent, voiceTriggered = voiceTriggered)
                                            }

                                            override fun onError(err: VolleyError) { }
                                        })
                                }
                            }
                        })

                    return

                    /*val location = actionResolver.getEntity("location")
                    Weather.getWeather(
                        cityName = location,
                        context = applicationContext,
                        callback = object : Weather.WeatherCallback {
                            override fun onResponse(res: String) {
                                speak(res)
                            }

                            override fun onError(err: VolleyError) {
                                if (err is ServerError) {
                                    Weather.getWeather(
                                        cityName = "Trieste",
                                        context = applicationContext,
                                        callback = object : Weather.WeatherCallback {
                                            override fun onResponse(res: String) {
                                                speak(res)
                                            }

                                            override fun onError(err: VolleyError) {}
                                        })
                                }
                            }
                        })*/
                    /*while (!mTTS.isSpeaking) Thread.sleep(100)
                    while (mTTS.isSpeaking) Thread.sleep(100)*/
                }
                Constants.ROUTINE_ACTION_TELL_EVENTS -> {
                    // TODO
                }
                Constants.ROUTINE_ACTION_TELL_DATE_TIME -> {
                    Handler().postDelayed({
                        speak(
                            text = "Sono le ${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)} e ${Calendar.getInstance().get(Calendar.MINUTE)} del ${
                                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)} ${Constants.MONTHS[Calendar.getInstance().get(Calendar.MONTH)]} ${
                                Calendar.getInstance().get(Calendar.YEAR)}",
                            wait = true
                        )
                        handleRoutineActions(commandsList.subList(index + 1, commandsList.size), actionResolver, speechRecognizer, speechRecognizerIntent, voiceTriggered = voiceTriggered)
                        return@postDelayed
                    }, 500)
                    return
                }
                Constants.ROUTINE_ACTION_MESSAGE_COMING_HOME -> {
                    // TODO
                }
                Constants.ROUTINE_ACTION_SET_RINGER_MUTE -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
                Constants.ROUTINE_ACTION_SET_RINGER_VIBRATION -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
                Constants.ROUTINE_ACTION_SET_RINGER_ON -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
                Constants.ROUTINE_ACTION_SET_DND_ON -> {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                }
                Constants.ROUTINE_ACTION_SET_DND_OFF -> {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
                Constants.ROUTINE_ACTION_SET_BLUETOOTH_ON -> {
                    bluetoothAdapter.enable()
                }
                Constants.ROUTINE_ACTION_SET_BLUETOOTH_OFF -> {
                    bluetoothAdapter.disable()
                }
                Constants.ROUTINE_ACTION_SET_WIFI_ON -> {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
                Constants.ROUTINE_ACTION_SET_WIFI_OFF -> {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
                Constants.ROUTINE_ACTION_STOP_LISTENING -> {
                    stopAfterRoutineExecution = true
                }
                Constants.ROUTINE_ACTION_START_LISTENING -> {
                    startAfterRoutineExecution = true
                }
                /**
                 * Add a routine action which executes the voice command specified in it (like google's personalized command)
                 * When trying to set an alarm at a time specified by voice, type in that action "Imposta una sveglia", which will trigger
                 * INTERACTION_SET_ALARM asking for a datetime.
                 * For date triggered routines just use the below code and implement a way to properly execute the actions after the alarm
                 * add disabled and enabled routines
                 */
                Constants.ROUTINE_ACTION_RING_ALARM -> {
                    if (!voiceTriggered) {
                        val alarmIntent = Intent(applicationContext, AlarmActivity::class.java)
                        val commandsArrayList = arrayListOf<String>()
                        commandsList.subList(index + 1, commandsList.size)
                            .forEach { commandsArrayList.add(it) }

                        alarmIntent.putExtra(Constants.ROUTINE_ALARM_COMMANDS, commandsArrayList)
                        alarmIntent.putExtra(Constants.ROUTINE_ALARM, true)

                        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(alarmIntent)
                        stopForeground(true)
                        stopSelf()
                        return
                    }
                }
                else -> {
                    if (command.contains(Constants.ROUTINE_ACTION_SPEAK)) {
                        Handler().postDelayed({
                            speak(command.split(Constants.ROUTINE_ACTION_SPEAK)[1], wait = true)
                            handleRoutineActions(commandsList.subList(index + 1, commandsList.size), actionResolver, speechRecognizer, speechRecognizerIntent, voiceTriggered = voiceTriggered)
                            return@postDelayed
                        }, 500)
                        return
                    }
                    else if (command.contains(Constants.ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE)) {
                        val number = command.split(Constants.ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE)[0]
                        val body = command.split(Constants.ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE)[1]

                        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
                        val mobileRef: DatabaseReference = database.getReference("mobile")

                        mobileRef.child("commandToServer").setValue(command)

                        /*val url = "https://api.whatsapp.com/send?phone=${number.replace(" ", "")}&text=$body${Constants.WHATSAPP_MESSAGE_SUFFIX}"
                        val i = Intent(Intent.ACTION_VIEW)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.data = Uri.parse(url)
                        startActivity(i)
                        Thread.sleep(5000)*/
                    }
                    else if (command.contains(Constants.ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME)) {
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            /*(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * command.split(
                                Constants.ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME
                            )[1].toInt()) / 100*/
                            command.split(
                                Constants.ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME
                            )[1].toInt(),
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                    else if (command.contains(Constants.ROUTINE_ACTION_SET_RINGER_VOLUME)) {
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_RING,
                            command.split(
                                Constants.ROUTINE_ACTION_SET_RINGER_VOLUME
                            )[1].toInt(),
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                    else if (command.contains(Constants.ROUTINE_ACTION_SET_NOTIFICATION_VOLUME)) {
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_NOTIFICATION,
                            command.split(
                                Constants.ROUTINE_ACTION_SET_NOTIFICATION_VOLUME
                            )[1].toInt(),
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                    else if (command.contains(Constants.ROUTINE_ACTION_CUSTOM)) {
                        sharedPrefs.edit().putString(Constants.ROUTINE_COMMANDS_LEFT, commandsList.subList(index + 1, commandsList.size).joinToString(Constants.ROUTINE_COMMANDS_SEPARATOR)).apply()
                        witAiRequest(
                            command.split(Constants.ROUTINE_ACTION_CUSTOM)[1],
                            fromRoutine = true,
                            speechRecognizer = speechRecognizer,
                            speechRecognizerIntent = speechRecognizerIntent
                        )
                        return
                    }
                    // executeIntents(actionResolver, command.split(Constants.ROUTINE_ACTION_CUSTOM)[1], command.split(Constants.ROUTINE_ACTION_CUSTOM)[1], fromRoutine = true, commandsLeft = commandsList.subList(index + 1, commandsList.size - 1))
                }
            }
        }
        Log.i("ROUTINES", "Activating hotword rec: " + ((voiceTriggered && !stopAfterRoutineExecution) || startAfterRoutineExecution ||
                (!stopAfterRoutineExecution && sharedPrefs.getBoolean(Constants.HOTWORD_RECOGNITION_SERVICE_RUNNING, false))).toString() + ", voiceTriggered: $voiceTriggered" +
                ", startAfterRoutineExecution: $startAfterRoutineExecution, stopAfterRoutineExecution: $stopAfterRoutineExecution, hotword service running: ${startIntent!!.getBooleanExtra(Constants.HOTWORD_RECOGNITION_SERVICE_RUNNING, false)}")
        if (
            (voiceTriggered && !stopAfterRoutineExecution) || startAfterRoutineExecution ||
            (!stopAfterRoutineExecution && sharedPrefs.getBoolean(Constants.HOTWORD_RECOGNITION_SERVICE_RUNNING, false))
        ) {
            restartHotwordDetection(speechRecognizer)
        } else {
            speechRecognizer.destroy()
            stopForeground(true)
            stopSelf()
        }

        sharedPrefs.edit().putBoolean(Constants.HOTWORD_RECOGNITION_SERVICE_RUNNING, false).apply()
        sharedPrefs.edit().putString(Constants.ROUTINE_COMMANDS_LEFT, null).apply()
    }

    private fun isBluetoothHeadsetConnected(): Boolean {
        return (BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled && BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(
            BluetoothHeadset.HEADSET
        ) == BluetoothHeadset.STATE_CONNECTED)
    }

    private fun setModeBluetooth(context: Context) {
        try {
            // sharedPrefs.edit().putBoolean(Constants.SPEAKER_ON, audioManager.isSpeakerphoneOn).apply()
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
            /*audioManager.isMicrophoneMute = true
            audioManager.isSpeakerphoneOn = false*/
        } catch (e: Exception) {
            makeToast(context, e.message.toString())
        }
    }

    private fun setModeNormal() {
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            /*audioManager.isMicrophoneMute = false
            audioManager.isSpeakerphoneOn = sharedPrefs.getBoolean(Constants.SPEAKER_ON, true)*/
        } catch (e: Exception) {
            makeToast(applicationContext, e.message.toString())
        }
    }

    private fun switchBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            speak("Bluetooth spento")
            bluetoothAdapter.disable()
        } else {
            bluetoothAdapter.enable()
            speak("Bluetooth acceso")
        }
    }

    private fun bluetoothDiscoverable() {
        if (bluetoothAdapter.isDiscovering) {
            speak("Dispositivo pronto a connettersi")
            val intent = Intent(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE))
            startActivity(intent)
        }
    }

    /*private fun setWifiOn() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("svc wifi enable"))
    }

    private fun setWifiOff() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("svc wifi disable"))
    }*/

    private fun pushAppointmentsToCalendar(
        title: String?,
        addInfo: String?,
        place: String?,
        status: Int?,
        startDate: Long?,
        needReminder: Boolean?,
        needMailService: Boolean?
    ): Long? {
        /***************** Event: note(without alert)  */
        val eventUriString = "content://com.android.calendar/events"
        val eventValues = ContentValues()
        eventValues.put("calendar_id", 1) // id, We need to choose from
        // our mobile for primary
        // its 1
        eventValues.put("title", title)
        eventValues.put("description", addInfo)
        eventValues.put("eventLocation", place)
        val endDate = startDate?.plus(1000 * 60 * 60) // For next 1hr
        eventValues.put("dtstart", startDate)
        eventValues.put("dtend", endDate)

        // values.put("allDay", 1); //If it is bithday alarm or such
        // kind (which should remind me for whole day) 0 for false, 1
        // for true
        eventValues.put("eventStatus", status) // This information is
        // sufficient for most
        // entries tentative (0),
        // confirmed (1) or canceled
        // (2):
        eventValues.put("eventTimezone", "UTC/GMT +2:00")
        /*Comment below visibility and transparency  column to avoid java.lang.IllegalArgumentException column visibility is invalid error */

        /*eventValues.put("visibility", 3); // visibility to default (0),
                                        // confidential (1), private
                                        // (2), or public (3):
    eventValues.put("transparency", 0); // You can control whether
                                        // an event consumes time
                                        // opaque (0) or transparent
                                        // (1).
      */eventValues.put("hasAlarm", 1) // 0 for false, 1 for true
        val eventUri: Uri? = applicationContext.contentResolver.insert(
            Uri.parse(eventUriString),
            eventValues
        )
        val eventID: Long? = eventUri?.lastPathSegment?.toLong()
        if (needReminder == true) {
            /***************** Event: Reminder(with alert) Adding reminder to event  */
            val reminderUriString = "content://com.android.calendar/reminders"
            val reminderValues = ContentValues()
            reminderValues.put("event_id", eventID)
            reminderValues.put("minutes", 5) // Default value of the
            // system. Minutes is a
            // integer
            reminderValues.put("method", 1) // Alert Methods: Default(0),
            // Alert(1), Email(2),
            // SMS(3)
            val reminderUri: Uri? = applicationContext.contentResolver.insert(
                Uri.parse(reminderUriString),
                reminderValues
            )
        }
        /***************** Event: Meeting(without alert) Adding Attendies to the meeting  */
        if (needMailService == true) {
            val attendeuesesUriString = "content://com.android.calendar/attendees"

            /********
             * To add multiple attendees need to insert ContentValues multiple
             * times
             */
            val attendeesValues = ContentValues()
            attendeesValues.put("event_id", eventID)
            attendeesValues.put("attendeeName", "xxxxx") // Attendees name
            attendeesValues.put("attendeeEmail", "yyyy@gmail.com") // Attendee
            // E
            // mail
            // id
            attendeesValues.put("attendeeRelationship", 0) // Relationship_Attendee(1),
            // Relationship_None(0),
            // Organizer(2),
            // Performer(3),
            // Speaker(4)
            attendeesValues.put("attendeeType", 0) // None(0), Optional(1),
            // Required(2), Resource(3)
            attendeesValues.put("attendeeStatus", 0) // NOne(0), Accepted(1),
            // Decline(2),
            // Invited(3),
            // Tentative(4)
            val attendeuesesUri: Uri? = applicationContext.contentResolver.insert(
                Uri.parse(attendeuesesUriString), attendeesValues
            )
        }
        return eventID
    }

    private fun getAlarmTime(strDate: String?): String {
        val format = SimpleDateFormat("yyyy-MM-dd't'HH:mm:ss")
        val date: Date = format.parse(strDate)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, 1)
        return when (date.date) {
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH) -> {
                "Sveglia impostata alle ${date.hours} e ${date.minutes}"
            }
            calendar.get(Calendar.DAY_OF_MONTH) -> {
                "Sveglia impostata domani alle ${date.hours} e ${date.minutes}"
            }
            else -> {
                "Sveglia impostata il ${date.date} ${Constants.MONTHS[date.month]} ${date.year} alle ${date.hours} e ${date.minutes}"
            }
        }
    }

    private fun setAlarmLongDate(date: Long, title: String? = null, intentExtras: Map<String, String?>? = null) {

        val correctedDate = Calendar.getInstance()
        correctedDate.timeInMillis = date
        correctedDate.set(Calendar.SECOND, 0)
        correctedDate.set(Calendar.MILLISECOND, 0)

        var lastId = 0
        alarmsDBDao.getAll().forEach { alarm ->
            if (alarm.date == correctedDate.timeInMillis) {
                speak("Signore, una sveglia è già stata impostata per quell'ora")
                // makeToast(applicationContext, "Una sveglia è già stata impostata per quell'ora")
                return
            }
            if (lastId < alarm.id) lastId = alarm.id
        }

        val alarm = Alarm(
                id = lastId + 1,
                date = correctedDate.timeInMillis,
                title = title
            )

        alarmsDBDao.insert(alarm)

        Log.i("ALARMS", "Alarm inserted with id: ${alarm.id}")

        val alarmIntent = Intent(
            applicationContext,
            ReminderBroadcast::class.java
        )

        intentExtras?.forEach { key, value ->
            alarmIntent.putExtra(key, value)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            alarm.id,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            correctedDate.timeInMillis,
            pendingIntent
        )
    }

    private fun setAlarm(strDate: String?, title: String? = null, intentExtras: Map<String, String?>? = null) {
        val format = SimpleDateFormat("yyyy-MM-dd't'HH:mm:ss")
        val date: Date = format.parse(strDate)
        setAlarmLongDate(date.toInstant().toEpochMilli(), title = title, intentExtras = intentExtras)
    }

    private fun deleteAlarm(title: String? = null, strDate: String? = null) {
        Log.i("ALARMS", "alarm title: $title")
        if (title.isNullOrEmpty() && strDate.isNullOrEmpty()) {
            speak("Non ho capito")
            return
        }

        val alarm = if (!strDate.isNullOrEmpty()) {
            val format = SimpleDateFormat("yyyy-MM-dd't'HH:mm:ss")
            val date: Long = format.parse(strDate).toInstant().toEpochMilli()
            val correctedDate = Calendar.getInstance()
            correctedDate.timeInMillis = date
            correctedDate.set(Calendar.SECOND, 0)
            correctedDate.set(Calendar.MILLISECOND, 0)

            alarmsDBDao.getByDate(correctedDate.timeInMillis)
        } else {
            try {
                title?.let {
                    alarmsDBDao.getByTitle(it)
                }
                null
            } catch (e: Exception) {
                Log.e("ALARMS", "Error retrieving alarm from db: ${e.message}")
                null
            }
        }

        Log.i("ALARMS", "${alarm?.id}, ${alarm?.title}, ${alarm?.date}")

        if (alarm != null) {
            val alarmIntent = Intent(
                applicationContext,
                ReminderBroadcast::class.java
            )

            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                alarm.id,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pendingIntent)

            alarmsDBDao.delete(alarm)
            speak("Sveglia cancellata")
        } else {
            speak("Non ho trovato la sveglia")
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

    private fun restartHotwordDetection(speechRecognizer: SpeechRecognizer?) {
        val activityIntent = Intent(applicationContext, MainActivity::class.java)
        activityIntent.putExtra("from_service_speech_rec", true)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        startActivity(activityIntent)
        /*val hotwordServiceIntent = Intent(applicationContext, PorcupineService::class.java)
        ContextCompat.startForegroundService(applicationContext, hotwordServiceIntent)*/
        stopForeground(true)
        speechRecognizer?.destroy()
        stopSelf()
    }

    private fun speak(text: String, wait: Boolean = false) {
        while (!mTTSReady) {
            SystemClock.sleep(100)
        }
        if (isBluetoothHeadsetConnected()) setModeNormal()
        audioManager.requestAudioFocus(
            AudioFocusRequest.Builder(
                AudioManager.STREAM_MUSIC
            ).build()
        )
        SystemClock.sleep(500)
        if (wait) {
            mTTS.speak(text, TextToSpeech.QUEUE_ADD, null, null)
        } else {
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
        /*if (pauseExecution) {
            while (mTTS.isSpeaking) {
                val dummy = "easter_egg"
            }
        }*/
        mTTS.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(p0: String?) {}

            override fun onDone(p0: String?) {
                audioManager.abandonAudioFocusRequest(
                    AudioFocusRequest.Builder(
                        AudioManager.STREAM_MUSIC
                    ).build()
                )
            }

            override fun onError(p0: String?) {
                audioManager.abandonAudioFocusRequest(
                    AudioFocusRequest.Builder(
                        AudioManager.STREAM_MUSIC
                    ).build()
                )
            }
        })
        while (mTTS.isSpeaking) {
            SystemClock.sleep(100)
            // val dummy = "easter egg"
        }
        if (currentInteraction != null && isBluetoothHeadsetConnected()) setModeBluetooth(applicationContext)
        sharedPrefs.edit().putString("lastSaid", text).apply()
    }

    private fun getContactName(context: Context?, number: String): String?{
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val selection = ContactsContract.Contacts.HAS_PHONE_NUMBER
        val cursor = context?.contentResolver?.query(
            uri, arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ), selection, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        var contactName: String? = ""
        var contactNumber: String? = ""

        cursor?.moveToFirst()
        while (!cursor?.isAfterLast!!) {
            //Toast.makeText(context, contactNumber, Toast.LENGTH_SHORT).show()
            contactNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            if (contactNumber.toLowerCase().trim().contains(number.toLowerCase().trim())) {
                return contactName
            }
            cursor.moveToNext()
        }
        cursor.close()
        return null
    }

    private fun getContactNumber(context: Context, name: String): String?{
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val selection = ContactsContract.Contacts.HAS_PHONE_NUMBER
        val cursor = context.contentResolver.query(
            uri, arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ), selection, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        var contactName: String? = ""
        var contactNumber: String? = ""

        cursor?.moveToFirst()
        while (!cursor?.isAfterLast!!) {
            //Toast.makeText(context, contactNumber, Toast.LENGTH_SHORT).show()
            contactNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            if (contactName.toLowerCase().trim().contains(name.toLowerCase().trim())) {
                return contactNumber
            }
            cursor.moveToNext()
        }
        cursor.close()
        return null
    }

    private fun startCall(contact: String) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val contactNumber = getContactNumber(applicationContext, contact)
        callIntent.data = Uri.parse("tel:$contactNumber")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            speak("Non ho i permessi per farlo")
        } else {
            speak("Chiamo $contact")
            try {
                startActivity(callIntent)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, e.message.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getWeather(actionResolver: ActionResolver, locationParam: String? = null) {
        val location = locationParam ?: actionResolver.getEntity("location")
        Weather.getWeather(
            cityName = location,
            context = applicationContext,
            callback = object : Weather.WeatherCallback {
                override fun onResponse(res: String) {
                    speak(res)
                }

                override fun onError(err: VolleyError) {
                    if (err is ServerError) {
                        getWeather(actionResolver, "Trieste")
                    }
                }
            })
    }

    companion object {
        private const val CHANNEL_ID = "SpeechRecognitionServiceChannel"
        fun getErrorText(errorCode: Int): String {
            return when (errorCode) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                SpeechRecognizer.ERROR_SERVER -> "error from server"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Non ho capito, riprova"
            }
        }
    }
}

/** USEFUL ONES
 * KEYCODE_MUTE - mutes mic
 **/

/*
/** Unknown key code. */
KEYCODE_UNKNOWN = 0;
/** Soft Left key.
 * Usually situated below the display on phones and used as a multi-function
 * feature key for selecting a software defined function shown on the bottom left
 * of the display. */
KEYCODE_SOFT_LEFT = 1;
/** Soft Right key.
 * Usually situated below the display on phones and used as a multi-function
 * feature key for selecting a software defined function shown on the bottom right
 * of the display. */
KEYCODE_SOFT_RIGHT = 2;
/** Home key.
 * This key is handled by the framework and is never delivered to applications. */
KEYCODE_HOME = 3;
/** Back key. */
KEYCODE_BACK = 4;
/** Call key. */
KEYCODE_CALL = 5;
/** End Call key. */
KEYCODE_ENDCALL = 6;
/** '0' key. */
KEYCODE_0 = 7;
/** '1' key. */
KEYCODE_1 = 8;
/** '2' key. */
KEYCODE_2 = 9;
/** '3' key. */
KEYCODE_3 = 10;
/** '4' key. */
KEYCODE_4 = 11;
/** '5' key. */
KEYCODE_5 = 12;
/** '6' key. */
KEYCODE_6 = 13;
/** '7' key. */
KEYCODE_7 = 14;
/** '8' key. */
KEYCODE_8 = 15;
/** '9' key. */
KEYCODE_9 = 16;
/** '*' key. */
KEYCODE_STAR = 17;
/** '#' key. */
KEYCODE_POUND = 18;
/** Directional Pad Up key.
 * May also be synthesized from trackball motions. */
KEYCODE_DPAD_UP = 19;
/** Directional Pad Down key.
 * May also be synthesized from trackball motions. */
KEYCODE_DPAD_DOWN = 20;
/** Directional Pad Left key.
 * May also be synthesized from trackball motions. */
KEYCODE_DPAD_LEFT = 21;
/** Directional Pad Right key.
 * May also be synthesized from trackball motions. */
KEYCODE_DPAD_RIGHT = 22;
/** Directional Pad Center key.
 * May also be synthesized from trackball motions. */
KEYCODE_DPAD_CENTER = 23;
/** Volume Up key.
 * Adjusts the speaker volume up. */
KEYCODE_VOLUME_UP = 24;
/** Volume Down key.
 * Adjusts the speaker volume down. */
KEYCODE_VOLUME_DOWN = 25;
/** Power key. */
KEYCODE_POWER = 26;
/** Camera key.
 * Used to launch a camera application or take pictures. */
KEYCODE_CAMERA = 27;
/** Clear key. */
KEYCODE_CLEAR = 28;
/** 'A' key. */
KEYCODE_A = 29;
/** 'B' key. */
KEYCODE_B = 30;
/** 'C' key. */
KEYCODE_C = 31;
/** 'D' key. */
KEYCODE_D = 32;
/** 'E' key. */
KEYCODE_E = 33;
/** 'F' key. */
KEYCODE_F = 34;
/** 'G' key. */
KEYCODE_G = 35;
/** 'H' key. */
KEYCODE_H = 36;
/** 'I' key. */
KEYCODE_I = 37;
/** 'J' key. */
KEYCODE_J = 38;
/** 'K' key. */
KEYCODE_K = 39;
/** 'L' key. */
KEYCODE_L = 40;
/** 'M' key. */
KEYCODE_M = 41;
/** 'N' key. */
KEYCODE_N = 42;
/** 'O' key. */
KEYCODE_O = 43;
/** 'P' key. */
KEYCODE_P = 44;
/** 'Q' key. */
KEYCODE_Q = 45;
/** 'R' key. */
KEYCODE_R = 46;
/** 'S' key. */
KEYCODE_S = 47;
/** 'T' key. */
KEYCODE_T = 48;
/** 'U' key. */
KEYCODE_U = 49;
/** 'V' key. */
KEYCODE_V = 50;
/** 'W' key. */
KEYCODE_W = 51;
/** 'X' key. */
KEYCODE_X = 52;
/** 'Y' key. */
KEYCODE_Y = 53;
/** 'Z' key. */
KEYCODE_Z = 54;
/** ',' key. */
KEYCODE_COMMA = 55;
/** '.' key. */
KEYCODE_PERIOD = 56;
/** Left Alt modifier key. */
KEYCODE_ALT_LEFT = 57;
/** Right Alt modifier key. */
KEYCODE_ALT_RIGHT = 58;
/** Left Shift modifier key. */
KEYCODE_SHIFT_LEFT = 59;
/** Right Shift modifier key. */
KEYCODE_SHIFT_RIGHT = 60;
/** Tab key. */
KEYCODE_TAB = 61;
/** Space key. */
KEYCODE_SPACE = 62;
/** Symbol modifier key.
 * Used to enter alternate symbols. */
KEYCODE_SYM = 63;
/** Explorer special function key.
 * Used to launch a browser application. */
KEYCODE_EXPLORER = 64;
/** Envelope special function key.
 * Used to launch a mail application. */
KEYCODE_ENVELOPE = 65;
/** Enter key. */
KEYCODE_ENTER = 66;
/** Backspace key.
 * Deletes characters before the insertion point, unlike {@link #KEYCODE_FORWARD_DEL}. */
KEYCODE_DEL = 67;
/** '`' (backtick) key. */
KEYCODE_GRAVE = 68;
/** '-'. */
KEYCODE_MINUS = 69;
/** '=' key. */
KEYCODE_EQUALS = 70;
/** '[' key. */
KEYCODE_LEFT_BRACKET = 71;
/** ']' key. */
KEYCODE_RIGHT_BRACKET = 72;
/** '\' key. */
KEYCODE_BACKSLASH = 73;
/** ';' key. */
KEYCODE_SEMICOLON = 74;
/** ''' (apostrophe) key. */
KEYCODE_APOSTROPHE = 75;
/** '/' key. */
KEYCODE_SLASH = 76;
/** '@' key. */
KEYCODE_AT = 77;
/** Number modifier key.
 * Used to enter numeric symbols.
 * This key is not Num Lock; it is more like {@link #KEYCODE_ALT_LEFT} and is
 * interpreted as an ALT key by {@link android.text.method.MetaKeyKeyListener}. */
KEYCODE_NUM = 78;
/** Headset Hook key.
 * Used to hang up calls and stop media. */
KEYCODE_HEADSETHOOK = 79;
/** Camera Focus key.
 * Used to focus the camera. */
KEYCODE_FOCUS = 80; // *Camera* focus
/** '+' key. */
KEYCODE_PLUS = 81;
/** Menu key. */
KEYCODE_MENU = 82;
/** Notification key. */
KEYCODE_NOTIFICATION = 83;
/** Search key. */
KEYCODE_SEARCH = 84;
/** Play/Pause media key. */
KEYCODE_MEDIA_PLAY_PAUSE= 85;
/** Stop media key. */
KEYCODE_MEDIA_STOP = 86;
/** Play Next media key. */
KEYCODE_MEDIA_NEXT = 87;
/** Play Previous media key. */
KEYCODE_MEDIA_PREVIOUS = 88;
/** Rewind media key. */
KEYCODE_MEDIA_REWIND = 89;
/** Fast Forward media key. */
KEYCODE_MEDIA_FAST_FORWARD = 90;
/** Mute key.
 * Mutes the microphone, unlike {@link #KEYCODE_VOLUME_MUTE}. */
KEYCODE_MUTE = 91;
/** Page Up key. */
KEYCODE_PAGE_UP = 92;
/** Page Down key. */
KEYCODE_PAGE_DOWN = 93;
/** Picture Symbols modifier key.
 * Used to switch symbol sets (Emoji, Kao-moji). */
KEYCODE_PICTSYMBOLS = 94; // switch symbol-sets (Emoji,Kao-moji)
/** Switch Charset modifier key.
 * Used to switch character sets (Kanji, Katakana). */
KEYCODE_SWITCH_CHARSET = 95; // switch char-sets (Kanji,Katakana)
/** A Button key.
 * On a game controller, the A button should be either the button labeled A
 * or the first button on the bottom row of controller buttons. */
KEYCODE_BUTTON_A = 96;
/** B Button key.
 * On a game controller, the B button should be either the button labeled B
 * or the second button on the bottom row of controller buttons. */
KEYCODE_BUTTON_B = 97;
/** C Button key.
 * On a game controller, the C button should be either the button labeled C
 * or the third button on the bottom row of controller buttons. */
KEYCODE_BUTTON_C = 98;
/** X Button key.
 * On a game controller, the X button should be either the button labeled X
 * or the first button on the upper row of controller buttons. */
KEYCODE_BUTTON_X = 99;
/** Y Button key.
 * On a game controller, the Y button should be either the button labeled Y
 * or the second button on the upper row of controller buttons. */
KEYCODE_BUTTON_Y = 100;
/** Z Button key.
 * On a game controller, the Z button should be either the button labeled Z
 * or the third button on the upper row of controller buttons. */
KEYCODE_BUTTON_Z = 101;
/** L1 Button key.
 * On a game controller, the L1 button should be either the button labeled L1 (or L)
 * or the top left trigger button. */
KEYCODE_BUTTON_L1 = 102;
/** R1 Button key.
 * On a game controller, the R1 button should be either the button labeled R1 (or R)
 * or the top right trigger button. */
KEYCODE_BUTTON_R1 = 103;
/** L2 Button key.
 * On a game controller, the L2 button should be either the button labeled L2
 * or the bottom left trigger button. */
KEYCODE_BUTTON_L2 = 104;
/** R2 Button key.
 * On a game controller, the R2 button should be either the button labeled R2
 * or the bottom right trigger button. */
KEYCODE_BUTTON_R2 = 105;
/** Left Thumb Button key.
 * On a game controller, the left thumb button indicates that the left (or only)
 * joystick is pressed. */
KEYCODE_BUTTON_THUMBL = 106;
/** Right Thumb Button key.
 * On a game controller, the right thumb button indicates that the right
 * joystick is pressed. */
KEYCODE_BUTTON_THUMBR = 107;
/** Start Button key.
 * On a game controller, the button labeled Start. */
KEYCODE_BUTTON_START = 108;
/** Select Button key.
 * On a game controller, the button labeled Select. */
KEYCODE_BUTTON_SELECT = 109;
/** Mode Button key.
 * On a game controller, the button labeled Mode. */
KEYCODE_BUTTON_MODE = 110;
/** Escape key. */
KEYCODE_ESCAPE = 111;
/** Forward Delete key.
 * Deletes characters ahead of the insertion point, unlike {@link #KEYCODE_DEL}. */
KEYCODE_FORWARD_DEL = 112;
/** Left Control modifier key. */
KEYCODE_CTRL_LEFT = 113;
/** Right Control modifier key. */
KEYCODE_CTRL_RIGHT = 114;
/** Caps Lock key. */
KEYCODE_CAPS_LOCK = 115;
/** Scroll Lock key. */
KEYCODE_SCROLL_LOCK = 116;
/** Left Meta modifier key. */
KEYCODE_META_LEFT = 117;
/** Right Meta modifier key. */
KEYCODE_META_RIGHT = 118;
/** Function modifier key. */
KEYCODE_FUNCTION = 119;
/** System Request / Print Screen key. */
KEYCODE_SYSRQ = 120;
/** Break / Pause key. */
KEYCODE_BREAK = 121;
/** Home Movement key.
 * Used for scrolling or moving the cursor around to the start of a line
 * or to the top of a list. */
KEYCODE_MOVE_HOME = 122;
/** End Movement key.
 * Used for scrolling or moving the cursor around to the end of a line
 * or to the bottom of a list. */
KEYCODE_MOVE_END = 123;
/** Insert key.
 * Toggles insert / overwrite edit mode. */
KEYCODE_INSERT = 124;
/** Forward key.
 * Navigates forward in the history stack. Complement of {@link #KEYCODE_BACK}. */
KEYCODE_FORWARD = 125;
/** Play media key. */
KEYCODE_MEDIA_PLAY = 126;
/** Pause media key. */
KEYCODE_MEDIA_PAUSE = 127;
/** Close media key.
 * May be used to close a CD tray, for example. */
KEYCODE_MEDIA_CLOSE = 128;
/** Eject media key.
 * May be used to eject a CD tray, for example. */
KEYCODE_MEDIA_EJECT = 129;
/** Record media key. */
KEYCODE_MEDIA_RECORD = 130;
/** F1 key. */
KEYCODE_F1 = 131;
/** F2 key. */
KEYCODE_F2 = 132;
/** F3 key. */
KEYCODE_F3 = 133;
/** F4 key. */
KEYCODE_F4 = 134;
/** F5 key. */
KEYCODE_F5 = 135;
/** F6 key. */
KEYCODE_F6 = 136;
/** F7 key. */
KEYCODE_F7 = 137;
/** F8 key. */
KEYCODE_F8 = 138;
/** F9 key. */
KEYCODE_F9 = 139;
/** F10 key. */
KEYCODE_F10 = 140;
/** F11 key. */
KEYCODE_F11 = 141;
/** F12 key. */
KEYCODE_F12 = 142;
/** Num Lock key.
 * This is the Num Lock key; it is different from {@link #KEYCODE_NUM}.
 * This key alters the behavior of other keys on the numeric keypad. */
KEYCODE_NUM_LOCK = 143;
/** Numeric keypad '0' key. */
KEYCODE_NUMPAD_0 = 144;
/** Numeric keypad '1' key. */
KEYCODE_NUMPAD_1 = 145;
/** Numeric keypad '2' key. */
KEYCODE_NUMPAD_2 = 146;
/** Numeric keypad '3' key. */
KEYCODE_NUMPAD_3 = 147;
/** Numeric keypad '4' key. */
KEYCODE_NUMPAD_4 = 148;
/** Numeric keypad '5' key. */
KEYCODE_NUMPAD_5 = 149;
/** Numeric keypad '6' key. */
KEYCODE_NUMPAD_6 = 150;
/** Numeric keypad '7' key. */
KEYCODE_NUMPAD_7 = 151;
/** Numeric keypad '8' key. */
KEYCODE_NUMPAD_8 = 152;
/** Numeric keypad '9' key. */
KEYCODE_NUMPAD_9 = 153;
/** Numeric keypad '/' key (for division). */
KEYCODE_NUMPAD_DIVIDE = 154;
/** Numeric keypad '*' key (for multiplication). */
KEYCODE_NUMPAD_MULTIPLY = 155;
/** Numeric keypad '-' key (for subtraction). */
KEYCODE_NUMPAD_SUBTRACT = 156;
/** Numeric keypad '+' key (for addition). */
KEYCODE_NUMPAD_ADD = 157;
/** Numeric keypad '.' key (for decimals or digit grouping). */
KEYCODE_NUMPAD_DOT = 158;
/** Numeric keypad ',' key (for decimals or digit grouping). */
KEYCODE_NUMPAD_COMMA = 159;
/** Numeric keypad Enter key. */
KEYCODE_NUMPAD_ENTER = 160;
/** Numeric keypad '=' key. */
KEYCODE_NUMPAD_EQUALS = 161;
/** Numeric keypad '(' key. */
KEYCODE_NUMPAD_LEFT_PAREN = 162;
/** Numeric keypad ')' key. */
KEYCODE_NUMPAD_RIGHT_PAREN = 163;
/** Volume Mute key.
 * Mutes the speaker, unlike {@link #KEYCODE_MUTE}.
 * This key should normally be implemented as a toggle such that the first press
 * mutes the speaker and the second press restores the original volume. */
KEYCODE_VOLUME_MUTE = 164;
/** Info key.
 * Common on TV remotes to show additional information related to what is
 * currently being viewed. */
KEYCODE_INFO = 165;
/** Channel up key.
 * On TV remotes, increments the television channel. */
KEYCODE_CHANNEL_UP = 166;
/** Channel down key.
 * On TV remotes, decrements the television channel. */
KEYCODE_CHANNEL_DOWN = 167;
/** Zoom in key. */
KEYCODE_ZOOM_IN = 168;
/** Zoom out key. */
KEYCODE_ZOOM_OUT = 169;
/** TV key.
 * On TV remotes, switches to viewing live TV. */
KEYCODE_TV = 170;
/** Window key.
 * On TV remotes, toggles picture-in-picture mode or other windowing functions. */
KEYCODE_WINDOW = 171;
/** Guide key.
 * On TV remotes, shows a programming guide. */
KEYCODE_GUIDE = 172;
/** DVR key.
 * On some TV remotes, switches to a DVR mode for recorded shows. */
KEYCODE_DVR = 173;
/** Bookmark key.
 * On some TV remotes, bookmarks content or web pages. */
KEYCODE_BOOKMARK = 174;
/** Toggle captions key.
 * Switches the mode for closed-captioning text, for example during television shows. */
KEYCODE_CAPTIONS = 175;
/** Settings key.
 * Starts the system settings activity. */
KEYCODE_SETTINGS = 176;
/** TV power key.
 * On TV remotes, toggles the power on a television screen. */
KEYCODE_TV_POWER = 177;
/** TV input key.
 * On TV remotes, switches the input on a television screen. */
KEYCODE_TV_INPUT = 178;
/** Set-top-box power key.
 * On TV remotes, toggles the power on an external Set-top-box. */
KEYCODE_STB_POWER = 179;
/** Set-top-box input key.
 * On TV remotes, switches the input mode on an external Set-top-box. */
KEYCODE_STB_INPUT = 180;
/** A/V Receiver power key.
 * On TV remotes, toggles the power on an external A/V Receiver. */
KEYCODE_AVR_POWER = 181;
/** A/V Receiver input key.
 * On TV remotes, switches the input mode on an external A/V Receiver. */
KEYCODE_AVR_INPUT = 182;
/** Red "programmable" key.
 * On TV remotes, acts as a contextual/programmable key. */
KEYCODE_PROG_RED = 183;
/** Green "programmable" key.
 * On TV remotes, actsas a contextual/programmable key. */
KEYCODE_PROG_GREEN = 184;
/** Yellow "programmable" key.
 * On TV remotes, acts as a contextual/programmable key. */
KEYCODE_PROG_YELLOW = 185;
/** Blue "programmable" key.
 * On TV remotes, acts as a contextual/programmable key. */
KEYCODE_PROG_BLUE = 186;
/** App switch key.
 * Should bring up the application switcher dialog. */
KEYCODE_APP_SWITCH = 187;
/** Generic Game Pad Button #1.*/
KEYCODE_BUTTON_1 = 188;
/** Generic Game Pad Button #2.*/
KEYCODE_BUTTON_2 = 189;
/** Generic Game Pad Button #3.*/
KEYCODE_BUTTON_3 = 190;
/** Generic Game Pad Button #4.*/
KEYCODE_BUTTON_4 = 191;
/** Generic Game Pad Button #5.*/
KEYCODE_BUTTON_5 = 192;
/** Generic Game Pad Button #6.*/
KEYCODE_BUTTON_6 = 193;
/** Generic Game Pad Button #7.*/
KEYCODE_BUTTON_7 = 194;
/** Generic Game Pad Button #8.*/
KEYCODE_BUTTON_8 = 195;
/** Generic Game Pad Button #9.*/
KEYCODE_BUTTON_9 = 196;
/** Generic Game Pad Button #10.*/
KEYCODE_BUTTON_10 = 197;
/** Generic Game Pad Button #11.*/
KEYCODE_BUTTON_11 = 198;
/** Generic Game Pad Button #12.*/
KEYCODE_BUTTON_12 = 199;
/** Generic Game Pad Button #13.*/
KEYCODE_BUTTON_13 = 200;
/** Generic Game Pad Button #14.*/
KEYCODE_BUTTON_14 = 201;
/** Generic Game Pad Button #15.*/
KEYCODE_BUTTON_15 = 202;
/** Generic Game Pad Button #16.*/
KEYCODE_BUTTON_16 = 203;
/** Language Switch key.
 * Toggles the current input language such as switching between English and Japanese on
 * a QWERTY keyboard. On some devices, the same function may be performed by
 * pressing Shift+Spacebar. */
KEYCODE_LANGUAGE_SWITCH = 204;
/** Manner Mode key.
 * Toggles silent or vibrate mode on and off to make the device behave more politely
 * in certain settings such as on a crowded train. On some devices, the key may only
 * operate when long-pressed. */
KEYCODE_MANNER_MODE = 205;
/** 3D Mode key.
 * Toggles the display between 2D and 3D mode. */
KEYCODE_3D_MODE = 206;
/** Contacts special function key.
 * Used to launch an address book application. */
KEYCODE_CONTACTS = 207;
/** Calendar special function key.
 * Used to launch a calendar application. */
KEYCODE_CALENDAR = 208;
/** Music special function key.
 * Used to launch a music player application. */
KEYCODE_MUSIC = 209;
/** Calculator special function key.
 * Used to launch a calculator application. */
KEYCODE_CALCULATOR = 210;
/** Japanese full-width / half-width key. */
KEYCODE_ZENKAKU_HANKAKU = 211;
/** Japanese alphanumeric key. */
KEYCODE_EISU = 212;
/** Japanese non-conversion key. */
KEYCODE_MUHENKAN = 213;
/** Japanese conversion key. */
KEYCODE_HENKAN = 214;
/** Japanese katakana / hiragana key. */
KEYCODE_KATAKANA_HIRAGANA = 215;
/** Japanese Yen key. */
KEYCODE_YEN = 216;
/** Japanese Ro key. */
KEYCODE_RO = 217;
/** Japanese kana key. */
KEYCODE_KANA = 218;
/** Assist key.
 * Launches the global assist activity. Not delivered to applications. */
KEYCODE_ASSIST = 219;
/** Brightness Down key.
 * Adjusts the screen brightness down. */
KEYCODE_BRIGHTNESS_DOWN = 220;
/** Brightness Up key.
 * Adjusts the screen brightness up. */
KEYCODE_BRIGHTNESS_UP = 221;
/** Audio Track key.
 * Switches the audio tracks. */
KEYCODE_MEDIA_AUDIO_TRACK = 222;
/** Sleep key.
 * Puts the device to sleep. Behaves somewhat like {@link #KEYCODE_POWER} but it
 * has no effect if the device is already asleep. */
KEYCODE_SLEEP = 223;
/** Wakeup key.
 * Wakes up the device. Behaves somewhat like {@link #KEYCODE_POWER} but it
 * has no effect if the device is already awake. */
KEYCODE_WAKEUP = 224;
/** Pairing key.
 * Initiates peripheral pairing mode. Useful for pairing remote control
 * devices or game controllers, especially if no other input mode is
 * available. */
KEYCODE_PAIRING = 225;
/** Media Top Menu key.
 * Goes to the top of media menu. */
KEYCODE_MEDIA_TOP_MENU = 226;
/** '11' key. */
KEYCODE_11 = 227;
/** '12' key. */
KEYCODE_12 = 228;
/** Last Channel key.
 * Goes to the last viewed channel. */
KEYCODE_LAST_CHANNEL = 229;
/** TV data service key.
 * Displays data services like weather, sports. */
KEYCODE_TV_DATA_SERVICE = 230;
/** Voice Assist key.
 * Launches the global voice assist activity. Not delivered to applications. */
KEYCODE_VOICE_ASSIST = 231;
/** Radio key.
 * Toggles TV service / Radio service. */
KEYCODE_TV_RADIO_SERVICE = 232;
/** Teletext key.
 * Displays Teletext service. */
KEYCODE_TV_TELETEXT = 233;
/** Number entry key.
 * Initiates to enter multi-digit channel nubmber when each digit key is assigned
 * for selecting separate channel. Corresponds to Number Entry Mode (0x1D) of CEC
 * User Control Code. */
KEYCODE_TV_NUMBER_ENTRY = 234;
/** Analog Terrestrial key.
 * Switches to analog terrestrial broadcast service. */
KEYCODE_TV_TERRESTRIAL_ANALOG = 235;
/** Digital Terrestrial key.
 * Switches to digital terrestrial broadcast service. */
KEYCODE_TV_TERRESTRIAL_DIGITAL = 236;
/** Satellite key.
 * Switches to digital satellite broadcast service. */
KEYCODE_TV_SATELLITE = 237;
/** BS key.
 * Switches to BS digital satellite broadcasting service available in Japan. */
KEYCODE_TV_SATELLITE_BS = 238;
/** CS key.
 * Switches to CS digital satellite broadcasting service available in Japan. */
KEYCODE_TV_SATELLITE_CS = 239;
/** BS/CS key.
 * Toggles between BS and CS digital satellite services. */
KEYCODE_TV_SATELLITE_SERVICE = 240;
/** Toggle Network key.
 * Toggles selecting broacast services. */
KEYCODE_TV_NETWORK = 241;
/** Antenna/Cable key.
 * Toggles broadcast input source between antenna and cable. */
KEYCODE_TV_ANTENNA_CABLE = 242;
/** HDMI #1 key.
 * Switches to HDMI input #1. */
KEYCODE_TV_INPUT_HDMI_1 = 243;
/** HDMI #2 key.
 * Switches to HDMI input #2. */
KEYCODE_TV_INPUT_HDMI_2 = 244;
/** HDMI #3 key.
 * Switches to HDMI input #3. */
KEYCODE_TV_INPUT_HDMI_3 = 245;
/** HDMI #4 key.
 * Switches to HDMI input #4. */
KEYCODE_TV_INPUT_HDMI_4 = 246;
/** Composite #1 key.
 * Switches to composite video input #1. */
KEYCODE_TV_INPUT_COMPOSITE_1 = 247;
/** Composite #2 key.
 * Switches to composite video input #2. */
KEYCODE_TV_INPUT_COMPOSITE_2 = 248;
/** Component #1 key.
 * Switches to component video input #1. */
KEYCODE_TV_INPUT_COMPONENT_1 = 249;
/** Component #2 key.
 * Switches to component video input #2. */
KEYCODE_TV_INPUT_COMPONENT_2 = 250;
/** VGA #1 key.
 * Switches to VGA (analog RGB) input #1. */
KEYCODE_TV_INPUT_VGA_1 = 251;
/** Audio description key.
 * Toggles audio description off / on. */
KEYCODE_TV_AUDIO_DESCRIPTION = 252;
/** Audio description mixing volume up key.
 * Louden audio description volume as compared with normal audio volume. */
KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP = 253;
/** Audio description mixing volume down key.
 * Lessen audio description volume as compared with normal audio volume. */
KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN = 254;
/** Zoom mode key.
 * Changes Zoom mode (Normal, Full, Zoom, Wide-zoom, etc.) */
KEYCODE_TV_ZOOM_MODE = 255;
/** Contents menu key.
 * Goes to the title list. Corresponds to Contents Menu (0x0B) of CEC User Control
 * Code */
KEYCODE_TV_CONTENTS_MENU = 256;
/** Media context menu key.
 * Goes to the context menu of media contents. Corresponds to Media Context-sensitive
 * Menu (0x11) of CEC User Control Code. */
KEYCODE_TV_MEDIA_CONTEXT_MENU = 257;
/** Timer programming key.
 * Goes to the timer recording menu. Corresponds to Timer Programming (0x54) of
 * CEC User Control Code. */
KEYCODE_TV_TIMER_PROGRAMMING = 258;
/** Help key. */
KEYCODE_HELP = 259;
/** Navigate to previous key.
 * Goes backward by one item in an ordered collection of items. */
KEYCODE_NAVIGATE_PREVIOUS = 260;
/** Navigate to next key.
 * Advances to the next item in an ordered collection of items. */
KEYCODE_NAVIGATE_NEXT = 261;
/** Navigate in key.
 * Activates the item that currently has focus or expands to the next level of a navigation
 * hierarchy. */
KEYCODE_NAVIGATE_IN = 262;
/** Navigate out key.
 * Backs out one level of a navigation hierarchy or collapses the item that currently has
 * focus. */
KEYCODE_NAVIGATE_OUT = 263;
/** Primary stem key for Wear
 * Main power/reset button on watch. */
KEYCODE_STEM_PRIMARY = 264;
/** Generic stem key 1 for Wear */
KEYCODE_STEM_1 = 265;
/** Generic stem key 2 for Wear */
KEYCODE_STEM_2 = 266;
/** Generic stem key 3 for Wear */
KEYCODE_STEM_3 = 267;
/** Directional Pad Up-Left */
KEYCODE_DPAD_UP_LEFT = 268;
/** Directional Pad Down-Left */
KEYCODE_DPAD_DOWN_LEFT = 269;
/** Directional Pad Up-Right */
KEYCODE_DPAD_UP_RIGHT = 270;
/** Directional Pad Down-Right */
KEYCODE_DPAD_DOWN_RIGHT = 271;
/** Skip forward media key. */
KEYCODE_MEDIA_SKIP_FORWARD = 272;
/** Skip backward media key. */
KEYCODE_MEDIA_SKIP_BACKWARD = 273;
/** Step forward media key.
 * Steps media forward, one frame at a time. */
KEYCODE_MEDIA_STEP_FORWARD = 274;
/** Step backward media key.
 * Steps media backward, one frame at a time. */
KEYCODE_MEDIA_STEP_BACKWARD = 275;
/** put device to sleep unless a wakelock is held. */
KEYCODE_SOFT_SLEEP = 276;
/** Cut key. */
KEYCODE_CUT = 277;
/** Copy key. */
KEYCODE_COPY = 278;
/** Paste key. */
KEYCODE_PASTE = 279;
/** Consumed by the system for navigation up */
KEYCODE_SYSTEM_NAVIGATION_UP = 280;
/** Consumed by the system for navigation down */
KEYCODE_SYSTEM_NAVIGATION_DOWN = 281;
/** Consumed by the system for navigation left*/
KEYCODE_SYSTEM_NAVIGATION_LEFT = 282;
/** Consumed by the system for navigation right */
KEYCODE_SYSTEM_NAVIGATION_RIGHT = 283;

  /** Key code constant: Show all apps */
public static final int KEYCODE_ALL_APPS = 284;
/** Key code constant: Refresh key. */
public static final int KEYCODE_REFRESH = 285;
/** Key code constant: Thumbs up key. Apps can use this to let user upvote content. */
public static final int KEYCODE_THUMBS_UP = 286;
/** Key code constant: Thumbs down key. Apps can use this to let user downvote content. */
public static final int KEYCODE_THUMBS_DOWN = 287;
 */