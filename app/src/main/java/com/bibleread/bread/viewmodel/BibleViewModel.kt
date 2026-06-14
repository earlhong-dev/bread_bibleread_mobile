package com.bibleread.bread.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bibleread.bread.data.BibleDatabase
import com.bibleread.bread.data.BibleRepository
import com.bibleread.bread.data.VerseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class BibleUiState {
    object Loading : BibleUiState()
    data class Success(val verses: List<VerseEntity>) : BibleUiState()
    data class Error(val message: String) : BibleUiState()
}

class BibleViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = BibleRepository(BibleDatabase.getInstance(app).verseDao())

    private val _uiState = MutableStateFlow<BibleUiState>(BibleUiState.Loading)
    val uiState: StateFlow<BibleUiState> = _uiState

    fun loadBook(book: String, chapterCount: Int) {
        _uiState.value = BibleUiState.Loading
        viewModelScope.launch {
            try {
                val verses = repository.getBook(book, chapterCount)
                _uiState.value = BibleUiState.Success(verses)
            } catch (e: Exception) {
                _uiState.value = BibleUiState.Error("Failed to load. Check your connection.")
            }
        }
    }

    fun loadChapter(book: String, chapter: Int) {
        _uiState.value = BibleUiState.Loading
        viewModelScope.launch {
            try {
                val verses = repository.getChapter(book, chapter)
                _uiState.value = BibleUiState.Success(verses)
            } catch (e: Exception) {
                _uiState.value = BibleUiState.Error("Failed to load. Check your connection.")
            }
        }
    }
}
