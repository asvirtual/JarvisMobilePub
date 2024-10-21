package com.example.jarvisdemo2.activities

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jarvisdemo2.R
import com.example.jarvisdemo2.database.Routine
import com.example.jarvisdemo2.database.RoutineDB
import com.example.jarvisdemo2.database.RoutinesDatabaseDAO
import com.example.jarvisdemo2.receivers.ReminderBroadcast
import com.example.jarvisdemo2.utilities.Constants
import com.example.jarvisdemo2.utilities.Constants.deleteAlarm
import com.example.jarvisdemo2.utilities.Constants.makeToast
import com.example.jarvisdemo2.utilities.Constants.setDateRoutine
import com.example.jarvisdemo2.viewadapters.RoutineActionsAdapter
import com.example.jarvisdemo2.viewmodels.RoutineActionsModel
import kotlinx.android.synthetic.main.activity_routine.*
import kotlinx.android.synthetic.main.activity_routine.dateTriggerLl
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class RoutineActivity : AppCompatActivity() {

    private lateinit var routinesDb: RoutineDB
    private lateinit var routinesDbDao: RoutinesDatabaseDAO
    private lateinit var routine: Routine
    private var name = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routine)
        supportActionBar?.hide()

        routinesDb = RoutineDB.getInstance(this)
        routinesDbDao = routinesDb.routinesDatabaseDAO()
        if (intent?.getStringExtra(Constants.ROUTINE_NAME) != null) {
            routine = routinesDbDao.get(intent.getStringExtra(Constants.ROUTINE_NAME)!!)!!
            routineNameEt.setText(routine.name)
            var triggerExists = false
            if (routine.triggerPhrase != null) {
                triggerExists = true
                triggerPhraseTv.text = Constants.fromVoiceTriggerListToString(routine.triggerPhrase!!)
                voiceTriggerLl.visibility = View.VISIBLE
            } else voiceTriggerLl.visibility = View.GONE
            if (routine.triggerDate != null && routine.triggerPeriod != null) {
                triggerExists = true
                var triggerDateText = ""
                triggerDateText += if (routine.triggerPeriod?.contains(0)!!) "Lun, " else ""
                triggerDateText += if (routine.triggerPeriod?.contains(1)!!) "Mar, " else ""
                triggerDateText += if (routine.triggerPeriod?.contains(2)!!) "Mer, " else ""
                triggerDateText += if (routine.triggerPeriod?.contains(3)!!) "Gio, " else ""
                triggerDateText += if (routine.triggerPeriod?.contains(4)!!) "Ven, " else ""
                triggerDateText += if (routine.triggerPeriod?.contains(5)!!) "Sab, " else ""
                triggerDateText += if (routine.triggerPeriod?.contains(6)!!) "Dom, " else ""
                triggerDateText = triggerDateText.substringBeforeLast(",")
                triggerDateTv.text = "${routine.triggerDate} ogni $triggerDateText"
                /*
                    TODO: change storage of period and time in db and display them properly here.
                     Period should be an integer list of weekdays (0 = Monday, 1 = Tuesday... 6 = Sunday)
                     or a boolean list (true = trigger, false = skip), with indexes representing weekdays.
                     Date should be a string hh:mm representing time trigger
                 */
                dateTriggerLl.visibility = View.VISIBLE
            } else dateTriggerLl.visibility = View.GONE
            if (routine.triggerLocation != null) {
                triggerExists = true
                triggerLocationTv.text = routine.triggerLocation
                positionTriggerLl.visibility = View.VISIBLE
            } else positionTriggerLl.visibility = View.GONE
            if (triggerExists) {
                llTriggerSelected.visibility = View.VISIBLE
                llNoTrigger.visibility = View.GONE
            } else {
                llNoTrigger.visibility = View.VISIBLE
                llTriggerSelected.visibility = View.GONE
            }
        } else {
            routine = Routine(
                "Nuova routine ${routinesDbDao.getAll().size}",
                true,
                null,
                null,
                null,
                null,
                arrayListOf(),
            )
            routinesDbDao.insert(routine)
        }

        if (intent?.getSerializableExtra("routineActions") != null) {
            val newAction = Constants.getActionFromHashMap((intent?.getSerializableExtra("routineActions") as HashMap<String, Any>))
            Log.i("ROUTINES", "received new action from prev activity: $newAction")
            var actions = routine.commands
            if (actions != null) actions.add(newAction)
            else actions = arrayListOf(newAction)
            routine.commands = actions
            Log.i("ROUTINES", "new routine actions: ${actions.joinToString(", ")}")

            routinesDbDao.update(routine)
        }
        if (!intent.getStringArrayListExtra(Constants.ROUTINE_VOICE_TRIGGER).isNullOrEmpty()) {
            voiceTriggerLl.visibility = View.VISIBLE
            llTriggerSelected.visibility = View.VISIBLE
            llNoTrigger.visibility = View.GONE

            val voiceTriggers = intent.getStringArrayListExtra(Constants.ROUTINE_VOICE_TRIGGER)!!
            voiceTriggers.forEach { voiceTrigger -> if (voiceTrigger.isNullOrEmpty()) voiceTriggers.remove(voiceTrigger) }
            routine.triggerPhrase = voiceTriggers
            triggerPhraseTv.text = Constants.fromVoiceTriggerListToString(intent.getStringArrayListExtra(Constants.ROUTINE_VOICE_TRIGGER)!!)

            routinesDbDao.update(routine)
        }
        if (!intent.getStringExtra(Constants.ROUTINE_DATE_TRIGGER).isNullOrEmpty()) {
            var text = intent.getStringExtra(Constants.ROUTINE_DATE_TRIGGER)
            routine.triggerDate = text
            routine.triggerPeriod = intent.getIntegerArrayListExtra(Constants.ROUTINE_PERIOD_TRIGGER)

            dateTriggerLl.visibility = View.VISIBLE
            llTriggerSelected.visibility = View.VISIBLE
            llNoTrigger.visibility = View.GONE

            text = ""
            text += if (routine.triggerPeriod?.contains(0)!!) "Lun, " else ""
            text += if (routine.triggerPeriod?.contains(1)!!) "Mar, " else ""
            text += if (routine.triggerPeriod?.contains(2)!!) "Mer, " else ""
            text += if (routine.triggerPeriod?.contains(3)!!) "Gio, " else ""
            text += if (routine.triggerPeriod?.contains(4)!!) "Ven, " else ""
            text += if (routine.triggerPeriod?.contains(5)!!) "Sab, " else ""
            text += if (routine.triggerPeriod?.contains(6)!!) "Dom, " else ""
            text = text?.substringBeforeLast(",")

            triggerDateTv.text = "${routine.triggerDate} ogni $text"

            routinesDbDao.update(routine)
        }

        name = routine.name
        routineNameEt.setText(routine.name)
        routineActiveCb.isChecked = routine.active

        // this creates a vertical layout Manager
        routineActionsRv.layoutManager = LinearLayoutManager(this)

        // ArrayList of class ItemsViewModel
        val data = ArrayList<RoutineActionsModel>()

        routine.commands?.forEach { action ->
            var name = ""
            if (Constants.GET_ROUTINE_ACTIONS_DISPLAY_NAME(action) != null) name = Constants.GET_ROUTINE_ACTIONS_DISPLAY_NAME(action)!!
            else if (action.contains(Constants.ROUTINE_ACTION_SPEAK))
                name = "Dì ${action.split(Constants.ROUTINE_ACTION_SPEAK)[1]}"
            else if (action.contains(Constants.ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE))
                name = "Manda un messaggio a ${getContactName(action.split(Constants.ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE)[0])} con testo \"${action.split(Constants.ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE)[1]}\""
            else if (action.contains(Constants.ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME))
                name = "Imposta il volume dei media a ${action.split(Constants.ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME)[1]}"
            else if (action.contains(Constants.ROUTINE_ACTION_SET_NOTIFICATION_VOLUME))
                name = "Imposta il volume delle notifiche a ${action.split(Constants.ROUTINE_ACTION_SET_NOTIFICATION_VOLUME)[1]}"
            else if (action.contains(Constants.ROUTINE_ACTION_SET_RINGER_VOLUME))
                name = "Imposta il volume della suoneria a ${action.split(Constants.ROUTINE_ACTION_SET_RINGER_VOLUME)[1]}"
            else if (action.contains(Constants.ROUTINE_ACTION_CUSTOM))
                name = "Esegui il comando \"${action.split(Constants.ROUTINE_ACTION_CUSTOM)[1]}\""

            Constants.GET_ROUTINE_ACTIONS_ICON_MAP(action)?.let { icon ->
                RoutineActionsModel(routine.commands!!.indexOf(action), routine, name, icon, action) }?.let {
                data.add(it)
            }
        }

        imgRoutine.setBackgroundResource(when (routine.name) {
            "Buonanotte" -> R.drawable.ic_moon
            "Buongiorno" -> R.drawable.ic_sun
            else -> R.drawable.ic_default_routine
        })

        /*data.add(RoutineActionsModel(R.drawable.ic_volume_off, "Disattiva la suoneria"))
        data.add(RoutineActionsModel(R.drawable.ic_mic_blue, "Dimmi come sarà il meteo domani"))*/

        // This will pass the ArrayList to our Adapter
        val adapter = RoutineActionsAdapter(data)

        // Setting the Adapter with the recyclerview
        routineActionsRv.adapter = adapter

        routineActiveCb.setOnCheckedChangeListener { _, checked ->
            routine.active = checked
            routinesDbDao.update(routine)
        }

        addTriggerBtn.setOnClickListener {
            val activityIntent = Intent(this, RoutineTriggersActivity::class.java)
            if (routine.triggerDate != null) {
                activityIntent.putExtra(Constants.ROUTINE_DATE_TRIGGER, routine.triggerDate)
                activityIntent.putExtra(Constants.ROUTINE_PERIOD_TRIGGER, routine.triggerPeriod)
            }
            if (routine.triggerPhrase != null) activityIntent.putExtra(Constants.ROUTINE_VOICE_TRIGGER, routine.triggerPhrase)
            if (routine.triggerLocation != null) activityIntent.putExtra(Constants.ROUTINE_LOCATION_TRIGGER, routine.triggerLocation)
            activityIntent.putExtra(Constants.ROUTINE_ADD_TRIGGER, true)
            activityIntent.putExtras(intent)
            startActivity(activityIntent)
            finish()
        }

        dateTriggerLl.setOnClickListener {
            val activityIntent = Intent(this, RoutineTriggersActivity::class.java)
            activityIntent.putExtras(intent)
            activityIntent.putExtra(Constants.ROUTINE_DATE_TRIGGER, routine.triggerDate)
            activityIntent.putExtra(Constants.ROUTINE_PERIOD_TRIGGER, routine.triggerPeriod)
            startActivity(activityIntent)
            finish()
        }

        dateTriggerDeleteBtn.setOnClickListener {
            routine.triggerDate = null
            routine.triggerPeriod = null
            routinesDbDao.update(routine)
            dateTriggerLl.visibility = View.GONE
            if (routine.triggerPhrase == null && routine.triggerLocation == null) llNoTrigger.visibility = View.VISIBLE
        }

        voiceTriggerLl.setOnClickListener {
            val activityIntent = Intent(this, RoutineTriggersActivity::class.java)
            activityIntent.putExtras(intent)
            activityIntent.putExtra(Constants.ROUTINE_VOICE_TRIGGER, routine.triggerPhrase)
            startActivity(activityIntent)
            finish()
        }

        voiceTriggerDeleteBtn.setOnClickListener {
            routine.triggerPhrase = null
            routinesDbDao.update(routine)
            voiceTriggerLl.visibility = View.GONE
            if (routine.triggerDate == null && routine.triggerLocation == null) llNoTrigger.visibility = View.VISIBLE
        }

        positionTriggerLl.setOnClickListener {
            val activityIntent = Intent(this, RoutineTriggersActivity::class.java)
            activityIntent.putExtras(intent)
            activityIntent.putExtra(Constants.ROUTINE_LOCATION_TRIGGER, routine.triggerLocation)
            startActivity(activityIntent)
            finish()
        }

        positionTriggerDeleteBtn.setOnClickListener {
            routine.triggerLocation = null
            routinesDbDao.update(routine)
            positionTriggerLl.visibility = View.GONE
            if (routine.triggerDate == null && routine.triggerPhrase == null) llNoTrigger.visibility = View.VISIBLE
        }

        addActionBtn.setOnClickListener {
            val activityIntent = Intent(this, RoutineActionsActivity::class.java)
            var validName = true
            if (name != routine.name) routinesDbDao.getAll().forEach { if (it.name.toLowerCase() == name.toLowerCase()) validName = false }
            if (validName) {
                if (name != routine.name) {
                    deleteAlarm(this, routine)
                    routinesDbDao.delete(routine)
                    routine.name = name
                    routinesDbDao.insert(routine)
                    if (!routine.triggerDate.isNullOrEmpty()) setDateRoutine(this, routine)
                }
                activityIntent.putExtra(Constants.ROUTINE_NAME, routine.name)
                startActivity(activityIntent)
                finish()
            } else makeToast(this, "Il nome inserito appartiene già a un'altra routine. Prima di continuare cambialo")
        }

        btnClose.setOnClickListener {
            var validName = true
            if (name != routine.name)
                routinesDbDao.getAll().forEach { if (it.name.toLowerCase() == name.toLowerCase()) validName = false }
            Log.i("ROUTINES", "validName: $validName, routine name: ${routine.name}")
            if (validName) {
                if (name != routine.name) {
                    deleteAlarm(this, routine)
                    routinesDbDao.delete(routine)
                    routine.name = name
                    routinesDbDao.insert(routine)
                }
                val activityIntent = Intent(this, RoutinesSettingsActivity::class.java)
                activityIntent.putExtra(
                    Constants.ROUTINE_NAME,
                    routine.name
                )

                if (!routine.triggerDate.isNullOrEmpty()) setDateRoutine(this, routine)

                startActivity(activityIntent)
                finish()
            } else makeToast(this, "Il nome inserito appartiene già a un'altra routine")
        }

        routineNameEt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(newText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                Log.i("ROUTINES", "text changing to ${newText.toString()}")
                name = newText.toString()
                Log.i("ROUTINES", "text changed ${routine.name}")
            }

            override fun afterTextChanged(p0: Editable?) {}
        })
    }

    private fun getContactName(number: String): String {
        var contactName: String
        var contactNumber: String

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val selection = ContactsContract.Contacts.HAS_PHONE_NUMBER
        val cursor = contentResolver?.query(
            uri, arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ), selection, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

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
        return number
    }
}
