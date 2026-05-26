package com.example.pocketnotev20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pocketnotev20.ui.auth.LoginScreen
import com.example.pocketnotev20.ui.auth.SignupScreen
import com.example.pocketnotev20.ui.calendar.CalendarScreen
import com.example.pocketnotev20.ui.bookmarks.ImportantDatesScreen
import com.example.pocketnotev20.ui.dashboard.DashboardScreen
import com.example.pocketnotev20.ui.dashboard.AdminDashboardScreen
import com.example.pocketnotev20.ui.admin.AddNoteScreen
import com.example.pocketnotev20.ui.admin.AddQuestionScreen
import com.example.pocketnotev20.ui.admin.AddRoutineScreen
import com.example.pocketnotev20.ui.admin.AddCalendarScreen
import com.example.pocketnotev20.ui.admin.AddCtSlotScreen
import com.example.pocketnotev20.ui.admin.ManageUsersScreen
import com.example.pocketnotev20.ui.admin.SubAdminRequestsScreen
import com.example.pocketnotev20.ui.admin.SubAdminStatusScreen
import com.example.pocketnotev20.ui.common.AdminBottomBar
import com.example.pocketnotev20.ui.gpa.GpaCalculatorScreen
import com.example.pocketnotev20.ui.notes.NotesScreen
import com.example.pocketnotev20.ui.profile.ProfileScreen
import com.example.pocketnotev20.ui.questions.QuestionBankScreen
import com.example.pocketnotev20.ui.reminder.AssignmentReminderScreen
import com.example.pocketnotev20.ui.routine.RoutineScreen
import com.example.pocketnotev20.ui.ctslots.CtSlotsScreen
import com.example.pocketnotev20.ui.ctmarks.CtMarksCalculatorScreen
import com.example.pocketnotev20.ui.common.UserBottomBar
import com.example.pocketnotev20.ui.theme.PocketNoteV20Theme
import android.util.Log
import com.example.pocketnotev20.repository.FirestoreRepository

class MainActivity : ComponentActivity() {
    private val repository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository.seedAllData(
            onSuccess = {
                Log.d("FirestoreSeed", "Seed data added successfully")
            },
            onFailure = { e ->
                Log.e("FirestoreSeed", "Failed to seed data", e)
            }
        )
        setContent {
            PocketNoteV20Theme {
                PocketNoteApp()
            }
        }
    }
}

@Composable
fun PocketNoteApp() {
    var currentScreen by remember { mutableStateOf("login") }
    var currentRole by remember { mutableStateOf("user") }
    
    val userSubScreens = remember {
        setOf(
            "questions", "calendar", "routine", "notes", "ctslots", 
            "profile", "gpa_calculator", "assignment_reminder", "important_dates", "ct_marks"
        )
    }
    
    val adminSubScreens = remember {
        setOf(
            "admin_profile", "sub_admin_requests", "sub_admin_status", 
            "add_question", "add_calendar", "add_routine", "add_note", 
            "add_ct_slot", "manage_users"
        )
    }

    val userBottomBarScreens = userSubScreens + "dashboard"
    val adminBottomBarScreens = adminSubScreens + "admin_dashboard"

    val showUserBottomBar = currentScreen in userBottomBarScreens
    val showAdminBottomBar = currentScreen in adminBottomBarScreens

    // Back button handling logic
    BackHandler(enabled = currentScreen != "login" && currentScreen != "dashboard" && currentScreen != "admin_dashboard") {
        currentScreen = when {
            currentScreen == "signup" -> "login"
            currentScreen in userSubScreens -> "dashboard"
            currentScreen in adminSubScreens -> "admin_dashboard"
            else -> "login"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            "login" -> LoginScreen(
                onLoginSuccess = { role ->
                    currentRole = role
                    currentScreen = if (role == "admin" || role.startsWith("sub_admin")) "admin_dashboard" else "dashboard"
                },
                onSignupClick = { currentScreen = "signup" }
            )

            "signup" -> SignupScreen(
                onSignupDone = { currentScreen = "login" },
                onBackClick = { currentScreen = "login" }
            )

            "dashboard" -> DashboardScreen(
                onNotesClick = { currentScreen = "notes" },
                onRoutineClick = { currentScreen = "routine" },
                onCtSlotsClick = { currentScreen = "ctslots" },
                onCalendarClick = { currentScreen = "calendar" },
                onQuestionsClick = { currentScreen = "questions" },
                onNotificationsClick = { currentScreen = "assignment_reminder" },
                onImportantDatesClick = { currentScreen = "important_dates" },
                onGpaClick = { currentScreen = "gpa_calculator" },
                onProfileClick = { currentScreen = "profile" },
                onLogoutClick = {
                    currentRole = "user"
                    currentScreen = "login"
                },
                onExamCountdownClick = { currentScreen = "important_dates" },
                onCtMarksClick = { currentScreen = "ct_marks" }
            )

            "admin_dashboard" -> AdminDashboardScreen(
                roleLabel = when (currentRole) {
                    "admin" -> "Main Admin"
                    "sub_admin_pending" -> "Pending Sub Admin"
                    "sub_admin_rejected" -> "Rejected Request"
                    else -> "Sub Admin"
                },
                isMainAdmin = currentRole == "admin",
                subAdminStatus = when (currentRole) {
                    "sub_admin_pending" -> "pending"
                    "sub_admin_rejected" -> "rejected"
                    else -> "approved"
                },
                showNotifications = currentRole == "admin" || currentRole.startsWith("sub_admin"),
                canManageContent = currentRole == "admin" || currentRole == "sub_admin_approved",
                onNotificationsClick = {
                    currentScreen = if (currentRole == "admin") "sub_admin_requests" else "sub_admin_status"
                },
                onAdminProfileClick = { currentScreen = "admin_profile" },
                onManageUsersClick = { currentScreen = "manage_users" },
                onAddQuestionClick = { currentScreen = "add_question" },
                onAddCalendarClick = { currentScreen = "add_calendar" },
                onAddRoutineClick = { currentScreen = "add_routine" },
                onAddNoteClick = { currentScreen = "add_note" },
                onAddCtSlotClick = { currentScreen = "add_ct_slot" },
                onLogoutClick = {
                    currentRole = "user"
                    currentScreen = "login"
                }
            )

            "add_note" -> AddNoteScreen(
                onBackClick = { currentScreen = "admin_dashboard" }
            )

            "add_question" -> AddQuestionScreen(
                onBackClick = { currentScreen = "admin_dashboard" }
            )

            "add_routine" -> AddRoutineScreen(
                onBackClick = { currentScreen = "admin_dashboard" }
            )

            "add_calendar" -> AddCalendarScreen(
                onBackClick = { currentScreen = "admin_dashboard" }
            )

            "add_ct_slot" -> AddCtSlotScreen(
                onBackClick = { currentScreen = "admin_dashboard" }
            )

            "sub_admin_requests" -> SubAdminRequestsScreen(
                onBackClick = { currentScreen = "admin_dashboard" }
            )

            "sub_admin_status" -> SubAdminStatusScreen(
                onBackClick = { currentScreen = "admin_dashboard" }
            )

            "manage_users" -> ManageUsersScreen(
                onBackClick = { currentScreen = "admin_dashboard" }
            )

            "questions" -> QuestionBankScreen(
                onBackClick = { currentScreen = "dashboard" }
            )

            "calendar" -> CalendarScreen(
                onBackClick = { currentScreen = "dashboard" }
            )

            "routine" -> RoutineScreen(
                onBackClick = { currentScreen = "dashboard" }
            )

            "notes" -> NotesScreen(
                onBackClick = { currentScreen = "dashboard" }
            )

            "ctslots" -> CtSlotsScreen(
                onBackClick = { currentScreen = "dashboard" }
            )

            "profile" -> ProfileScreen(
                onBackClick = { currentScreen = "dashboard" },
                reserveBottomBarSpace = true,
                onLogoutClick = {
                    currentRole = "user"
                    currentScreen = "login"
                }
            )

            "admin_profile" -> ProfileScreen(
                onBackClick = { currentScreen = "admin_dashboard" },
                reserveBottomBarSpace = true,
                onLogoutClick = {
                    currentRole = "user"
                    currentScreen = "login"
                }
            )

            "gpa_calculator" -> GpaCalculatorScreen(
                onBackClick = { currentScreen = "dashboard" }
            )

            "assignment_reminder" -> AssignmentReminderScreen(
                onBackClick = { currentScreen = "dashboard" }
            )

            "important_dates" -> ImportantDatesScreen(
                onBackClick = { currentScreen = "dashboard" }
            )

            "ct_marks" -> CtMarksCalculatorScreen(
                onBackClick = { currentScreen = "dashboard" }
            )
        }

        if (showUserBottomBar) {
            UserBottomBar(
                currentScreen = currentScreen,
                onDashboardClick = { currentScreen = "dashboard" },
                onNotesClick = { currentScreen = "notes" },
                onProfileClick = { currentScreen = "profile" },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            )
        }

        if (showAdminBottomBar) {
            AdminBottomBar(
                currentScreen = currentScreen,
                isMainAdmin = currentRole == "admin",
                onRequestsClick = {
                    currentScreen = if (currentRole == "admin") "sub_admin_requests" else "sub_admin_status"
                },
                onWorkspaceClick = { currentScreen = "admin_dashboard" },
                onProfileClick = { currentScreen = "admin_profile" },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            )
        }
    }
}
