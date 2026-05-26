package com.example.pocketnotev20.ui.questions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketnotev20.model.QuestionItem
import com.example.pocketnotev20.ui.common.AppInfoStrip
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPanelMuted
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold
import com.example.pocketnotev20.viewmodel.QuestionBankViewModel

@Composable
fun QuestionBankScreen(
    onBackClick: () -> Unit,
    viewModel: QuestionBankViewModel = viewModel()
) {
    val questions = viewModel.questions.value
    val isLoading = viewModel.isLoading.value
    val errorMessage = viewModel.errorMessage.value
    var selectedLevel by remember { mutableStateOf("") }
    var selectedTerm by remember { mutableStateOf("") }
    var selectedSession by remember { mutableStateOf("") }

    val levelOptions = remember { listOf("1", "2", "3", "4") }
    val termOptions = remember { listOf("I", "II") }
    val sessionOptions = remember { listOf("Winter", "Summer") }
    val filteredQuestions = remember(questions, selectedLevel, selectedTerm, selectedSession) {
        if (selectedLevel.isBlank() || selectedTerm.isBlank() || selectedSession.isBlank()) {
            emptyList()
        } else {
            questions.filter {
                normalizeLevelValue(it.level) == selectedLevel &&
                    normalizeTermValue(it.term) == selectedTerm &&
                    (
                        it.session.isBlank() ||
                            it.session.equals(selectedSession, ignoreCase = true)
                        )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadQuestions()
    }

    ProfessionalPageScaffold(
        title = "Question Bank",
        subtitle = "Select the academic level, term, and session first, then browse only the matching question collection.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoStrip(
                label = "Selection",
                value = if (selectedLevel.isBlank() || selectedTerm.isBlank() || selectedSession.isBlank()) {
                    "Choose"
                } else {
                    "$selectedLevel / $selectedTerm / $selectedSession"
                }
            )
            AppInfoStrip(
                label = "Matches",
                value = if (isLoading) "..." else filteredQuestions.size.toString()
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        AppPanelCard {
            AppSectionTitle(
                title = "Choose Level, Term, And Session",
                subtitle = "Questions will appear only after all filters are selected."
            )
            Spacer(modifier = Modifier.height(16.dp))
            SelectionDropdownField(
                label = "Level",
                value = selectedLevel,
                options = levelOptions,
                onOptionSelected = {
                    selectedLevel = it
                    selectedTerm = ""
                    selectedSession = ""
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SelectionDropdownField(
                label = "Term",
                value = selectedTerm,
                options = termOptions,
                onOptionSelected = { selectedTerm = it },
                enabled = selectedLevel.isNotBlank()
            )
            Spacer(modifier = Modifier.height(12.dp))
            SelectionDropdownField(
                label = "Session",
                value = selectedSession,
                options = sessionOptions,
                onOptionSelected = { selectedSession = it },
                enabled = selectedLevel.isNotBlank() && selectedTerm.isNotBlank()
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        when {
            isLoading -> {
                AppPanelCard {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading question bank...")
                }
            }

            errorMessage.isNotEmpty() -> {
                AppPanelCard {
                    AppSectionTitle(title = "Unable To Load Questions")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            questions.isEmpty() -> {
                AppPanelCard {
                    AppSectionTitle(title = "No Questions Found")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Question bank entries have not been added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            selectedLevel.isBlank() || selectedTerm.isBlank() || selectedSession.isBlank() -> {
                AppPanelCard {
                    AppSectionTitle(title = "Select Filters To Continue")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose level, term, and session from the selectors above. Then only the related questions will be shown here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            filteredQuestions.isEmpty() -> {
                AppPanelCard {
                    AppSectionTitle(title = "No Questions For This Selection")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No question-bank entry matches $selectedLevel, $selectedTerm, and $selectedSession yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                filteredQuestions.forEach { item ->
                    QuestionCard(item)
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(item: QuestionItem) {
    AppPanelCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.subject,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Level ${normalizeLevelValue(item.level)} • Term ${normalizeTermValue(item.term)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.session.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.session,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = AppPanelMuted,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${item.years.size} Years",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        AppSectionTitle(title = "Available Years")
        Spacer(modifier = Modifier.height(8.dp))
        item.years.forEach { year ->
            Text(
                text = "- $year",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SelectionDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val placeholderText = when {
        enabled -> "Select $label"
        label == "Term" -> "Choose level first"
        label == "Session" -> "Choose term first"
        else -> "Select $label"
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                label = { Text(label) },
                placeholder = {
                    Text(text = placeholderText)
                },
                trailingIcon = {
                    IconButton(
                        onClick = { if (enabled) expanded = true },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "$label dropdown"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { expanded = true }
            )
        }

        DropdownMenu(
            expanded = expanded && enabled && options.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
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

private fun normalizeLevelValue(level: String): String {
    return when (level.trim().lowercase()) {
        "1", "level 1" -> "1"
        "2", "level 2" -> "2"
        "3", "level 3" -> "3"
        "4", "level 4" -> "4"
        else -> level.trim()
    }
}

private fun normalizeTermValue(term: String): String {
    return when (term.trim().lowercase()) {
        "i", "term 1", "term i" -> "I"
        "ii", "term 2", "term ii" -> "II"
        else -> term.trim()
    }
}
