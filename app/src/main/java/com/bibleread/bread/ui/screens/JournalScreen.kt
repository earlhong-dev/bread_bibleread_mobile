package com.bibleread.bread.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JournalScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val prefs = remember { context.getSharedPreferences("journal_prefs", Context.MODE_PRIVATE) }
    
    // State to hold the custom title. Default is "Journal".
    var journalTitle by remember {
        mutableStateOf(prefs.getString("journal_title", "Journal") ?: "Journal")
    }
    
    var isEditing by remember { mutableStateOf(false) }
    
    // Use TextFieldValue to control cursor selection position
    var tempTitleTextFieldValue by remember {
        mutableStateOf(TextFieldValue(journalTitle))
    }

    // Request focus when edit mode is toggled on
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    // Unified text style with increased size (22.sp)
    val unifiedTitleStyle = TextStyle(
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center
    )

    // Animated top padding for the title area based on whether subheading is visible
    val isCustomTitle = journalTitle != "Journal"
    val titleTopPadding by animateDpAsState(
        targetValue = if (isCustomTitle) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 350),
        label = "titlePadding"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Clicking outside the header text field clears focus and saves
                if (isEditing) {
                    val finalTitle = tempTitleTextFieldValue.text.trim().ifEmpty { "Journal" }
                    journalTitle = finalTitle
                    prefs.edit().putString("journal_title", finalTitle).apply()
                    isEditing = false
                    focusManager.clearFocus()
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Section Container (64.dp original height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isEditing) {
                            tempTitleTextFieldValue = TextFieldValue(
                                text = journalTitle,
                                selection = TextRange(journalTitle.length)
                            )
                            isEditing = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Subheading (Animated Fade-in + Slide-in from top)
                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = isCustomTitle,
                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = tween(350)
                        ),
                        exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                            targetOffsetY = { -it / 2 },
                            animationSpec = tween(250)
                        )
                    ) {
                        Text(
                            text = "Journal",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Title Area (Height 40.dp with smooth transition padding)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(top = titleTopPadding),
                    contentAlignment = Alignment.Center
                ) {
                    if (isEditing) {
                        BasicTextField(
                            value = tempTitleTextFieldValue,
                            onValueChange = { tempTitleTextFieldValue = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                                .focusRequester(focusRequester),
                            textStyle = unifiedTitleStyle,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val finalTitle = tempTitleTextFieldValue.text.trim().ifEmpty { "Journal" }
                                    journalTitle = finalTitle
                                    prefs.edit().putString("journal_title", finalTitle).apply()
                                    isEditing = false
                                    focusManager.clearFocus()
                                }
                            )
                        )
                    } else {
                        Text(
                            text = journalTitle,
                            style = unifiedTitleStyle
                        )
                    }
                }
            }
            
            // Separator Line
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
            )

            // Screen Content Placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap the header above to rename your journal",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }
    }
}
