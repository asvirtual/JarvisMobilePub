/*
    Copyright 2021 Picovoice Inc.

    You may not use this file except in compliance with the license. A copy of the license is
    located in the "LICENSE" file accompanying this source.

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
    express or implied. See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.example.jarvisdemo2.services

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.*
import android.content.*
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.jarvisdemo2.R
import com.example.jarvisdemo2.activities.MainActivity
import com.example.jarvisdemo2.utilities.Constants
import com.example.jarvisdemo2.utilities.Location

/*import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase*/


class PorcupineService: Service() {

    private lateinit var audioManager: AudioManager
    private lateinit var sharedPrefs: SharedPreferences
    private var porcupineManager: PorcupineManager? = null
    private var numUtterances = 0

    private lateinit var mediaSession: MediaSession
    private var lastVolumeCommand: Int = 0
    private var lastVolumeCommandTime: Long = 0

    /* private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothHeadset: BluetoothHeadset? = null
    private var mConnectedHeadset: BluetoothDevice? = null
    private lateinit var mHeadsetBroadcastReceiver: BroadcastReceiver */
    private lateinit var bluetoothReceiver: BroadcastReceiver

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                "Porcupine",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(notificationChannel)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sharedPrefs = applicationContext.getSharedPreferences("jarvis", Context.MODE_PRIVATE)

        lastVolumeCommandTime = System.currentTimeMillis()

        setModeNormal()

        /*try {
            dbRef = Firebase.database.reference

            dbRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    // val value = dataSnapshot.getValue<String>()
                    Constants.makeToast(
                        applicationContext,
                        "${dataSnapshot.value}, ${dataSnapshot.key}"
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Constants.makeToast(applicationContext, error.message)
                }
            })
        } catch (e: Exception) {
            Constants.makeToast(applicationContext, e.message.toString())
        }*/

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val locationListener: LocationListener = Location(applicationContext)
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 0f, locationListener
            )
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 5000, 0f, locationListener
            )
        }

        if (!isAccessibilityOn(this, KeyEventService::class.java)) {
            mediaSession = MediaSession(this, "PlayerService")
            mediaSession.setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            mediaSession.setPlaybackState(
                PlaybackState.Builder()
                    .setState(
                        PlaybackState.STATE_PLAYING,
                        0,
                        0F
                    ) //you simulate a player which plays something.
                    .build()
            )

            // volume up then down triggers jarvis
            val myVolumeProvider: VolumeProvider = object : VolumeProvider(
                VOLUME_CONTROL_RELATIVE,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), /*max volume*/
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) /* initial volume level*/
            ) {
                override fun onAdjustVolume(direction: Int) {
                    when (direction) {
                        1 -> { // volume up
                            Log.i("VOLUME DETECTOR", "volume increased")
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_RAISE,
                                AudioManager.FLAG_SHOW_UI
                            )
                            if (System.currentTimeMillis() - lastVolumeCommandTime <= 500 && lastVolumeCommand == -1) {
                                val activityIntent = Intent(
                                    applicationContext,
                                    MainActivity::class.java
                                )
                                activityIntent.putExtra("from_service_hotword", true)

                                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                activityIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

                                if (!isMyServiceRunning(SpeechRecognitionService::class.java)) {
                                    startActivity(activityIntent)
                                    stopForeground(true)
                                    stopSelf()
                                }
                            }
                            lastVolumeCommand = direction
                        }
                        -1 -> { // volume down
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_LOWER,
                                AudioManager.FLAG_SHOW_UI
                            )
                            Log.i("VOLUME DETECTOR", "volume decreased")
                            if (System.currentTimeMillis() - lastVolumeCommandTime <= 500 && lastVolumeCommand == 1) {
                                val activityIntent = Intent(
                                    applicationContext,
                                    MainActivity::class.java
                                )
                                activityIntent.putExtra("from_service_hotword", true)

                                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                activityIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

                                if (!isMyServiceRunning(SpeechRecognitionService::class.java)) {
                                    startActivity(activityIntent)
                                    stopForeground(true)
                                    stopSelf()
                                }
                            }
                            lastVolumeCommand = direction
                        }
                    }
                    currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    lastVolumeCommandTime = System.currentTimeMillis()
                }
            }

            mediaSession.setCallback(object : MediaSession.Callback() {})
            mediaSession.setPlaybackToRemote(myVolumeProvider)
            mediaSession.isActive = true

            bluetoothReceiver = object: BroadcastReceiver() {

                override fun onReceive(context: Context, intent: Intent) {
                    val action = intent.action

                    if (BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT == action) {

                        val command =
                            intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD)
                        val type = intent.getIntExtra(
                            BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE,
                            -1
                        )
                        Log.i("BLUETOOTH_RECEIVER", "command: $command, type: $type")
                    }
                }
            }

            val bluetoothIntentFilter = IntentFilter()
            bluetoothIntentFilter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + "." + BluetoothAssignedNumbers.PLANTRONICS)
            bluetoothIntentFilter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + "." + BluetoothAssignedNumbers.SAMSUNG_ELECTRONICS)
            bluetoothIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            bluetoothIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            bluetoothIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            bluetoothIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            bluetoothIntentFilter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
            bluetoothIntentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
            bluetoothIntentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            applicationContext.registerReceiver(bluetoothReceiver, bluetoothIntentFilter)
        }
        
        /* mHeadsetBroadcastReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                if (intent == null || context == null || mBluetoothHeadset == null) return
                var action = intent.action
                var state: Int
                var previousState = intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, BluetoothHeadset.STATE_DISCONNECTED)
                var log = ""

                if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED))
                {
                    state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED)
                    if (state == BluetoothHeadset.STATE_CONNECTED)
                    {
                        mConnectedHeadset = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                        // Audio should not be connected yet but just to make sure.
                        if (mBluetoothHeadset!!.isAudioConnected(mConnectedHeadset))
                        {
                            log = "Headset connected audio already connected"
                        }
                        else
                        {

                            // Calling startVoiceRecognition always returns false here, 
                            // that why a count down timer is implemented to call
                            // startVoiceRecognition in the onTick and onFinish.
                            if (mBluetoothHeadset!!.startVoiceRecognition(mConnectedHeadset))
                            {
                                log = "Headset connected startVoiceRecognition returns true"
                            }
                            else
                            {
                                log = "Headset connected startVoiceRecognition returns false"
                            }
                        }
                    }
                    else if (state == BluetoothHeadset.STATE_DISCONNECTED)
                    {
                        // Calling stopVoiceRecognition always returns false here
                        // as it should since the headset is no longer connected.
                        mConnectedHeadset = null
                    }
                }
                else // audio
                {
                    state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED)

                    mBluetoothHeadset!!.stopVoiceRecognition(mConnectedHeadset)

                    if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED)
                    {
                        log = "Head set audio connected, cancel countdown timer"
                    }
                    else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED)
                    {
                        // The headset audio is disconnected, but calling
                        // stopVoiceRecognition always returns true here.
                        var returnValue = mBluetoothHeadset!!.stopVoiceRecognition(mConnectedHeadset)
                        log = "Audio disconnected stopVoiceRecognition return " + returnValue
                    }
                }

                log += "\nAction = $action\nState = $state previous state = $previousState"
                Log.d("BLUETOOTH", log)
            }
        } */
    }

    @RequiresApi(30)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            0
        )
        numUtterances = 0
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Jarvis")
                .setContentText("In attesa")
                .setSmallIcon(R.drawable.icon_jarvis)
                .setContentIntent(pendingIntent)
                .build()

        startForeground(12345678, notification, FOREGROUND_SERVICE_TYPE_MICROPHONE)
        sendBroadcast(Intent("closeActivity"))

        /*BufferedInputStream(
            resources.openRawResource(R.raw.friday_model), 256
        ).use { `is` ->
            BufferedOutputStream(
                openFileOutput("friday_model.ppn", MODE_PRIVATE), 256
            ).use { os ->
                var r: Int
                while (`is`.read().also { r = it } != -1) {
                    os.write(r)
                }
                os.flush()
            }
        }*/

        try {
            porcupineManager = PorcupineManager.Builder()
                // .setKeywordPath("friday_model.ppn")
                .setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                .setSensitivity(0.7f).build(
                    applicationContext
                ) { keywordIndex: Int ->
                    numUtterances++
                    /*val contentIntent = PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        0
                    )
                    val contentText = if (numUtterances == 1) " time!" else " times!"
                    val n =
                        NotificationCompat.Builder(this, CHANNEL_ID)
                            .setContentTitle("Wake word")
                            .setContentText("Detected $numUtterances$contentText")
                            .setSmallIcon(R.drawable.ic_launcher_background)
                            .setContentIntent(contentIntent)
                            .build()
                    val notificationManager =
                        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    notificationManager.notify(1234, n)*/

                    /*Thread {
                        startSpeechToText()
                    }.start()*/

                    /*val serviceIntent = Intent(applicationContext, SpeechRecognitionService::class.java)
                    startService(serviceIntent)*/
                    val activityIntent = Intent(applicationContext, MainActivity::class.java)
                    activityIntent.putExtra("from_service_hotword", true)

                    if (sharedPrefs.getBoolean(Constants.INTENT_EXTRA_ALARM_RUNNING, false) && !sharedPrefs.getString(
                            Constants.ROUTINE_ALARM_COMMANDS,
                            null
                        ).isNullOrEmpty()) {
                        val actions = arrayListOf<String>()
                        sharedPrefs.getString(Constants.ROUTINE_ALARM_COMMANDS, null)?.split(", ")?.forEach { action -> actions.add(
                            action
                        ) }
                        activityIntent.putExtra(Constants.ROUTINE_ALARM, actions)
                        sharedPrefs.edit().putString(Constants.ROUTINE_ALARM_COMMANDS, null).apply()
                    }

                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    startActivity(activityIntent)
                    stopForeground(true)
                    stopSelf()
                    // startForegroundService(serviceIntent)

                    // speech!!.startListening(recognizerIntent)
                }

            porcupineManager?.start()
            sendBroadcast(Intent("closeActivity"))
        } catch (e: Exception) {
            Toast.makeText(applicationContext, e.message.toString(), Toast.LENGTH_SHORT).show()
            // Log.e("PORCUPINE", e.toString())
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        try {
            mediaSession.release()
        } catch (e: Exception) {
            Log.e("MEDIASESSION", "Error $e while destroying mediasession")
        }
        try {
            porcupineManager!!.stop()
            porcupineManager!!.delete()
        } catch (e: PorcupineException) {
            Log.e("PORCUPINE", e.toString())
        }
        super.onDestroy()
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

    /* private var mHeadsetProfileListener: BluetoothProfile.ServiceListener = object : BluetoothProfile.ServiceListener {
        /**
         * This method is never called, even when we closeProfileProxy on onPause.
         * When or will it ever be called???
         */
        override fun onServiceDisconnected(profile: Int) {
            mBluetoothHeadset?.stopVoiceRecognition(mConnectedHeadset)
            unregisterReceiver(mHeadsetBroadcastReceiver)
            mBluetoothHeadset = null
        }

        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            // mBluetoothHeadset is just a head set profile, 
            // it does not represent a head set device.
            mBluetoothHeadset = proxy as BluetoothHeadset

            // If a head set is connected before this application starts,
            // ACTION_CONNECTION_STATE_CHANGED will not be broadcast. 
            // So we need to check for already connected head set.
            val devices: List<BluetoothDevice> = mBluetoothHeadset!!.connectedDevices
            if (devices.isNotEmpty()) {
                // Only one head set can be connected at a time, 
                // so the connected head set is at index 0.
                mConnectedHeadset = devices[0]

                // The audio should not yet be connected at this stage.
                // But just to make sure we check.
                val log: String = if (mBluetoothHeadset!!.isAudioConnected(mConnectedHeadset)) {
                    "Profile listener audio already connected"
                } else {
                    // The if statement is just for debug. So far startVoiceRecognition always 
                    // returns true here. What can we do if it returns false? Perhaps the only
                    // sensible thing is to inform the user.
                    // Well actually, it only returns true if a call to stopVoiceRecognition is
                    // call somewhere after a call to startVoiceRecognition. Otherwise, if 
                    // stopVoiceRecognition is never called, then when the application is restarted
                    // startVoiceRecognition always returns false whenever it is called.
                    if (mBluetoothHeadset!!.startVoiceRecognition(mConnectedHeadset)) {
                        "Profile listener startVoiceRecognition returns true" //$NON-NLS-1$
                    } else {
                        "Profile listener startVoiceRecognition returns false" //$NON-NLS-1$
                    }
                }
                Log.d("BLUETOOTH", log)
            }

            // During the active life time of the app, a user may turn on and off the head set.
            // So register for broadcast of connection states.
            registerReceiver(
                mHeadsetBroadcastReceiver,
                IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            )

            // Calling startVoiceRecognition does not result in immediate audio connection.
            // So register for broadcast of audio connection states. This broadcast will
            // only be sent if startVoiceRecognition returns true.
            val f = IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
            f.priority = Int.MAX_VALUE
            registerReceiver(mHeadsetBroadcastReceiver, f)
        }
    }*/

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

    private fun isBluetoothHeadsetConnected(): Boolean {
        return (BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled && BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(
            BluetoothHeadset.HEADSET
        ) == BluetoothHeadset.STATE_CONNECTED)
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

    private fun setModeNormal() {
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            /*audioManager.isMicrophoneMute = false
            audioManager.isSpeakerphoneOn = sharedPrefs.getBoolean(Constants.SPEAKER_ON, true)*/
        } catch (e: Exception) {
            Constants.makeToast(applicationContext, e.message.toString())
        }
    }

    companion object {
        private const val CHANNEL_ID = "PorcupineServiceChannel"
    }
}