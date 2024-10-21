package com.example.jarvisdemo2.viewmodels

import com.example.jarvisdemo2.database.Routine

data class RoutineViewModel (
    val routine: Routine,
    val icon: Int,
    val name: String,
)