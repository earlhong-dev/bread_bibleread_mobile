package com.bibleread.bread.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bibleread.bread.NoteCallbacks
import com.bibleread.bread.R
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent

// ── Note model ────────────────────────────────────────────────────────────────
data class NoteEntry(
    val id: Long,
    val title: String,
    val body: String,
    val timestamp: Long,
    val fontSizesJson: String = "", // Line-level font sizes
    val charStylesJson: String = "", // Character-level bold/italic/underline
    val lineIdsJson: String = ""     // Stable per-line identity, parallel to body's lines
)

// Line formatting that sticks to content, not position
private data class LineFormatData(
    val contentHash: String, // Hash of the line content to identify it
    val fontSize: String = "Aa",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false
)

// Character-level style tracking
private data class CharacterStyles(
    val charIndex: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false
)

private enum class TextStyleFlag { BOLD, ITALIC, UNDERLINE }

// Apply style to selected character range
private fun applyStyleToRange(
    text: String,
    selection: TextRange,
    existingStyles: Map<Int, CharacterStyles>,
    flag: TextStyleFlag,
    enabled: Boolean
): Map<Int, CharacterStyles> {
    val result = existingStyles.toMutableMap()
    val start = selection.start.coerceIn(0, text.length)
    val end = selection.end.coerceIn(0, text.length)
    
    if (start == end) return result // No selection
    
    for (i in start until end) {
        val currentStyle = result[i] ?: CharacterStyles(i)
        result[i] = when (flag) {
            TextStyleFlag.BOLD -> currentStyle.copy(bold = enabled)
            TextStyleFlag.ITALIC -> currentStyle.copy(italic = enabled)
            TextStyleFlag.UNDERLINE -> currentStyle.copy(underline = enabled)
        }
    }
    
    return result
}

// Update character styles when text is edited (insert/delete)
private fun updateCharacterStylesForEdit(
    oldText: String,
    newText: String,
    oldStyles: Map<Int, CharacterStyles>
): Map<Int, CharacterStyles> {
    if (oldText == newText) return oldStyles
    
    val result = mutableMapOf<Int, CharacterStyles>()
    
    // Find the common prefix
    val prefixLength = oldText.zip(newText).takeWhile { it.first == it.second }.count()
    
    // Find the common suffix
    val suffixLength = oldText.drop(prefixLength).reversed()
        .zip(newText.drop(prefixLength).reversed())
        .takeWhile { it.first == it.second }.count()
    
    val oldEditStart = prefixLength
    val oldEditEnd = oldText.length - suffixLength
    val newEditStart = prefixLength
    val newEditEnd = newText.length - suffixLength
    
    val charsDeleted = oldEditEnd - oldEditStart
    val charsInserted = newEditEnd - newEditStart
    
    // Copy styles before edit point
    for (i in 0 until oldEditStart) {
        oldStyles[i]?.let { result[i] = it.copy(charIndex = i) }
    }
    
    // For inserted characters, inherit style from character at cursor position
    // Exception: Don't inherit underline when pressing Enter (inserting newline)
    if (charsInserted > 0 && oldEditStart > 0) {
        val inheritStyle = oldStyles[oldEditStart - 1]
        if (inheritStyle != null) {
            val insertedText = newText.substring(newEditStart, newEditEnd)
            val isPressedEnter = insertedText == "\n"
            
            for (i in newEditStart until newEditEnd) {
                // If user pressed Enter, don't inherit underline for the newline
                result[i] = if (isPressedEnter) {
                    inheritStyle.copy(charIndex = i, underline = false)
                } else {
                    // Normal typing - inherit all styles including underline
                    inheritStyle.copy(charIndex = i)
                }
            }
        }
    }
    
    // Copy styles after edit point (shifted by the difference)
    for (i in oldEditEnd until oldText.length) {
        oldStyles[i]?.let { 
            val newIndex = i - charsDeleted + charsInserted
            if (newIndex >= 0 && newIndex < newText.length) {
                result[newIndex] = it.copy(charIndex = newIndex)
            }
        }
    }
    
    return result
}

// Serialize character styles to JSON
private fun characterStylesToJson(styles: Map<Int, CharacterStyles>): String {
    val array = JSONArray()
    styles.values.forEach { style ->
        array.put(JSONObject().apply {
            put("index", style.charIndex)
            put("bold", style.bold)
            put("italic", style.italic)
            put("underline", style.underline)
        })
    }
    return array.toString()
}

// Deserialize character styles from JSON
private fun jsonToCharacterStyles(json: String?): Map<Int, CharacterStyles> {
    if (json.isNullOrBlank()) return emptyMap()
    return try {
        val result = mutableMapOf<Int, CharacterStyles>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val style = CharacterStyles(
                charIndex = obj.getInt("index"),
                bold = obj.optBoolean("bold", false),
                italic = obj.optBoolean("italic", false),
                underline = obj.optBoolean("underline", false)
            )
            result[style.charIndex] = style
        }
        result
    } catch (_: Exception) {
        emptyMap()
    }
}

// Create a stable hash from line content
// Stable per-line identity — NOT derived from text content, so duplicate
// lines never collide on the same formatting entry.
private fun getLineId(lineIds: List<String>, index: Int): String =
    lineIds.getOrNull(index) ?: java.util.UUID.randomUUID().toString()

private fun newLineId(): String = java.util.UUID.randomUUID().toString()

private fun lineIdsToJson(ids: List<String>): String {
    val arr = JSONArray()
    ids.forEach { arr.put(it) }
    return arr.toString()
}

private fun jsonToLineIds(json: String?, fallbackLineCount: Int): List<String> {
    if (json.isNullOrBlank()) {
        return List(fallbackLineCount) { newLineId() }
    }
    return try {
        val arr = JSONArray(json)
        val result = (0 until arr.length()).map { arr.getString(it) }
        if (result.size == fallbackLineCount) result
        else {
            // Mismatch (e.g. corrupted/old data) — regenerate safely
            List(fallbackLineCount) { newLineId() }
        }
    } catch (_: Exception) {
        List(fallbackLineCount) { newLineId() }
    }
}

// Diff old line list vs new line list and update ids to match, by POSITION,
// never by content. This is what replaces updateFormatMapForEdit.
private fun updateLineIdsForEdit(
    oldLineCount: Int,
    newLineCount: Int,
    oldLineIds: List<String>,
    editedAtLineIndex: Int
): List<String> {
    val ids = oldLineIds.toMutableList()
    // Defensive: make sure ids matches oldLineCount before diffing
    while (ids.size < oldLineCount) ids.add(newLineId())
    while (ids.size > oldLineCount) ids.removeAt(ids.lastIndex)

    when {
        newLineCount > oldLineCount -> {
            val insertCount = newLineCount - oldLineCount
            repeat(insertCount) {
                val insertAt = (editedAtLineIndex + 1).coerceIn(0, ids.size)
                ids.add(insertAt, newLineId())
            }
        }
        newLineCount < oldLineCount -> {
            val removeCount = oldLineCount - newLineCount
            repeat(removeCount) {
                if (ids.size > 1) {
                    val removeAt = (editedAtLineIndex + 1).coerceIn(0, ids.size - 1)
                    ids.removeAt(removeAt)
                }
            }
        }
        // same count: in-place edit, ids unchanged
    }

    while (ids.size < newLineCount) ids.add(newLineId())
    while (ids.size > newLineCount) ids.removeAt(ids.lastIndex)

    return ids
}

// Update format map when text changes


// Convert format map to JSON
private fun formatMapToJson(formatMap: Map<String, LineFormatData>): String {
    val obj = JSONObject()
    formatMap.forEach { (hash, format) ->
        obj.put(hash, JSONObject().apply {
            put("font_size", format.fontSize)
            put("bold", format.bold)
            put("italic", format.italic)
            put("underline", format.underline)
        })
    }
    return obj.toString()
}

// Parse JSON to format map
private fun jsonToFormatMap(json: String?): Map<String, LineFormatData> {
    if (json.isNullOrBlank()) return emptyMap()
    return try {
        val result = mutableMapOf<String, LineFormatData>()
        val obj = JSONObject(json)
        obj.keys().forEach { hash ->
            val formatObj = obj.getJSONObject(hash)
            result[hash] = LineFormatData(
                contentHash = hash,
                fontSize = formatObj.optString("font_size", "Aa"),
                bold = formatObj.optBoolean("bold", false),
                italic = formatObj.optBoolean("italic", false),
                underline = formatObj.optBoolean("underline", false)
            )
        }
        result
    } catch (_: Exception) {
        emptyMap()
    }
}

// Get format for a line by its content
// Get format for a line by its stable id (position-based, not content-based)
private fun getFormatForLine(lineIds: List<String>, lineIndex: Int, formatMap: Map<String, LineFormatData>): LineFormatData {
    val id = getLineId(lineIds, lineIndex)
    return formatMap[id] ?: LineFormatData(contentHash = id)
}

// ── Persistence ───────────────────────────────────────────────────────────────
private const val MAX_NOTE_BODY_LENGTH = 20_000

private fun loadNotes(context: Context): List<NoteEntry> {
    val json = context.getSharedPreferences("journal_notes", Context.MODE_PRIVATE)
        .getString("notes", "[]") ?: "[]"
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            NoteEntry(
                o.getLong("id"),
                o.getString("title"),
                o.getString("body"),
                o.getLong("timestamp"),
                o.optString("font_sizes_json", ""),
                o.optString("char_styles_json", ""),
                o.optString("line_ids_json", "")
            )
        }
    } catch (e: Exception) {
        android.util.Log.w("JournalScreen", "notes JSON corrupted, resetting: ${e.message}")
        emptyList()
    }
}

private fun saveNotes(context: Context, notes: List<NoteEntry>) {
    val arr = JSONArray()
    notes.forEach { n ->
        arr.put(JSONObject().apply {
            put("id", n.id); put("title", n.title)
            put("body", n.body); put("timestamp", n.timestamp)
            put("font_sizes_json", n.fontSizesJson)
            put("char_styles_json", n.charStylesJson)
        })
    }
    context.getSharedPreferences("journal_notes", Context.MODE_PRIVATE)
        .edit { putString("notes", arr.toString()) }
}

private fun formatDate(ts: Long): String =
    SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(ts))

private fun lineFontSizesToJson(lineFontSizes: Map<Int, String>): String {
    val obj = JSONObject()
    lineFontSizes.forEach { (line, size) -> obj.put(line.toString(), size) }
    return obj.toString()
}

private fun parseLineFontSizes(json: String?): Map<Int, String> {
    if (json.isNullOrBlank()) return emptyMap()
    return try {
        val obj = JSONObject(json)
        obj.keys().asSequence().associate { key -> key.toInt() to obj.getString(key) }
    } catch (_: Exception) {
        emptyMap()
    }
}

// Line format persistence - uses line IDs and content matching
private fun saveNoteWithLineObjects(context: Context, notes: List<NoteEntry>) {
    saveNotes(context, notes)
}

// ── Journal Screen ────────────────────────────────────────────────────────────
@Composable
fun JournalScreen(
    onOpenNewNote: (NoteCallbacks) -> Unit = { _ -> },
    onOpenViewNote: (NoteEntry, NoteCallbacks) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val prefs = remember { context.getSharedPreferences("journal_prefs", Context.MODE_PRIVATE) }

    var journalTitle by remember { mutableStateOf(prefs.getString("journal_title", "Journal") ?: "Journal") }
    var isEditing by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf(TextFieldValue(journalTitle)) }
    var notes by remember { mutableStateOf(loadNotes(context)) }

    val callbacks = remember(notes) {
        NoteCallbacks(
            onSaveNew = { title, body ->
                val n = NoteEntry(System.currentTimeMillis(), title.ifBlank { "Untitled" },
                    body.take(MAX_NOTE_BODY_LENGTH), System.currentTimeMillis())
                val updated = listOf(n) + notes
                saveNotes(context, updated); notes = updated
            },
            onSaveEdit = { updated ->
                val list = notes.map { if (it.id == updated.id) updated else it }
                saveNotes(context, list); notes = list
            },
            onDelete = { note ->
                val list = notes.filter { it.id != note.id }
                saveNotes(context, list); notes = list
            }
        )
    }

    LaunchedEffect(isEditing) { if (isEditing) focusRequester.requestFocus() }

    val isCustomTitle = journalTitle != "Journal"
    val titleTopPadding by animateDpAsState(
        targetValue = if (isCustomTitle) 14.dp else 0.dp,
        animationSpec = tween(350), label = "titlePadding"
    )
    val titleStyle = TextStyle(
        color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp,
        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Default,
        letterSpacing = 0.sp, textAlign = TextAlign.Center
    )

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (isEditing) {
                    val t = tempTitle.text.trim().ifEmpty { "Journal" }
                    journalTitle = t; prefs.edit { putString("journal_title", t) }
                    isEditing = false; focusManager.clearFocus()
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Journal header ────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (!isEditing) {
                            tempTitle = TextFieldValue(journalTitle, TextRange(journalTitle.length))
                            isEditing = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.animation.AnimatedVisibility(visible = isCustomTitle,
                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { -it / 2 }, animationSpec = tween(350)),
                        exit  = fadeOut(animationSpec = tween(200)) + slideOutVertically(targetOffsetY = { -it / 2 }, animationSpec = tween(250))
                    ) {
                        Text("Journal", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(40.dp).padding(top = titleTopPadding),
                    contentAlignment = Alignment.Center) {
                    if (isEditing) {
                        BasicTextField(value = tempTitle, onValueChange = { tempTitle = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).focusRequester(focusRequester),
                            textStyle = titleStyle, cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                            singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val t = tempTitle.text.trim().ifEmpty { "Journal" }
                                journalTitle = t; prefs.edit { putString("journal_title", t) }
                                isEditing = false; focusManager.clearFocus()
                            }))
                    } else { Text(journalTitle, style = titleStyle) }
                }
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            // ── Notes list / empty ────────────────────────────────────────────
            if (notes.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Tap + to write your first note",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(note = note, onClick = { onOpenViewNote(note, callbacks) })
                    }
                }
            }
        }

        // ── Plus FAB ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 24.dp)
            .size(52.dp).background(MaterialTheme.colorScheme.onBackground, CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onOpenNewNote(callbacks)
            },
            contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_plus_lucide), "New Entry",
                tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(22.dp))
        }
    }
}

// ── Note card ─────────────────────────────────────────────────────────────────
@Composable
private fun NoteCard(note: NoteEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(note.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp,
            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_chevron_right), null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.padding(start = 8.dp).size(16.dp))
    }
}

// Cache for styled text to avoid rebuilding on every frame
private data class StyledTextCacheKey(
    val text: String,
    val formatMapHash: Int,
    val charStylesHash: Int
)

@Composable
private fun NoteBodyEditor(
    editBody: TextFieldValue,
    onBodyChange: (TextFieldValue) -> Unit,
    formatMap: Map<String, LineFormatData>,
    onFormatMapChange: (Map<String, LineFormatData>) -> Unit,
    getLatestFormatMap: () -> Map<String, LineFormatData>,
    lineIds: List<String>,
    onLineIdsChange: (List<String>) -> Unit,
    getLatestLineIds: () -> List<String>,
    characterStyles: Map<Int, CharacterStyles>,
    onCharacterStylesChange: (Map<Int, CharacterStyles>) -> Unit,
    getLatestCharacterStyles: () -> Map<Int, CharacterStyles>,
    currentLineIndex: Int,
    onCurrentLineIndexChange: (Int) -> Unit,
    isEditMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    bodyTextColor: Color,
    bodyFocusRequester: FocusRequester,
    activeCursor: SolidColor,
    hiddenCursor: SolidColor,
    onCharLimitReached: () -> Unit = {},
    scrollState: ScrollState? = null,
    viewportHeightPx: Int = 0,
    modifier: Modifier = Modifier
) {

    var previousText by remember { mutableStateOf(editBody.text) }

    // Cache for styled text - only rebuild when text or styles actually change
    val styledTextCache = remember { mutableMapOf<StyledTextCacheKey, AnnotatedString>() }

    // ── Cursor-follow auto-scroll (Keep-style, speed-controllable) ────────────
val density = LocalDensity.current
val imeHeightPx = WindowInsets.ime.getBottom(density)
var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
var textFieldCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

// 🔧 Tune scroll speed here:
    val autoScrollDurationMs = 220
    val toolbarHeightPx = with(density) { 60.dp.toPx() }  // matches your "Yellow Container" height
    val scrollMarginPx = with(density) { 32.dp.toPx() } + toolbarHeightPx

    LaunchedEffect(editBody.selection, textLayoutResult, imeHeightPx, isEditMode, viewportHeightPx) {
        if (!isEditMode || scrollState == null) return@LaunchedEffect
        val layout = textLayoutResult ?: return@LaunchedEffect
        val coords = textFieldCoordinates ?: return@LaunchedEffect
        if (imeHeightPx <= 0 || viewportHeightPx <= 0) return@LaunchedEffect

        val cursorOffset = editBody.selection.end.coerceIn(0, editBody.text.length)
        val cursorRect = try {
            layout.getCursorRect(cursorOffset)
        } catch (_: Exception) {
            return@LaunchedEffect
        }

        // Cursor's Y position relative to the scrollable container (not just the field)
        val fieldTopInScroll = coords.positionInParent().y
        val cursorTopInScroll = fieldTopInScroll + cursorRect.top
        val cursorBottomInScroll = fieldTopInScroll + cursorRect.bottom

        // Visible viewport height above the keyboard — from the real screen container,
        // not the scrollable Column's parent (which grows with content, was the bug)
        val visibleBottom = scrollState.value + viewportHeightPx - imeHeightPx - scrollMarginPx

        if (cursorBottomInScroll > visibleBottom) {
            val target = (scrollState.value + (cursorBottomInScroll - visibleBottom))
                .toInt()
                .coerceIn(0, scrollState.maxValue)
            scrollState.animateScrollTo(
                value = target,
                animationSpec = tween(durationMillis = autoScrollDurationMs)
            )
        } else if (cursorTopInScroll < scrollState.value) {
            val target = cursorTopInScroll.toInt().coerceIn(0, scrollState.maxValue)
            scrollState.animateScrollTo(
                value = target,
                animationSpec = tween(durationMillis = autoScrollDurationMs)
            )
        }
    }

    fun getCursorLineIndex(text: String, offset: Int): Int {
        val safeOffset = offset.coerceIn(0, text.length)
        return text.substring(0, safeOffset).count { it == '\n' }
    }

    fun getFontSizeForLabel(label: String): TextUnit = when (label) {
        "H1" -> 24.sp
        "H2" -> 20.sp
        else -> 16.sp
    }

    fun buildStyledBodyText(text: String, formats: Map<String, LineFormatData>, charStyles: Map<Int, CharacterStyles>, ids: List<String>): AnnotatedString {
        val cacheKey = StyledTextCacheKey(
            text = text,
            formatMapHash = formats.hashCode(),
            charStylesHash = charStyles.hashCode() * 31 + ids.hashCode()
        )

        styledTextCache[cacheKey]?.let { return it }

        if (styledTextCache.size > 10) {
            styledTextCache.clear()
        }

        val lines = text.split("\n")

        val result = buildAnnotatedString {
            var charIndex = 0

            lines.forEachIndexed { lineIndex, lineText ->
                val id = getLineId(lineIds, lineIndex)
                val lineFormat = formats[id] ?: LineFormatData(contentHash = id, fontSize = "Aa")
                val fontSize = getFontSizeForLabel(lineFormat.fontSize)

                var batchStart = 0
                var currentStyle: CharacterStyles? = if (lineText.isNotEmpty()) {
                    charStyles[charIndex]
                } else null

                lineText.forEachIndexed { charInLine, _ ->
                    val globalCharIndex = charIndex + charInLine
                    val charStyle = charStyles[globalCharIndex]

                    if (charStyle != currentStyle) {
                        if (batchStart < charInLine) {
                            val spanStyle = SpanStyle(
                                color = bodyTextColor,
                                fontSize = fontSize,
                                fontFamily = FontFamily.Default,
                                fontWeight = if (currentStyle?.bold == true) FontWeight.Bold else null,
                                fontStyle = if (currentStyle?.italic == true) FontStyle.Italic else null,
                                textDecoration = if (currentStyle?.underline == true) TextDecoration.Underline else null
                            )
                            withStyle(spanStyle) {
                                append(lineText.substring(batchStart, charInLine))
                            }
                        }
                        batchStart = charInLine
                        currentStyle = charStyle
                    }
                }

                if (batchStart < lineText.length) {
                    val spanStyle = SpanStyle(
                        color = bodyTextColor,
                        fontSize = fontSize,
                        fontFamily = FontFamily.Default,
                        fontWeight = if (currentStyle?.bold == true) FontWeight.Bold else null,
                        fontStyle = if (currentStyle?.italic == true) FontStyle.Italic else null,
                        textDecoration = if (currentStyle?.underline == true) TextDecoration.Underline else null
                    )
                    withStyle(spanStyle) {
                        append(lineText.substring(batchStart))
                    }
                }

                charIndex += lineText.length

                if (lineIndex < lines.lastIndex) {
                    val spanStyle = SpanStyle(
                        color = bodyTextColor,
                        fontSize = fontSize,
                        fontFamily = FontFamily.Default
                    )
                    withStyle(spanStyle) {
                        append("\n")
                    }
                    charIndex += 1
                }
            }
        }

        styledTextCache[cacheKey] = result
        return result
    }

    BasicTextField(
        value = editBody,
        onValueChange = { newValue ->
            val isAtLimit = editBody.text.length >= MAX_NOTE_BODY_LENGTH
            val isAddingText = newValue.text.length > editBody.text.length

            if (isAtLimit && isAddingText) {
                onCharLimitReached()
                return@BasicTextField
            }

            val limitedText = newValue.text.take(MAX_NOTE_BODY_LENGTH)
            val limitedValue = if (limitedText.length < newValue.text.length) {
                TextFieldValue(
                    text = limitedText,
                    selection = TextRange(limitedText.length.coerceAtMost(newValue.selection.start))
                )
            } else {
                newValue
            }

            // Figure out which line the edit happened at BEFORE text changes,
            // using the cursor position in the OLD text.
            val editedAtLineIndex = getCursorLineIndex(previousText, editBody.selection.start)

            val oldLineCount = previousText.split("\n").size
            val newLineCount = limitedValue.text.split("\n").size

            val liveLineIds = getLatestLineIds()
            val updatedLineIds = updateLineIdsForEdit(
                oldLineCount = oldLineCount,
                newLineCount = newLineCount,
                oldLineIds = liveLineIds,
                editedAtLineIndex = editedAtLineIndex
            )
            onLineIdsChange(updatedLineIds)

            // formatMap itself doesn't need content-based patching anymore —
            // it's keyed by line id. Just drop entries for ids that no longer exist.
            val liveFormatMap = getLatestFormatMap()
            val prunedFormatMap = liveFormatMap.filterKeys { it in updatedLineIds }
            onFormatMapChange(prunedFormatMap)

            val liveCharacterStyles = getLatestCharacterStyles()
            val updatedCharStyles = updateCharacterStylesForEdit(previousText, limitedValue.text, liveCharacterStyles)
            onCharacterStylesChange(updatedCharStyles)

            previousText = limitedValue.text

            val newLineIndex = getCursorLineIndex(limitedValue.text, limitedValue.selection.start)
            onCurrentLineIndexChange(newLineIndex)
            onBodyChange(limitedValue)
            if (!isEditMode) onEditModeChange(true)
        },
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 200.dp)
            .focusRequester(bodyFocusRequester)
            .onGloballyPositioned { coordinates -> textFieldCoordinates = coordinates },
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontFamily = FontFamily.Default,
            lineHeight = 26.sp
        ),
        cursorBrush = if (isEditMode) activeCursor else hiddenCursor,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        onTextLayout = { layoutResult -> textLayoutResult = layoutResult },
        visualTransformation = remember(editBody.text, formatMap, characterStyles, lineIds) {
            { annotatedString ->
                val styledText = buildStyledBodyText(annotatedString.text, formatMap, characterStyles, lineIds)
                TransformedText(styledText, OffsetMapping.Identity)
            }
        },
        decorationBox = { inner ->
            if (editBody.text.isEmpty() && isEditMode) {
                Text(
                    "Start writing...",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    fontSize = 16.sp,
                    lineHeight = 26.sp
                )
            }
            inner()
        }
    )

    LaunchedEffect(Unit) {
        previousText = editBody.text
    }
}

// ── View / Edit Note Screen ───────────────────────────────────────────────────
@Composable
fun ViewNoteScreen(
    note: NoteEntry,
    onBack: () -> Unit,
    onSaveEdit: (NoteEntry) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val titleFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester  = remember { FocusRequester() }

    var isEditMode by remember { mutableStateOf(false) }
    var editTitle by remember(note.id) { mutableStateOf(TextFieldValue(note.title)) }
    var editBody by remember(note.id) { mutableStateOf(TextFieldValue(note.body)) }
    
    // Format map: line-level font sizes
    // Stable per-line identity — one id per line, survives edits by position
    var lineIds by remember(note.id) {
        mutableStateOf(jsonToLineIds(note.lineIdsJson, note.body.split("\n").size))
    }

    // Format map: line-level font sizes, keyed by line id (NOT content)
    var formatMap by remember(note.id) {
        mutableStateOf(jsonToFormatMap(note.fontSizesJson))
    }

    // Character styles: character-level bold/italic/underline
    var characterStyles by remember(note.id) {
        mutableStateOf(jsonToCharacterStyles(note.charStylesJson))
    }
    
    var currentLineIndex by remember { mutableStateOf(0) }
    var showActions by remember { mutableStateOf(false) }
    var showStyleTools by remember { mutableStateOf(false) }
    var charLimitTriggerCount by remember { mutableIntStateOf(0) }
    var showCharLimitNotification by remember { mutableStateOf(false) }
    val bodyTextColor = MaterialTheme.colorScheme.onBackground
    
    // Auto-dismiss notification after 3 seconds, restarts on every trigger
    LaunchedEffect(charLimitTriggerCount) {
        if (charLimitTriggerCount > 0) {
            showCharLimitNotification = true
            kotlinx.coroutines.delay(3000)
            showCharLimitNotification = false
        }
    }
    
    fun getCurrentLineText(): String {
        val lines = editBody.text.split("\n")
        return lines.getOrNull(currentLineIndex) ?: ""
    }
    
    // Current line's font size for UI
    // Current line's font size for UI
    val currentLineId = getLineId(lineIds, currentLineIndex)
    val currentFormat = formatMap[currentLineId] ?: LineFormatData(contentHash = currentLineId)
    
    var currentFontSize by remember(currentLineIndex, editBody.text, formatMap) { 
        mutableStateOf(currentFormat.fontSize)
    }
    
    // Character styles based on selection - memoized to avoid recomputation
    val selectionStyles = remember(editBody.selection.start, editBody.selection.end, characterStyles.hashCode()) {
        val start = editBody.selection.start
        val end = editBody.selection.end
        if (start == end) {
            // No selection - check character before cursor
            if (start > 0) characterStyles[start - 1] else null
        } else {
            // Has selection - check if all selected characters have same style
            val selectedStyles = (start until end).mapNotNull { characterStyles[it] }
            if (selectedStyles.isEmpty()) {
                null
            } else {
                val allBold = selectedStyles.all { it.bold }
                val allItalic = selectedStyles.all { it.italic }
                val allUnderline = selectedStyles.all { it.underline }
                CharacterStyles(0, allBold, allItalic, allUnderline)
            }
        }
    }
    
    var isBold by remember(selectionStyles) { 
        mutableStateOf(selectionStyles?.bold ?: false)
    }
    var isItalic by remember(selectionStyles) { 
        mutableStateOf(selectionStyles?.italic ?: false)
    }
    var isUnderline by remember(selectionStyles) { 
        mutableStateOf(selectionStyles?.underline ?: false)
    }

    fun applyBodyFontSize(label: String) {
        val id = getLineId(lineIds, currentLineIndex)
        val updatedFormat = (formatMap[id] ?: LineFormatData(contentHash = id)).copy(
            fontSize = label,
            contentHash = id
        )
        formatMap = formatMap + (id to updatedFormat)
        currentFontSize = label
    }

    fun applyCharacterStyle(flag: TextStyleFlag, enabled: Boolean) {
        if (!isEditMode) isEditMode = true
        val updatedStyles = applyStyleToRange(
            editBody.text,
            editBody.selection,
            characterStyles,
            flag,
            enabled
        )
        characterStyles = updatedStyles
        
        when (flag) {
            TextStyleFlag.BOLD -> isBold = enabled
            TextStyleFlag.ITALIC -> isItalic = enabled
            TextStyleFlag.UNDERLINE -> isUnderline = enabled
        }
        bodyFocusRequester.requestFocus()
    }

    fun syncStyleButtonState() {
        val id = getLineId(lineIds, currentLineIndex)
        val format = formatMap[id] ?: LineFormatData(contentHash = id)
        currentFontSize = format.fontSize
        
        // Recalculate selection styles
        val start = editBody.selection.start
        val end = editBody.selection.end
        val styles = if (start == end) {
            if (start > 0) characterStyles[start - 1] else null
        } else {
            val selectedStyles = (start until end).mapNotNull { characterStyles[it] }
            if (selectedStyles.isEmpty()) {
                null
            } else {
                val allBold = selectedStyles.all { it.bold }
                val allItalic = selectedStyles.all { it.italic }
                val allUnderline = selectedStyles.all { it.underline }
                CharacterStyles(0, allBold, allItalic, allUnderline)
            }
        }
        
        isBold = styles?.bold ?: false
        isItalic = styles?.italic ?: false
        isUnderline = styles?.underline ?: false
    }

    // Debounced style sync - only update when cursor actually moves or selection changes meaningfully
    LaunchedEffect(currentLineIndex, editBody.selection.start, editBody.selection.end) {
        kotlinx.coroutines.delay(50) // Small debounce to batch rapid changes
        syncStyleButtonState()
    }

    fun commitEdit() {
        val updated = note.copy(
            title = editTitle.text.trim().ifBlank { "Untitled" },
            body  = editBody.text.take(MAX_NOTE_BODY_LENGTH), // Don't trim - preserves empty lines
            timestamp = note.timestamp,
            fontSizesJson = formatMapToJson(formatMap),
            charStylesJson = characterStylesToJson(characterStyles),
            lineIdsJson = lineIdsToJson(lineIds)
        )
        focusManager.clearFocus()
        isEditMode = false
        onSaveEdit(updated)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = { focusManager.clearFocus(); onBack() },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_left), "Back",
                    tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
            }
            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isEditMode,
                    enter = fadeIn(tween(200)),
                    exit  = fadeOut(tween(150))
                ) {
                    IconButton(onClick = { commitEdit() }) {
                        Icon(painterResource(R.drawable.ic_check_lucide), "Save changes",
                            tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

        // Google Keep-style scrolling: Detect IME early for use in multiple places
        val density = LocalDensity.current
        val imeHeight = WindowInsets.ime.getBottom(density)
        val isKeyboardVisible = imeHeight > 0

        var viewportHeightPx by remember { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { viewportHeightPx = it.size.height }
        ) {
            val activeCursor = SolidColor(MaterialTheme.colorScheme.onBackground)
            val hiddenCursor = SolidColor(Color.Transparent)
            val selectionColors = TextSelectionColors(
                handleColor = MaterialTheme.colorScheme.onBackground,
                backgroundColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
            )
            
            // Create scroll state that we can control
            val scrollState = rememberScrollState()


            // Calculate dynamic bottom padding based on IME visibility
            // When keyboard is visible, add extra space to make content scrollable
            val dynamicBottomPadding = with(density) {
                if (isKeyboardVisible) {
                    // Add significant padding when keyboard is visible to enable scrolling
                    // This allows even short notes to scroll upward comfortably
                    (imeHeight + 200).toDp()
                } else {
                    // Normal padding when keyboard is hidden
                    76.dp
                }
            }
            
            CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = dynamicBottomPadding))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDate(note.timestamp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${editBody.text.length} / $MAX_NOTE_BODY_LENGTH",
                            color = MaterialTheme.colorScheme.onBackground.copy(
                                alpha = if (editBody.text.length > MAX_NOTE_BODY_LENGTH * 0.9) 0.7f else 0.4f
                            ),
                            fontSize = 12.sp,
                            fontWeight = if (editBody.text.length > MAX_NOTE_BODY_LENGTH * 0.9) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    BasicTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it; if (!isEditMode) isEditMode = true },
                        modifier = Modifier.fillMaxWidth().focusRequester(titleFocusRequester),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 24.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default, lineHeight = 32.sp
                        ),
                        cursorBrush = if (isEditMode) activeCursor else hiddenCursor,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { bodyFocusRequester.requestFocus() })
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    Spacer(Modifier.height(16.dp))
                    NoteBodyEditor(
                        editBody = editBody,
                        onBodyChange = { newValue ->
                            editBody = newValue
                        },
                        formatMap = formatMap,
                        onFormatMapChange = { newMap ->
                            formatMap = newMap
                        },
                        getLatestFormatMap = { formatMap },
                        lineIds = lineIds,
                        onLineIdsChange = { newIds ->
                            lineIds = newIds
                        },
                        getLatestLineIds = { lineIds },
                        characterStyles = characterStyles,
                        onCharacterStylesChange = { newStyles ->
                            characterStyles = newStyles
                        },
                        getLatestCharacterStyles = { characterStyles },
                        currentLineIndex = currentLineIndex,
                        onCurrentLineIndexChange = { index ->
                            currentLineIndex = index
                            syncStyleButtonState()
                        },
                        isEditMode = isEditMode,
                        onEditModeChange = { enabled -> isEditMode = enabled },
                        bodyTextColor = bodyTextColor,
                        bodyFocusRequester = bodyFocusRequester,
                        activeCursor = activeCursor,
                        hiddenCursor = hiddenCursor,
                        onCharLimitReached = { charLimitTriggerCount++ },
                        scrollState = scrollState,
                        viewportHeightPx = viewportHeightPx,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp)
                    )
                }
            }

            val actionContainerWidth by animateDpAsState(
                targetValue = if (showActions) 112.dp else 40.dp,
                animationSpec = tween(220), label = "actionContainerWidth"
            )


            // ── Bottom Overlays Container (moves up with keyboard) ────────────────
            // Cancel out the navigation bar's height from the IME inset, since the
            // screen's content area is likely already shifted above the nav bar by
            // an ancestor. Without this, 3-button nav double-counts the nav bar
            // height (once from the ancestor, once baked into the IME inset here),
            // causing the visible gap. Gesture nav has ~0 nav bar height, so this
            // had no visible effect there — which is why it only showed up in 3-button mode.
            val navBarHeightPx = WindowInsets.navigationBars.getBottom(density)
            val imeBottomDp = with(density) {
                (imeHeight - navBarHeightPx).coerceAtLeast(0).toDp()
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = imeBottomDp)
            ) {
                // Character limit notification (appears above toolbar)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showCharLimitNotification,
                    enter = scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(200)
                    ) + fadeIn(tween(200)),
                    exit = scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(200)
                    ) + fadeOut(tween(200)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 68.dp)
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You've reached the character limit",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Yellow Container at very bottom with all UI
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .zIndex(10f),
                    contentAlignment = Alignment.Center
                ) {
                    // Style tools panel (appears on top when visible)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showStyleTools,
                        enter = fadeIn(tween(180)),
                        exit = fadeOut(tween(140)),
                        modifier = Modifier.align(Alignment.Center).zIndex(21f)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState())
                                        .padding(end = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = {
                                        applyBodyFontSize("H1")
                                        bodyFocusRequester.requestFocus()
                                    }) {
                                        Text("H1", color = if (currentFontSize == "H1") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f))
                                    }
                                    TextButton(onClick = {
                                        applyBodyFontSize("H2")
                                        bodyFocusRequester.requestFocus()
                                    }) {
                                        Text("H2", color = if (currentFontSize == "H2") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f))
                                    }
                                    TextButton(onClick = {
                                        applyBodyFontSize("Aa")
                                        bodyFocusRequester.requestFocus()
                                    }) {
                                        Text("Aa", color = if (currentFontSize == "Aa") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f))
                                    }
                                    VerticalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                        modifier = Modifier.height(24.dp)
                                    )
                                    TextButton(onClick = {
                                        val nextValue = !isBold
                                        isBold = nextValue
                                        applyCharacterStyle(TextStyleFlag.BOLD, nextValue)
                                    }) {
                                        Text(
                                            "B",
                                            color = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    TextButton(onClick = {
                                        val nextValue = !isItalic
                                        isItalic = nextValue
                                        applyCharacterStyle(TextStyleFlag.ITALIC, nextValue)
                                    }) {
                                        Text(
                                            "I",
                                            color = if (isItalic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                            fontStyle = FontStyle.Italic,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 16.sp
                                        )
                                    }
                                    TextButton(onClick = {
                                        val nextValue = !isUnderline
                                        isUnderline = nextValue
                                        applyCharacterStyle(TextStyleFlag.UNDERLINE, nextValue)
                                    }) {
                                        Text(
                                            "U",
                                            color = if (isUnderline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                            textDecoration = TextDecoration.Underline,
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { showStyleTools = false },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_x_lucide),
                                        contentDescription = "Close style tools",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Main button row (always visible)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left side buttons (Attach + Style)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { /* Attach */ },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_file_plus_corner),
                                    contentDescription = "Attach",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { showStyleTools = !showStyleTools },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_a_large_small),
                                    contentDescription = "Style",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        // Right side: 3-dot button with actions
                        Box(
                            modifier = Modifier.height(40.dp).width(actionContainerWidth)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 6.dp)
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showActions,
                                modifier = Modifier.align(Alignment.CenterStart).padding(end = 4.dp),
                                enter = fadeIn(animationSpec = tween(180)),
                                exit = fadeOut(animationSpec = tween(140))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = {
                                            showActions = false
                                            val shareText = buildString {
                                                append(editTitle.text.trim())
                                                if (editBody.text.isNotBlank()) { append("\n\n"); append(editBody.text.trim()) }
                                            }
                                            context.startActivity(Intent.createChooser(
                                                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) },
                                                "Share note"
                                            ))
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(painterResource(R.drawable.ic_share2_lucide), contentDescription = "Share",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            showActions = false
                                            onDelete()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(painterResource(R.drawable.ic_trash_lucide), contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier.align(Alignment.CenterEnd).size(28.dp)
                            ) {
                                IconButton(
                                    onClick = { showActions = !showActions },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = showActions,
                                            enter = fadeIn(animationSpec = tween(180)) + scaleIn(animationSpec = tween(180)),
                                            exit = fadeOut(animationSpec = tween(140)) + scaleOut(animationSpec = tween(140))
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_x_lucide),
                                                contentDescription = "Close actions",
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = !showActions,
                                            enter = fadeIn(animationSpec = tween(180)) + scaleIn(animationSpec = tween(180)),
                                            exit = fadeOut(animationSpec = tween(140)) + scaleOut(animationSpec = tween(140))
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_more_vertical),
                                                contentDescription = "More options",
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Remove old toolbar - no longer needed
            }
        } // Close Bottom Overlays Container
    }
}


// ── New Note Editor Screen ────────────────────────────────────────────────────
@Composable
fun NewNoteScreen(
    onBack: () -> Unit,
    onSave: (title: String, body: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val titleFocus = remember { FocusRequester() }
    val bodyFocus  = remember { FocusRequester() }
    var noteTitle by remember { mutableStateOf(TextFieldValue("")) }
    var noteBody  by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(Unit) { titleFocus.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = { focusManager.clearFocus(); onBack() },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_left), "Back",
                    tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = { focusManager.clearFocus(); onSave(noteTitle.text.trim(), noteBody.text.trim()) },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                Icon(painterResource(R.drawable.ic_save), "Save",
                    tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

        val selectionColors = TextSelectionColors(
            handleColor = MaterialTheme.colorScheme.onBackground,
            backgroundColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
        )
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                value = noteTitle, onValueChange = { noteTitle = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).focusRequester(titleFocus),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Default),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { bodyFocus.requestFocus() }),
                decorationBox = { inner ->
                    if (noteTitle.text.isEmpty()) Text("Title",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    inner()
                }
            )
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = 20.dp))
            BasicTextField(
                value = noteBody, onValueChange = { noteBody = it },
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp, vertical = 16.dp).focusRequester(bodyFocus),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp, fontFamily = FontFamily.Default, lineHeight = 26.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorationBox = { inner ->
                    if (noteBody.text.isEmpty()) Text("Start writing...",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        fontSize = 16.sp, lineHeight = 26.sp)
                    inner()
                }
            )
        }
    }
}
