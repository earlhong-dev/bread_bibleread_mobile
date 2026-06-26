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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(verses: List<VerseEntity>)

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
        // Cache open databases by translation code so we don't reopen on every read
        @Volatile private var instances: MutableMap<String, BibleDatabase> = mutableMapOf()

        /**
         * Returns a database instance for the given translation code.
         *
         * Auto-detection logic (no manual flag needed):
         *  - If  assets/translations/<code>.db  exists  →  load from asset (pre-built, fast)
         *  - Otherwise                                  →  open empty DB (will be filled by parser)
         */
        fun getInstance(context: Context, translationCode: String): BibleDatabase {
            return instances[translationCode] ?: synchronized(this) {
                instances[translationCode] ?: buildDatabase(context, translationCode)
                    .also { instances[translationCode] = it }
            }
        }

        /**
         * Convenience overload — uses the user's currently selected translation.
         */
        fun getInstance(context: Context): BibleDatabase {
            val code = TranslationManager.getActiveTranslation(context)
            return getInstance(context, code)
        }

        /**
         * Call this when the user switches translations so the next
         * getInstance() opens the new DB fresh.
         */
        fun clearInstance(translationCode: String) {
            synchronized(this) {
                instances.remove(translationCode)
            }
        }

        private fun buildDatabase(context: Context, translationCode: String): BibleDatabase {
            val dbName    = TranslationManager.dbName(translationCode)
            val assetPath = TranslationManager.assetPath(translationCode)

            val builder = Room.databaseBuilder(
                context.applicationContext,
                BibleDatabase::class.java,
                dbName
            ).fallbackToDestructiveMigration()

            // Check if a pre-built .db exists in assets/translations/
            val hasPrebuilt = try {
                context.assets.open(assetPath).use { true }
            } catch (e: Exception) {
                false
            }

            if (hasPrebuilt) {
                // Phase 2: load from the shipped asset — zero parse time on first launch
                builder.createFromAsset(assetPath)
            }
            // Phase 1 (no asset yet): opens an empty DB; BibleXmlParser will populate it

            return builder.build()
        }
    }
}
