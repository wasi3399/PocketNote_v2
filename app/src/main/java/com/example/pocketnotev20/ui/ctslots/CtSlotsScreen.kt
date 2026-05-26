package com.example.pocketnotev20.ui.ctslots

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.model.CtSlotItem
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.ui.common.AppInfoStrip
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPanelMuted
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold

private val CtRoomColumns = listOf(
    "302 (45)",
    "307 (35)",
    "308 (60)",
    "311 (45)",
    "402 (45)",
    "407 (40)",
    "408 (60)",
    "411 (50)"
)

@Composable
fun CtSlotsScreen(
    onBackClick: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var slots by remember { mutableStateOf<List<CtSlotItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.getCtSlots(
            onSuccess = {
                slots = it
                isLoading = false
            },
            onFailure = { error ->
                errorMessage = error.message ?: "Unable to load CT slots."
                isLoading = false
            }
        )
    }

    val dateRows = remember(slots) {
        slots
            .groupBy { it.date }
            .values
            .sortedWith(
                compareBy<List<CtSlotItem>>(
                    { it.firstOrNull()?.weekNo ?: Int.MAX_VALUE },
                    { ctDateSortOrder(it.firstOrNull()?.date.orEmpty()) }
                )
            )
    }

    ProfessionalPageScaffold(
        title = "CT Slots",
        subtitle = "Review the class-test room schedule in a table layout based on the shared slot sheet.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoStrip(label = "Dates", value = dateRows.size.toString())
            AppInfoStrip(label = "Rooms", value = CtRoomColumns.size.toString())
        }

        Spacer(modifier = Modifier.height(18.dp))

        when {
            isLoading -> {
                AppPanelCard {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading CT slot table...")
                }
            }

            !errorMessage.isNullOrBlank() -> {
                AppPanelCard {
                    AppSectionTitle(title = "Unable To Load CT Slots")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            dateRows.isEmpty() -> {
                AppPanelCard {
                    AppSectionTitle(
                        title = "No CT Slots Found",
                        subtitle = "Admin can add CT slot entries from the dashboard."
                    )
                }
            }

            else -> {
                AppPanelCard {
                    AppSectionTitle(
                        title = "Winter 2026 CT Schedule",
                        subtitle = "Rooms are shown across the top. Filled cells represent the scheduled slot."
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        CtHeaderRow()
                        dateRows.forEach { rowSlots ->
                            CtDataRow(rowSlots = rowSlots)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CtHeaderRow() {
    Row {
        HeaderCell(text = "Week", width = 70.dp)
        HeaderCell(text = "Date", width = 86.dp)
        CtRoomColumns.forEach { room ->
            HeaderCell(text = room, width = 92.dp)
        }
    }
}

@Composable
private fun CtDataRow(
    rowSlots: List<CtSlotItem>
) {
    val firstSlot = rowSlots.first()
    Row {
        BodyCell(
            text = firstSlot.weekNo.toString(),
            width = 70.dp,
            backgroundColor = Color(0xFFF1F5F9),
            fontWeight = FontWeight.SemiBold
        )
        BodyCell(
            text = firstSlot.date,
            width = 86.dp,
            backgroundColor = Color(0xFFF8FAFC),
            fontWeight = FontWeight.SemiBold
        )
        CtRoomColumns.forEach { room ->
            val slot = rowSlots.firstOrNull { it.room == room }
            SlotCell(
                text = slot?.courseLabel.orEmpty(),
                width = 92.dp,
                isActive = slot != null
            )
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .background(Color(0xFFDBEAFE), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF1E3A8A),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BodyCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    backgroundColor: Color,
    fontWeight: FontWeight
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(top = 8.dp, end = 8.dp)
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 11.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF334155),
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun SlotCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isActive: Boolean
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(top = 8.dp, end = 8.dp)
            .background(
                color = if (isActive) Color(0xFFDBEAFE) else Color.White,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                color = if (isActive) Color(0xFF93C5FD) else AppPanelMuted,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 8.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (text.isBlank()) "-" else text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) Color(0xFF1D4ED8) else Color(0xFF94A3B8),
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
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
