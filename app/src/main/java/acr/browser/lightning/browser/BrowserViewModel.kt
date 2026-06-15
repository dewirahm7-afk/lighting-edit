package acr.browser.lightning.browser

import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.database.downloads.DownloadsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val downloadsRepository: DownloadsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Logika inisialisasi data dari BrowserPresenter akan dipindahkan ke sini secara bertahap
        }
    }
}

data class BrowserUiState(
    val displayUrl: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0
)
