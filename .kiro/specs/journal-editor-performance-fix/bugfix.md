# Bugfix Requirements Document

## Introduction

The journal note editor (ViewNoteScreen) in the Bread application experiences severe performance degradation when editing notes with many characters. Users report typing lag and UI freezing when notes approach the current 50,000 character limit. The performance issue becomes noticeable at approximately 5,000+ characters and worsens significantly as the character count increases.

The root cause has been identified as the `buildStyledBodyText()` function, which rebuilds styled text for all characters on every keystroke (O(n) complexity where n = total character count). Additional contributing factors include character-by-character style processing, synchronous visual transformation on the UI thread without caching, constant LaunchedEffect triggers, and expensive recomposition with complex dependencies.

This bugfix will optimize the text rendering pipeline while preserving all existing styling features including H1/H2/Aa line-level font sizes, Bold/Italic/Underline character styles, style inheritance during typing, and format persistence through JSON save/load. Additionally, the character limit will be reduced from 50,000 to 20,000 characters to improve performance boundaries.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a note contains 5,000 or more characters AND the user types a character THEN the system exhibits noticeable input lag with delayed keystroke response

1.2 WHEN a note contains 10,000 or more characters AND the user types a character THEN the system experiences significant UI freezing and slow text rendering

1.3 WHEN a note approaches 50,000 characters AND the user performs any edit operation (typing, deleting, cursor movement) THEN the system becomes nearly unusable with severe lag

1.4 WHEN any text edit occurs in a note of any size THEN the system rebuilds styled text for ALL characters in the entire note on every single keystroke

1.5 WHEN the `buildStyledBodyText()` function processes a note THEN the system performs character-by-character iteration creating 10,000+ individual style operations for a 10,000 character note

1.6 WHEN visual transformation occurs during text rendering THEN the system executes synchronously on the UI thread without any caching mechanism

1.7 WHEN text selection or cursor position changes THEN the system triggers LaunchedEffect blocks causing unnecessary recomposition cycles

1.8 WHEN the user types in a large note THEN the system recomposes remember blocks with complex dependencies on every keystroke

### Expected Behavior (Correct)

2.1 WHEN a note contains 5,000 or more characters AND the user types a character THEN the system SHALL provide smooth, responsive typing with no noticeable input lag

2.2 WHEN a note contains 10,000 or more characters AND the user types a character THEN the system SHALL maintain responsive UI performance without freezing

2.3 WHEN a note reaches the character limit AND the user performs any edit operation THEN the system SHALL remain responsive and usable

2.4 WHEN any text edit occurs THEN the system SHALL optimize text rebuilding to process only changed portions instead of the entire note

2.5 WHEN the `buildStyledBodyText()` function processes a note THEN the system SHALL use optimized algorithms to minimize character-by-character iterations

2.6 WHEN visual transformation occurs during text rendering THEN the system SHALL implement caching mechanisms to avoid redundant computation

2.7 WHEN text selection or cursor position changes THEN the system SHALL debounce or optimize effect triggers to prevent excessive recomposition

2.8 WHEN the user types in a large note THEN the system SHALL minimize recomposition scope and optimize dependency tracking in remember blocks

2.9 WHEN a user attempts to enter text that would exceed 20,000 characters THEN the system SHALL enforce the new character limit and prevent further input

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a note uses H1 line-level formatting THEN the system SHALL CONTINUE TO display text at 24sp font size

3.2 WHEN a note uses H2 line-level formatting THEN the system SHALL CONTINUE TO display text at 20sp font size

3.3 WHEN a note uses Aa (normal) line-level formatting THEN the system SHALL CONTINUE TO display text at 16sp font size

3.4 WHEN a user applies bold style to selected text THEN the system SHALL CONTINUE TO render that text with FontWeight.Bold

3.5 WHEN a user applies italic style to selected text THEN the system SHALL CONTINUE TO render that text with FontStyle.Italic

3.6 WHEN a user applies underline style to selected text THEN the system SHALL CONTINUE TO render that text with TextDecoration.Underline

3.7 WHEN a user types at a cursor position with existing character styles THEN the system SHALL CONTINUE TO inherit the style from the character immediately before the cursor

3.8 WHEN a note with formatting is saved THEN the system SHALL CONTINUE TO persist line-level font sizes to fontSizesJson field in JSON format

3.9 WHEN a note with formatting is saved THEN the system SHALL CONTINUE TO persist character-level styles to charStylesJson field in JSON format

3.10 WHEN a note with formatting is loaded THEN the system SHALL CONTINUE TO restore all line-level font sizes correctly

3.11 WHEN a note with formatting is loaded THEN the system SHALL CONTINUE TO restore all character-level styles (bold, italic, underline) correctly

3.12 WHEN a user creates a new line within formatted text THEN the system SHALL CONTINUE TO handle line splitting and format inheritance correctly

3.13 WHEN a user deletes a line break to merge lines THEN the system SHALL CONTINUE TO handle format merging correctly

3.14 WHEN a note contains less than 5,000 characters THEN the system SHALL CONTINUE TO provide the same smooth editing experience

3.15 WHEN the toolbar shows formatting buttons (H1, H2, Aa, B, I, U) THEN the system SHALL CONTINUE TO apply those formats to selected text or current cursor position

3.16 WHEN existing notes contain more than 20,000 characters THEN the system SHALL CONTINUE TO display and edit those notes without data loss (read-only or truncation warning acceptable)
