package acr.browser.lightning.database.room

import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.history.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomHistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override suspend fun deleteHistory() = historyDao.deleteAll()

    override suspend fun deleteHistoryEntry(url: String) = historyDao.deleteByUrl(url)

    override suspend fun visitHistoryEntry(url: String, title: String) {
        if (title.isBlank()) return
        historyDao.insert(HistoryEntity(url = url, title = title, time = System.currentTimeMillis()))
    }

    override suspend fun findHistoryEntriesContaining(query: String): List<HistoryEntry> =
        historyDao.findHistoryEntriesContaining("%$query%").map { it.toDomain() }

    override suspend fun lastHundredVisitedHistoryEntries(): List<HistoryEntry> =
        historyDao.lastHundredVisitedHistoryEntries().map { it.toDomain() }

    private fun HistoryEntity.toDomain() = HistoryEntry(
        url = url,
        title = title,
        lastTimeVisited = time
    )
}
