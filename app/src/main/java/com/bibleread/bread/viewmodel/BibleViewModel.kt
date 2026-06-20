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
    object Idle : BibleUiState()
    object Loading : BibleUiState()
    data class Success(val verses: List<VerseEntity>) : BibleUiState()
    data class Error(val message: String) : BibleUiState()
}

class BibleViewModel(app: Application) : AndroidViewModel(app) {

    private val db = BibleDatabase.getInstance(app)
    private val repository = BibleRepository(db.verseDao(), db.bookmarkDao())

    private val _uiState = MutableStateFlow<BibleUiState>(BibleUiState.Idle)
    val uiState: StateFlow<BibleUiState> = _uiState

    init {
        // Load Genesis 1 as default — waits if DB is still being populated
        viewModelScope.launch {
            // Poll until DB has data (parse may still be running on first launch)
            var attempts = 0
            while (attempts < 30) {
                val count = repository.getTotalVerseCount()
                if (count > 0) {
                    loadChapter("Genesis", 1)
                    break
                }
                kotlinx.coroutines.delay(1000)
                attempts++
            }
        }
    }

    fun loadBook(book: String) {
        _uiState.value = BibleUiState.Loading
        viewModelScope.launch {
            try {
                val verses = repository.getBook(book)
                if (verses.isEmpty()) {
                    _uiState.value = BibleUiState.Error("Book not found in database.")
                } else {
                    _uiState.value = BibleUiState.Success(verses)
                }
            } catch (e: Exception) {
                _uiState.value = BibleUiState.Error("Failed to load from database.")
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
