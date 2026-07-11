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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibleread.bread.NoteCallbacks
import com.bibleread.bread.R
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Note model ────────────────────────────────────────────────────────────────
data class NoteEntry(
    val id: Long,
    val title: String,
    val body: String,
    val timestamp: Long
)

// ── Persistence ───────────────────────────────────────────────────────────────
private const val MAX_NOTE_BODY_LENGTH = 50_000

private fun loadNotes(context: Context): List<NoteEntry> {
    val json = context.getSharedPreferences("journal_notes", Context.MODE_PRIVATE)
        .getString("notes", "[]") ?: "[]"
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            NoteEntry(o.getLong("id"), o.getString("title"), o.getString("body"), o.getLong("timestamp"))
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
        })
    }
    context.getSharedPreferences("journal_notes", Context.MODE_PRIVATE)
        .edit().putString("notes", arr.toString()).apply()
}

private fun formatDate(ts: Long): String =
    SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(ts))

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
                    journalTitle = t; prefs.edit().putString("journal_title", t).apply()
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
                    AnimatedVisibility(visible = isCustomTitle,
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
                                journalTitle = t; prefs.edit().putString("journal_title", t).apply()
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
    var editBody  by remember(note.id) { mutableStateOf(TextFieldValue(note.body)) }
    var showMenu  by remember { mutableStateOf(false) }

    fun commitEdit() {
        val updated = note.copy(
            title = editTitle.text.trim().ifBlank { "Untitled" },
            body  = editBody.text.trim().take(MAX_NOTE_BODY_LENGTH),
            timestamp = note.timestamp
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
            val titleStyle = TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default, lineHeight = 32.sp
            )
            val bodyStyle = TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp, fontFamily = FontFamily.Default, lineHeight = 26.sp
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
                        textStyle = titleStyle,
                        cursorBrush = if (isEditMode) activeCursor else hiddenCursor,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { bodyFocusRequester.requestFocus() })
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    Spacer(Modifier.height(16.dp))
                    BasicTextField(
                        value = editBody,
                        onValueChange = { editBody = it; if (!isEditMode) isEditMode = true },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp).focusRequester(bodyFocusRequester),
                        textStyle = bodyStyle,
                        cursorBrush = if (isEditMode) activeCursor else hiddenCursor,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        decorationBox = { inner ->
                            if (editBody.text.isEmpty() && isEditMode) {
                                Text("Start writing...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                    fontSize = 16.sp, lineHeight = 26.sp)
                            }
                            inner()
                        }
                    )
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp).imePadding()) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_more_vertical), contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                    DropdownMenuItem(
                        text = { Text("Share", color = MaterialTheme.colorScheme.onBackground) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_share2_lucide), null,
                            tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMenu = false
                            val shareText = buildString {
                                append(editTitle.text.trim())
                                if (editBody.text.isNotBlank()) { append("\n\n"); append(editBody.text.trim()) }
                            }
                            context.startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) },
                                "Share note"
                            ))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_trash_lucide), null,
                            tint = Color.Red, modifier = Modifier.size(18.dp)) },
                        onClick = { showMenu = false; onDelete() }
                    )
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
