package com.example.jarvisdemo2.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Alarm::class], version = 3, exportSchema = false)
abstract class AlarmsDB: RoomDatabase() {

    abstract fun alarmsDatabaseDao(): AlarmsDatabaseDAO

    companion object {

        @Volatile
        private var INSTANCE: AlarmsDB? = null

        fun getInstance(context: Context): AlarmsDB {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AlarmsDB::class.java,
                        "alarms_database"
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