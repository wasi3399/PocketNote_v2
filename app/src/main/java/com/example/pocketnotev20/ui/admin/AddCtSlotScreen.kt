package com.example.pocketnotev20.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.pocketnotev20.model.CtSlotItem
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPrimaryButton
import com.example.pocketnotev20.ui.common.AppSecondaryButton
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold

@Composable
fun AddCtSlotScreen(
    onBackClick: () -> Unit
) {
    val repository = remember { FirestoreRepository() }

    var weekNo by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var courseLabel by remember { mutableStateOf("3-I") }
    var editingSlotId by remember { mutableStateOf<String?>(null) }

    var slotItems by remember { mutableStateOf<List<CtSlotItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun resetForm() {
        weekNo = ""
        date = ""
        room = ""
        courseLabel = "3-I"
        editingSlotId = null
        errorMessage = null
    }

    fun loadCtSlots() {
        isLoading = true
        repository.getCtSlots(
            onSuccess = { slots ->
                slotItems = slots.sortedWith(
                    compareBy<CtSlotItem>(
                        { it.weekNo },
                        { ctDateSortOrder(it.date) },
                        { it.room }
                    )
                )
                isLoading = false
            },
            onFailure = { error ->
                errorMessage = error.message ?: "Unable to load CT slots."
                isLoading = false
            }
        )
    }

    LaunchedEffect(Unit) {
        loadCtSlots()
    }

    ProfessionalPageScaffold(
        title = "Manage CT Slots",
        subtitle = "Add, edit, and delete class-test slot entries that appear in the CT slots table.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        AppPanelCard {
            AppSectionTitle(
                title = if (editingSlotId == null) "Add Slot" else "Edit Slot",
                subtitle = "Use values like Week 4, 22-Feb, and room names such as 307 (35)."
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = weekNo,
                onValueChange = { weekNo = it },
                label = { Text("Week No") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Room") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = courseLabel,
                onValueChange = { courseLabel = it },
                label = { Text("Slot Label") },
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSaving) {
            AppPanelCard {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text(if (editingSlotId == null) "Saving CT slot..." else "Updating CT slot...")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        AppPrimaryButton(
            text = if (editingSlotId == null) "Save CT Slot" else "Update CT Slot",
            onClick = {
                val parsedWeekNo = weekNo.toIntOrNull()
                if (parsedWeekNo == null || date.isBlank() || room.isBlank() || courseLabel.isBlank()) {
                    errorMessage = "Please enter week number, date, room, and slot label."
                } else {
                    isSaving = true
                    errorMessage = null
                    repository.submitCtSlotForReview(
                        CtSlotItem(
                            id = editingSlotId.orEmpty(),
                            weekNo = parsedWeekNo,
                            date = date,
                            room = room,
                            courseLabel = courseLabel
                        ),
                        onSuccess = {
                            isSaving = false
                            resetForm()
                            onBackClick()
                        },
                        onFailure = { error ->
                            isSaving = false
                            errorMessage = error.message ?: "Unable to save CT slot."
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppSecondaryButton(
            text = if (editingSlotId == null) "Back" else "Clear Form",
            onClick = {
                if (editingSlotId == null) onBackClick() else resetForm()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )

        Spacer(modifier = Modifier.height(16.dp))

        AppPanelCard {
            AppSectionTitle(
                title = "Existing CT Slots",
                subtitle = "Tap edit to load a row into the form, or delete to remove it."
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Loading existing CT slots...")
                }

                slotItems.isEmpty() -> {
                    Text(
                        text = "No CT slot entries available yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    slotItems.forEachIndexed { index, item ->
                        CtSlotAdminItem(
                            item = item,
                            onEditClick = {
                                editingSlotId = item.id
                                weekNo = item.weekNo.toString()
                                date = item.date
                                room = item.room
                                courseLabel = item.courseLabel
                                errorMessage = null
                            },
                            onDeleteClick = {
                                isSaving = true
                                errorMessage = null
                                repository.deleteCtSlot(
                                    id = item.id,
                                    onSuccess = {
                                        isSaving = false
                                        if (editingSlotId == item.id) {
                                            resetForm()
                                        }
                                        loadCtSlots()
                                    },
                                    onFailure = { error ->
                                        isSaving = false
                                        errorMessage = error.message ?: "Unable to delete CT slot."
                                    }
                                )
                            }
                        )
                        if (index != slotItems.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CtSlotAdminItem(
    item: CtSlotItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AppPanelCard {
        AppSectionTitle(
            title = "${item.date} • ${item.room}",
            subtitle = "Week ${item.weekNo} • Label ${item.courseLabel}"
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onEditClick) {
                Text(
                    text = "Edit",
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(onClick = onDeleteClick) {
                Text(
                    text = "Delete",
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun ctDateSortOrder(date: String): Int {
    val parts = date.split("-")
    val day = parts.firstOrNull()?.trim()?.toIntOrNull() ?: Int.MAX_VALUE
    val month = when (parts.getOrNull(1)?.trim()?.lowercase()) {
        "jan" -> 1
        "feb" -> 2
        "mar" -> 3
        "apr" -> 4
        "may" -> 5
        "jun" -> 6
        "jul" -> 7
        "aug" -> 8
        "sep" -> 9
        "oct" -> 10
        "nov" -> 11
        "dec" -> 12
        else -> Int.MAX_VALUE
    }
    return month * 100 + day
}
