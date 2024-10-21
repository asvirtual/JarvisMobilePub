package com.example.jarvisdemo2.activities

import android.animation.ObjectAnimator
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.media.RingtoneManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.VolleyError
import com.example.jarvisdemo2.utilities.Constants
import com.example.jarvisdemo2.services.PorcupineService
import com.example.jarvisdemo2.R
import com.example.jarvisdemo2.database.Alarm
import com.example.jarvisdemo2.database.AlarmsDB
import com.example.jarvisdemo2.database.AlarmsDatabaseDAO
import com.example.jarvisdemo2.services.SpeechRecognitionService
import com.example.jarvisdemo2.receivers.ReminderBroadcast
import com.example.jarvisdemo2.utilities.JarvisMediaPlayer
import com.example.jarvisdemo2.utilities.Weather
import kotlinx.android.synthetic.main.activity_alarm.*
import java.util.*
import kotlin.math.cos
import kotlin.math.pow


class AlarmActivity : AppCompatActivity() {

    private lateinit var mTTS: TextToSpeech
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var alarmsDBDao: AlarmsDatabaseDAO
    private var alarm: Alarm? = null

    private val mMessageReceiver: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, p1: Intent?) {
            Constants.makeToast(context!!, "finishing activity")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        sharedPrefs = getSharedPreferences("jarvis", Context.MODE_PRIVATE)
        alarmsDBDao = AlarmsDB.getInstance(this).alarmsDatabaseDao()

        var now = Calendar.getInstance()
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        alarm = alarmsDBDao.getByDate(now.timeInMillis)
        if (alarm != null && !alarm!!.title.isNullOrEmpty()) alarmTitleTv.text = alarm!!.title

        registerReceiver(mMessageReceiver, IntentFilter("finish_alarm"))

        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (intent.extras?.getBoolean("trip") == true) {
            val serviceIntent = Intent(this, SpeechRecognitionService::class.java)
            serviceIntent.putExtras(intent)
            stopService(Intent(applicationContext, PorcupineService::class.java))
            startService(serviceIntent)
            finish()
        } else {
            sharedPrefs.edit().putBoolean(Constants.INTENT_EXTRA_ALARM_RUNNING, true).apply()
            sharedPrefs.edit().putString(Constants.INTENT_EXTRA_ALARM_RUNNING, alarm?.title).apply()

            if (intent?.getBooleanExtra(Constants.ROUTINE_ALARM, false) == true) {
                sharedPrefs.edit().putString(Constants.ROUTINE_ALARM_COMMANDS, intent.getStringArrayListExtra(Constants.ROUTINE_ALARM_COMMANDS)?.joinToString(", ")).apply()

                while (isMyServiceRunning(SpeechRecognitionService::class.java)) {
                    Thread.sleep(100)
                }
                if (isMyServiceRunning(PorcupineService::class.java)) {
                    stopService(Intent(this, PorcupineService::class.java))
                }
                startForegroundService(Intent(this, PorcupineService::class.java))
            }

            JarvisMediaPlayer.playAudio(
                this,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                raiseVolume = true
            )

            val animation = AnimationUtils.loadAnimation(this, R.anim.bubble_animation)
            val interpolator = MyBounceInterpolator(0.2, 20.0)
            animation.interpolator = interpolator
            animation.repeatCount = ObjectAnimator.INFINITE
            animation.repeatMode = ObjectAnimator.REVERSE
            bubbleBg.startAnimation(animation)

            stopBtn.setOnClickListener {
                JarvisMediaPlayer.stopAudio()
                bubbleBg.clearAnimation()
                if (intent?.getBooleanExtra(Constants.ROUTINE_ALARM, false) == true) {
                    val speechRecIntent = Intent(applicationContext, SpeechRecognitionService::class.java)
                    speechRecIntent.putExtra(Constants.ROUTINE_ALARM, intent.getStringArrayListExtra(Constants.ROUTINE_ALARM_COMMANDS))
                    startForegroundService(speechRecIntent)
                }
                finish()
            }

            snoozeBtn.setOnClickListener {
                JarvisMediaPlayer.stopAudio()
                bubbleBg.clearAnimation()

                val alarmIntent = Intent(
                    applicationContext,
                    ReminderBroadcast::class.java
                )

                val pendingIntent = PendingIntent.getBroadcast(
                    applicationContext,
                    1234567,
                    alarmIntent,
                    0
                )

                now = Calendar.getInstance()
                now.add(Calendar.MINUTE, 5)

                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    now.timeInMillis,
                    pendingIntent
                )
                finish()
            }
        }
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

    override fun onStop() {
        super.onStop()
        unregisterReceiver(mMessageReceiver)
        sharedPrefs.edit().putBoolean("wakeUp", false).apply()
        sharedPrefs.edit().putBoolean(Constants.INTENT_EXTRA_ALARM_RUNNING, false).apply()
        sharedPrefs.edit().putString(Constants.INTENT_EXTRA_ALARM_RUNNING, null).apply()
        if (alarm != null) alarmsDBDao.delete(alarm!!)
    }

    internal inner class MyBounceInterpolator(amplitude: Double, frequency: Double) :
        Interpolator {
        private var mAmplitude = 1.0
        private var mFrequency = 10.0
        override fun getInterpolation(time: Float): Float {
            return (-1 * Math.E.pow(-time / mAmplitude) *
                    cos(mFrequency * time) + 1).toFloat()
        }

        init {
            mAmplitude = amplitude
            mFrequency = frequency
        }
    }
}
