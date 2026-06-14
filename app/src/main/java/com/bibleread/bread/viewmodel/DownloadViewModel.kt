package com.bibleread.bread.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bibleread.bread.data.BibleDatabase
import com.bibleread.bread.data.BibleRepository
import com.bibleread.bread.ui.screens.BIBLE_BOOKS
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BookDownloadState(
    val book: String,
    val isDownloaded: Boolean,
    val isDownloading: Boolean
)

class DownloadViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = BibleRepository(BibleDatabase.getInstance(app).verseDao())

    private val _downloading = MutableStateFlow<Set<String>>(emptySet())

    val bookStates: StateFlow<List<BookDownloadState>> =
        combine(repository.getDownloadedBooks(), _downloading) { downloaded, downloading ->
            BIBLE_BOOKS.keys.map { book ->
                BookDownloadState(
                    book = book,
                    isDownloaded = downloaded.contains(book),
                    isDownloading = downloading.contains(book)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun downloadBook(book: String) {
        if (_downloading.value.contains(book)) return
        viewModelScope.launch {
            _downloading.value = _downloading.value + book
            try {
                val chapterCount = BIBLE_BOOKS[book] ?: return@launch
                repository.downloadBook(book, chapterCount)
            } finally {
                _downloading.value = _downloading.value - book
            }
        }
    }
}
