package com.bibleread.bread.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "verses", primaryKeys = ["book", "chapter", "verse"])
data class VerseEntity(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String
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
}

@Database(entities = [VerseEntity::class], version = 1, exportSchema = false)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun verseDao(): VerseDao

    companion object {
        @Volatile private var INSTANCE: BibleDatabase? = null

        fun getInstance(context: Context): BibleDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BibleDatabase::class.java,
                    "bible.db"
                ).build().also { INSTANCE = it }
            }
    }
}
