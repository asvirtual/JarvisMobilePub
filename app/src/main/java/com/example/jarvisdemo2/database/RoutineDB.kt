package com.example.jarvisdemo2.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Routine::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RoutineDB: RoomDatabase() {

    abstract fun routinesDatabaseDAO(): RoutinesDatabaseDAO

    companion object {

        @Volatile
        private var INSTANCE: RoutineDB? = null

        fun getInstance(context: Context): RoutineDB {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        RoutineDB::class.java,
                        "routines_database"
                    )
                        .fallbackToDestructiveMigration()
                        .allowMainThreadQueries()
                        //.addCallback(roomCallback)
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}