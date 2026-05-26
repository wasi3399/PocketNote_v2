package com.example.pocketnotev20.model

data class RoutineItem(
    val id: String = "",
    val day: String = "",
    val level: String = "",
    val term: String = "",
    val section: String = "",
    val classes: List<String> = emptyList(),
    val uploadName: String = "",
    val fileUrl: String = "",
    val storagePath: String = "",
    val fileType: String = ""
)
