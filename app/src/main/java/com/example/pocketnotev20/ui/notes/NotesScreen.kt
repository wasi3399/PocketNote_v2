package com.example.pocketnotev20.ui.notes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.model.NoteItem
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.ui.common.AppInfoStrip
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPanelMuted
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onBackClick: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var allNotes by remember { mutableStateOf<List<NoteItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val levels = listOf("All", "Level 1", "Level 2", "Level 3", "Level 4")
    val terms = listOf("All", "Term 1", "Term 2")

    var selectedLevel by remember { mutableStateOf("All") }
    var selectedTerm by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        repository.getNotes(
            onSuccess = { notes ->
                allNotes = notes
                isLoading = false
            },
            onFailure = {
                isLoading = false
            }
        )
    }

    val filteredNotes = remember(allNotes, selectedLevel, selectedTerm) {
        allNotes.filter { note ->
            (selectedLevel == "All" || note.level == selectedLevel) &&
                (selectedTerm == "All" || note.term == selectedTerm)
        }
    }

    ProfessionalPageScaffold(
        title = "Study Notes",
        subtitle = "Filter notes by level and term, then jump directly into the subject material you need.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoStrip(label = "Total Notes", value = allNotes.size.toString())
            AppInfoStrip(label = "Filtered", value = filteredNotes.size.toString())
        }

        Spacer(modifier = Modifier.height(18.dp))

        AppPanelCard {
            AppSectionTitle(
                title = "Filter Notes",
                subtitle = "Refine the list by level and term."
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilterRow(items = levels, selectedItem = selectedLevel, onSelect = { selectedLevel = it })
            Spacer(modifier = Modifier.height(10.dp))
            FilterRow(items = terms, selectedItem = selectedTerm, onSelect = { selectedTerm = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                AppPanelCard {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading notes...")
                }
            }

            filteredNotes.isEmpty() -> {
                AppPanelCard {
                    AppSectionTitle(title = "No Notes Found")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "There are no notes for the current filter selection yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                filteredNotes.forEach { note ->
                    NoteCard(note = note)
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            FilterChip(
                selected = selectedItem == item,
                onClick = { onSelect(item) },
                label = { Text(item) }
            )
        }
    }
}

@Composable
private fun NoteCard(note: NoteItem) {
    AppPanelCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.subject,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.noteTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Surface(
                color = AppPanelMuted,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${note.level} ${note.term}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = note.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
