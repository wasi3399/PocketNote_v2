package com.example.pocketnotev20.model

data class QuestionItem(
    val id: String = "",
    val subject: String = "",
    val level: String = "",
    val term: String = "",
    val session: String = "",
    val years: List<String> = emptyList()
)