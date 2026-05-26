package com.example.pocketnotev20.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.model.AdminAccessRequestItem
import com.example.pocketnotev20.model.ContentApprovalRequestItem
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPrimaryButton
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.AppSecondaryButton
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold

@Composable
fun SubAdminRequestsScreen(
    onBackClick: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var accessRequests by remember { mutableStateOf(listOf<AdminAccessRequestItem>()) }
    var contentRequests by remember { mutableStateOf(listOf<ContentApprovalRequestItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var actionInProgressFor by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val refreshRequests = {
        isLoading = true
        errorMessage = null
        repository.getPendingSubAdminRequests(
            onSuccess = {
                accessRequests = it
                repository.getPendingContentRequests(
                    onSuccess = { contentItems ->
                        contentRequests = contentItems
                        isLoading = false
                    },
                    onFailure = {
                        errorMessage = it.message ?: "Failed to load content requests."
                        isLoading = false
                    }
                )
            },
            onFailure = {
                errorMessage = it.message ?: "Failed to load admin notifications."
                isLoading = false
            }
        )
    }

    LaunchedEffect(Unit) {
        refreshRequests()
    }

    ProfessionalPageScaffold(
        title = "Admin Notifications",
        subtitle = "Review new sub admin requests and submitted content from sub admins before it goes live.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        if (isLoading) {
            AppPanelCard {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text("Loading pending requests...")
            }
            return@ProfessionalPageScaffold
        }

        if (!errorMessage.isNullOrBlank()) {
            AppPanelCard {
                AppSectionTitle(title = "Request Status")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (accessRequests.isEmpty() && contentRequests.isEmpty()) {
            AppPanelCard {
                AppSectionTitle(title = "Pending Notifications")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No pending sub admin or content approval requests right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@ProfessionalPageScaffold
        }

        if (accessRequests.isNotEmpty()) {
            AppPanelCard {
                AppSectionTitle(
                    title = "Sub Admin Access Requests",
                    subtitle = "Approve or reject new admin sign-ups that are asking for sub admin access."
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            accessRequests.forEachIndexed { index, request ->
                AppPanelCard {
                    AppSectionTitle(
                        title = request.name.ifBlank { "Unnamed Request" },
                        subtitle = request.email
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    RequestInfo(label = "Requested Access", value = "Sub Admin")
                    RequestInfo(label = "Current Role", value = request.role.ifBlank { "User" })
                    RequestInfo(label = "Status", value = request.approvalStatus.ifBlank { "pending" })

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppPrimaryButton(
                            text = "Approve",
                            onClick = {
                                actionInProgressFor = "access_${request.uid}"
                                repository.updateSubAdminRequest(
                                    uid = request.uid,
                                    approve = true,
                                    onSuccess = {
                                        actionInProgressFor = null
                                        refreshRequests()
                                    },
                                    onFailure = {
                                        actionInProgressFor = null
                                        errorMessage = it.message ?: "Failed to approve request."
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = actionInProgressFor != "access_${request.uid}"
                        )
                        AppSecondaryButton(
                            text = "Reject",
                            onClick = {
                                actionInProgressFor = "access_${request.uid}"
                                repository.updateSubAdminRequest(
                                    uid = request.uid,
                                    approve = false,
                                    onSuccess = {
                                        actionInProgressFor = null
                                        refreshRequests()
                                    },
                                    onFailure = {
                                        actionInProgressFor = null
                                        errorMessage = it.message ?: "Failed to reject request."
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = actionInProgressFor != "access_${request.uid}"
                        )
                    }
                }

                if (index != accessRequests.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (accessRequests.isNotEmpty() && contentRequests.isNotEmpty()) {
            Spacer(modifier = Modifier.height(18.dp))
        }

        if (contentRequests.isNotEmpty()) {
            AppPanelCard {
                AppSectionTitle(
                    title = "Content Approval Requests",
                    subtitle = "Review what type of content a sub admin submitted before publishing it."
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            contentRequests.forEachIndexed { index, request ->
                AppPanelCard {
                    AppSectionTitle(
                        title = request.submittedByName.ifBlank { "Sub Admin" },
                        subtitle = request.submittedByEmail
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    RequestInfo(label = "Content Type", value = requestTypeLabel(request.requestType))
                    RequestInfo(label = "Submitted Data", value = request.summary.ifBlank { "No summary available" })
                    RequestInfo(label = "Status", value = request.approvalStatus.ifBlank { "pending" })

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppPrimaryButton(
                            text = "Approve",
                            onClick = {
                                actionInProgressFor = "content_${request.id}"
                                repository.updateContentRequest(
                                    requestId = request.id,
                                    approve = true,
                                    onSuccess = {
                                        actionInProgressFor = null
                                        refreshRequests()
                                    },
                                    onFailure = {
                                        actionInProgressFor = null
                                        errorMessage = it.message ?: "Failed to approve content request."
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = actionInProgressFor != "content_${request.id}"
                        )
                        AppSecondaryButton(
                            text = "Reject",
                            onClick = {
                                actionInProgressFor = "content_${request.id}"
                                repository.updateContentRequest(
                                    requestId = request.id,
                                    approve = false,
                                    onSuccess = {
                                        actionInProgressFor = null
                                        refreshRequests()
                                    },
                                    onFailure = {
                                        actionInProgressFor = null
                                        errorMessage = it.message ?: "Failed to reject content request."
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = actionInProgressFor != "content_${request.id}"
                        )
                    }
                }

                if (index != contentRequests.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RequestInfo(
    label: String,
    value: String
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(12.dp))
}

private fun requestTypeLabel(requestType: String): String {
    return when (requestType) {
        "question" -> "Question Bank Data"
        "note" -> "Note"
        "routine" -> "Routine"
        "calendar" -> "Calendar Event"
        "ct_slot" -> "CT Slot"
        else -> requestType.ifBlank { "Unknown" }
    }
}
