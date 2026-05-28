package com.example.pocketnotev20.ui.admin

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.model.QuestionItem
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPrimaryButton
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.AppSecondaryButton
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold

@Composable
fun AddQuestionScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = FirestoreRepository()
    var subject by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var term by remember { mutableStateOf("") }
    var session by remember { mutableStateOf("") }
    var yearInput by remember { mutableStateOf("") }
    var yearsList by remember { mutableStateOf(listOf<String>()) }
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
            selectedFileName = getQuestionUploadDisplayName(context, uri)
            selectedFileType = context.contentResolver.getType(uri).orEmpty()
            errorMessage = null
        }
    }

    ProfessionalPageScaffold(
        title = "Add Question Bank",
        subtitle = "Contribute to the shared resources by adding previous years' questions.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        AppPanelCard {
            AppSectionTitle(title = "General Info")
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = level, onValueChange = { level = it }, label = { Text("Level") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = term, onValueChange = { term = it }, label = { Text("Term") }, modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppPanelCard {
            AppSectionTitle(title = "Question Data")
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = session, onValueChange = { session = it }, label = { Text("Session (e.g., 2021-22)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = yearInput, onValueChange = { yearInput = it }, label = { Text("Year (e.g., 2023)") }, modifier = Modifier.weight(1f))
                AppPrimaryButton(text = "Add Year", onClick = { if (yearInput.isNotBlank()) { yearsList = yearsList + yearInput; yearInput = "" } }, modifier = Modifier.weight(0.6f))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = if (yearsList.isEmpty()) "No years added." else "Added Years: ${yearsList.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppPanelCard {
            AppSectionTitle(
                title = "Question File",
                subtitle = "Upload a PDF or image for this question entry."
            )
            Spacer(modifier = Modifier.height(16.dp))
            AppSecondaryButton(
                text = if (selectedFileName.isBlank()) "Upload PDF/Image" else "Change File",
                onClick = {
                    uploadLauncher.launch(
                        arrayOf(
                            "application/pdf",
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
                Text("Submitting for review...")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        AppPrimaryButton(
            text = "Submit for Review",
            onClick = {
                isLoading = true
                errorMessage = null
                val question = QuestionItem(
                    subject = subject,
                    level = level,
                    term = term,
                    session = session,
                    years = yearsList,
                    uploadName = selectedFileName,
                    fileType = selectedFileType
                )
                val fileUri = selectedFileUri
                val onSuccess: (String) -> Unit = {
                    isLoading = false
                    onBackClick()
                }
                val onFailure: (Exception) -> Unit = { error ->
                    isLoading = false
                    errorMessage = error.message ?: "Unable to submit question."
                }
                if (fileUri != null) {
                    repository.submitQuestionUploadForReview(question, fileUri, onSuccess, onFailure)
                } else {
                    repository.submitQuestionForReview(question, onSuccess, onFailure)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && subject.isNotBlank() && yearsList.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppSecondaryButton(text = "Cancel", onClick = onBackClick, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
    }
}

private fun getQuestionUploadDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex).orEmpty().ifBlank { uri.lastPathSegment.orEmpty() }
        }
    }
    return uri.lastPathSegment.orEmpty().ifBlank { "question_upload" }
}
