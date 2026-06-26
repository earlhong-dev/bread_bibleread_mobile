package com.bibleread.bread.data

import kotlinx.coroutines.flow.firstOrNull

class BibleRepository(
    private val verseDao: VerseDao,
    private val bookmarkDao: BookmarkDao
) {
    suspend fun getChapter(book: String, chapter: Int) = verseDao.getChapter(book, chapter)
    suspend fun getTotalVerseCount() = verseDao.getTotalVerseCount()

    fun getAllBookmarks() = bookmarkDao.getAllBookmarks()
    fun isBookmarked(book: String, chapter: Int, verse: Int) = bookmarkDao.isBookmarked(book, chapter, verse)

    suspend fun toggleBookmark(verse: VerseEntity) {
        val isBookmarked = bookmarkDao.isBookmarked(verse.book, verse.chapter, verse.verse).firstOrNull() ?: false
        if (isBookmarked) {
            bookmarkDao.deleteBookmark(verse.book, verse.chapter, verse.verse)
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(book = verse.book, chapter = verse.chapter, verse = verse.verse, text = verse.text)
            )
        }
    }
}
