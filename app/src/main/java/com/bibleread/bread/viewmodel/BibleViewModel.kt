package com.bibleread.bread.viewmodel

import android.app.Application
import android.content.Context
import java.io.File
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

    // Scroll position is intentionally not persisted so the Bible tab always opens at the start of the chapter.
    var lastScrollIndex = 0
        private set

    // Observable appearance settings so composables recompose immediately
    var fontSize by mutableStateOf(prefs.getFloat("font_size", 18f))
        private set

    fun saveFontSize(size: Float) {
        if (size == fontSize) return
        fontSize = size
        prefs.edit().putFloat("font_size", size).apply()
    }

    var fontStyle by mutableStateOf(prefs.getString("font_style", "Serif") ?: "Serif")
        private set

    fun saveFontStyle(style: String) {
        if (style == fontStyle) return
        fontStyle = style
        prefs.edit().putString("font_style", style).apply()
    }

    var scriptureFontStyle by mutableStateOf(prefs.getString("scripture_font_style", fontStyle) ?: fontStyle)
        private set

    fun saveScriptureFontStyle(style: String) {
        if (style == scriptureFontStyle) return
        scriptureFontStyle = style
        prefs.edit().putString("scripture_font_style", style).apply()
    }

    var selectedThemeIndex by mutableStateOf(prefs.getInt("theme_index", 0)) // Default to 0 (Dark)
        private set

    fun saveThemeIndex(index: Int) {
        if (index == selectedThemeIndex) return
        selectedThemeIndex = index
        prefs.edit().putInt("theme_index", index).apply()
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
        val saved = prefs.getString("highlights_json", "{}") ?: "{}"
        try {
            val json = JSONObject(saved)
            json.keys().forEach { key ->
                // Silently skip any malformed entries instead of crashing
                try { put(key, Color(json.getInt(key))) } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            android.util.Log.w("BibleViewModel", "highlights_json corrupted, resetting: ${e.message}")
            // Leave map empty — user loses highlights but app stays functional
        }
    }

    private fun saveHighlights() {
        val json = JSONObject()
        highlights.forEach { (key, color) -> json.put(key, color.toArgb()) }
        prefs.edit().putString("highlights_json", json.toString()).apply()
    }

    // Custom colors saved by the user
    val customColors = mutableStateListOf<Color>().apply {
        val saved = prefs.getString("custom_colors_json", "[]") ?: "[]"
        try {
            val array = JSONArray(saved)
            for (i in 0 until array.length()) {
                try { add(Color(array.getInt(i))) } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            android.util.Log.w("BibleViewModel", "custom_colors_json corrupted, resetting: ${e.message}")
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
        private set

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
                            val rawName = cursor.getString(nameIndex) ?: fileName
                            // Strip any path components from the display name to prevent traversal
                            fileName = File(rawName).name.ifBlank { fileName }
                        }
                    }
                }

                // Only allow .ttf and .otf files
                val ext = fileName.substringAfterLast('.', "").lowercase()
                if (ext != "ttf" && ext != "otf") {
                    android.util.Log.w("BibleViewModel", "Rejected non-font file: $fileName")
                    return@launch
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
                android.util.Log.e("BibleViewModel", "Failed to import font: ${e.message}", e)
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
            if (scriptureFontStyle == fontName) {
                saveScriptureFontStyle("Sans-Serif")
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
            prefs.edit().putString("last_book", book).putInt("last_chapter", chapter).apply()
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
        // Note: Room database is unencrypted since it only contains public Bible text
        // No sensitive user data is stored in this database
        val db = BibleDatabase.getInstance(getApplication(), translationCode)
        return BibleRepository(db.verseDao(), db.bookmarkDao())
    }

    // Bible tab view mode: "carousel" or "list"
    var bibleViewMode by mutableStateOf(prefs.getString("bible_view_mode", "carousel") ?: "carousel")
        private set

    fun saveBibleViewMode(mode: String) {
        if (mode == bibleViewMode) return
        bibleViewMode = mode
        prefs.edit().putString("bible_view_mode", mode).apply()
    }

    // Bible tab last selected filter — session only (not persisted)
    var bibleSelectedFilter by mutableStateOf("All Books")
        private set

    fun saveBibleFilter(filter: String) {
        bibleSelectedFilter = filter
    }

    // Bible tab last carousel index — session only (not persisted)
    var bibleCarouselIndex by mutableStateOf(0)
        private set

    fun saveBibleCarouselIndex(index: Int) {
        bibleCarouselIndex = index
    }
    val readChapters = androidx.compose.runtime.mutableStateSetOf<String>().apply {
        val saved = prefs.getStringSet("read_chapters", emptySet()) ?: emptySet()
        addAll(saved)
    }

    fun markChapterRead(book: String, chapter: Int) {
        val key = "$book-$chapter"
        if (readChapters.add(key)) {
            prefs.edit().putStringSet("read_chapters", readChapters.toSet()).apply()
        }
    }

    fun unmarkChapterRead(book: String, chapter: Int) {
        val key = "$book-$chapter"
        if (readChapters.remove(key)) {
            prefs.edit().putStringSet("read_chapters", readChapters.toSet()).apply()
        }
    }

    fun isChapterRead(book: String, chapter: Int): Boolean =
        readChapters.contains("$book-$chapter")

    fun readChapterCount(book: String): Int =
        readChapters.count { it.startsWith("$book-") }

    fun totalChapterCount(book: String): Int =
        CHAPTER_COUNT[book] ?: 1

    fun applyHighlight(verseKeys: Set<String>, color: Color) {
        verseKeys.forEach { highlights[it] = color }
        saveHighlights()
    }

    fun removeHighlight(verseKey: String) {
        highlights.remove(verseKey)
        saveHighlights()
    }
}

// Chapter counts mirrored from ScriptureScreen — used for progress calculation
val CHAPTER_COUNT = mapOf(
    "Genesis" to 50, "Exodo" to 40, "Levitico" to 27, "Mga Bilang" to 36,
    "Deuteronomio" to 34, "Josue" to 24, "Mga Hukom" to 21, "Ruth" to 4,
    "1 Samuel" to 31, "2 Samuel" to 24, "1 Mga Hari" to 22, "2 Mga Hari" to 25,
    "1 Mga Cronica" to 29, "2 Mga Cronica" to 36, "Ezra" to 10, "Nehemias" to 13,
    "Ester" to 10, "Job" to 42, "Mga Awit" to 150, "Mga Kawikaan" to 31,
    "Ang Mangangaral" to 12, "Ang Awit ni Solomon" to 8, "Isaias" to 66,
    "Jeremias" to 52, "Mga Panaghoy" to 5, "Ezekiel" to 48, "Daniel" to 12,
    "Hosea" to 14, "Joel" to 3, "Amos" to 9, "Obadias" to 1, "Jonas" to 4,
    "Mikas" to 7, "Nahum" to 3, "Habakuk" to 3, "Zefanias" to 3, "Hagai" to 2,
    "Zacarias" to 14, "Malakias" to 4,
    "Mateo" to 28, "Marcos" to 16, "Lucas" to 24, "Juan" to 21, "Mga Gawa" to 28,
    "Mga Taga-Roma" to 16, "1 Mga Taga-Corinto" to 16, "2 Mga Taga-Corinto" to 13,
    "Mga Taga-Galacia" to 6, "Mga Taga-Efeso" to 6, "Mga Taga-Filipos" to 4,
    "Mga Taga-Colosas" to 4, "1 Mga Taga-Tesalonica" to 5, "2 Mga Taga-Tesalonica" to 3,
    "1 Timoteo" to 6, "2 Timoteo" to 4, "Tito" to 3, "Filemon" to 1,
    "Mga Hebreo" to 13, "Santiago" to 5, "1 Pedro" to 5, "2 Pedro" to 3,
    "1 Juan" to 5, "2 Juan" to 1, "3 Juan" to 1, "Judas" to 1, "Pahayag" to 22
)
