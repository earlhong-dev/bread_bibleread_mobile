package com.bibleread.bread.viewmodel

import android.app.Application
import android.content.Context
import java.io.File
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
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

    private val prefs = app.getSharedPreferences("bible_prefs", Context.MODE_PRIVATE)

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

    // Remember last position so we can reload it after a translation switch and app restart
    var lastBook    = prefs.getString("last_book", "Genesis") ?: "Genesis"
        private set
    var lastChapter = prefs.getInt("last_chapter", 1)
        private set

    // Last scroll index within the chapter list (item index in LazyColumn)
    var lastScrollIndex = prefs.getInt("last_scroll_index", 0)
        private set

    fun saveScrollIndex(index: Int) {
        if (index == lastScrollIndex) return
        lastScrollIndex = index
        prefs.edit().putInt("last_scroll_index", index).apply()
    }

    var fontSize = prefs.getFloat("font_size", 18f)
        private set

    fun saveFontSize(size: Float) {
        if (size == fontSize) return
        fontSize = size
        prefs.edit().putFloat("font_size", size).apply()
    }

    var fontStyle = prefs.getString("font_style", "Sans-Serif") ?: "Sans-Serif"
        private set

    fun saveFontStyle(style: String) {
        if (style == fontStyle) return
        fontStyle = style
        prefs.edit().putString("font_style", style).apply()
    }

    // Persisted selected highlight color
    val selectedHighlightColor = mutableStateOf<Color?>(
        if (prefs.contains("last_color")) Color(prefs.getInt("last_color", 0)) else null
    )

    fun selectHighlightColor(color: Color?) {
        selectedHighlightColor.value = color
        if (color != null) {
            prefs.edit().putInt("last_color", color.toArgb()).apply()
        }
    }

    // Highlights: verseKey ("book-chapter-verse") → Color
    val highlights = mutableStateMapOf<String, Color>().apply {
        val saved = prefs.getString("highlights_json", "{}")
        try {
            val json = JSONObject(saved!!)
            json.keys().forEach { key ->
                put(key, Color(json.getInt(key)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveHighlights() {
        val json = JSONObject()
        highlights.forEach { (key, color) -> json.put(key, color.toArgb()) }
        prefs.edit().putString("highlights_json", json.toString()).apply()
    }

    // Custom colors saved by the user
    val customColors = mutableStateListOf<Color>().apply {
        val saved = prefs.getString("custom_colors_json", "[]")
        try {
            val array = JSONArray(saved!!)
            for (i in 0 until array.length()) {
                add(Color(array.getInt(i)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCustomColors() {
        val array = JSONArray()
        customColors.forEach { array.put(it.toArgb()) }
        prefs.edit().putString("custom_colors_json", array.toString()).apply()
    }

    fun addCustomColor(color: Color) {
        if (!customColors.contains(color)) {
            customColors.add(color)
            saveCustomColors()
        }
    }

    fun removeCustomColor(color: Color) {
        customColors.remove(color)
        saveCustomColors()
    }

    var lastCustomHex = prefs.getString("last_custom_hex", "FF0000") ?: "FF0000"

    fun saveLastCustomHex(hex: String) {
        lastCustomHex = hex
        prefs.edit().putString("last_custom_hex", hex).apply()
    }

    val customFonts = mutableStateListOf<File>()

    private fun loadCustomFonts() {
        val fontsDir = File(getApplication<Application>().filesDir, "custom_fonts")
        if (!fontsDir.exists()) fontsDir.mkdirs()
        customFonts.clear()
        fontsDir.listFiles()?.forEach { file ->
            if (file.extension.equals("ttf", true) || file.extension.equals("otf", true)) {
                customFonts.add(file)
            }
        }
    }

    fun importCustomFont(uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val fontsDir = File(context.filesDir, "custom_fonts")
                if (!fontsDir.exists()) fontsDir.mkdirs()
                
                var fileName = "custom_font_${System.currentTimeMillis()}.ttf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                }
                
                val targetFile = File(fontsDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    loadCustomFonts()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeCustomFont(fontName: String) {
        val fileToRemove = customFonts.find { it.nameWithoutExtension == fontName }
        if (fileToRemove != null && fileToRemove.exists()) {
            fileToRemove.delete()
            customFonts.remove(fileToRemove)
            // If the deleted font was currently selected, revert to default
            if (fontStyle == fontName) {
                saveFontStyle("Sans-Serif")
            }
        }
    }

    init {
        loadCustomFonts()
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

    fun loadChapter(book: String, chapter: Int, resetScroll: Boolean = true) {
        lastBook    = book
        lastChapter = chapter
        if (resetScroll) {
            lastScrollIndex = 0
            prefs.edit().putString("last_book", book).putInt("last_chapter", chapter).putInt("last_scroll_index", 0).apply()
        } else {
            prefs.edit().putString("last_book", book).putInt("last_chapter", chapter).apply()
        }
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
        saveHighlights()
    }

    fun removeHighlight(verseKey: String) {
        highlights.remove(verseKey)
        saveHighlights()
    }
}
