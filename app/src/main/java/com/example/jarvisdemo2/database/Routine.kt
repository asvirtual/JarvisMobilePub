package com.example.jarvisdemo2.database

import androidx.room.*
import java.util.*

@Entity(tableName = "routines_database")
data class Routine (
    @PrimaryKey
    var name: String,

    @ColumnInfo(name="active")
    var active: Boolean = true,
    @ColumnInfo(name="trigger_location")
    var triggerLocation: String? = null,
    @ColumnInfo(name="trigger_phrase")
    var triggerPhrase: ArrayList<String>? = null,
    @ColumnInfo(name="trigger_date")
    var triggerDate: String? = null,
    @ColumnInfo(name="trigger_period")
    var triggerPeriod: ArrayList<Int>? = null,
    @ColumnInfo(name="commands")
    var commands: ArrayList<String>? = null,
)