package com.example.jarvisdemo2.activities

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.allViews
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jarvisdemo2.R
import com.example.jarvisdemo2.utilities.Constants
import com.example.jarvisdemo2.utilities.Constants.makeToast
import com.example.jarvisdemo2.viewadapters.RoutineTriggersAdapter
import kotlinx.android.synthetic.main.activity_routine.*
import kotlinx.android.synthetic.main.activity_routine_triggers.*
import kotlinx.android.synthetic.main.activity_routine_triggers.dateTriggerLl
import kotlinx.android.synthetic.main.voice_trigger_layout.*
import kotlinx.android.synthetic.main.voice_trigger_layout.view.*
import java.util.*


class RoutineTriggersActivity : AppCompatActivity(), View.OnClickListener {

    private var triggers: ArrayList<String> = arrayListOf()
    private lateinit var daysViewsIds: ArrayList<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routine_triggers)
        supportActionBar?.hide()

        setVoiceTriggerRl.visibility = View.GONE
        setDateTriggerRl.visibility = View.GONE
        setLocationTriggerRl.visibility = View.GONE
        chooseTriggerTypeRl.visibility = View.VISIBLE

        triggerTypeChoiceDateLl.isClickable = true
        triggerTypeChoiceLocationLl.isClickable = true
        triggerTypeChoiceVoiceLl.isClickable = true

        dateTriggerMonTv.setOnClickListener(this)
        dateTriggerTueTv.setOnClickListener(this)
        dateTriggerWedTv.setOnClickListener(this)
        dateTriggerThuTv.setOnClickListener(this)
        dateTriggerFriTv.setOnClickListener(this)
        dateTriggerSatTv.setOnClickListener(this)
        dateTriggerSunTv.setOnClickListener(this)
        dateTriggerLl.setOnClickListener(this)
        triggerTypeChoiceVoiceLl.setOnClickListener(this)
        triggerTypeChoiceDateLl.setOnClickListener(this)
        triggerTypeChoiceLocationLl.setOnClickListener(this)
        routineTriggerBtnClose.setOnClickListener(this)
        voiceTriggerAddSentenceBtn.setOnClickListener(this)
        btnVoiceTriggerFinished.setOnClickListener(this)
        btnDateTriggerFinished.setOnClickListener(this)

        daysViewsIds = arrayListOf(
            R.id.dateTriggerMonTv,
            R.id.dateTriggerTueTv,
            R.id.dateTriggerWedTv,
            R.id.dateTriggerThuTv,
            R.id.dateTriggerFriTv,
            R.id.dateTriggerSatTv,
            R.id.dateTriggerSunTv,
        )

        if (!intent.getBooleanExtra(Constants.ROUTINE_ADD_TRIGGER, false)) {
            if (intent.getStringExtra(Constants.ROUTINE_DATE_TRIGGER) != null) {
                chooseTriggerTypeRl.visibility = View.GONE
                setDateTriggerRl.visibility = View.VISIBLE
                dateTriggerTv.text = intent.getStringExtra(Constants.ROUTINE_DATE_TRIGGER)
                val weekdaysTrigger =
                    intent.getIntegerArrayListExtra(Constants.ROUTINE_PERIOD_TRIGGER)
                if (weekdaysTrigger != null) {
                    if (weekdaysTrigger.contains(0)) dateTriggerMonTv.setBackgroundResource(R.drawable.circle_background_blue_filled)
                    if (weekdaysTrigger.contains(1)) dateTriggerTueTv.setBackgroundResource(R.drawable.circle_background_blue_filled)
                    if (weekdaysTrigger.contains(2)) dateTriggerWedTv.setBackgroundResource(R.drawable.circle_background_blue_filled)
                    if (weekdaysTrigger.contains(3)) dateTriggerThuTv.setBackgroundResource(R.drawable.circle_background_blue_filled)
                    if (weekdaysTrigger.contains(4)) dateTriggerFriTv.setBackgroundResource(R.drawable.circle_background_blue_filled)
                    if (weekdaysTrigger.contains(5)) dateTriggerSatTv.setBackgroundResource(R.drawable.circle_background_blue_filled)
                    if (weekdaysTrigger.contains(6)) dateTriggerSunTv.setBackgroundResource(R.drawable.circle_background_blue_filled)
                }
            } else if (!intent.getStringArrayListExtra(Constants.ROUTINE_VOICE_TRIGGER).isNullOrEmpty()) {
                chooseTriggerTypeRl.visibility = View.GONE
                setVoiceTriggerRl.visibility = View.VISIBLE
                intent.getStringArrayListExtra(Constants.ROUTINE_VOICE_TRIGGER)!!.forEach { voiceTrigger ->
                    if (!voiceTrigger.isNullOrEmpty()) triggers.add(voiceTrigger)
                }
            } else if (intent.getStringExtra(Constants.ROUTINE_LOCATION_TRIGGER) != null) {
                chooseTriggerTypeRl.visibility = View.GONE
                setLocationTriggerRl.visibility = View.VISIBLE
                triggerLocationTv.text = intent.getStringExtra(Constants.ROUTINE_LOCATION_TRIGGER)
            }
        } else {
            if (intent.getStringExtra(Constants.ROUTINE_DATE_TRIGGER) != null) triggerTypeChoiceDateLl.visibility = View.GONE
            if (!intent.getStringArrayListExtra(Constants.ROUTINE_VOICE_TRIGGER).isNullOrEmpty()) triggerTypeChoiceVoiceLl.visibility = View.GONE
            if (intent.getStringExtra(Constants.ROUTINE_LOCATION_TRIGGER) != null) triggerTypeChoiceLocationLl.visibility = View.GONE
        }

        // this creates a vertical layout Manager
        voiceTriggersRv.layoutManager = LinearLayoutManager(this)

        // This will pass the ArrayList to our Adapter
        val adapter = RoutineTriggersAdapter(triggers)

        // Setting the Adapter with the recyclerview
        voiceTriggersRv.adapter = adapter
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            in (daysViewsIds) -> {
                if (view?.background?.constantState == resources.getDrawable(R.drawable.circle_background_blue_filled).constantState) {
                    view?.setBackgroundResource(R.drawable.circle_background_blue)
                } else {
                    view?.setBackgroundResource(R.drawable.circle_background_blue_filled)
                }
            }

            R.id.dateTriggerLl -> {
                pickTime()
            }

            R.id.triggerTypeChoiceVoiceLl -> {
                chooseTriggerTypeRl.visibility = View.GONE
                setDateTriggerRl.visibility = View.GONE
                setLocationTriggerRl.visibility = View.GONE

                setVoiceTriggerRl.visibility = View.VISIBLE
            }

            R.id.triggerTypeChoiceDateLl -> {
                chooseTriggerTypeRl.visibility = View.GONE
                setVoiceTriggerRl.visibility = View.GONE
                setLocationTriggerRl.visibility = View.GONE

                setDateTriggerRl.visibility = View.VISIBLE
            }

            R.id.triggerTypeChoiceLocationLl -> {
                chooseTriggerTypeRl.visibility = View.GONE
                setVoiceTriggerRl.visibility = View.GONE
                setDateTriggerRl.visibility = View.GONE

                setLocationTriggerRl.visibility = View.VISIBLE
            }

            R.id.voiceTriggerAddSentenceBtn -> {
                triggers.add("")
                voiceTriggersRv.adapter?.notifyItemInserted(triggers.size - 1)
            }

            R.id.routineTriggerBtnClose -> {
                if (chooseTriggerTypeRl.visibility == View.GONE && intent.getBooleanExtra(Constants.ROUTINE_ADD_TRIGGER, false)) {
                    setVoiceTriggerRl.visibility = View.GONE
                    setDateTriggerRl.visibility = View.GONE
                    setLocationTriggerRl.visibility = View.GONE
                    chooseTriggerTypeRl.visibility = View.VISIBLE

                    // btnVoiceTriggerFinished.setBackgroundResource(R.drawable.rounded_button_background_deactivated)
                } else {
                    val activityIntent = Intent(this, RoutineActivity::class.java)
                    activityIntent.putExtra(Constants.ROUTINE_NAME, intent.getStringExtra(Constants.ROUTINE_NAME))
                    startActivity(activityIntent)
                    finish()
                }
            }

            R.id.btnVoiceTriggerFinished -> {
                if (voiceTriggersRv.childCount > 0) {
                    val triggers = arrayListOf<String>()
                    var brokeLoop = false
                    for (i in 0..voiceTriggersRv.childCount) {
                        try {
                            val holder = voiceTriggersRv.getChildViewHolder(voiceTriggersRv.getChildAt(i))
                            if (holder.itemView.voiceTriggerEt.text.toString().isNotEmpty()) {
                                triggers.add(holder.itemView.voiceTriggerEt.text.toString())
                            } else {
                                makeToast(this, "Devi riempire tutti i campi, o eliminare quelli vuoti")
                                brokeLoop = true
                                break
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (!brokeLoop) {
                        /* collect result */
                        val activityIntent = Intent(this, RoutineActivity::class.java)
                        activityIntent.putExtra(Constants.ROUTINE_NAME, intent.getStringExtra(Constants.ROUTINE_NAME))
                        activityIntent.putExtra(Constants.ROUTINE_VOICE_TRIGGER, triggers)
                        startActivity(activityIntent)
                        finish()
                    }
                }
            }

            R.id.btnDateTriggerFinished -> {
                val daysSelected = arrayListOf<Int>()
                daysViewsIds.forEach { id ->
                    if (findViewById<TextView>(id).background.constantState == resources.getDrawable(R.drawable.circle_background_blue_filled).constantState)
                        daysSelected.add(daysViewsIds.indexOf(id))
                }
                if (daysSelected.isNotEmpty()) {
                    val strTime = findViewById<TextView>(R.id.dateTriggerTv).text.toString()

                    val activityIntent = Intent(this, RoutineActivity::class.java)
                    activityIntent.putExtra(Constants.ROUTINE_NAME, intent.getStringExtra(Constants.ROUTINE_NAME))
                    activityIntent.putExtra(Constants.ROUTINE_DATE_TRIGGER, strTime)
                    activityIntent.putExtra(Constants.ROUTINE_PERIOD_TRIGGER, daysSelected)
                    startActivity(activityIntent)
                    finish()
                } else makeToast(this, "Seleziona almeno un giorno")
            }
        }
    }

    private fun pickTime() {
        val currentDateTime = Calendar.getInstance()
        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, hour, minute ->
            dateTriggerTv.text = "${formatTime(hour.toString())}:${formatTime(minute.toString())}"
            // TODO set hour and minute to textview
        }, startHour, startMinute, true).show()
    }

    private fun formatTime(timeStr: String): String {
        return if (timeStr.length == 1) "0$timeStr"
        else timeStr
    }

    private fun pixelsToDP(dps: Int): Int {
        val scale = this.resources.displayMetrics.density
        return (dps * scale).toInt()
    }
}
