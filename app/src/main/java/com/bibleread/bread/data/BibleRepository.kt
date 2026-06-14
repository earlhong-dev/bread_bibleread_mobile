package com.bibleread.bread.data

import java.net.URLEncoder

class BibleRepository(private val dao: VerseDao) {

    suspend fun getChapter(book: String, chapter: Int): List<VerseEntity> {
        val cached = dao.getChapter(book, chapter)
        if (cached.isNotEmpty()) return cached

        val reference = URLEncoder.encode("$book $chapter", "UTF-8")
        val response = BibleApi.service.getChapter(reference)
        val entities = response.verses.map {
            VerseEntity(book = it.book_name, chapter = it.chapter, verse = it.verse, text = it.text)
        }
        dao.insertAll(entities)
        return entities
    }

    suspend fun getBook(book: String, chapterCount: Int): List<VerseEntity> {
        val cached = dao.getBook(book)
        if (cached.isNotEmpty()) return cached

        val allVerses = mutableListOf<VerseEntity>()
        (1..chapterCount).forEach { chapter ->
            val verses = getChapter(book, chapter)
            allVerses.addAll(verses)
        }
        return allVerses
    }

    suspend fun downloadBook(book: String, chapterCount: Int) {
        (1..chapterCount).forEach { chapter ->
            val cached = dao.getChapter(book, chapter)
            if (cached.isEmpty()) {
                val reference = URLEncoder.encode("$book $chapter", "UTF-8")
                val response = BibleApi.service.getChapter(reference)
                val entities = response.verses.map {
                    VerseEntity(book = it.book_name, chapter = it.chapter, verse = it.verse, text = it.text)
                }
                dao.insertAll(entities)
            }
        }
    }

    fun isBookDownloaded(book: String) = dao.isBookDownloaded(book)

    fun getDownloadedBooks() = dao.getDownloadedBooks()
}
