package com.example.jarvisdemo2.database

import android.util.Log
import androidx.room.TypeConverter
import java.util.*
import kotlin.collections.ArrayList

class Converters {

    @TypeConverter
    fun stringListToString(list: ArrayList<String>?): String? {
        if (list == null) return null
        var toReturn = ""
        list.forEach { item ->
            toReturn += "$item,"
        }
        return toReturn
    }

    @TypeConverter
    fun stringToStringList(string: String?): ArrayList<String>? {
        if (string == null) return null
        val toReturn = arrayListOf<String>()
        string.split(",").forEach { element ->
            toReturn.add(element)
        }
        return toReturn
    }

    @TypeConverter
    fun intListToString(list: ArrayList<Int>?): String? {
        if (list == null) return null
        var toReturn = ""
        list.forEach { item ->
            toReturn += "$item,"
        }
        return toReturn
    }

    @TypeConverter
    fun stringToIntList(string: String?): ArrayList<Int>? {
        if (string == null) return null
        val toReturn = arrayListOf<Int>()
        string.split(",").forEach { element ->
            if (element.toIntOrNull() != null)
                toReturn.add(element.toInt())
        }
        return toReturn
    }

    @TypeConverter
    fun calendarToLong(calendar: Calendar?): Long? {
        if (calendar == null) return null
        return calendar.timeInMillis
    }

    @TypeConverter
    fun longToCalendar(long: Long?): Calendar? {
        if (long == null) return null
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = long
        return calendar
    }
}