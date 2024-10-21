package com.example.jarvisdemo2.database

import androidx.room.*

@Dao
interface RoutinesDatabaseDAO {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(routine: Routine)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(routine: Routine)

    @Delete
    fun delete(routine: Routine)

    @Query("SELECT * from routines_database WHERE name = :name")
    fun get(name: String): Routine?

    @Query("SELECT * from routines_database")
    fun getAll(): List<Routine>
}