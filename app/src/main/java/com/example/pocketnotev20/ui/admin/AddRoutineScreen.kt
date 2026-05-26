package com.example.pocketnotev20.ui.admin

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.model.RoutineItem
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPrimaryButton
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.AppSecondaryButton
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold

@Composable
fun AddRoutineScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = FirestoreRepository()
    val levelOptions = remember { listOf("Level 1", "Level 2", "Level 3", "Level 4") }
    val termOptions = remember { listOf("Term I", "Term II") }
    val sectionOptions = remember { listOf("Section A", "Section B") }

    var level by remember { mutableStateOf("") }
    var term by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileType by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedFileUri = uri
            selectedFileName = getDisplayName(context, uri)
            selectedFileType = context.contentResolver.getType(uri).orEmpty()
            errorMessage = null
        }
    }

    ProfessionalPageScaffold(
        title = "Add Routine",
        subtitle = "Select level, term, and section, then upload a routine file.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        AppPanelCard {
            AppSectionTitle(title = "Routine Details")
            Spacer(modifier = Modifier.height(16.dp))
            SelectionDropdownField(
                label = "Level",
                value = level,
                options = levelOptions,
                onOptionSelected = { level = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SelectionDropdownField(
                label = "Term",
                value = term,
                options = termOptions,
                onOptionSelected = { term = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SelectionDropdownField(
                label = "Section",
                value = section,
                options = sectionOptions,
                onOptionSelected = { section = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            AppSecondaryButton(
                text = if (selectedFileName.isBlank()) "Upload" else "Change Upload",
                onClick = {
                    uploadLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "image/*"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            if (selectedFileName.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Selected: $selectedFileName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            AppPanelCard {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text("Saving routine...")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        AppPrimaryButton(
            text = "Save Routine",
            onClick = {
                val fileUri = selectedFileUri
                if (level.isBlank() || term.isBlank() || section.isBlank() || fileUri == null) {
                    errorMessage = "Please select level, term, section, and upload a file."
                    return@AppPrimaryButton
                }

                isLoading = true
                val routine = RoutineItem(
                    level = level,
                    term = term,
                    section = section,
                    uploadName = selectedFileName,
                    fileType = selectedFileType
                )
                repository.submitRoutineUploadForReview(routine, fileUri, onSuccess = {
                    isLoading = false
                    onBackClick()
                }, onFailure = { error ->
                    isLoading = false
                    errorMessage = error.message ?: "Unable to save routine."
                })
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading &&
                level.isNotBlank() &&
                term.isNotBlank() &&
                section.isNotBlank() &&
                selectedFileUri != null
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppSecondaryButton(
            text = "Back",
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
    }
}

private fun getDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex).orEmpty().ifBlank { uri.lastPathSegment.orEmpty() }
        }
    }
    return uri.lastPathSegment.orEmpty().ifBlank { "routine_upload" }
}

@Composable
private fun SelectionDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "$label dropdown"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        // Overlay a clickable box to handle the click for the dropdown
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
