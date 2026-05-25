package com.example.pocketnotev20.data.model

data class AdminAccessRequestItem(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val requestedRole: String = "",
    val approvalStatus: String = "",
    val reviewedBy: String = ""
)
