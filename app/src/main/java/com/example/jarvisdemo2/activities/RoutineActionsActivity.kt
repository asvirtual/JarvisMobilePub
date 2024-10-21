package com.example.jarvisdemo2.activities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.room.util.CursorUtil.getColumnIndex
import com.example.jarvisdemo2.R
import com.example.jarvisdemo2.database.Routine
import com.example.jarvisdemo2.database.RoutineDB
import com.example.jarvisdemo2.database.RoutinesDatabaseDAO
import com.example.jarvisdemo2.utilities.Constants
import com.example.jarvisdemo2.utilities.Constants.makeToast
import kotlinx.android.synthetic.main.activity_routine_actions.*


class RoutineActionsActivity : AppCompatActivity() {

    private var routineActions: HashMap<String, Any> = hashMapOf()
    private lateinit var routine: Routine
    private lateinit var routineDbDao: RoutinesDatabaseDAO
    private var checkboxList = arrayListOf<CompoundButton>()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routine_actions)
        supportActionBar?.hide()

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        routineDbDao = RoutineDB.getInstance(this).routinesDatabaseDAO()
        if (!intent.getStringExtra(Constants.ROUTINE_NAME).isNullOrEmpty()) {
            routine = routineDbDao.get(intent.getStringExtra(Constants.ROUTINE_NAME)!!)!!
        } else {
            makeToast(this, "Routine non trovata nel database")
            finish()
        }
        resetActions()

        checkboxList = arrayListOf(
            routineActionRingRingerCb,
            routineActionVibrateRingerCb,
            routineActionMuteRingerCb,
            routineActionSpeakCb,
            routineActionSendWhatsappMessageCb,
            routineActionMessageComingHomeCb,
            routineActionTellDateTimeCb,
            routineActionDndOnCb,
            routineActionDndOffCb,
            routineActionEventsCb,
            routineActionWeatherCb,
            mediaVolumeCb,
            notificationVolumeCb,
            ringVolumeCb,
            routineActionStartListeningCb,
            routineActionStopListeningCb,
            routineActionWifiOnCb,
            routineActionWifiOffCb,
            routineActionBluetoothOffCb,
            routineActionBluetoothOnCb,
        )

        actionCommunicationsLl.setOnClickListener {
            chooseRoutineActionCategoryRl.visibility = View.GONE
            editRoutineActionCommunicationsRl.visibility = View.VISIBLE
            if (routine.triggerDate == null) routineActionSetAlarmCb.visibility = View.GONE
            else routineActionSetAlarmCb.visibility = View.VISIBLE
            actionFinishedBtn.visibility = View.VISIBLE
        }

        actionAnnouncementsLl.setOnClickListener {
            chooseRoutineActionCategoryRl.visibility = View.GONE
            editRoutineActionAnnouncementsRl.visibility = View.VISIBLE
            actionFinishedBtn.visibility = View.VISIBLE
        }

        actionVolumeLl.setOnClickListener {
            chooseRoutineActionCategoryRl.visibility = View.GONE
            editRoutineActionVolumeRl.visibility = View.VISIBLE
            actionFinishedBtn.visibility = View.VISIBLE
        }

        actionPhoneSettingsLl.setOnClickListener {
            chooseRoutineActionCategoryRl.visibility = View.GONE
            editRoutineActionPhoneSettingsRl.visibility = View.VISIBLE
            actionFinishedBtn.visibility = View.VISIBLE
        }

        actionConnectivitySettingsLl.setOnClickListener {
            chooseRoutineActionCategoryRl.visibility = View.GONE
            editRoutineActionConnectivitySettingsRl.visibility = View.VISIBLE
            actionFinishedBtn.visibility = View.VISIBLE
        }

        actionAssistantSettingsLl.setOnClickListener {
            chooseRoutineActionCategoryRl.visibility = View.GONE
            editRoutineActionAssistantSettingsRl.visibility = View.VISIBLE
            actionFinishedBtn.visibility = View.VISIBLE
        }

        actionCustomLl.setOnClickListener {
            chooseRoutineActionCategoryRl.visibility = View.GONE
            editRoutineActionCustomRl.visibility = View.VISIBLE
            actionFinishedBtn.visibility = View.VISIBLE
        }

        routineActionSendWhatsappMessageOptionsIb.setOnClickListener {
            editRoutineActionAnnouncementsRl.visibility = View.GONE
            actionWhatsappMessageOptionsLl.visibility = View.VISIBLE
        }

        routineActionSpeakOptionsIb.setOnClickListener {
            editRoutineActionAnnouncementsRl.visibility = View.GONE
            actionSpeakOptionsLl.visibility = View.VISIBLE
        }

        routineActionDndOnCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_SET_DND_ON] = checked
        }

        routineActionDndOffCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_SET_DND_OFF] = checked
        }

        routineActionRingRingerCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_SET_RINGER_ON] = checked
        }

        routineActionVibrateRingerCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_SET_RINGER_VIBRATION] = checked
        }

        routineActionMuteRingerCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_SET_RINGER_MUTE] = checked
        }

        routineActionBluetoothOnCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_SET_BLUETOOTH_ON] = checked
        }

        routineActionBluetoothOffCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_SET_BLUETOOTH_OFF] = checked
        }

        routineActionWifiOnCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_SET_WIFI_ON] = checked
        }

        routineActionWifiOffCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_SET_WIFI_OFF] = checked
        }

        notificationVolumeSb.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        ringVolumeSb.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        mediaVolumeSb.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        notificationVolumeSb.min = audioManager.getStreamMinVolume(AudioManager.STREAM_NOTIFICATION)
        ringVolumeSb.min = audioManager.getStreamMinVolume(AudioManager.STREAM_RING)
        mediaVolumeSb.min = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)

        notificationVolumeSb.progress = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        ringVolumeSb.progress = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        mediaVolumeSb.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        notificationVolumeTv.text = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION).toString()
        ringVolumeTv.text = audioManager.getStreamVolume(AudioManager.STREAM_RING).toString()
        mediaVolumeTv.text = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toString()

        notificationVolumeSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                notificationVolumeTv.text = progress.toString()
                (routineActions[Constants.ROUTINE_ACTION_SET_NOTIFICATION_VOLUME] as HashMap<String, Any>)["volume"] =
                    progress.toString()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        ringVolumeSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                ringVolumeTv.text = progress.toString()
                (routineActions[Constants.ROUTINE_ACTION_SET_RINGER_VOLUME] as HashMap<String, Any>)["volume"] =
                    progress.toString()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        mediaVolumeSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mediaVolumeTv.text = progress.toString()
                Log.i("ROUTINES", "Changed media volume to $progress")
                (routineActions[Constants.ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME] as HashMap<String, Any>)["volume"] =
                    progress.toString()
                Log.i(
                    "ROUTINES",
                    "New action hashmap: ${routineActions.entries.joinToString(", ")}"
                )
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        routineActionSpeakCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            (routineActions[Constants.ROUTINE_ACTION_SPEAK] as HashMap<String, Any>)["checked"] = checked
        }

        routineActionSendWhatsappMessageCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            (routineActions[Constants.ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE] as HashMap<String, Any>)["checked"] = checked
        }

        routineActionWeatherCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_TELL_WEATHER] = checked
        }

        routineActionEventsCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_TELL_EVENTS] = checked
        }

        routineActionTellDateTimeCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_TELL_DATE_TIME] = checked
        }

        routineActionMessageComingHomeCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_MESSAGE_COMING_HOME] = checked
        }

        mediaVolumeCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            (routineActions[Constants.ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME] as HashMap<String, Any>)["checked"] = checked
            (routineActions[Constants.ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME] as HashMap<String, Any>)["volume"] = mediaVolumeSb.progress.toString()
        }

        ringVolumeCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            (routineActions[Constants.ROUTINE_ACTION_SET_RINGER_VOLUME] as HashMap<String, Any>)["checked"] = checked
            (routineActions[Constants.ROUTINE_ACTION_SET_RINGER_VOLUME] as HashMap<String, Any>)["volume"] = ringVolumeSb.progress.toString()
        }

        notificationVolumeCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            (routineActions[Constants.ROUTINE_ACTION_SET_NOTIFICATION_VOLUME] as HashMap<String, Any>)["checked"] = checked
            (routineActions[Constants.ROUTINE_ACTION_SET_NOTIFICATION_VOLUME] as HashMap<String, Any>)["volume"] = notificationVolumeSb.progress.toString()
        }

        routineActionSetAlarmCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_RING_ALARM] = checked
        }

        routineActionStopListeningCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_STOP_LISTENING] = checked
        }

        routineActionStartListeningCb.setOnCheckedChangeListener { checkbox, checked ->
            uncheckOthers(checkbox)
            routineActions[Constants.ROUTINE_ACTION_START_LISTENING] = checked
        }

        actionSpeakEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(newText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                (routineActions[Constants.ROUTINE_ACTION_SPEAK] as HashMap<String, Any>)["text"] =
                    if (newText.toString().isNotEmpty()) newText.toString()
                    else false
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

        actionWhatsappMessageNumberEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(newText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                (routineActions[Constants.ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE] as HashMap<String, Any>)["number"] =
                    if (newText.toString().isNotEmpty()) newText.toString()
                    else false
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

        actionWhatsappMessageBodyEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(newText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                (routineActions[Constants.ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE] as HashMap<String, Any>)["body"] =
                    if (newText.toString().isNotEmpty()) newText.toString()
                    else false
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

        actionCustomEt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(newText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                routineActions[Constants.ROUTINE_ACTION_CUSTOM] = newText.toString()
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

        actionWhatsappMessagePickContactIb.setOnClickListener {
            val contactPickerIntent = Intent(
                Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            )
            startActivityForResult(contactPickerIntent, Constants.PICK_CONTACT_INTENT_RESULT)
        }

        actionInfoStopListeningIb.setOnClickListener {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.action_info_dialog_layout)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val body = dialog.findViewById(R.id.dialogBody) as TextView
            body.text = "Termina il servizio di riconoscimento vocale. Può essere utile in periodi nei quali l'assistente è inutilizzato per risparmiare la batteria del dispositivo"
            val closeBtn = dialog.findViewById(R.id.closeDialogBtn) as TextView
            closeBtn.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }

        actionInfoStartListeningIb.setOnClickListener {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.action_info_dialog_layout)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val body = dialog.findViewById(R.id.dialogBody) as TextView
            body.text = "Riavvia il servizio di riconoscimento vocale."
            val closeBtn = dialog.findViewById(R.id.closeDialogBtn) as TextView
            closeBtn.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }

        actionFinishedBtn.setOnClickListener {
            if (checkValid()) {
                val activityIntent = Intent(this, RoutineActivity::class.java)
                activityIntent.putExtra("routineActions", routineActions)
                activityIntent.putExtra(
                    Constants.ROUTINE_NAME,
                    intent.getStringExtra(Constants.ROUTINE_NAME)
                )
                startActivity(activityIntent)
                finish()
            }
        }

        backBtn.setOnClickListener {
            if (chooseRoutineActionCategoryRl.visibility == View.VISIBLE) {
                val activityIntent = Intent(this, RoutineActivity::class.java)
                activityIntent.putExtra(
                    Constants.ROUTINE_NAME,
                    intent.getStringExtra(Constants.ROUTINE_NAME)
                )
                startActivity(activityIntent)
                finish()
            } else {
                when (View.VISIBLE) {
                    editRoutineActionCommunicationsRl.visibility -> {
                        editRoutineActionCommunicationsRl.visibility = View.GONE
                        chooseRoutineActionCategoryRl.visibility = View.VISIBLE
                        actionFinishedBtn.visibility = View.GONE
                    }
                    editRoutineActionAnnouncementsRl.visibility -> {
                        editRoutineActionAnnouncementsRl.visibility = View.GONE
                        chooseRoutineActionCategoryRl.visibility = View.VISIBLE
                        actionFinishedBtn.visibility = View.GONE
                    }
                    editRoutineActionVolumeRl.visibility -> {
                        editRoutineActionVolumeRl.visibility = View.GONE
                        chooseRoutineActionCategoryRl.visibility = View.VISIBLE
                        actionFinishedBtn.visibility = View.GONE
                    }
                    editRoutineActionPhoneSettingsRl.visibility -> {
                        editRoutineActionPhoneSettingsRl.visibility = View.GONE
                        chooseRoutineActionCategoryRl.visibility = View.VISIBLE
                        actionFinishedBtn.visibility = View.GONE
                    }
                    editRoutineActionConnectivitySettingsRl.visibility -> {
                        editRoutineActionConnectivitySettingsRl.visibility = View.GONE
                        chooseRoutineActionCategoryRl.visibility = View.VISIBLE
                        actionFinishedBtn.visibility = View.GONE
                    }
                    editRoutineActionAssistantSettingsRl.visibility -> {
                        editRoutineActionAssistantSettingsRl.visibility = View.GONE
                        chooseRoutineActionCategoryRl.visibility = View.VISIBLE
                        actionFinishedBtn.visibility = View.GONE
                    }
                    editRoutineActionCustomRl.visibility -> {
                        editRoutineActionCustomRl.visibility = View.GONE
                        chooseRoutineActionCategoryRl.visibility = View.VISIBLE
                        actionFinishedBtn.visibility = View.GONE
                    }
                    actionWhatsappMessageOptionsLl.visibility -> {
                        actionWhatsappMessageOptionsLl.visibility = View.GONE
                        editRoutineActionAnnouncementsRl.visibility = View.VISIBLE
                    }
                    actionSpeakOptionsLl.visibility -> {
                        actionSpeakOptionsLl.visibility = View.GONE
                        editRoutineActionAnnouncementsRl.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun uncheckOthers(clickedCheckbox: CompoundButton) {
        resetActions()
        checkboxList.filter{ it.id != clickedCheckbox.id }.forEach { it.isChecked = false }
    }

    private fun resetActions() {
        routineActions = hashMapOf(
            Constants.ROUTINE_ACTION_TELL_WEATHER to false,
            Constants.ROUTINE_ACTION_TELL_EVENTS to false,
            Constants.ROUTINE_ACTION_TELL_DATE_TIME to false,
            Constants.ROUTINE_ACTION_MESSAGE_COMING_HOME to false,
            Constants.ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE to hashMapOf(
                "checked" to false,
                "body" to false,
                "number" to false
            ),
            Constants.ROUTINE_ACTION_SPEAK to hashMapOf(
                "checked" to false,
                "text" to ""
            ),
            Constants.ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME to hashMapOf(
                "checked" to false,
                "volume" to false
            ),
            Constants.ROUTINE_ACTION_SET_RINGER_VOLUME to hashMapOf(
                "checked" to false,
                "volume" to false
            ),
            Constants.ROUTINE_ACTION_SET_NOTIFICATION_VOLUME to hashMapOf(
                "checked" to false,
                "volume" to false
            ),
            Constants.ROUTINE_ACTION_SET_RINGER_MUTE to false,
            Constants.ROUTINE_ACTION_SET_RINGER_VIBRATION to false,
            Constants.ROUTINE_ACTION_SET_RINGER_ON to false,
            Constants.ROUTINE_ACTION_SET_DND_ON to false,
            Constants.ROUTINE_ACTION_SET_DND_OFF to false,
            Constants.ROUTINE_ACTION_RING_ALARM to false,
            Constants.ROUTINE_ACTION_SET_BLUETOOTH_ON to false,
            Constants.ROUTINE_ACTION_SET_BLUETOOTH_OFF to false,
            Constants.ROUTINE_ACTION_SET_WIFI_ON to false,
            Constants.ROUTINE_ACTION_SET_WIFI_OFF to false,
            Constants.ROUTINE_ACTION_START_LISTENING to false,
            Constants.ROUTINE_ACTION_STOP_LISTENING to false,
            Constants.ROUTINE_ACTION_CUSTOM to "",
        )
    }

    private fun checkValid(): Boolean {
        return when (true) {
            routineActionSendWhatsappMessageCb.isChecked -> !actionWhatsappMessageBodyEt.text.isNullOrEmpty() && !actionWhatsappMessageNumberEt.text.isNullOrEmpty()
            routineActionSpeakCb.isChecked -> !actionSpeakEt.text.isNullOrEmpty()
            else -> true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            // Check for the request code, we might be usign multiple startActivityForResult
            when (requestCode) {
                Constants.PICK_CONTACT_INTENT_RESULT -> {
                    val contactData: Uri? = data!!.data
                    val cursor: Cursor? = contactData?.let { contentResolver.query(it, null, null, null, null) }
                    if (cursor!!.moveToFirst()) {
                        val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val phoneName: String = cursor.getString(phoneIndex)
                        actionWhatsappMessageNumberEt.setText(phoneName)
                    }
                }
            }
        } else {
            Log.e("ROUTINES", "Failed to pick contact")
        }
    }
}
