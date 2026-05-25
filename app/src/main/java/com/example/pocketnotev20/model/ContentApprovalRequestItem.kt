package com.example.pocketnotev20.data.model

data class ContentApprovalRequestItem(
    val id: String = "",
    val requestType: String = "",
    val summary: String = "",
    val submittedByUid: String = "",
    val submittedByName: String = "",
    val submittedByEmail: String = "",
    val approvalStatus: String = "",
    val reviewedBy: String = "",
    val payload: Map<String, Any?> = emptyMap()
)
