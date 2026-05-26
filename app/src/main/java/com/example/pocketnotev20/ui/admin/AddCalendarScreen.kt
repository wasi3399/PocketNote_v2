package com.example.pocketnotev20.ui.admin

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.model.CalendarItem
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPrimaryButton
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.AppSecondaryButton
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold

@Composable
fun AddCalendarScreen(
    onBackClick: () -> Unit
) {
    val repository = FirestoreRepository()
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    ProfessionalPageScaffold(
        title = "Add Calendar Event",
        subtitle = "Publish academic events in a structured format so the shared calendar stays useful and consistent.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        AppPanelCard {
            AppSectionTitle(title = "Event Details")
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event title") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (Exam, Holiday, Academic)") }, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            AppPanelCard {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text("Saving event...")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        AppPrimaryButton(
            text = "Save Event",
            onClick = {
                isLoading = true
                val calendar = CalendarItem(
                    title = title,
                    date = date,
                    type = type
                )
                repository.submitCalendarForReview(calendar, onSuccess = {
                    isLoading = false
                    onBackClick()
                }, onFailure = {
                    isLoading = false
                })
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && title.isNotBlank()
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
