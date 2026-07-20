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

// ── Note model ────────────────────────────────────────────────────────────────
data class NoteEntry(
    val id: Long,
    val title: String,
    val body: String,
    val timestamp: Long,
    val fontSizesJson: String = "", // Line-level font sizes
    val charStylesJson: String = "" // Character-level bold/italic/underline
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
    if (charsInserted > 0 && oldEditStart > 0) {
        val inheritStyle = oldStyles[oldEditStart - 1]
        if (inheritStyle != null) {
            for (i in newEditStart until newEditEnd) {
                result[i] = inheritStyle.copy(charIndex = i)
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
private fun hashLineContent(text: String, lineIndex: Int = -1): String {
    val key = text.take(50).trim()
    // For empty lines, use position-based identifier when available
    return if (key.isEmpty() && lineIndex >= 0) {
        "empty_line_$lineIndex"
    } else if (key.isEmpty()) {
        "empty_line_${System.nanoTime()}"
    } else {
        key
    }
}

// Update format map when text changes
private fun updateFormatMapForEdit(
    oldText: String,
    newText: String,
    oldFormatMap: Map<String, LineFormatData>
): Map<String, LineFormatData> {
    val oldLines = oldText.split("\n")
    val newLines = newText.split("\n")
    val result = mutableMapOf<String, LineFormatData>()
    
    // Detect if Enter was pressed (line count increased)
    val enterPressed = newLines.size > oldLines.size
    val linesDeleted = newLines.size < oldLines.size
    
    if (enterPressed) {
        // Enter was pressed - need to handle line splitting intelligently
        newLines.forEachIndexed { index, newLine ->
            val hash = hashLineContent(newLine, index)
            
            // Check if this exact content existed before (unchanged line)
            val existingFormat = if (newLine.trim().isEmpty()) {
                // Empty line - check if there was a format at this position
                val oldHash = hashLineContent("", index)
                oldFormatMap[oldHash]
            } else {
                // Non-empty line - match by content
                oldFormatMap.entries.find { it.key == newLine.take(50).trim() }?.value
            }
            
            if (existingFormat != null) {
                // Line existed before with this exact content
                result[hash] = existingFormat.copy(contentHash = hash)
            } else {
                // New or split line - check if it came from splitting an existing line
                var foundSplitSource = false
                
                // Check if this line's content is a substring of any old line (line was split)
                oldLines.forEachIndexed { oldIndex, oldLine ->
                    if (!foundSplitSource && oldLine.contains(newLine) && newLine.isNotEmpty()) {
                        // This new line was part of an old line - inherit that format
                        val oldFormat = oldFormatMap.entries.find { 
                            it.key == oldLine.take(50).trim() 
                        }?.value
                        
                        if (oldFormat != null) {
                            result[hash] = oldFormat.copy(contentHash = hash)
                            foundSplitSource = true
                        }
                    }
                }
                
                if (!foundSplitSource) {
                    // Truly new line - use default format
                    result[hash] = LineFormatData(contentHash = hash, fontSize = "Aa")
                }
            }
        }
    } else if (linesDeleted) {
        // Lines were merged - inherit format from the FIRST line involved in the merge
        newLines.forEachIndexed { index, newLine ->
            val newHash = hashLineContent(newLine, index)
            
            // Check if exact content match exists
            val exactMatch = oldFormatMap.entries.find { 
                it.key == newLine.take(50).trim()
            }?.value
            
            if (exactMatch != null) {
                result[newHash] = exactMatch.copy(contentHash = newHash)
            } else {
                // This line is likely a merged line
                // Find the first old line at this position - it should provide the format
                val firstOldLine = oldLines.getOrNull(index) ?: ""
                val firstOldFormat = oldFormatMap.entries.find { 
                    it.key == firstOldLine.take(50).trim() 
                }?.value
                
                if (firstOldFormat != null) {
                    // Merged line inherits from first line (the H1 in your example)
                    result[newHash] = firstOldFormat.copy(contentHash = newHash)
                } else {
                    // Fallback to default
                    result[newHash] = LineFormatData(contentHash = newHash, fontSize = "Aa")
                }
            }
        }
    } else {
        // Same line count - normal editing (typing/deleting within a line)
        newLines.forEachIndexed { index, newLine ->
            val newHash = hashLineContent(newLine, index)
            
            // Check if this exact line content already has format
            val existingByContent = oldFormatMap.entries.find { 
                it.key == newLine.take(50).trim() || (newLine.trim().isEmpty() && it.key.startsWith("empty_line_"))
            }?.value
            
            if (existingByContent != null) {
                result[newHash] = existingByContent.copy(contentHash = newHash)
            } else {
                // Line was edited - try to inherit from old line at same position
                val oldLine = oldLines.getOrNull(index) ?: ""
                if (oldLine.isNotEmpty() && newLine.isNotEmpty()) {
                    val oldHash = hashLineContent(oldLine, index)
                    val oldFormat = oldFormatMap.entries.find { 
                        it.key == oldLine.take(50).trim() 
                    }?.value
                    
                    if (oldFormat != null) {
                        // Check if this is truly an edit of the same line (similar content)
                        val oldKey = oldLine.take(30).trim()
                        val newKey = newLine.take(30).trim()
                        
                        // Only inherit if there's clear overlap (you're editing the same line)
                        if (oldKey.isNotEmpty() && newKey.isNotEmpty()) {
                            if (newKey.contains(oldKey.take(10)) || oldKey.contains(newKey.take(10))) {
                                result[newHash] = oldFormat.copy(contentHash = newHash)
                            }
                        }
                    }
                }
                
                // If still no format, save with default
                if (!result.containsKey(newHash)) {
                    result[newHash] = LineFormatData(contentHash = newHash, fontSize = "Aa")
                }
            }
        }
    }
    
    return result
}

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
private fun getFormatForLine(lineText: String, formatMap: Map<String, LineFormatData>): LineFormatData {
    val hash = hashLineContent(lineText)
    return formatMap[hash] ?: LineFormatData(contentHash = hash)
}

// ── Persistence ───────────────────────────────────────────────────────────────
private const val MAX_NOTE_BODY_LENGTH = 50_000

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
                o.optString("char_styles_json", "")
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

@Composable
private fun NoteBodyEditor(
    editBody: TextFieldValue,
    onBodyChange: (TextFieldValue) -> Unit,
    formatMap: Map<String, LineFormatData>,
    onFormatMapChange: (Map<String, LineFormatData>) -> Unit,
    characterStyles: Map<Int, CharacterStyles>,
    onCharacterStylesChange: (Map<Int, CharacterStyles>) -> Unit,
    currentLineIndex: Int,
    onCurrentLineIndexChange: (Int) -> Unit,
    isEditMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    bodyTextColor: Color,
    bodyFocusRequester: FocusRequester,
    activeCursor: SolidColor,
    hiddenCursor: SolidColor,
    modifier: Modifier = Modifier
) {
    var previousText by remember { mutableStateOf(editBody.text) }
    
    fun getCursorLineIndex(text: String, offset: Int): Int {
        val safeOffset = offset.coerceIn(0, text.length)
        return text.substring(0, safeOffset).count { it == '\n' }
    }

    fun getFontSizeForLabel(label: String): TextUnit = when (label) {
        "H1" -> 24.sp
        "H2" -> 20.sp
        else -> 16.sp
    }

    fun buildStyledBodyText(text: String, formats: Map<String, LineFormatData>, charStyles: Map<Int, CharacterStyles>): AnnotatedString {
        val lines = text.split("\n")
        
        return buildAnnotatedString {
            var charIndex = 0
            
            lines.forEachIndexed { lineIndex, lineText ->
                val hash = hashLineContent(lineText, lineIndex)
                val lineFormat = formats[hash] ?: LineFormatData(contentHash = hash, fontSize = "Aa")
                val fontSize = getFontSizeForLabel(lineFormat.fontSize)
                
                // Apply styles character by character
                lineText.forEachIndexed { charInLine, char ->
                    val globalCharIndex = charIndex + charInLine
                    val charStyle = charStyles[globalCharIndex]
                    
                    val spanStyle = SpanStyle(
                        color = bodyTextColor,
                        fontSize = fontSize,
                        fontFamily = FontFamily.Default,
                        fontWeight = if (charStyle?.bold == true) FontWeight.Bold else null,
                        fontStyle = if (charStyle?.italic == true) FontStyle.Italic else null,
                        textDecoration = if (charStyle?.underline == true) TextDecoration.Underline else null
                    )

                    withStyle(spanStyle) {
                        append(char.toString())
                    }
                }
                
                charIndex += lineText.length
                
                // Add newline between lines (except after last line)
                if (lineIndex < lines.lastIndex) {
                    val spanStyle = SpanStyle(
                        color = bodyTextColor,
                        fontSize = fontSize,
                        fontFamily = FontFamily.Default
                    )
                    withStyle(spanStyle) {
                        append("\n")
                    }
                    charIndex += 1 // for the newline character
                }
            }
        }
    }

    BasicTextField(
        value = editBody,
        onValueChange = { newValue ->
            // Update format map to track line changes
            val updatedFormatMap = updateFormatMapForEdit(previousText, newValue.text, formatMap)
            onFormatMapChange(updatedFormatMap)
            
            // Update character styles to track character changes
            val updatedCharStyles = updateCharacterStylesForEdit(previousText, newValue.text, characterStyles)
            onCharacterStylesChange(updatedCharStyles)
            
            previousText = newValue.text
            
            val newLineIndex = getCursorLineIndex(newValue.text, newValue.selection.start)
            onCurrentLineIndexChange(newLineIndex)
            onBodyChange(newValue)
            if (!isEditMode) onEditModeChange(true)
        },
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp).focusRequester(bodyFocusRequester),
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontFamily = FontFamily.Default,
            lineHeight = 26.sp
        ),
        cursorBrush = if (isEditMode) activeCursor else hiddenCursor,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        visualTransformation = { annotatedString ->
            val styledText = buildStyledBodyText(annotatedString.text, formatMap, characterStyles)
            TransformedText(styledText, OffsetMapping.Identity)
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
    
    // Initialize previousText
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
    val bodyTextColor = MaterialTheme.colorScheme.onBackground
    
    fun getCurrentLineText(): String {
        val lines = editBody.text.split("\n")
        return lines.getOrNull(currentLineIndex) ?: ""
    }
    
    // Current line's font size for UI
    val currentLineText = getCurrentLineText()
    val currentLineHash = hashLineContent(currentLineText, currentLineIndex)
    val currentFormat = formatMap[currentLineHash] ?: LineFormatData(contentHash = currentLineHash)
    
    var currentFontSize by remember(currentLineIndex, editBody.text, formatMap) { 
        mutableStateOf(currentFormat.fontSize)
    }
    
    // Character styles based on selection
    fun getSelectionStyles(): CharacterStyles? {
        val start = editBody.selection.start
        val end = editBody.selection.end
        if (start == end) {
            // No selection - check character before cursor
            return if (start > 0) characterStyles[start - 1] else null
        }
        // Has selection - check if all selected characters have same style
        val selectedStyles = (start until end).mapNotNull { characterStyles[it] }
        if (selectedStyles.isEmpty()) return null
        
        val allBold = selectedStyles.all { it.bold }
        val allItalic = selectedStyles.all { it.italic }
        val allUnderline = selectedStyles.all { it.underline }
        
        return CharacterStyles(0, allBold, allItalic, allUnderline)
    }
    
    val selectionStyles = getSelectionStyles()
    var isBold by remember(editBody.selection, characterStyles) { 
        mutableStateOf(selectionStyles?.bold ?: false)
    }
    var isItalic by remember(editBody.selection, characterStyles) { 
        mutableStateOf(selectionStyles?.italic ?: false)
    }
    var isUnderline by remember(editBody.selection, characterStyles) { 
        mutableStateOf(selectionStyles?.underline ?: false)
    }

    fun applyBodyFontSize(label: String) {
        val lineText = getCurrentLineText()
        val hash = hashLineContent(lineText, currentLineIndex)
        val updatedFormat = (formatMap[hash] ?: LineFormatData(contentHash = hash)).copy(
            fontSize = label,
            contentHash = hash
        )
        formatMap = formatMap + (hash to updatedFormat)
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
        val lineText = getCurrentLineText()
        val hash = hashLineContent(lineText, currentLineIndex)
        val format = formatMap[hash] ?: LineFormatData(contentHash = hash)
        currentFontSize = format.fontSize
        
        val styles = getSelectionStyles()
        isBold = styles?.bold ?: false
        isItalic = styles?.italic ?: false
        isUnderline = styles?.underline ?: false
    }

    LaunchedEffect(currentLineIndex, editBody.text, editBody.selection, formatMap, characterStyles) {
        syncStyleButtonState()
    }
    
    fun commitEdit() {
        val updated = note.copy(
            title = editTitle.text.trim().ifBlank { "Untitled" },
            body  = editBody.text.take(MAX_NOTE_BODY_LENGTH), // Don't trim - preserves empty lines
            timestamp = note.timestamp,
            fontSizesJson = formatMapToJson(formatMap),
            charStylesJson = characterStylesToJson(characterStyles)
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

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val activeCursor = SolidColor(MaterialTheme.colorScheme.onBackground)
            val hiddenCursor = SolidColor(Color.Transparent)
            val selectionColors = TextSelectionColors(
                handleColor = MaterialTheme.colorScheme.onBackground,
                backgroundColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
            )
            CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 76.dp))
                ) {
                    Text(text = formatDate(note.timestamp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 12.sp)
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
                        characterStyles = characterStyles,
                        onCharacterStylesChange = { newStyles ->
                            characterStyles = newStyles
                        },
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
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp)
                    )
                }
            }

            val actionContainerWidth by animateDpAsState(
                targetValue = if (showActions) 112.dp else 40.dp,
                animationSpec = tween(220), label = "actionContainerWidth"
            )

            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .imePadding()
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showStyleTools,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(140)),
                    modifier = Modifier.align(Alignment.BottomCenter).zIndex(1f)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { /* Attach */ },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_file_plus_corner),
                                contentDescription = "Attach",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
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
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

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
                                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
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
                                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
