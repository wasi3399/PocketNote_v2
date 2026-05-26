package com.example.pocketnotev20.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.School
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketnotev20.repository.FirestoreRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class AdminDashboardTile(
    val title: String,
    val caption: String,
    val badge: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val background: Color,
    val iconTint: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    roleLabel: String,
    isMainAdmin: Boolean,
    subAdminStatus: String,
    showNotifications: Boolean,
    canManageContent: Boolean,
    onNotificationsClick: () -> Unit,
    onAdminProfileClick: () -> Unit,
    onManageUsersClick: () -> Unit,
    onAddQuestionClick: () -> Unit,
    onAddCalendarClick: () -> Unit,
    onAddRoutineClick: () -> Unit,
    onAddNoteClick: () -> Unit,
    onAddCtSlotClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var pendingRequestCount by remember { mutableIntStateOf(0) }
    var liveSubAdminStatus by remember { mutableStateOf(subAdminStatus) }
    var liveContentStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (isMainAdmin) {
            repository.getPendingSubAdminRequests(
                onSuccess = { accessRequests ->
                    repository.getPendingContentRequests(
                        onSuccess = { contentRequests ->
                            pendingRequestCount = accessRequests.size + contentRequests.size
                        },
                        onFailure = { pendingRequestCount = accessRequests.size }
                    )
                },
                onFailure = { pendingRequestCount = 0 }
            )
        } else {
            repository.getCurrentUserAdminAccessStatus(
                onSuccess = {
                    liveSubAdminStatus = it.approvalStatus.ifBlank { subAdminStatus }
                },
                onFailure = {
                    liveSubAdminStatus = subAdminStatus
                }
            )
            repository.getCurrentUserContentRequests(
                onSuccess = {
                    liveContentStatus = mostImportantContentStatus(it)
                },
                onFailure = {
                    liveContentStatus = null
                }
            )
        }
    }

    val effectiveSubAdminStatus = if (isMainAdmin) "approved" else liveSubAdminStatus
    val effectiveCanManageContent = if (isMainAdmin) true else canManageContent && effectiveSubAdminStatus.lowercase() == "approved"
    val displayRoleLabel = if (isMainAdmin) roleLabel else resolvedSubAdminRoleLabel(effectiveSubAdminStatus)
    val subAdminBadgeStatus = if (effectiveSubAdminStatus.lowercase() == "approved") {
        liveContentStatus ?: effectiveSubAdminStatus
    } else {
        effectiveSubAdminStatus
    }
    
    val notificationAccent = if (isMainAdmin) Color(0xFFF59E0B) else subAdminBadgeColor(subAdminBadgeStatus)
    val todayLabel = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.ENGLISH)
        )
    }

    val tiles = listOf(
        AdminDashboardTile(
            title = "Admin Profile",
            caption = "Account settings",
            badge = "ME",
            icon = Icons.Outlined.PersonOutline,
            enabled = true,
            background = Color(0xFFF1F5F9),
            iconTint = Color(0xFF475569),
            onClick = onAdminProfileClick
        ),
        AdminDashboardTile(
            title = "Manage Users",
            caption = if (isMainAdmin) "Control access" else "Restricted",
            badge = if (isMainAdmin) "USER" else "LOCK",
            icon = Icons.Outlined.PersonOutline,
            enabled = isMainAdmin,
            background = Color(0xFFE0E7FF),
            iconTint = Color(0xFF4338CA),
            onClick = onManageUsersClick
        ),
        AdminDashboardTile(
            title = "Questions",
            caption = "Manage bank",
            badge = "Q",
            icon = Icons.Outlined.MenuBook,
            enabled = effectiveCanManageContent,
            background = Color(0xFFFEF3C7),
            iconTint = Color(0xFFB45309),
            onClick = onAddQuestionClick
        ),
        AdminDashboardTile(
            title = "Calendar",
            caption = "Events",
            badge = "CAL",
            icon = Icons.Outlined.Event,
            enabled = effectiveCanManageContent,
            background = Color(0xFFDCFCE7),
            iconTint = Color(0xFF15803D),
            onClick = onAddCalendarClick
        ),
        AdminDashboardTile(
            title = "Routine",
            caption = "Schedule",
            badge = "RT",
            icon = Icons.Outlined.CalendarToday,
            enabled = effectiveCanManageContent,
            background = Color(0xFFF3E8FF),
            iconTint = Color(0xFF7E22CE),
            onClick = onAddRoutineClick
        ),
        AdminDashboardTile(
            title = "Notes",
            caption = "Publish notes",
            badge = "NOTE",
            icon = Icons.Outlined.Notes,
            enabled = effectiveCanManageContent,
            background = Color(0xFFE0F2FE),
            iconTint = Color(0xFF0369A1),
            onClick = onAddNoteClick
        ),
        AdminDashboardTile(
            title = "CT Slots",
            caption = "Assessment",
            badge = "CT",
            icon = Icons.Outlined.School,
            enabled = effectiveCanManageContent,
            background = Color(0xFFFFEDD5),
            iconTint = Color(0xFFC2410C),
            onClick = onAddCtSlotClick
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
                .offset(x = (-30).dp, y = (-30).dp)
                .size(200.dp)
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append("Pocket") }
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) { append("Admin") }
                        },
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = displayRoleLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showNotifications) {
                        AdminTopIconButton(
                            icon = Icons.Outlined.Notifications,
                            badgeCount = if (isMainAdmin) pendingRequestCount else null,
                            accentColor = notificationAccent,
                            onClick = onNotificationsClick,
                            tooltipText = "Notifications"
                        )
                    }
                    AdminTopIconButton(
                        icon = Icons.Outlined.Logout,
                        onClick = onLogoutClick,
                        tooltipText = "Logout"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = todayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(subAdminBadgeColor(effectiveSubAdminStatus))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isMainAdmin) "System Active" else "Status: ${subAdminStatusTitle(subAdminBadgeStatus)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Control Center",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            tiles.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowItems.forEach { tile ->
                        AdminActionCard(
                            tile = tile,
                            modifier = Modifier.weight(1f)
                        )
                        if (rowItems.size > 1 && tile == rowItems.first()) {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTopIconButton(
    icon: ImageVector,
    badgeCount: Int? = null,
    accentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tooltipText: String? = null,
    onClick: () -> Unit
) {
    val content = @Composable {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = tooltipText,
                    tint = if (badgeCount != null && badgeCount > 0) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                if (badgeCount != null && badgeCount > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        color = accentColor,
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (tooltipText != null) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(tooltipText)
                }
            },
            state = rememberTooltipState()
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
private fun AdminActionCard(
    tile: AdminDashboardTile,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .then(if (tile.enabled) Modifier.clickable(onClick = tile.onClick) else Modifier)
            .alpha(if (tile.enabled) 1f else 0.5f),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tile.background,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = tile.icon,
                        contentDescription = null,
                        tint = tile.iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tile.caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = tile.badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun subAdminBadgeColor(status: String): Color {
    return when (status.lowercase()) {
        "approved" -> Color(0xFF22C55E)
        "rejected" -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B)
    }
}

private fun subAdminStatusTitle(status: String): String {
    return status.replaceFirstChar { it.uppercase() }
}

private fun resolvedSubAdminRoleLabel(status: String): String {
    return when (status.lowercase()) {
        "pending" -> "Pending Sub-Admin"
        "rejected" -> "Rejected Request"
        else -> "Sub-Admin"
    }
}

private fun mostImportantContentStatus(requests: List<com.example.pocketnotev20.model.ContentApprovalRequestItem>): String? {
    return when {
        requests.any { it.approvalStatus.equals("pending", ignoreCase = true) } -> "pending"
        requests.any { it.approvalStatus.equals("rejected", ignoreCase = true) } -> "rejected"
        requests.any { it.approvalStatus.equals("approved", ignoreCase = true) } -> "approved"
        else -> null
    }
}
