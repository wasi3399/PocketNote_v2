package com.example.pocketnotev20.ui.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.model.AssignmentReminderItem
import com.example.pocketnotev20.repository.UserLocalRepository
import com.example.pocketnotev20.ui.common.AppInfoStrip
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold

@Composable
fun AssignmentReminderScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { UserLocalRepository(context) }

    var reminders by remember { mutableStateOf<List<AssignmentReminderItem>>(emptyList()) }

    fun reloadData() {
        reminders = repository.getAssignmentReminders()
    }

    LaunchedEffect(Unit) {
        reloadData()
    }

    ProfessionalPageScaffold(
        title = "Notifications",
        subtitle = "View and manage your current reminders and notifications.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoStrip(label = "Active", value = reminders.count { !it.isDone }.toString())
            AppInfoStrip(label = "Completed", value = reminders.count { it.isDone }.toString())
        }

        Spacer(modifier = Modifier.height(18.dp))

        AppPanelCard {
            AppSectionTitle(title = "Your Notifications")
            Spacer(modifier = Modifier.height(12.dp))
            if (reminders.isEmpty()) {
                Text(
                    text = "No notifications or reminders at the moment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                reminders.forEachIndexed { index, reminder ->
                    ReminderItem(
                        reminder = reminder,
                        onToggleDone = {
                            repository.toggleAssignmentReminderDone(reminder.id)
                            reloadData()
                        },
                        onDelete = {
                            repository.deleteAssignmentReminder(reminder.id)
                            reloadData()
                        }
                    )
                    if (index != reminders.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderItem(
    reminder: AssignmentReminderItem,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit
) {
    AppPanelCard {
        AppSectionTitle(
            title = reminder.title,
            subtitle = "${reminder.course} • ${reminder.dueDate}"
        )
        if (reminder.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reminder.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onToggleDone) {
                Text(
                    text = if (reminder.isDone) "Mark Active" else "Mark Done",
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(onClick = onDelete) {
                Text(
                    text = "Remove",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
