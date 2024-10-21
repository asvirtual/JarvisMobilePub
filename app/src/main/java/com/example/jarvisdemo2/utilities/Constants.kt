package com.example.jarvisdemo2.utilities

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.jarvisdemo2.R
import com.example.jarvisdemo2.database.Routine
import com.example.jarvisdemo2.receivers.ReminderBroadcast
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object Constants {
    const val PHONE_CALL = "phone_call"
    const val NO_RESULT = "no_result"
    const val RINGER_VIBRATE = "ringer_vibrate"
    const val RINGER_SILENT = "ringer_silent"
    const val RINGER_NORMAL = "ringer_normal"
    const val VOLUME_DOWN = "volume_down"
    const val VOLUME_UP = "volume_up"
    const val VOLUME_MUTE = "volume_mute"
    const val VOLUME_UNMUTE = "volume_unmute"
    const val VOICE_CALL_DOWN = "call_down"
    const val VOICE_CALL_UP = "call_up"
    const val VOICE_CALL_MUTE = "call_mute"
    const val VOICE_CALL_UNMUTE = "call_unmute"
    const val TELL_TIME = "tell_time"
    const val TELL_DATE = "tell_date"
    const val SET_TIMER = "set_timer"
    const val END_CALL = "end_call"
    const val TRIP = "trip"
    const val GENERAL_QUESTION = "general_question"
    const val SET_REMINDER = "set_reminder"
    const val GET_WEATHER = "get_weather"
    const val MATH_PROBLEM = "math_problem"
    const val DELETE_TIMER = "delete_timer"

    const val INTERACTION_CONFIRM_REMINDER = "interaction_confirm_reminder"
    const val INTERACTION_CONFIRM_CALL = "interaction_confirm_call"
    const val INTERACTION_SET_ALARM = "interaction_set_alarm"
    const val INTERACTION_SET_TRIP_REMINDER = "interaction_set_trip_reminder"

    const val HOTWORD_RECOGNITION_SERVICE_RUNNING = "hotword_recognition_service_running"

    const val WEATHER_API_KEY = ""
    const val GEOCODING_API_KEY = ""

    val MONTHS = arrayListOf("gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno", "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre")

    /**
     * INTENT EXTRAS
     */
    const val INTENT_EXTRA_ALARM_TITLE = "intent_extra_alarm_title"
    const val INTENT_EXTRA_ALARM_RUNNING = "intent_extra_alarm_running"

    /**
     * SPOTIFY
     */
    const val SPOTIFY_CLIENT_ID = ""
    const val SPOTIFY_REDIRECT_URI = "https://www.google.com"
    const val SPOTIFY_CLIENT_SECRET = ""

    const val SPOTIFY_ENCODED_CLIENT_ID_SECRET = "" // https://www.base64encode.org/ <SPOTIFY_CLIENT_ID:SPOTIFY_CLIENT_SECRET>

    fun makeToast(context: Context, text: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, text, duration).show()
    }

    /**
     * INTENTS CONSTANTS
     */

    fun encodeStringToInt(string: String): Int {
        var toReturn = 0
        string.forEach { char ->
            toReturn += char.toInt()
        }
        return toReturn
    }

    val PICK_CONTACT_INTENT_RESULT = encodeStringToInt("pick_contact_intent")

    /**
     * ROUTINES
     */

    const val ROUTINE_ALARM = "routine_alarm"
    const val ROUTINE_ALARM_COMMANDS = "routine_alarm_commands"
    const val ROUTINE_COMMANDS_LEFT = "routine_commands_left"
    const val ROUTINE_COMMANDS_SEPARATOR = "-----routine_commands_separator-----"

    const val WHATSAPP_MESSAGE_SUFFIX = "whatsapp_message_jarvis_suffix"

    /**
     * ROUTINES DATABASE
     */

    const val ROUTINE_VOICE_TRIGGER = "routine_voice_trigger"
    const val ROUTINE_DATE_TRIGGER = "routine_time_trigger"
    const val ROUTINE_PERIOD_TRIGGER = "routine_period_trigger"
    const val ROUTINE_LOCATION_TRIGGER = "routine_location_trigger"
    const val ROUTINE_ADD_TRIGGER = "routine_add_trigger"

    /**
     * ROUTINES ACTIONS
     */
    const val ROUTINE_ACTION_TELL_WEATHER = "routine_action_tell_weather"
    const val ROUTINE_ACTION_TELL_EVENTS = "routine_action_tell_events"
    const val ROUTINE_ACTION_TELL_DATE_TIME = "routine_action_tell_date_time"
    const val ROUTINE_ACTION_MESSAGE_COMING_HOME = "routine_action_message_coming_home"
    const val ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE = "routine_action_send_whatsapp_message"
    const val ROUTINE_ACTION_SPEAK = "routine_action_speak"
    const val ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME = "routine_action_set_multimedia_volume"
    const val ROUTINE_ACTION_SET_RINGER_VOLUME = "routine_action_set_ringer_volume"
    const val ROUTINE_ACTION_SET_NOTIFICATION_VOLUME = "routine_action_set_notification_volume"
    const val ROUTINE_ACTION_SET_RINGER_MUTE = "routine_action_set_ringer_mute"
    const val ROUTINE_ACTION_SET_RINGER_VIBRATION = "routine_action_set_ringer_vibration"
    const val ROUTINE_ACTION_SET_RINGER_ON = "routine_action_set_ringer_on"
    const val ROUTINE_ACTION_SET_DND_ON = "routine_action_set_dnd_on"
    const val ROUTINE_ACTION_SET_DND_OFF = "routine_action_dnd_off"
    const val ROUTINE_ACTION_RING_ALARM = "routine_action_ring_alarm"
    const val ROUTINE_ACTION_SET_BLUETOOTH_ON = "routine_action_set_bluetooth_on"
    const val ROUTINE_ACTION_SET_BLUETOOTH_OFF = "routine_action_set_bluetooth_off"
    const val ROUTINE_ACTION_SET_WIFI_ON = "routine_action_set_wifi_on"
    const val ROUTINE_ACTION_SET_WIFI_OFF = "routine_action_set_wifi_off"
    const val ROUTINE_ACTION_STOP_LISTENING = "routine_action_stop_listening"
    const val ROUTINE_ACTION_START_LISTENING = "routine_action_start_listening"
    const val ROUTINE_ACTION_CUSTOM = "routine_action_custom"

    const val ROUTINE_NAME = "routine_name"

    fun GET_ROUTINE_ACTIONS_ICON_MAP(action: String): Int? {
        if (action.contains(ROUTINE_ACTION_TELL_WEATHER)) return R.drawable.ic_announce
        else if (action.contains(ROUTINE_ACTION_TELL_EVENTS)) return R.drawable.ic_announce
        else if (action.contains(ROUTINE_ACTION_TELL_WEATHER)) return R.drawable.ic_announce
        else if (action.contains(ROUTINE_ACTION_TELL_DATE_TIME)) return R.drawable.ic_announce
        else if (action.contains(ROUTINE_ACTION_MESSAGE_COMING_HOME)) return R.drawable.ic_message
        else if (action.contains(ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE)) return R.drawable.ic_message
        else if (action.contains(ROUTINE_ACTION_SPEAK)) return R.drawable.ic_message
        else if (action.contains(ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME)) return R.drawable.ic_volume
        else if (action.contains(ROUTINE_ACTION_SET_RINGER_VOLUME)) return R.drawable.ic_volume
        else if (action.contains(ROUTINE_ACTION_SET_NOTIFICATION_VOLUME)) return  R.drawable.ic_volume
        else if (action.contains(ROUTINE_ACTION_SET_RINGER_MUTE)) return R.drawable.ic_phone
        else if (action.contains(ROUTINE_ACTION_SET_RINGER_VIBRATION)) return R.drawable.ic_phone
        else if (action.contains(ROUTINE_ACTION_SET_RINGER_ON)) return  R.drawable.ic_phone
        else if (action.contains(ROUTINE_ACTION_SET_DND_ON)) return R.drawable.ic_phone
        else if (action.contains(ROUTINE_ACTION_SET_DND_OFF)) return R.drawable.ic_phone
        else if (action.contains(ROUTINE_ACTION_RING_ALARM)) return R.drawable.ic_announce
        else if (action.contains(ROUTINE_ACTION_SET_BLUETOOTH_ON)) return R.drawable.ic_network_check
        else if (action.contains(ROUTINE_ACTION_SET_BLUETOOTH_OFF)) return R.drawable.ic_network_check
        else if (action.contains(ROUTINE_ACTION_SET_WIFI_ON)) return R.drawable.ic_network_check
        else if (action.contains(ROUTINE_ACTION_SET_WIFI_OFF)) return R.drawable.ic_network_check
        else if (action.contains(ROUTINE_ACTION_STOP_LISTENING)) return R.drawable.ic_default_routine
        else if (action.contains(ROUTINE_ACTION_START_LISTENING)) return R.drawable.ic_default_routine
        else if (action.contains(ROUTINE_ACTION_CUSTOM)) return R.drawable.icon_jarvis_blue
        else return null
    }

    fun GET_ROUTINE_ACTIONS_DISPLAY_NAME(action: String): String? {
        return when (action) {
            ROUTINE_ACTION_TELL_WEATHER -> "Informa sul meteo"
            ROUTINE_ACTION_TELL_EVENTS -> "Elenca gli impegni di oggi"
            ROUTINE_ACTION_TELL_DATE_TIME -> "Dì data e ora"
            ROUTINE_ACTION_MESSAGE_COMING_HOME -> "Dì che sto arrivando a casa a tutti"
            ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME -> "Regola il volume della musica"
            ROUTINE_ACTION_SET_RINGER_VOLUME -> "Regola il volume della suoneria"
            ROUTINE_ACTION_SET_NOTIFICATION_VOLUME -> "Regola il volume delle notifiche"
            ROUTINE_ACTION_SET_RINGER_MUTE -> "Disattiva la suoneria"
            ROUTINE_ACTION_SET_RINGER_VIBRATION -> "Metti il telefono in vibrazione"
            ROUTINE_ACTION_SET_RINGER_ON -> "Attiva la suoneria"
            ROUTINE_ACTION_SET_DND_ON -> "Attiva la modalità non disturbare"
            ROUTINE_ACTION_SET_DND_OFF -> "Disattiva la modalità non disturbare"
            ROUTINE_ACTION_RING_ALARM -> "Fai suonare una sveglia"
            ROUTINE_ACTION_SET_BLUETOOTH_ON -> "Accendi il Bluetooth"
            ROUTINE_ACTION_SET_BLUETOOTH_OFF -> "Spegni il Bluetooth"
            ROUTINE_ACTION_SET_WIFI_ON -> "Accendi il Wi-Fi"
            ROUTINE_ACTION_SET_WIFI_OFF -> "Spegni il Wi-Fi"
            ROUTINE_ACTION_STOP_LISTENING -> "Termina il riconoscimento vocale in background"
            ROUTINE_ACTION_START_LISTENING -> "Avvia il riconoscimento vocale in background"
            else -> null
        }
    }

    fun getActionFromHashMap(hashmap: HashMap<String, Any>): String {
        var toReturn = ""
        if (hashmap[ROUTINE_ACTION_TELL_WEATHER] == true) toReturn = ROUTINE_ACTION_TELL_WEATHER
        if (hashmap[ROUTINE_ACTION_TELL_EVENTS] == true) toReturn = ROUTINE_ACTION_TELL_EVENTS
        if (hashmap[ROUTINE_ACTION_TELL_DATE_TIME] == true) toReturn = ROUTINE_ACTION_TELL_DATE_TIME
        if (hashmap[ROUTINE_ACTION_MESSAGE_COMING_HOME] == true) toReturn = ROUTINE_ACTION_MESSAGE_COMING_HOME
        if ((hashmap[ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE] as HashMap<String, Any>)["checked"] == true)
            toReturn = "${(hashmap[ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE] as HashMap<String, Any>)["number"]}$ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE${(hashmap[ROUTINE_ACTION_SEND_WHATSAPP_MESSAGE] as HashMap<String, Any>)["body"]}"
        if ((hashmap[ROUTINE_ACTION_SPEAK] as HashMap<String, Any>)["checked"] == true) toReturn = "$ROUTINE_ACTION_SPEAK${(hashmap[ROUTINE_ACTION_SPEAK] as HashMap<String, Any>)["text"]}"
        if ((hashmap[ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME] as HashMap<String, Any>)["checked"] == true) toReturn = "$ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME${(hashmap[ROUTINE_ACTION_SET_MULTIMEDIA_VOLUME] as HashMap<String, Any>)["volume"].toString()}"
        if ((hashmap[ROUTINE_ACTION_SET_RINGER_VOLUME] as HashMap<String, Any>)["checked"] == true) toReturn = "$ROUTINE_ACTION_SET_RINGER_VOLUME${(hashmap[ROUTINE_ACTION_SET_RINGER_VOLUME] as HashMap<String, Any>)["volume"].toString()}"
        if ((hashmap[ROUTINE_ACTION_SET_NOTIFICATION_VOLUME] as HashMap<String, Any>)["checked"] == true) toReturn = "$ROUTINE_ACTION_SET_NOTIFICATION_VOLUME${(hashmap[ROUTINE_ACTION_SET_NOTIFICATION_VOLUME] as HashMap<String, Any>)["volume"].toString()}"
        if (hashmap[ROUTINE_ACTION_SET_RINGER_MUTE] == true) toReturn = ROUTINE_ACTION_SET_RINGER_MUTE
        if (hashmap[ROUTINE_ACTION_SET_RINGER_VIBRATION] == true) toReturn = ROUTINE_ACTION_SET_RINGER_VIBRATION
        if (hashmap[ROUTINE_ACTION_SET_RINGER_ON] == true) toReturn = ROUTINE_ACTION_SET_RINGER_ON
        if (hashmap[ROUTINE_ACTION_SET_DND_ON] == true) toReturn = ROUTINE_ACTION_SET_DND_ON
        if (hashmap[ROUTINE_ACTION_SET_DND_OFF] == true) toReturn = ROUTINE_ACTION_SET_DND_OFF
        if (hashmap[ROUTINE_ACTION_RING_ALARM] == true) toReturn = ROUTINE_ACTION_RING_ALARM
        if (hashmap[ROUTINE_ACTION_SET_BLUETOOTH_ON] == true) toReturn = ROUTINE_ACTION_SET_BLUETOOTH_ON
        if (hashmap[ROUTINE_ACTION_SET_BLUETOOTH_OFF] == true) toReturn = ROUTINE_ACTION_SET_BLUETOOTH_OFF
        if (hashmap[ROUTINE_ACTION_SET_WIFI_ON] == true) toReturn = ROUTINE_ACTION_SET_WIFI_ON
        if (hashmap[ROUTINE_ACTION_SET_WIFI_OFF] == true) toReturn = ROUTINE_ACTION_SET_WIFI_OFF
        if (hashmap[ROUTINE_ACTION_STOP_LISTENING] == true) toReturn = ROUTINE_ACTION_STOP_LISTENING
        if (hashmap[ROUTINE_ACTION_START_LISTENING] == true) toReturn = ROUTINE_ACTION_START_LISTENING
        if (hashmap[ROUTINE_ACTION_CUSTOM].toString().isNotEmpty()) toReturn = "$ROUTINE_ACTION_CUSTOM${hashmap[ROUTINE_ACTION_CUSTOM]}"

        return toReturn
    }

    fun fromVoiceTriggerListToString(voiceTriggerList: ArrayList<String>): String {
        var toReturn = ""
        // voiceTriggerList.removeLast()
        voiceTriggerList.forEach { voiceTrigger ->
            if (voiceTrigger.isNotEmpty()) {
                if (toReturn.isEmpty()) toReturn = "\"$voiceTrigger\""
                else toReturn += " o \"$voiceTrigger\""
            }
        }

        return toReturn
    }

    fun setDateRoutine(context: Context, routine: Routine) {
        val date = Calendar.getInstance()
        val triggerHour = Calendar.getInstance()
        val nextTrigger: Calendar

        Log.i("ROUTINES", "today's weekday: ${date.get(Calendar.DAY_OF_WEEK)}, ${Calendar.SATURDAY}")

        triggerHour.set(
            date.get(Calendar.YEAR),
            date.get(Calendar.MONTH),
            date.get(Calendar.DAY_OF_MONTH),
            routine.triggerDate!!.split(":")[0].toInt(),
            routine.triggerDate!!.split(":")[1].toInt(),
            0
        )

        Log.i("ROUTINES", "triggerHour: ${routine.triggerDate}")

        /**
         * Mon: 0
         * Tue: 1
         * ...
         * Sun: 6
         */

        /**
         * Calendar:
         * Sun: 1
         * Mon: 2
         * ...
         * Sat: 7
         */

        fun isRoutineWeekDay(date: Calendar, triggerWeekdays: java.util.ArrayList<Int>): Boolean {
            triggerWeekdays.forEach { triggerWeekday ->
                val calendarWeekDay = routineWeekdayToCalendarWeekday(triggerWeekday)
                if (date.get(Calendar.DAY_OF_WEEK) == calendarWeekDay) return true
            }
            return false
        }

        nextTrigger =
            if (date.before(triggerHour) && isRoutineWeekDay(triggerHour, routine.triggerPeriod!!)) {
                triggerHour
            }
            else {
                date.set(
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH),
                    date.get(Calendar.DAY_OF_MONTH),
                    routine.triggerDate!!.split(":")[0].toInt(),
                    routine.triggerDate!!.split(":")[1].toInt(),
                    0
                )
                date.add(Calendar.DAY_OF_YEAR, 1)
                while (!isRoutineWeekDay(date, routine.triggerPeriod!!)) date.add(Calendar.DAY_OF_YEAR, 1)
                date
            }

        Log.i("ROUTINES", "nextTrigger for routine ${routine.name}: ${nextTrigger.get(Calendar.DAY_OF_MONTH)}/${MONTHS[nextTrigger.get(Calendar.MONTH)]}/${nextTrigger.get(Calendar.YEAR)} ${nextTrigger.get(Calendar.HOUR)}:${nextTrigger.get(Calendar.MINUTE)}")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val alarmIntent = Intent(
            context,
            ReminderBroadcast::class.java
        )

        alarmIntent.putExtra(ROUTINE_ALARM, routine.commands)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            encodeStringToInt(routine.name),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            nextTrigger.timeInMillis,
            pendingIntent
        )
    }

    fun deleteAlarm(context: Context, routine: Routine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val alarmIntent = Intent(
            context,
            ReminderBroadcast::class.java
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            encodeStringToInt(routine.name),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)
    }

    fun routineWeekdayToCalendarWeekday(routineWeekday: Int): Int {
        return when (routineWeekday) {
            0 -> Calendar.MONDAY
            1 -> Calendar.TUESDAY
            2 -> Calendar.WEDNESDAY
            3 -> Calendar.THURSDAY
            4 -> Calendar.FRIDAY
            5 -> Calendar.SATURDAY
            6 -> Calendar.SUNDAY
            else -> 999
        }
    }
}