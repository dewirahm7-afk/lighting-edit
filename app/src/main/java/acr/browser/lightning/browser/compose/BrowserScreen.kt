package acr.browser.lightning.browser.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BrowserScreen(
    url: String,
    onUrlClick: () -> Unit,
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Scaffold(
        topBar = {
            BrowserToolbar(
                url = url,
                onUrlClick = onUrlClick,
                onMenuClick = onMenuClick,
                onRefreshClick = onRefreshClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder for WebView content
            Text(text = "WebView will be here content: $url")
        }
    }
}
