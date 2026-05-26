package com.example.pocketnotev20.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.repository.UserLocalRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNotesClick: () -> Unit,
    onRoutineClick: () -> Unit,
    onCtSlotsClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onQuestionsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onImportantDatesClick: () -> Unit,
    onGpaClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onExamCountdownClick: () -> Unit,
    onCtMarksClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirestoreRepository() }
    val localRepository = remember { UserLocalRepository(context) }
    var userName by remember { mutableStateOf("Student") }

    LaunchedEffect(Unit) {
        repository.getCurrentUserProfile(
            onSuccess = { profile ->
                if (profile.name.isNotBlank()) {
                    userName = profile.name
                }
            },
            onFailure = { /* Keep default */ }
        )
    }

    val todayLabel = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.ENGLISH)
        )
    }

    val upcomingExam = remember { localRepository.getUpcomingImportantDate() }
    val daysLeft = upcomingExam?.let { localRepository.getDaysUntil(it.date) }
    
    val examDateLabel = remember(upcomingExam) {
        upcomingExam?.let {
            try {
                LocalDate.parse(it.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.ENGLISH))
            } catch (e: Exception) {
                it.date
            }
        } ?: todayLabel
    }

    val menuItems = listOf(
        DashboardItem(
            title = "Personal Notes",
            subtitle = "Manage your study materials",
            badge = "12 Items",
            icon = Icons.Outlined.EditNote,
            background = Color(0xFFEFF6FF),
            iconTint = Color(0xFF2563EB),
            onClick = onNotesClick
        ),
        DashboardItem(
            title = "Class Routine",
            subtitle = "Your daily schedule",
            badge = "Active",
            icon = Icons.Outlined.School,
            background = Color(0xFFF0FDF4),
            iconTint = Color(0xFF16A34A),
            onClick = onRoutineClick
        ),
        DashboardItem(
            title = "CT Slots",
            subtitle = "Test dates and rooms",
            badge = "Updated",
            icon = Icons.Outlined.Class,
            background = Color(0xFFFEF2F2),
            iconTint = Color(0xFFDC2626),
            onClick = onCtSlotsClick
        ),
        DashboardItem(
            title = "Academic Calendar",
            subtitle = "University events",
            badge = "2026",
            icon = Icons.Outlined.CalendarMonth,
            background = Color(0xFFFFF7ED),
            iconTint = Color(0xFFEA580C),
            onClick = onCalendarClick
        ),
        DashboardItem(
            title = "Question Bank",
            subtitle = "Previous year questions",
            badge = "NEW",
            icon = Icons.Outlined.HelpOutline,
            background = Color(0xFFF5F3FF),
            iconTint = Color(0xFF7C3AED),
            onClick = onQuestionsClick
        ),
        DashboardItem(
            title = "CT Marks Calculator",
            subtitle = "Calculate your CT average",
            badge = "NEW",
            icon = Icons.Outlined.Calculate,
            background = Color(0xFFFFF1F2),
            iconTint = Color(0xFFE11D48),
            onClick = onCtMarksClick
        ),
        DashboardItem(
            title = "Important Dates",
            subtitle = "Reminders & deadlines",
            badge = "View",
            icon = Icons.Outlined.Badge,
            background = Color(0xFFF0FDFA),
            iconTint = Color(0xFF0D9488),
            onClick = onImportantDatesClick
        ),
        DashboardItem(
            title = "GPA Calculator",
            subtitle = "Calculate your result",
            badge = "GPA",
            icon = Icons.Outlined.Calculate,
            background = Color(0xFFE0E7FF),
            iconTint = Color(0xFF4338CA),
            onClick = onGpaClick
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-40).dp, y = (-40).dp)
                .size(240.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .padding(bottom = 100.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, $userName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Pocket Note v2.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Notification Icon
                    Surface(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { onNotificationsClick() },
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Reminders",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Logout Icon
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text("Logout")
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .clickable { onLogoutClick() },
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                                    contentDescription = "Logout",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Stats/Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExamCountdownClick() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = examDateLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Exam Countdown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = upcomingExam?.title ?: "No Upcoming Exam",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (daysLeft != null) "$daysLeft Days Left" else "Tap to set countdown",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Academic Services",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Grid Items
            menuItems.forEach { item ->
                DashboardMenuRow(item)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun DashboardMenuRow(item: DashboardItem) {
    Surface(
        onClick = item.onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = item.background
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

data class DashboardItem(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val background: Color,
    val iconTint: Color,
    val onClick: () -> Unit
)
