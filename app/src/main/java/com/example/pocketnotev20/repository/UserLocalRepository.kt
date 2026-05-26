package com.example.pocketnotev20.repository

import android.content.Context
import com.example.pocketnotev20.model.AssignmentReminderItem
import com.example.pocketnotev20.model.ImportantDateItem
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class UserLocalRepository(context: Context) {

    private val preferences = context.getSharedPreferences("user_feature_prefs", Context.MODE_PRIVATE)

    fun getAssignmentReminders(): List<AssignmentReminderItem> {
        val rawValue = preferences.getString(KEY_ASSIGNMENTS, "[]").orEmpty()
        val items = mutableListOf<AssignmentReminderItem>()
        val array = JSONArray(rawValue)

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            items += AssignmentReminderItem(
                id = item.optString("id"),
                title = item.optString("title"),
                course = item.optString("course"),
                dueDate = item.optString("dueDate"),
                note = item.optString("note"),
                isDone = item.optBoolean("isDone")
            )
        }

        return items.sortedWith(
            compareBy<AssignmentReminderItem>({ it.isDone }, { dateSortValue(it.dueDate) }, { it.title })
        )
    }

    fun saveAssignmentReminder(item: AssignmentReminderItem) {
        val updatedItems = getAssignmentReminders().toMutableList()
        val itemToSave = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
        val existingIndex = updatedItems.indexOfFirst { it.id == itemToSave.id }

        if (existingIndex >= 0) {
            updatedItems[existingIndex] = itemToSave
        } else {
            updatedItems += itemToSave
        }

        persistAssignments(updatedItems)
    }

    fun deleteAssignmentReminder(id: String) {
        persistAssignments(getAssignmentReminders().filterNot { it.id == id })
    }

    fun toggleAssignmentReminderDone(id: String) {
        val updatedItems = getAssignmentReminders().map { item ->
            if (item.id == id) item.copy(isDone = !item.isDone) else item
        }
        persistAssignments(updatedItems)
    }

    fun getImportantDates(): List<ImportantDateItem> {
        val rawValue = preferences.getString(KEY_IMPORTANT_DATES, "[]").orEmpty()
        val items = mutableListOf<ImportantDateItem>()
        val array = JSONArray(rawValue)

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            items += ImportantDateItem(
                id = item.optString("id"),
                title = item.optString("title"),
                date = item.optString("date"),
                category = item.optString("category"),
                note = item.optString("note")
            )
        }

        return items.sortedWith(compareBy<ImportantDateItem>({ dateSortValue(it.date) }, { it.title }))
    }

    fun saveImportantDate(item: ImportantDateItem) {
        val updatedItems = getImportantDates().toMutableList()
        val itemToSave = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
        val existingIndex = updatedItems.indexOfFirst { it.id == itemToSave.id }

        if (existingIndex >= 0) {
            updatedItems[existingIndex] = itemToSave
        } else {
            updatedItems += itemToSave
        }

        persistImportantDates(updatedItems)
    }

    fun deleteImportantDate(id: String) {
        persistImportantDates(getImportantDates().filterNot { it.id == id })
    }

    fun getUpcomingImportantDate(): ImportantDateItem? {
        val today = startOfDay(Calendar.getInstance())
        return getImportantDates()
            .filter { dateToCalendar(it.date)?.timeInMillis?.let { time -> time >= today.timeInMillis } == true }
            .minByOrNull { dateSortValue(it.date) }
    }

    fun getDaysUntil(date: String): Long? {
        val targetDate = dateToCalendar(date) ?: return null
        val today = startOfDay(Calendar.getInstance())
        val diffMillis = targetDate.timeInMillis - today.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMillis)
    }

    private fun persistAssignments(items: List<AssignmentReminderItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("course", item.course)
                    put("dueDate", item.dueDate)
                    put("note", item.note)
                    put("isDone", item.isDone)
                }
            )
        }
        preferences.edit().putString(KEY_ASSIGNMENTS, array.toString()).apply()
    }

    private fun persistImportantDates(items: List<ImportantDateItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("date", item.date)
                    put("category", item.category)
                    put("note", item.note)
                }
            )
        }
        preferences.edit().putString(KEY_IMPORTANT_DATES, array.toString()).apply()
    }

    private fun dateSortValue(date: String): Long {
        return dateToCalendar(date)?.timeInMillis ?: Long.MAX_VALUE
    }

    private fun dateToCalendar(date: String): Calendar? {
        val formatter = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
        formatter.isLenient = false
        val parsedDate = runCatching { formatter.parse(date) }.getOrNull() ?: return null
        return Calendar.getInstance().apply { time = parsedDate }
    }

    private fun startOfDay(calendar: Calendar): Calendar {
        return (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    companion object {
        private const val KEY_ASSIGNMENTS = "assignment_reminders"
        private const val KEY_IMPORTANT_DATES = "important_dates"
        const val DATE_PATTERN = "yyyy-MM-dd"
    }
}
