package com.example.pocketnotev20.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.model.AdminAccessRequestItem
import com.example.pocketnotev20.model.ContentApprovalRequestItem
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.ui.common.AppInfoStrip
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold

@Composable
fun SubAdminStatusScreen(
    onBackClick: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var requestInfo by remember { mutableStateOf<AdminAccessRequestItem?>(null) }
    var contentRequests by remember { mutableStateOf(listOf<ContentApprovalRequestItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.getCurrentUserAdminAccessStatus(
            onSuccess = {
                requestInfo = it
                repository.getCurrentUserContentRequests(
                    onSuccess = { requests ->
                        contentRequests = requests
                        isLoading = false
                    },
                    onFailure = {
                        errorMessage = it.message ?: "Unable to load content request updates."
                        isLoading = false
                    }
                )
            },
            onFailure = {
                errorMessage = it.message ?: "Unable to load request status."
                isLoading = false
            }
        )
    }

    ProfessionalPageScaffold(
        title = "Sub Admin Status",
        subtitle = "Track whether your admin access request is pending, approved, or rejected.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        if (isLoading) {
            AppPanelCard {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text("Loading request status...")
            }
            return@ProfessionalPageScaffold
        }

        if (!errorMessage.isNullOrBlank()) {
            AppPanelCard {
                AppSectionTitle(title = "Status Error")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@ProfessionalPageScaffold
        }

        val status = requestInfo?.approvalStatus?.ifBlank { "pending" } ?: "pending"
        val roleText = if (requestInfo?.requestedRole == "sub_admin") {
            "sub_admin"
        } else {
            requestInfo?.role?.ifBlank { "user" } ?: "user"
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppInfoStrip(label = "Request", value = statusLabel(status))
                AppInfoStrip(label = "Role", value = roleLabel(roleText))
            }
            if (contentRequests.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppInfoStrip(label = "Content", value = overallContentStatus(contentRequests))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        AppPanelCard {
            AppSectionTitle(
                title = "Access Review",
                subtitle = requestInfo?.email ?: "No account email found."
            )
            Spacer(modifier = Modifier.height(12.dp))
            StatusField(label = "Current Status", value = statusLabel(status), color = statusColor(status))
            StatusField(label = "Requested Role", value = "Sub Admin")
            StatusField(label = "Reviewed By", value = requestInfo?.reviewedBy?.ifBlank { "Pending main admin review" } ?: "Pending main admin review")
        }

        Spacer(modifier = Modifier.height(18.dp))

        AppPanelCard {
            AppSectionTitle(
                title = "Content Request Updates",
                subtitle = "See whether your submitted data is pending, approved, or rejected by the main admin."
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (contentRequests.isEmpty()) {
                Text(
                    text = "You have not submitted any content approval requests yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                contentRequests.forEachIndexed { index, request ->
                    StatusField(
                        label = requestTypeLabel(request.requestType),
                        value = request.summary.ifBlank { "No summary available" }
                    )
                    StatusField(
                        label = "Approval Status",
                        value = statusLabel(request.approvalStatus),
                        color = statusColor(request.approvalStatus)
                    )
                    StatusField(
                        label = "Reviewed By",
                        value = request.reviewedBy.ifBlank { "Pending main admin review" }
                    )

                    if (index != contentRequests.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusField(
    label: String,
    value: String,
    color: Color? = null
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
        color = color ?: MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(12.dp))
}

private fun statusLabel(status: String): String {
    return when (status.lowercase()) {
        "approved" -> "Approved"
        "rejected" -> "Rejected"
        else -> "Pending"
    }
}

private fun statusColor(status: String): Color {
    return when (status.lowercase()) {
        "approved" -> Color(0xFF16A34A)
        "rejected" -> Color(0xFFDC2626)
        else -> Color(0xFFF59E0B)
    }
}

private fun requestTypeLabel(requestType: String): String {
    return when (requestType) {
        "question" -> "Question Bank Data"
        "note" -> "Note"
        "routine" -> "Routine"
        "calendar" -> "Calendar Event"
        "ct_slot" -> "CT Slot"
        else -> requestType.ifBlank { "Content Request" }
    }
}

private fun overallContentStatus(requests: List<ContentApprovalRequestItem>): String {
    return when {
        requests.any { it.approvalStatus.equals("pending", ignoreCase = true) } -> "Pending"
        requests.any { it.approvalStatus.equals("rejected", ignoreCase = true) } -> "Rejected"
        requests.any { it.approvalStatus.equals("approved", ignoreCase = true) } -> "Approved"
        else -> "Unknown"
    }
}

private fun roleLabel(role: String): String {
    return when (role.lowercase()) {
        "sub_admin" -> "Sub Admin"
        "admin" -> "Admin"
        "user" -> "User"
        else -> role.replaceFirstChar { it.uppercase() }
    }
}
