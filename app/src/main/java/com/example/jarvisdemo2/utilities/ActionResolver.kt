package com.example.jarvisdemo2.utilities

import android.content.Context
import android.util.Log
import com.example.jarvisdemo2.database.Routine
import com.example.jarvisdemo2.database.RoutineDB

class ActionResolver(private val jsonRes: Map<String, Any>) {

    fun resolve(confidence: Double = 0.6): String {
        val intents: List<Map<String, Any>> = jsonRes["intents"] as List<Map<String, Any>>
        intents.forEach { intent ->
            if(intent["confidence"].toString().toDouble() > confidence) {
                return when(intent["name"]) {
                    "phone_call" -> Constants.PHONE_CALL
                    "ringer_normal" -> Constants.RINGER_NORMAL
                    "ringer_vibrate" -> Constants.RINGER_VIBRATE
                    "ringer_silent" -> Constants.RINGER_SILENT
                    "volume_mute" -> {
                        if ("chiamata" in jsonRes["text"].toString().toLowerCase()) Constants.VOICE_CALL_MUTE
                        else Constants.VOLUME_MUTE
                    }
                    "volume_unmute" -> {
                        if ("chiamat" in jsonRes["text"].toString().toLowerCase()) Constants.VOICE_CALL_UNMUTE
                        else Constants.VOLUME_UNMUTE
                    }
                    "volume_up" -> {
                        if ("chiamat" in jsonRes["text"].toString().toLowerCase()) Constants.VOICE_CALL_UP
                        else Constants.VOLUME_UP
                    }
                    "volume_down" -> {
                        if ("chiamat" in jsonRes["text"].toString().toLowerCase()) Constants.VOICE_CALL_DOWN
                        else Constants.VOLUME_DOWN
                    }
                    "tell_time" -> Constants.TELL_TIME
                    "tell_date" -> Constants.TELL_DATE
                    "set_timer" -> Constants.SET_TIMER
                    "delete_timer" -> Constants.DELETE_TIMER
                    "end_call" -> Constants.END_CALL
                    "trip" -> Constants.TRIP
                    "general_question" -> Constants.GENERAL_QUESTION
                    "set_reminder" -> Constants.SET_REMINDER
                    "get_weather" -> Constants.GET_WEATHER
                    "math_problem" -> Constants.MATH_PROBLEM
                    else -> Constants.NO_RESULT
                }
            }
        }
        return Constants.NO_RESULT
    }

    fun resolveVoiceRoutine(context: Context, text: String): Routine? {
        val routineDbDao = RoutineDB.getInstance(context).routinesDatabaseDAO()
        routineDbDao.getAll().forEach { routine ->
            if (routine.active)
                routine.triggerPhrase?.forEach { triggerPhrase ->
                    Log.i(
                        "ROUTINES",
                        "resolving voice routine, $triggerPhrase, $text, ${
                            text.contains(triggerPhrase)
                        }"
                    )
                    if (triggerPhrase.isNotEmpty()) {
                        if (text.contains(triggerPhrase)) return routine
                    }
                }
        }
        return null
    }

    fun getEntities(entitiesName: String): List<Map<String, Any>>? {
        return try {
            ((jsonRes["entities"] as Map<String, List<Map<String, String>>>)["wit$$entitiesName:$entitiesName"]) as List<Map<String, Any>>
        } catch (e: Exception) {
            null
        }

    }

    fun getEntity(entityName: String): String ? {
        return try {
            (jsonRes["entities"] as Map<String, List<Map<String, String>>>)["wit$$entityName:$entityName"]?.get(0)?.get("value")?.toLowerCase()
        } catch (e: Exception) {
            null
        }
    }
}