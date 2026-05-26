package com.example.pocketnotev20.ui.gpa

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.ui.common.AppInfoStrip
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPrimaryButton
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold
import kotlin.math.round

private data class GradeEntry(
    val courseName: String,
    val credit: Double,
    val grade: String
)

private val GradePoints = linkedMapOf(
    "A+" to 4.0,
    "A" to 3.75,
    "A-" to 3.5,
    "B+" to 3.25,
    "B" to 3.0,
    "B-" to 2.75,
    "C+" to 2.5,
    "C" to 2.25,
    "D" to 2.0,
    "F" to 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpaCalculatorScreen(
    onBackClick: () -> Unit
) {
    var courseName by remember { mutableStateOf("") }
    var credit by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf("A+") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val entries = remember { mutableStateListOf<GradeEntry>() }

    val totalCredits = entries.sumOf { it.credit }
    val gpa = if (totalCredits == 0.0) 0.0 else {
        entries.sumOf { it.credit * (GradePoints[it.grade] ?: 0.0) } / totalCredits
    }

    ProfessionalPageScaffold(
        title = "Result / GPA Calculator",
        subtitle = "Add your courses, credits, and grades to calculate a quick GPA summary.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoStrip(label = "Courses", value = entries.size.toString())
            AppInfoStrip(label = "GPA", value = formatGpa(gpa))
        }

        Spacer(modifier = Modifier.height(18.dp))

        AppPanelCard {
            AppSectionTitle(title = "Add Course")
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = courseName,
                onValueChange = { courseName = it },
                label = { Text("Course name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = credit,
                onValueChange = { credit = it },
                label = { Text("Credit") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GradePoints.keys.forEach { grade ->
                    FilterChip(
                        selected = selectedGrade == grade,
                        onClick = { selectedGrade = grade },
                        label = { Text(grade) }
                    )
                }
            }

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
                text = "Add Result",
                onClick = {
                    val parsedCredit = credit.toDoubleOrNull()
                    if (courseName.isBlank() || parsedCredit == null || parsedCredit <= 0.0) {
                        errorMessage = "Please enter a course name and valid credit."
                    } else {
                        entries += GradeEntry(courseName, parsedCredit, selectedGrade)
                        courseName = ""
                        credit = ""
                        selectedGrade = "A+"
                        errorMessage = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppPanelCard {
            AppSectionTitle(title = "Result Summary")
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Total Credit: ${if (totalCredits == 0.0) "0.0" else totalCredits}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Current GPA: ${formatGpa(gpa)}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppPanelCard {
            AppSectionTitle(title = "Added Courses")
            Spacer(modifier = Modifier.height(12.dp))
            if (entries.isEmpty()) {
                Text(
                    text = "No result rows added yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entries.forEachIndexed { index, entry ->
                    Text(
                        text = "${entry.courseName} • ${entry.credit} Credit • ${entry.grade}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { entries.remove(entry) }) {
                            Text("Remove", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (index != entries.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

private fun formatGpa(gpa: Double): String {
    return (round(gpa * 100) / 100).toString()
}
