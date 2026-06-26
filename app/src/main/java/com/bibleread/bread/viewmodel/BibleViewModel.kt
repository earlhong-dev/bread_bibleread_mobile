package com.bibleread.bread.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bibleread.bread.data.BibleDatabase
import com.bibleread.bread.data.BibleRepository
import com.bibleread.bread.data.TranslationManager
import com.bibleread.bread.data.VerseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class BibleUiState {
    object Idle    : BibleUiState()
    object Loading : BibleUiState()
    data class Success(val verses: List<VerseEntity>) : BibleUiState()
    data class Error(val message: String) : BibleUiState()
}

class BibleViewModel(app: Application) : AndroidViewModel(app) {

    // Active translation code, e.g. "mbbtag05"
    private val _activeTranslation = MutableStateFlow(
        TranslationManager.getActiveTranslation(app)
    )
    val activeTranslation: StateFlow<String> = _activeTranslation

    // Available translations detected from assets/translations/
    val availableTranslations: List<String> =
        TranslationManager.getAvailableTranslations(app)

    private var repository = buildRepository(_activeTranslation.value)

    private val _uiState = MutableStateFlow<BibleUiState>(BibleUiState.Idle)
    val uiState: StateFlow<BibleUiState> = _uiState

    // Remember last position so we can reload it after a translation switch
    private var lastBook    = "Genesis"
    private var lastChapter = 1

    // Highlights: verseKey ("book-chapter-verse") → Color
    // Stored in ViewModel so they persist across chapter navigation
    val highlights = mutableStateMapOf<String, Color>()

    init {
        // Pre-built DB is ready immediately — load the default chapter directly
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val t0 = android.os.SystemClock.elapsedRealtime()
            loadChapter(lastBook, lastChapter)
            android.util.Log.d("Bread.Startup", "First chapter loaded in ${android.os.SystemClock.elapsedRealtime() - t0}ms")
        }
    }

    /** Switch to a different translation and reload the current position. */
    fun switchTranslation(translationCode: String) {
        if (translationCode == _activeTranslation.value) return

        // Persist the choice
        TranslationManager.setActiveTranslation(getApplication(), translationCode)
        _activeTranslation.value = translationCode

        // Rebuild repository pointing at the new DB
        repository = buildRepository(translationCode)

        // Reload the same book/chapter in the new translation
        loadChapter(lastBook, lastChapter)
    }

    fun loadChapter(book: String, chapter: Int) {
        lastBook    = book
        lastChapter = chapter
        _uiState.value = BibleUiState.Loading
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val verses = repository.getChapter(book, chapter)
                _uiState.value = BibleUiState.Success(verses)
            } catch (e: Exception) {
                _uiState.value = BibleUiState.Error("Failed to load.")
            }
        }
    }

    private fun buildRepository(translationCode: String): BibleRepository {
        val db = BibleDatabase.getInstance(getApplication(), translationCode)
        return BibleRepository(db.verseDao(), db.bookmarkDao())
    }

    fun applyHighlight(verseKeys: Set<String>, color: Color) {
        verseKeys.forEach { highlights[it] = color }
    }

    fun removeHighlight(verseKey: String) {
        highlights.remove(verseKey)
    }
}
