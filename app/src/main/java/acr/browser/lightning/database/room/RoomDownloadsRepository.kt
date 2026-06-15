package acr.browser.lightning.database.room

import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.database.downloads.DownloadsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomDownloadsRepository @Inject constructor(
    private val downloadDao: DownloadDao
) : DownloadsRepository {

    override suspend fun findDownloadForUrl(url: String): DownloadEntry? =
        downloadDao.findDownloadForUrl(url)?.toDomain()

    override suspend fun isDownload(url: String): Boolean =
        downloadDao.isDownload(url) > 0

    override suspend fun addDownloadIfNotExists(entry: DownloadEntry): Boolean {
        if (isDownload(entry.url)) return false
        return downloadDao.insert(entry.toEntity()) != -1L
    }

    override suspend fun addDownloadsList(downloadEntries: List<DownloadEntry>) {
        downloadDao.insertList(downloadEntries.map { it.toEntity() })
    }

    override suspend fun deleteDownload(url: String): Boolean =
        downloadDao.deleteByUrl(url) > 0

    override suspend fun deleteAllDownloads() =
        downloadDao.deleteAll()

    override suspend fun getAllDownloads(): List<DownloadEntry> =
        downloadDao.getAllDownloads().map { it.toDomain() }

    override suspend fun count(): Long =
        downloadDao.count()

    private fun DownloadEntity.toDomain() = DownloadEntry(
        url = url,
        title = title,
        contentSize = size
    )

    private fun DownloadEntry.toEntity() = DownloadEntity(
        url = url,
        title = title,
        size = contentSize
    )
}
