package com.example.pocketnotev20.data.model

data class RoutineItem(
    val id: String = "",
    val day: String = "",
    val level: String = "",
    val term: String = "",
    val classes: List<String> = emptyList()
)