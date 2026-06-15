package acr.browser.lightning.bookmark

import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.bookmark.BookmarkRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _currentFolder = MutableStateFlow<Bookmark.Folder>(Bookmark.Folder.Root)
    val currentFolder: StateFlow<Bookmark.Folder> = _currentFolder.asStateFlow()

    init {
        loadBookmarks()
    }

    fun loadBookmarks(folder: Bookmark.Folder = Bookmark.Folder.Root) {
        viewModelScope.launch {
            _currentFolder.value = folder
            val list = if (folder == Bookmark.Folder.Root) {
                bookmarkRepository.getAllBookmarksSorted()
            } else {
                bookmarkRepository.getBookmarksFromFolderSorted(folder.title)
            }
            _bookmarks.value = list
        }
    }

    fun deleteBookmark(bookmark: Bookmark.Entry) {
        viewModelScope.launch {
            bookmarkRepository.deleteBookmark(bookmark)
            loadBookmarks(_currentFolder.value)
        }
    }
}
