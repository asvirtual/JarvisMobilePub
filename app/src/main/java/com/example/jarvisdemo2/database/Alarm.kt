package com.example.jarvisdemo2.database

import androidx.room.*

@Entity(tableName = "alarms_database")
data class Alarm (
    @PrimaryKey()
    var id: Int,

    @ColumnInfo(name="date")
    var date: Long,
    @ColumnInfo(name="title")
    var title: String? = null,
)