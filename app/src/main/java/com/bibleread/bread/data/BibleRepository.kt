package com.bibleread.bread.data

import kotlinx.coroutines.flow.firstOrNull

class BibleRepository(
    private val verseDao: VerseDao,
    private val bookmarkDao: BookmarkDao
) {

    suspend fun getChapter(book: String, chapter: Int): List<VerseEntity> {
        return verseDao.getChapter(book, chapter)
    }

    suspend fun getBook(book: String): List<VerseEntity> {
        return verseDao.getBook(book)
    }

    suspend fun getTotalVerseCount(): Int = verseDao.getTotalVerseCount()

    fun isBookDownloaded(book: String) = verseDao.isBookDownloaded(book)

    fun getDownloadedBooks() = verseDao.getDownloadedBooks()

    // Bookmark methods
    fun getAllBookmarks() = bookmarkDao.getAllBookmarks()

    suspend fun toggleBookmark(verse: VerseEntity) {
        val isBookmarked = bookmarkDao.isBookmarked(verse.book, verse.chapter, verse.verse).firstOrNull() ?: false
        if (isBookmarked) {
            bookmarkDao.deleteBookmark(verse.book, verse.chapter, verse.verse)
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    book = verse.book,
                    chapter = verse.chapter,
                    verse = verse.verse,
                    text = verse.text
                )
            )
        }
    }

    fun isBookmarked(book: String, chapter: Int, verse: Int) = 
        bookmarkDao.isBookmarked(book, chapter, verse)
}
