package com.example.pocketnotev20.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketnotev20.model.QuestionItem
import com.example.pocketnotev20.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QuestionBankViewModel : ViewModel() {
    private val repository = FirestoreRepository()

    private val _questions = MutableStateFlow<List<QuestionItem>>(emptyList())
    val questions: StateFlow<List<QuestionItem>> = _questions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    init {
        loadQuestions()
    }

    fun loadQuestions() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""
            repository.getQuestions(
                onSuccess = { list ->
                    _questions.value = list
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Unable to load questions."
                    _isLoading.value = false
                }
            )
        }
    }
}
