package com.example.jarvisdemo2.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.jarvisdemo2.database.RoutineDB
import com.example.jarvisdemo2.utilities.Constants

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.i("JARVIDEMO2", "Boot receiver triggered")
        // startDateRoutines(context)
    }

    private fun startDateRoutines(context: Context) {
        val routinesDbDAO = RoutineDB.getInstance(context).routinesDatabaseDAO()
        routinesDbDAO.getAll().forEach { routine ->
            if (!routine.triggerDate.isNullOrEmpty()) {
                Constants.setDateRoutine(context, routine)
            }
        }
    }
}