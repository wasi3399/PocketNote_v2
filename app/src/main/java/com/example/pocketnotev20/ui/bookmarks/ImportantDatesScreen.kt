package com.example.pocketnotev20.ui.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.model.ImportantDateItem
import com.example.pocketnotev20.repository.UserLocalRepository
import com.example.pocketnotev20.ui.common.AppInfoStrip
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPrimaryButton
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportantDatesScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { UserLocalRepository(context) }

    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Exam") }
    var note by remember { mutableStateOf("") }
    var importantDates by remember { mutableStateOf<List<ImportantDateItem>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    fun reloadData() {
        importantDates = repository.getImportantDates()
    }

    LaunchedEffect(Unit) {
        reloadData()
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        date = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val nextDate = remember(importantDates) { repository.getUpcomingImportantDate() }

    ProfessionalPageScaffold(
        title = "Bookmark Important Dates",
        subtitle = "Save exam, academic, and milestone dates so they stay easy to find.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoStrip(label = "Bookmarks", value = importantDates.size.toString())
            AppInfoStrip(label = "Next Up", value = nextDate?.date ?: "None")
        }

        Spacer(modifier = Modifier.height(18.dp))

        AppPanelCard {
            AppSectionTitle(title = "Add Important Date", subtitle = "Use date format yyyy-MM-dd.")
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Select Date"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth()
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            AppPrimaryButton(
                text = "Save Important Date",
                onClick = {
                    if (title.isBlank() || date.isBlank() || category.isBlank()) {
                        errorMessage = "Please enter title, date, and category."
                    } else {
                        repository.saveImportantDate(
                            ImportantDateItem(
                                title = title,
                                date = date,
                                category = category,
                                note = note
                            )
                        )
                        title = ""
                        date = ""
                        category = "Exam"
                        note = ""
                        errorMessage = null
                        reloadData()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppPanelCard {
            AppSectionTitle(title = "Saved Important Dates")
            Spacer(modifier = Modifier.height(12.dp))
            if (importantDates.isEmpty()) {
                Text(
                    text = "No important date bookmarked yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                importantDates.forEachIndexed { index, item ->
                    ImportantDateCard(
                        item = item,
                        daysLeft = repository.getDaysUntil(item.date),
                        onDelete = {
                            repository.deleteImportantDate(item.id)
                            reloadData()
                        }
                    )
                    if (index != importantDates.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportantDateCard(
    item: ImportantDateItem,
    daysLeft: Long?,
    onDelete: () -> Unit
) {
    AppPanelCard {
        AppSectionTitle(
            title = item.title,
            subtitle = "${item.category} • ${item.date}"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (daysLeft == null) "Countdown unavailable" else "$daysLeft day(s) remaining",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (item.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDelete) {
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
