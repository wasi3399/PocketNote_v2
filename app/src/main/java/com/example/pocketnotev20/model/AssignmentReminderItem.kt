package com.example.pocketnotev20.model

data class AssignmentReminderItem(
    val id: String = "",
    val title: String = "",
    val course: String = "",
    val dueDate: String = "",
    val note: String = "",
    val isDone: Boolean = false
)
