package acr.browser.lightning.browser.tab

import dagger.hilt.android.scopes.ActivityScoped
import acr.browser.lightning.browser.view.WebViewLongPressHandler
import acr.browser.lightning.browser.view.WebViewScrollCoordinator
import acr.browser.lightning.browser.view.targetUrl.LongPress
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.view.children
import javax.inject.Inject

/**
 * A sort of coordinator that manages the relationship between [WebViews][WebView] and the container
 * the views are placed in.
 */
@ActivityScoped
class TabPager @Inject constructor(
    private val webViewScrollCoordinator: WebViewScrollCoordinator,
    private val webViewLongPressHandler: WebViewLongPressHandler
) {

    private var container: FrameLayout? = null
    private val webViews: MutableMap<Int, Lazy<WebView>> = mutableMapOf()

    var longPressListener: ((id: Int, longPress: LongPress) -> Unit)? = null

    /**
     * Attach the container to the pager.
     */
    fun attach(container: FrameLayout) {
        this.container = container
    }

    /**
     * Select the tab with the provided [id] to be displayed by the pager.
     */
    fun selectTab(id: Int) {
        val container = this.container ?: return
        container.removeWebViews(excludeId = id)
        val webView = webViews[id]?.value ?: return
        if (webView.parent != container) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            container.addView(
                webView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        webView.visibility = android.view.View.VISIBLE
        webView.alpha = 1.0f
        webView.bringToFront()
        webView.requestFocus()

        webViewScrollCoordinator.configure(webView)
        webViewLongPressHandler.configure(webView, onLongClick = {
            longPressListener?.invoke(id, it)
        })
    }

    /**
     * Clear the container of the [WebView] currently shown.
     */
    fun clearTab() {
        container?.removeWebViews()
    }

    /**
     * Add a [WebView] to the list of views shown by this pager.
     */
    fun addTab(id: Int, webView: Lazy<WebView>) {
        webViews[id] = webView
    }

    /**
     * Show the toolbar/search box if it is currently hidden.
     */
    fun showToolbar() {
        webViewScrollCoordinator.showToolbar()
    }

    fun isBottomTabDrawerOpen() = webViewScrollCoordinator.isBottomTabDrawerOpen()

    fun openBottomTabDrawer() {
        webViewScrollCoordinator.openBottomTabDrawer()
    }

    fun closeBottomTabDrawer() {
        webViewScrollCoordinator.closeBottomTabDrawer()
    }

    private fun FrameLayout.removeWebViews(excludeId: Int = -1) {
        children
            .filterIsInstance<WebView>()
            .filter { it.id != excludeId }
            .forEach(this::removeView)
    }

}
