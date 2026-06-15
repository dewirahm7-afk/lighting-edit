package acr.browser.lightning.database.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmark WHERE url = :url OR url = :alternateUrl LIMIT 1")
    suspend fun findBookmarkForUrl(url: String, alternateUrl: String): BookmarkEntity?

    @Query("SELECT COUNT(*) FROM bookmark WHERE url = :url OR url = :alternateUrl")
    suspend fun isBookmark(url: String, alternateUrl: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertList(bookmarks: List<BookmarkEntity>)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity): Int

    @Query("DELETE FROM bookmark WHERE url = :url OR url = :alternateUrl")
    suspend fun deleteByUrl(url: String, alternateUrl: String): Int

    @Query("UPDATE bookmark SET folder = :newName WHERE folder = :oldName")
    suspend fun renameFolder(oldName: String, newName: String)

    @Query("DELETE FROM bookmark")
    suspend fun deleteAll()

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmark ORDER BY folder, position ASC, title COLLATE NOCASE ASC, url ASC")
    suspend fun getAllBookmarksSorted(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmark WHERE folder = :folder ORDER BY position ASC, title COLLATE NOCASE ASC, url ASC")
    suspend fun getBookmarksFromFolderSorted(folder: String): List<BookmarkEntity>

    @Query("SELECT DISTINCT folder FROM bookmark WHERE folder != '' ORDER BY folder ASC")
    suspend fun getFoldersSorted(): List<String>

    @Query("SELECT COUNT(*) FROM bookmark")
    suspend fun count(): Long
}
