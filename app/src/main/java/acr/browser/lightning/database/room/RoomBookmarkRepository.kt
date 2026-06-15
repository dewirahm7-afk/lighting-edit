package acr.browser.lightning.database.room

import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.asFolder
import acr.browser.lightning.database.bookmark.BookmarkRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomBookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {

    override suspend fun findBookmarkForUrl(url: String): Bookmark.Entry? =
        bookmarkDao.findBookmarkForUrl(url, url.alternateSlashUrl())?.toDomain()

    override suspend fun isBookmark(url: String): Boolean =
        bookmarkDao.isBookmark(url, url.alternateSlashUrl()) > 0

    override suspend fun addBookmarkIfNotExists(entry: Bookmark.Entry): Boolean {
        if (isBookmark(entry.url)) return false
        return bookmarkDao.insert(entry.toEntity()) != -1L
    }

    override suspend fun addBookmarkList(bookmarkItems: List<Bookmark.Entry>) {
        bookmarkDao.insertList(bookmarkItems.map { it.toEntity() })
    }

    override suspend fun deleteBookmark(entry: Bookmark.Entry): Boolean =
        bookmarkDao.deleteByUrl(entry.url, entry.url.alternateSlashUrl()) > 0

    override suspend fun renameFolder(oldName: String, newName: String) =
        bookmarkDao.renameFolder(oldName, newName)

    override suspend fun deleteFolder(folderToDelete: String) =
        bookmarkDao.renameFolder(folderToDelete, "")

    override suspend fun deleteAllBookmarks() =
        bookmarkDao.deleteAll()

    override suspend fun editBookmark(oldBookmark: Bookmark.Entry, newBookmark: Bookmark.Entry) {
        val entity = bookmarkDao.findBookmarkForUrl(oldBookmark.url, oldBookmark.url.alternateSlashUrl())
        if (entity != null) {
            bookmarkDao.update(newBookmark.toEntity().copy(id = entity.id))
        }
    }

    override suspend fun getAllBookmarksSorted(): List<Bookmark.Entry> =
        bookmarkDao.getAllBookmarksSorted().map { it.toDomain() }

    override suspend fun getBookmarksFromFolderSorted(folder: String?): List<Bookmark> =
        bookmarkDao.getBookmarksFromFolderSorted(folder ?: "").map { it.toDomain() }

    override suspend fun getFoldersSorted(): List<Bookmark.Folder> =
        bookmarkDao.getFoldersSorted().map { it.asFolder() }

    override suspend fun getFolderNames(): List<String> =
        bookmarkDao.getFoldersSorted()

    override suspend fun count(): Long =
        bookmarkDao.count()

    private fun BookmarkEntity.toDomain() = Bookmark.Entry(
        url = url,
        title = title,
        position = position,
        folder = folder.asFolder()
    )

    private fun Bookmark.Entry.toEntity() = BookmarkEntity(
        url = url,
        title = title,
        folder = folder.title,
        position = position
    )

    private fun String.alternateSlashUrl(): String = if (endsWith("/")) {
        substring(0, length - 1)
    } else {
        "$this/"
    }
}
