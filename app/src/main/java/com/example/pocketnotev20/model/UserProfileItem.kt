package com.example.pocketnotev20.model

data class UserProfileItem(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val idNumber: String = "",
    val phone: String = "",
    val department: String = "",
    val session: String = "",
    val role: String = "user",
    val requestedRole: String = "",
    val approvalStatus: String = "",
    val reviewedBy: String = "",
    val accountStatus: String = "active",
    val removedBy: String = ""
)