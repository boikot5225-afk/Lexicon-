package com.lexicon.app

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "cards",
    indices = [Index(value = ["language", "word", "translation"], unique = true)]
)
data class WordCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val language: String,
    val word: String,
    val reading: String,
    val translation: String,
    val example: String,
    val exampleTranslation: String,
    val createdAt: Long = System.currentTimeMillis(),
    val nextReviewAt: Long = 0,
    val successfulReviews: Int = 0,
    val intervalDays: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val writingNextReviewAt: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val writingSuccessfulReviews: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val writingIntervalDays: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val introducedAt: Long = 0
)

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE language = :language ORDER BY createdAt DESC, id DESC")
    fun observeByLanguage(language: String): Flow<List<WordCard>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(cards: List<WordCard>): List<Long>

    @Update
    suspend fun update(card: WordCard)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [WordCard::class], version = 2, exportSchema = false)
abstract class LexiconDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    companion object {
        @Volatile private var instance: LexiconDatabase? = null
        // Keep old recognition progress and all imported cards when upgrading from 0.2.
        // Writing history starts empty and is scheduled independently.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN writingNextReviewAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cards ADD COLUMN writingSuccessfulReviews INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cards ADD COLUMN writingIntervalDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cards ADD COLUMN introducedAt INTEGER NOT NULL DEFAULT 0")
                // Previously reviewed cards are no longer considered unintroduced.
                db.execSQL(
                    "UPDATE cards SET introducedAt = createdAt " +
                        "WHERE nextReviewAt > 0 OR successfulReviews > 0"
                )
            }
        }

        fun get(context: Context): LexiconDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, LexiconDatabase::class.java, "lexicon.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
