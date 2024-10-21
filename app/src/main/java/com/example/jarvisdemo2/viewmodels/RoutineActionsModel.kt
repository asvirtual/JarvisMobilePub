package com.example.jarvisdemo2.viewmodels

import com.example.jarvisdemo2.database.Routine

data class RoutineActionsModel (
    val id: Int,
    var routine: Routine,
    var name: String,
    var icon: Int,
    var action: String
)