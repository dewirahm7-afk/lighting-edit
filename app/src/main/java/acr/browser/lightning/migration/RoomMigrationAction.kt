package acr.browser.lightning.migration

import acr.browser.lightning.database.bookmark.BookmarkDatabase
import acr.browser.lightning.database.downloads.DownloadsDatabase
import acr.browser.lightning.database.history.HistoryDatabase
import acr.browser.lightning.database.room.BookmarkDao
import acr.browser.lightning.database.room.BookmarkEntity
import acr.browser.lightning.database.room.DownloadDao
import acr.browser.lightning.database.room.DownloadEntity
import acr.browser.lightning.database.room.HistoryDao
import acr.browser.lightning.database.room.HistoryEntity
import acr.browser.lightning.preference.UserPreferencesDataStore
import javax.inject.Inject

/**
 * Migrates data from the legacy SQLite databases to the Room database.
 */
class RoomMigrationAction @Inject constructor(
    private val legacyBookmarkDatabase: BookmarkDatabase,
    private val legacyHistoryDatabase: HistoryDatabase,
    private val legacyDownloadsDatabase: DownloadsDatabase,
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val downloadDao: DownloadDao,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : Cleanup.Action {

    override val versionCode: Int = 103 // Next version code

    override suspend fun execute() {
        if (userPreferencesDataStore.roomMigrationComplete.get()) {
            return
        }

        // Migrate Bookmarks
        val bookmarks = legacyBookmarkDatabase.getAllBookmarksSorted()
        if (bookmarks.isNotEmpty()) {
            bookmarkDao.insertList(bookmarks.map { 
                BookmarkEntity(
                    url = it.url,
                    title = it.title,
                    folder = it.folder.title,
                    position = it.position
                )
            })
            legacyBookmarkDatabase.deleteAllBookmarks()
        }

        // Migrate History
        val history = legacyHistoryDatabase.lastHundredVisitedHistoryEntries()
        if (history.isNotEmpty()) {
            history.forEach {
                historyDao.insert(HistoryEntity(url = it.url, title = it.title, time = it.lastTimeVisited))
            }
            legacyHistoryDatabase.deleteHistory()
        }

        // Migrate Downloads
        val downloads = legacyDownloadsDatabase.getAllDownloads()
        if (downloads.isNotEmpty()) {
            downloadDao.insertList(downloads.map {
                DownloadEntity(url = it.url, title = it.title, size = it.contentSize)
            })
            legacyDownloadsDatabase.deleteAllDownloads()
        }

        userPreferencesDataStore.roomMigrationComplete.set(true)
    }
}
