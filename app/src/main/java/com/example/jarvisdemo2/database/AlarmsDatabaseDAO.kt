package com.example.jarvisdemo2.database

import androidx.room.*

@Dao
interface AlarmsDatabaseDAO {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(alarm: Alarm)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(alarm: Alarm)

    @Delete
    fun delete(alarm: Alarm)

    @Query("SELECT * from alarms_database WHERE date = :date")
    fun getByDate(date: Long): Alarm?

    @Query("SELECT * from alarms_database WHERE title = :title")
    fun getByTitle(title: String): Alarm?

    @Query("SELECT * from alarms_database")
    fun getAll(): List<Alarm>
}