package com.lexicon.app

import android.content.Context
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
    val intervalDays: Int = 0
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

@Database(entities = [WordCard::class], version = 1, exportSchema = false)
abstract class LexiconDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    companion object {
        @Volatile private var instance: LexiconDatabase? = null
        fun get(context: Context): LexiconDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, LexiconDatabase::class.java, "lexicon.db"
                ).build().also { instance = it }
            }
    }
}
