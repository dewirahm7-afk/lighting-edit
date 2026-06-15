package acr.browser.lightning.bookmark

import acr.browser.lightning.database.Bookmark
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun BookmarkScreen(
    viewModel: BookmarkViewModel,
    onBookmarkClick: (String) -> Unit
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (currentFolder != Bookmark.Folder.Root) {
            TextButton(
                onClick = { viewModel.loadBookmarks(Bookmark.Folder.Root) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("< Back to Root")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bookmarks) { item ->
                BookmarkItem(
                    bookmark = item,
                    onClick = {
                        when (item) {
                            is Bookmark.Entry -> onBookmarkClick(item.url)
                            is Bookmark.Folder.Entry -> viewModel.loadBookmarks(item)
                            else -> Unit
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BookmarkItem(
    bookmark: Bookmark,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (bookmark is Bookmark.Folder) Icons.Default.Folder else Icons.Default.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (bookmark is Bookmark.Entry) {
                    Text(
                        text = bookmark.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
