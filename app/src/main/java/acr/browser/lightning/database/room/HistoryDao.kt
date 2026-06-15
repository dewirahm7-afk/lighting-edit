package acr.browser.lightning.database.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HistoryDao {

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    @Query("SELECT * FROM history WHERE title LIKE :query OR url LIKE :query ORDER BY time DESC LIMIT 5")
    suspend fun findHistoryEntriesContaining(query: String): List<HistoryEntity>

    @Query("SELECT * FROM history ORDER BY time DESC LIMIT 100")
    suspend fun lastHundredVisitedHistoryEntries(): List<HistoryEntity>

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Long
}
