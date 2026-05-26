package com.example.pocketnotev20.ui.profile

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.model.UserProfileItem
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.ui.common.AppInfoStrip
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPrimaryButton
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.AppSecondaryButton
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    reserveBottomBarSpace: Boolean = false
) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
    }
    val repository = remember { FirestoreRepository() }
    var profile by remember { mutableStateOf<UserProfileItem?>(null) }
    var editableProfile by remember { mutableStateOf(UserProfileItem()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        repository.getCurrentUserProfile(
            onSuccess = { loadedProfile ->
                profile = loadedProfile
                editableProfile = loadedProfile
                isLoading = false
            },
            onFailure = { exception ->
                errorMessage = exception.message ?: "Unable to load profile."
                isLoading = false
            }
        )
    }

    ProfessionalPageScaffold(
        title = "Profile",
        subtitle = "Review and edit your account details, including ID number, name, email, and the rest of your saved profile information.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = reserveBottomBarSpace
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoStrip(
                label = "Role",
                value = profile?.let(::resolveRoleLabel) ?: "Loading"
            )
            AppInfoStrip(
                label = "Status",
                value = profile?.let(::resolveApprovalLabel) ?: "Loading"
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        AppPanelCard {
            AppSectionTitle(title = "Account Information")
            Spacer(modifier = Modifier.height(12.dp))
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Loading profile...",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (profile == null) {
                Text(
                    text = "Profile data is not available right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ProfileEditor(
                    profile = editableProfile,
                    isEditing = isEditing,
                    isSaving = isSaving,
                    onProfileChange = { editableProfile = it },
                    onEditClick = {
                        editableProfile = profile ?: editableProfile
                        isEditing = true
                        successMessage = null
                    },
                    onCancelClick = {
                        editableProfile = profile ?: editableProfile
                        isEditing = false
                        errorMessage = null
                        successMessage = null
                    },
                    onSaveClick = {
                        isSaving = true
                        errorMessage = null
                        successMessage = null
                        val previousProfile = profile
                        repository.updateCurrentUserProfile(
                            profile = editableProfile,
                            onSuccess = {
                                val updatedProfile = editableProfile.copy(
                                    uid = previousProfile?.uid.orEmpty().ifBlank { editableProfile.uid }
                                )
                                if (previousProfile != null) {
                                    syncSavedLoginEmails(
                                        sharedPreferences = sharedPreferences,
                                        oldEmail = previousProfile.email,
                                        newEmail = updatedProfile.email
                                    )
                                }
                                profile = updatedProfile
                                editableProfile = updatedProfile
                                isSaving = false
                                isEditing = false
                                successMessage = "Profile updated successfully."
                            },
                            onFailure = { exception ->
                                isSaving = false
                                errorMessage = exception.message ?: "Unable to save profile."
                            }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppSecondaryButton(
            text = "Logout from Account",
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth()
        )

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            AppPanelCard {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (!successMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            AppPanelCard {
                Text(
                    text = successMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ProfileEditor(
    profile: UserProfileItem,
    isEditing: Boolean,
    isSaving: Boolean,
    onProfileChange: (UserProfileItem) -> Unit,
    onEditClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = profile.name,
            onValueChange = { onProfileChange(profile.copy(name = it)) },
            label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = profile.email,
            onValueChange = { onProfileChange(profile.copy(email = it)) },
            label = { Text("Email address") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = profile.idNumber,
            onValueChange = { onProfileChange(profile.copy(idNumber = it)) },
            label = { Text("ID number") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = profile.phone,
            onValueChange = { onProfileChange(profile.copy(phone = it)) },
            label = { Text("Phone number") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = profile.department,
            onValueChange = { onProfileChange(profile.copy(department = it)) },
            label = { Text("Department") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = profile.session,
            onValueChange = { onProfileChange(profile.copy(session = it)) },
            label = { Text("Session") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (isEditing) {
            AppPrimaryButton(
                text = if (isSaving) "Saving..." else "Save Changes",
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppSecondaryButton(
                text = "Cancel",
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
        } else {
            AppPrimaryButton(
                text = "Edit Profile",
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProfileField(
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
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(12.dp))
}

private fun resolveRoleLabel(profile: UserProfileItem): String {
    return when {
        profile.role == "admin" -> "Main Admin"
        profile.role == "sub_admin" -> "Sub Admin"
        profile.requestedRole == "sub_admin" && profile.approvalStatus.equals("pending", ignoreCase = true) -> "Sub Admin Request"
        profile.requestedRole == "sub_admin" && profile.approvalStatus.equals("rejected", ignoreCase = true) -> "Sub Admin Rejected"
        else -> "User"
    }
}

private fun resolveApprovalLabel(profile: UserProfileItem): String {
    return when {
        profile.role == "admin" -> "Approved"
        profile.requestedRole == "sub_admin" && profile.approvalStatus.isNotBlank() -> profile.approvalStatus.replaceFirstChar { it.uppercase() }
        profile.approvalStatus.isNotBlank() -> profile.approvalStatus.replaceFirstChar { it.uppercase() }
        else -> "Approved"
    }
}

private fun syncSavedLoginEmails(
    sharedPreferences: SharedPreferences,
    oldEmail: String,
    newEmail: String
) {
    val trimmedNewEmail = newEmail.trim()
    if (trimmedNewEmail.isBlank()) return

    val currentRememberedEmail = sharedPreferences.getString("email", "").orEmpty()
    val updatedEmails = readSavedEmails(sharedPreferences)
        .filterNot { it.equals(oldEmail, ignoreCase = true) }
        .plus(trimmedNewEmail)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

    sharedPreferences.edit().apply {
        if (currentRememberedEmail.equals(oldEmail, ignoreCase = true)) {
            putString("email", trimmedNewEmail)
        }
        putString("all_accounts", updatedEmails.joinToString(";"))
        apply()
    }
}

private fun readSavedEmails(sharedPreferences: SharedPreferences): List<String> {
    val rawValue = sharedPreferences.getString("all_accounts", "").orEmpty()
    if (rawValue.isBlank()) return emptyList()

    return rawValue.split(";")
        .mapNotNull { entry ->
            val value = entry.substringBefore("|").trim()
            value.takeIf { it.isNotEmpty() }
        }
        .distinct()
}
