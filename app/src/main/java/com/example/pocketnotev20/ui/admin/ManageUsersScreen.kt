package com.example.pocketnotev20.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.model.UserProfileItem
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.ui.common.AppInfoStrip
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPrimaryButton
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold

@Composable
fun ManageUsersScreen(
    onBackClick: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var accounts by remember { mutableStateOf<List<UserProfileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var actionInProgressFor by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingRemoval by remember { mutableStateOf<UserProfileItem?>(null) }

    val refreshAccounts = {
        isLoading = true
        errorMessage = null
        repository.getManageableAccounts(
            onSuccess = {
                accounts = it
                isLoading = false
            },
            onFailure = {
                errorMessage = it.message ?: "Unable to load user accounts."
                isLoading = false
            }
        )
    }

    LaunchedEffect(Unit) {
        refreshAccounts()
    }

    pendingRemoval?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove Account") },
            text = {
                Text(
                    "Remove ${account.name.ifBlank { account.email }} from the app? They will not be able to sign in again."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        actionInProgressFor = account.uid
                        repository.removeManagedAccount(
                            uid = account.uid,
                            onSuccess = {
                                actionInProgressFor = null
                                pendingRemoval = null
                                refreshAccounts()
                            },
                            onFailure = {
                                actionInProgressFor = null
                                pendingRemoval = null
                                errorMessage = it.message ?: "Unable to remove this account."
                            }
                        )
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    val subAdminCount = accounts.count {
        it.role.equals("sub_admin", ignoreCase = true) ||
            it.requestedRole.equals("sub_admin", ignoreCase = true)
    }
    val userCount = accounts.size - subAdminCount

    ProfessionalPageScaffold(
        title = "Manage Accounts",
        subtitle = "See who signed up as a user or sub admin request, then remove app access when needed.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoStrip(label = "Users", value = userCount.toString())
            AppInfoStrip(label = "Sub Admins", value = subAdminCount.toString())
        }

        Spacer(modifier = Modifier.height(18.dp))

        when {
            isLoading -> {
                AppPanelCard {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading signed-up accounts...")
                }
            }

            !errorMessage.isNullOrBlank() -> {
                AppPanelCard {
                    AppSectionTitle(title = "Account Status")
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            accounts.isEmpty() -> {
                AppPanelCard {
                    AppSectionTitle(
                        title = "No Active Accounts",
                        subtitle = "New user and sub admin signups will appear here."
                    )
                }
            }

            else -> {
                AppPanelCard {
                    AppSectionTitle(
                        title = "Signed-Up Accounts",
                        subtitle = "Main admin can review these accounts and remove access if needed."
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                accounts.forEachIndexed { index, account ->
                    ManageUserCard(
                        account = account,
                        isRemoving = actionInProgressFor == account.uid,
                        onRemoveClick = { pendingRemoval = account }
                    )

                    if (index != accounts.lastIndex) {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageUserCard(
    account: UserProfileItem,
    isRemoving: Boolean,
    onRemoveClick: () -> Unit
) {
    AppPanelCard {
        AppSectionTitle(
            title = account.name.ifBlank { "Unnamed Account" },
            subtitle = account.email.ifBlank { "No email found" }
        )

        Spacer(modifier = Modifier.height(14.dp))
        ManageUserInfo(label = "Role", value = managedRoleLabel(account))
        ManageUserInfo(label = "Status", value = managedStatusLabel(account))
        ManageUserInfo(label = "Department", value = account.department.ifBlank { "Not set" })
        ManageUserInfo(label = "Session", value = account.session.ifBlank { "Not set" })
        ManageUserInfo(label = "UID", value = account.uid.ifBlank { "Unavailable" })

        Spacer(modifier = Modifier.height(8.dp))

        AppPrimaryButton(
            text = if (isRemoving) "Removing..." else "Remove Account",
            onClick = onRemoveClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRemoving
        )
    }
}

@Composable
private fun ManageUserInfo(
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
        style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(10.dp))
}

private fun managedRoleLabel(account: UserProfileItem): String {
    return when {
        account.role.equals("sub_admin", ignoreCase = true) -> "Sub Admin"
        account.requestedRole.equals("sub_admin", ignoreCase = true) -> "Sub Admin Request"
        else -> "User"
    }
}

private fun managedStatusLabel(account: UserProfileItem): String {
    return when {
        account.accountStatus.equals("removed", ignoreCase = true) -> "Removed"
        account.approvalStatus.isNotBlank() -> account.approvalStatus.replaceFirstChar { it.uppercase() }
        else -> "Approved"
    }
}
