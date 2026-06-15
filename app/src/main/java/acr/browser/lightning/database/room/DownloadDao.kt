package acr.browser.lightning.database.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DownloadDao {

    @Query("SELECT * FROM download WHERE url = :url LIMIT 1")
    suspend fun findDownloadForUrl(url: String): DownloadEntity?

    @Query("SELECT COUNT(*) FROM download WHERE url = :url")
    suspend fun isDownload(url: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(download: DownloadEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertList(downloads: List<DownloadEntity>)

    @Query("DELETE FROM download WHERE url = :url")
    suspend fun deleteByUrl(url: String): Int

    @Query("DELETE FROM download")
    suspend fun deleteAll()

    @Query("SELECT * FROM download ORDER BY id DESC")
    suspend fun getAllDownloads(): List<DownloadEntity>

    @Query("SELECT COUNT(*) FROM download")
    suspend fun count(): Long
}
