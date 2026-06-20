package com.bibleread.bread.data

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

object BibleXmlParser {

    private const val TAG = "BibleXmlParser"

    suspend fun parse(context: Context, dao: VerseDao, fileName: String = "mbbtag05.xml") {
        Log.d(TAG, "Starting parse of $fileName")

        val verses = mutableListOf<VerseEntity>()

        context.assets.open(fileName).use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

            var currentBook = ""
            var currentChapter = 0
            var pendingHeading: String? = null
            var pendingSubheading: String? = null

            val bookRegex      = Regex("""<book[^>]+name="([^"]+)"""")
            val chapterRegex   = Regex("""<chapter\s+number="(\d+)"""")
            val headingRegex   = Regex("""<heading>(.*?)</heading>""")
            val subheadingRegex= Regex("""<subheading>(.*?)</subheading>""")
            val verseStartRegex= Regex("""<verse\s+number="(\d+)"(?:\s+display="([^"]*)")?>(.*)</verse>""")
            // verse with inline heading: text before + <heading>X</heading> + text after
            val inlineHeadingRegex = Regex("""(.*?)<heading>(.*?)</heading>(.*)""")

            reader.forEachLine { rawLine ->
                val line = rawLine.trim()

                bookRegex.find(line)?.let {
                    currentBook = it.groupValues[1]
                    currentChapter = 0
                    pendingHeading = null
                    pendingSubheading = null
                    return@forEachLine
                }

                chapterRegex.find(line)?.let {
                    currentChapter = it.groupValues[1].toIntOrNull() ?: 0
                    return@forEachLine
                }

                // Standalone heading (not inside a verse)
                if (!line.contains("<verse") && headingRegex.containsMatchIn(line)) {
                    pendingHeading = headingRegex.find(line)?.groupValues?.get(1)?.trim()
                    return@forEachLine
                }

                // Standalone subheading
                if (!line.contains("<verse") && subheadingRegex.containsMatchIn(line)) {
                    pendingSubheading = subheadingRegex.find(line)?.groupValues?.get(1)?.trim()
                    return@forEachLine
                }

                // Verse line
                verseStartRegex.find(line)?.let { match ->
                    val verseNum = match.groupValues[1].toIntOrNull() ?: 0
                    val display  = match.groupValues[2].ifBlank { null }
                    var content  = match.groupValues[3]

                    var inlineHeading: String? = null
                    var inlineSubheading: String? = null

                    // Extract inline heading if present
                    inlineHeadingRegex.find(content)?.let { ih ->
                        inlineHeading = ih.groupValues[2].trim()
                        content = (ih.groupValues[1] + ih.groupValues[3]).trim()
                    }

                    // Extract inline subheading if present
                    val inlineSub = subheadingRegex.find(content)
                    if (inlineSub != null) {
                        inlineSubheading = inlineSub.groupValues[1].trim()
                        content = content.replace(inlineSub.value, "").trim()
                    }

                    val finalHeading    = inlineHeading    ?: pendingHeading
                    val finalSubheading = inlineSubheading ?: pendingSubheading

                    if (currentBook.isNotEmpty() && currentChapter > 0 && verseNum > 0) {
                        verses.add(
                            VerseEntity(
                                book       = currentBook,
                                chapter    = currentChapter,
                                verse      = verseNum,
                                text       = content.trim(),
                                display    = display,
                                heading    = finalHeading?.ifBlank { null },
                                subheading = finalSubheading?.ifBlank { null }
                            )
                        )
                    }

                    pendingHeading    = null
                    pendingSubheading = null
                }
            }
        }

        Log.d(TAG, "Parsed ${verses.size} verses — inserting into database")
        verses.chunked(500).forEach { batch -> dao.insertAll(batch) }
        Log.d(TAG, "Parse complete")
    }
}


