package com.example.jarvisdemo2.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jarvisdemo2.R
import com.example.jarvisdemo2.database.*
import com.example.jarvisdemo2.utilities.Constants.ROUTINE_VOICE_TRIGGER
import com.example.jarvisdemo2.viewadapters.RoutinesViewAdapter
import com.example.jarvisdemo2.viewmodels.RoutineViewModel
import kotlinx.android.synthetic.main.activity_routines_settings.*
import kotlin.collections.ArrayList

class RoutinesSettingsActivity : AppCompatActivity() {

    private var routines = arrayListOf<Routine>()
    private lateinit var routinesDb: RoutineDB
    private lateinit var routinesDbDao: RoutinesDatabaseDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routines_settings)
        supportActionBar?.hide()

        routinesDb = RoutineDB.getInstance(this)
        routinesDbDao = routinesDb.routinesDatabaseDAO()

        // routinesDbDao.getAll().forEach { routine -> routinesDbDao.delete(routine) }
        if (routinesDbDao.getAll().isEmpty()) {
            listOf(
                Routine(
                    "Buonanotte",
                    true,
                    null,
                    arrayListOf("notte"),
                    null,
                    null,
                    arrayListOf()
                ),
                Routine(
                    "Buongiorno",
                    true,
                    null,
                    arrayListOf("giorno"),
                    null,
                    null,
                    arrayListOf()
                ),
            ).forEach { routine ->
                routinesDbDao.insert(routine)
            }
        }

        routines = routinesDbDao.getAll() as ArrayList<Routine>

        // this creates a vertical layout Manager
        routinesRv.layoutManager = LinearLayoutManager(this)

        // ArrayList of class ItemsViewModel
        val data = ArrayList<RoutineViewModel>()

        routines.forEach { routine: Routine ->
            var icon = R.drawable.ic_default_routine
            if (routine.name.trim().toLowerCase() == "buongiorno") icon = R.drawable.ic_sun
            if (routine.name.trim().toLowerCase() == "buonanotte") icon = R.drawable.ic_moon
            data.add(RoutineViewModel(routine, icon, routine.name))
        }
        // data.add(RoutineViewModel(R.drawable.ic_default_routine, "test"))

        // This will pass the ArrayList to our Adapter
        val adapter = RoutinesViewAdapter(data)

        // Setting the Adapter with the recyclerview
        routinesRv.adapter = adapter

        addRoutineBtn.setOnClickListener {
            startActivity(Intent(this, RoutineActivity::class.java))
        }
    }
}
