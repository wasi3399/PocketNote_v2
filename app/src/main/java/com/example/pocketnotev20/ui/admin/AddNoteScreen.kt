package com.example.pocketnotev20.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.model.NoteItem
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPrimaryButton
import com.example.pocketnotev20.ui.common.AppSecondaryButton
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold

@Composable
fun AddNoteScreen(
    onBackClick: () -> Unit
) {
    val repository = FirestoreRepository()
    
    val subjectOptions = remember {
        listOf(
            "Structured Programming",
            "Discrete Mathematics",
            "Data Structures",
            "Algorithms",
            "Database Management System",
            "Object Oriented Programming",
            "Operating Systems",
            "Computer Networks"
        )
    }
    val levelOptions = remember { listOf("1", "2", "3", "4") }
    val termOptions = remember { listOf("I", "II") }

    var subject by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var term by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    ProfessionalPageScaffold(
        title = "Add Study Note",
        subtitle = "Create professional study notes with subject, level, term, and detailed descriptions.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        AppPanelCard {
            AppSectionTitle(title = "Note Information")
            Spacer(modifier = Modifier.height(16.dp))
            
            SelectionDropdownField(
                label = "Subject",
                value = subject,
                options = subjectOptions,
                onOptionSelected = { subject = it }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
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
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Note Title") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            AppPanelCard {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text("Saving study note...")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        AppPrimaryButton(
            text = "Save Note",
            onClick = {
                isLoading = true
                val note = NoteItem(
                    subject = subject,
                    level = level,
                    term = term,
                    noteTitle = title,
                    description = description
                )
                repository.submitNoteForReview(note, onSuccess = {
                    isLoading = false
                    onBackClick()
                }, onFailure = {
                    isLoading = false
                })
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && subject.isNotBlank() && title.isNotBlank()
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
