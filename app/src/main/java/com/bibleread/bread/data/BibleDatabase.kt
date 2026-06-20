package com.bibleread.bread.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "verses", primaryKeys = ["book", "chapter", "verse"])
data class VerseEntity(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val display: String? = null,     // combined verse label e.g. "2-6a", "6b-11"
    val heading: String? = null,     // section heading above this verse
    val subheading: String? = null   // author/musical note below heading
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface VerseDao {
    @Query("SELECT * FROM verses WHERE book = :book AND chapter = :chapter ORDER BY verse ASC")
    suspend fun getChapter(book: String, chapter: Int): List<VerseEntity>

    @Query("SELECT * FROM verses WHERE book = :book ORDER BY chapter ASC, verse ASC")
    suspend fun getBook(book: String): List<VerseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(verses: List<VerseEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM verses WHERE book = :book LIMIT 1)")
    fun isBookDownloaded(book: String): Flow<Boolean>

    @Query("SELECT DISTINCT book FROM verses")
    fun getDownloadedBooks(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM verses")
    suspend fun getTotalVerseCount(): Int
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE book = :book AND chapter = :chapter AND verse = :verse)")
    fun isBookmarked(book: String, chapter: Int, verse: Int): Flow<Boolean>

    @Query("DELETE FROM bookmarks WHERE book = :book AND chapter = :chapter AND verse = :verse")
    suspend fun deleteBookmark(book: String, chapter: Int, verse: Int)
}

@Database(entities = [VerseEntity::class, BookmarkEntity::class], version = 4, exportSchema = false)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun verseDao(): VerseDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile private var INSTANCE: BibleDatabase? = null

        fun getInstance(context: Context): BibleDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BibleDatabase::class.java,
                    "bible.db"
                )
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
    }
}
