package acr.browser.lightning.browser

import acr.browser.lightning.AppTheme
import acr.browser.lightning.R
import acr.browser.lightning.ThemableBrowserActivity
import acr.browser.lightning.animation.AnimationUtils
import acr.browser.lightning.browser.bookmark.BookmarkRecyclerViewAdapter
import acr.browser.lightning.browser.color.ColorAnimator
import acr.browser.lightning.browser.di.MainHandler
import acr.browser.lightning.browser.image.ImageLoader
import acr.browser.lightning.browser.keys.KeyEventAdapter
import acr.browser.lightning.browser.menu.MenuItemAdapter
import acr.browser.lightning.browser.search.IntentExtractor
import acr.browser.lightning.browser.search.SearchListener
import acr.browser.lightning.browser.search.StyleRemovingTextWatcher
import acr.browser.lightning.browser.tab.BottomDrawerTabRecyclerViewAdapter
import acr.browser.lightning.browser.tab.DesktopTabRecyclerViewAdapter
import acr.browser.lightning.browser.tab.DrawerTabRecyclerViewAdapter
import acr.browser.lightning.browser.tab.TabPager
import acr.browser.lightning.browser.tab.TabViewHolder
import acr.browser.lightning.browser.tab.TabViewState
import acr.browser.lightning.browser.theme.ThemeProvider
import acr.browser.lightning.browser.ui.BookmarkConfiguration
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.browser.ui.UiConfiguration
import acr.browser.lightning.browser.view.ViewDelegate
import acr.browser.lightning.browser.view.WebViewScrollCoordinator
import acr.browser.lightning.browser.view.delegates.BottomTabViewDelegate
import acr.browser.lightning.browser.view.delegates.DesktopTabViewDelegate
import acr.browser.lightning.browser.view.delegates.DrawerTabViewDelegate
import acr.browser.lightning.browser.view.targetUrl.LongPress
import acr.browser.lightning.constant.HTTP
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.database.WebPage
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.databinding.BrowserActivityBottomBinding
import acr.browser.lightning.databinding.BrowserActivityDesktopBinding
import acr.browser.lightning.databinding.BrowserActivityDrawerBinding
import acr.browser.lightning.databinding.BrowserBottomTabsBinding
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.extensions.color
import acr.browser.lightning.extensions.drawable
import acr.browser.lightning.extensions.resizeAndShow
import acr.browser.lightning.extensions.takeIfInstance
import acr.browser.lightning.extensions.tint
import acr.browser.lightning.preference.datastore.getUnsafe
import acr.browser.lightning.search.SuggestionsAdapter
import acr.browser.lightning.ssl.createSslDrawableForState
import acr.browser.lightning.utils.value
import acr.browser.lightning.di.PrelaunchEntryPoint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import acr.browser.lightning.bookmark.BookmarkScreen
import acr.browser.lightning.bookmark.BookmarkViewModel
import acr.browser.lightning.compose.BrowserTheme
import androidx.activity.viewModels
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.EntryPoints
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The base browser activity that governs the browsing experience for both default and incognito
 * browsers.
 */
@AndroidEntryPoint
abstract class BrowserActivity : ThemableBrowserActivity() {

    internal lateinit var viewDelegate: ViewDelegate
    internal var bottomTabsBinding: BrowserBottomTabsBinding? = null

    private lateinit var tabsAdapter: ListAdapter<TabViewState, TabViewHolder>
    private var activeRecyclerView: RecyclerView? = null

    private var menuItemShare: MenuItem? = null
    private var menuItemCopyLink: MenuItem? = null
    private var menuItemAddToHome: MenuItem? = null
    private var menuItemAddBookmark: MenuItem? = null

    private val defaultColor by lazy { color(R.color.primary_color) }
    private val backgroundDrawable by lazy { defaultColor.toDrawable() }

    private var customView: View? = null

    private var pendingScroll = -1

    private val bookmarkViewModel: BookmarkViewModel by viewModels()

    @Suppress("ConvertLambdaToReference")
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { presenter.onFileChooserResult(it) }

    @Inject
    internal lateinit var imageLoader: ImageLoader

    @Inject
    internal lateinit var keyEventAdapter: KeyEventAdapter

    @Inject
    internal lateinit var menuItemAdapter: MenuItemAdapter

    @Inject
    internal lateinit var inputMethodManager: InputMethodManager

    @Inject
    internal lateinit var presenter: BrowserPresenter

    @Inject
    internal lateinit var tabPager: TabPager

    @Inject
    internal lateinit var intentExtractor: IntentExtractor

    @Inject
    internal lateinit var lightningDialogBuilder: LightningDialogBuilder

    @Inject
    internal lateinit var uiConfiguration: UiConfiguration

    @Inject
    internal lateinit var themeProvider: ThemeProvider

    @MainHandler
    @Inject
    internal lateinit var mainHandler: Handler

    @Inject
    internal lateinit var webViewScrollCoordinator: WebViewScrollCoordinator

    /**
     * True if the activity is operating in incognito mode, false otherwise.
     */
    abstract fun isIncognito(): Boolean

    /**
     * Provide the menu used by the browser instance.
     */
    @MenuRes
    abstract fun menu(): Int

    /**
     * Provide the home icon used by the browser instance.
     */
    @DrawableRes
    abstract fun homeIcon(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val entryPoint = EntryPoints.get(applicationContext, PrelaunchEntryPoint::class.java)
        val userPrefs = entryPoint.userPreferencesDataStore()

        viewDelegate = when (userPrefs.tabConfiguration.getUnsafe()) {
            TabConfiguration.DESKTOP -> {
                val actualBinding = BrowserActivityDesktopBinding.inflate(layoutInflater)
                DesktopTabViewDelegate(actualBinding)
            }

            TabConfiguration.DRAWER_SIDE -> {
                val actualBinding = BrowserActivityDrawerBinding.inflate(layoutInflater)
                DrawerTabViewDelegate(actualBinding)
            }

            TabConfiguration.DRAWER_BOTTOM -> {
                val actualBinding = BrowserActivityBottomBinding.inflate(layoutInflater)
                BottomTabViewDelegate(actualBinding)
            }
        }

        bottomTabsBinding = if (viewDelegate.browserLayoutContainer != null) {
            BrowserBottomTabsBinding.inflate(layoutInflater)
        } else {
            null
        }

        viewDelegate.root.visibility = View.VISIBLE
        setContentView(viewDelegate.root)
        setSupportActionBar(viewDelegate.toolbar)

        tabPager.attach(viewDelegate.contentFrame)
        webViewScrollCoordinator.attach(
            bottomTabsLayout = bottomTabsBinding,
            browserLayoutContainer = viewDelegate.browserLayoutContainer,
            browserFrame = viewDelegate.contentFrame,
            toolbarRoot = viewDelegate.uiLayout,
            toolbar = viewDelegate.toolbarLayout
        )

        viewDelegate.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {

            override fun onDrawerOpened(drawerView: View) {
                if (drawerView == viewDelegate.tabDrawer) {
                    presenter.onTabDrawerMoved(isOpen = true)
                } else if (drawerView == viewDelegate.bookmarkDrawer) {
                    presenter.onBookmarkDrawerMoved(isOpen = true)
                }
            }

            override fun onDrawerClosed(drawerView: View) {
                if (drawerView == viewDelegate.tabDrawer) {
                    presenter.onTabDrawerMoved(isOpen = false)
                } else if (drawerView == viewDelegate.bookmarkDrawer) {
                    presenter.onBookmarkDrawerMoved(isOpen = false)
                }
            }
        })

        viewDelegate.bookmarkDrawer.layoutParams =
            (viewDelegate.bookmarkDrawer.layoutParams as DrawerLayout.LayoutParams).apply {
                gravity = when (uiConfiguration.bookmarkConfiguration) {
                    BookmarkConfiguration.LEFT -> Gravity.START
                    BookmarkConfiguration.RIGHT -> Gravity.END
                }
            }

        viewDelegate.tabDrawer.layoutParams =
            (viewDelegate.tabDrawer.layoutParams as DrawerLayout.LayoutParams).apply {
                gravity = when (uiConfiguration.bookmarkConfiguration) {
                    BookmarkConfiguration.LEFT -> Gravity.END
                    BookmarkConfiguration.RIGHT -> Gravity.START
                }
            }

        viewDelegate.homeImageView.isVisible =
            uiConfiguration.tabConfiguration == TabConfiguration.DESKTOP || isIncognito()
        viewDelegate.homeImageView.setImageResource(homeIcon())
        viewDelegate.tabCountView.isVisible =
            uiConfiguration.tabConfiguration != TabConfiguration.DESKTOP && !isIncognito()

        if (uiConfiguration.tabConfiguration != TabConfiguration.DRAWER_SIDE) {
            viewDelegate.drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                viewDelegate.tabDrawer
            )
        }

        if (uiConfiguration.tabConfiguration != TabConfiguration.DESKTOP) {
            if (viewDelegate.browserLayoutContainer == null) {
                tabsAdapter = DrawerTabRecyclerViewAdapter(
                    onClick = presenter::onTabClick,
                    onCloseClick = presenter::onTabClose,
                    onLongClick = presenter::onTabLongClick
                )
                viewDelegate.drawerTabsList.isVisible = true
                viewDelegate.drawerTabsList.adapter = tabsAdapter
                viewDelegate.drawerTabsList.layoutManager = LinearLayoutManager(this)
                viewDelegate.drawerTabsList.itemAnimator?.takeIfInstance<SimpleItemAnimator>()
                    ?.supportsChangeAnimations = false
                viewDelegate.desktopTabsList.isVisible = false
                activeRecyclerView = viewDelegate.desktopTabsList
            } else {
                tabsAdapter = BottomDrawerTabRecyclerViewAdapter(
                    themeProvider,
                    onClick = presenter::onTabClick,
                    onLongClick = presenter::onTabLongClick,
                    onCloseClick = presenter::onTabClose,
                    onBackClick = { presenter.onBackClick() },
                    onForwardClick = { presenter.onForwardClick() },
                    onHomeClick = { presenter.onHomeClick() }
                )
                bottomTabsBinding!!.bottomTabList.adapter = tabsAdapter
                bottomTabsBinding!!.bottomTabList.layoutManager =
                    LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
                bottomTabsBinding!!.bottomTabList.itemAnimator?.takeIfInstance<SimpleItemAnimator>()
                    ?.supportsChangeAnimations = false
                viewDelegate.drawerTabsList.isVisible = false
                viewDelegate.desktopTabsList.isVisible = false
                activeRecyclerView = bottomTabsBinding!!.bottomTabList
            }
        } else {
            tabsAdapter = DesktopTabRecyclerViewAdapter(
                context = this,
                onClick = presenter::onTabClick,
                onCloseClick = presenter::onTabClose,
                onLongClick = presenter::onTabLongClick
            )
            viewDelegate.desktopTabsList.isVisible = true
            viewDelegate.desktopTabsList.adapter = tabsAdapter
            viewDelegate.desktopTabsList.layoutManager =
                LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
            viewDelegate.desktopTabsList.itemAnimator?.takeIfInstance<SimpleItemAnimator>()
                ?.supportsChangeAnimations = false
            viewDelegate.drawerTabsList.isVisible = false
            activeRecyclerView = viewDelegate.desktopTabsList
        }

        viewDelegate.bookmarkComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BrowserTheme(activity = this@BrowserActivity) {
                    BookmarkScreen(
                        viewModel = bookmarkViewModel,
                        onBookmarkClick = { url ->
                            presenter.onNewAction(BrowserContract.Action.LoadUrl(url))
                            closeBookmarkDrawer()
                        }
                    )
                }
            }
        }

        presenter.onViewAttached(BrowserStateAdapter(this))

        val suggestionsAdapter = SuggestionsAdapter(this, isIncognito = isIncognito()).apply {
            onSuggestionInsertClick = {
                if (it is SearchSuggestion) {
                    viewDelegate.search.setText(it.title)
                    viewDelegate.search.setSelection(it.title.length)
                } else {
                    viewDelegate.search.setText(it.url)
                    viewDelegate.search.setSelection(it.url.length)
                }
            }
        }
        viewDelegate.search.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            viewDelegate.search.clearFocus()
            presenter.onSearchSuggestionClicked(suggestionsAdapter.getItem(position) as WebPage)
            inputMethodManager.hideSoftInputFromWindow(viewDelegate.root.windowToken, 0)
        }
        viewDelegate.search.setAdapter(suggestionsAdapter)
        val searchListener = SearchListener(
            onConfirm = { presenter.onSearch(viewDelegate.search.text.toString()) },
            inputMethodManager = inputMethodManager
        )
        viewDelegate.search.setOnEditorActionListener(searchListener)
        viewDelegate.search.setOnKeyListener(searchListener)
        viewDelegate.search.addTextChangedListener(StyleRemovingTextWatcher())
        viewDelegate.search.setOnFocusChangeListener { _, hasFocus ->
            presenter.onSearchFocusChanged(hasFocus)
            viewDelegate.search.selectAll()
        }

        viewDelegate.findPrevious.setOnClickListener { presenter.onFindPrevious() }
        viewDelegate.findNext.setOnClickListener { presenter.onFindNext() }
        viewDelegate.findQuit.setOnClickListener { presenter.onFindDismiss() }

        viewDelegate.homeButton.setOnClickListener { presenter.onTabCountViewClick() }
        viewDelegate.actionBack.setOnClickListener { presenter.onBackClick() }
        viewDelegate.actionForward.setOnClickListener { presenter.onForwardClick() }
        viewDelegate.actionHome.setOnClickListener { presenter.onHomeClick() }
        viewDelegate.newTabButton.setOnClickListener { presenter.onNewTabClick() }
        viewDelegate.newTabButton.setOnLongClickListener {
            presenter.onNewTabLongClick()
            true
        }
        viewDelegate.searchRefresh.setOnClickListener { presenter.onRefreshOrStopClick() }
        viewDelegate.actionAddBookmark.setOnClickListener { presenter.onStarClick() }
        viewDelegate.actionPageTools.setOnClickListener { presenter.onToolsClick() }
        viewDelegate.tabHeaderButton.setOnClickListener { presenter.onTabMenuClick() }
        viewDelegate.bookmarkBackButton.setOnClickListener { presenter.onBookmarkMenuClick() }
        viewDelegate.searchSslStatus.setOnClickListener { presenter.onSslIconClick() }

        tabPager.longPressListener = presenter::onPageLongPress

        onBackPressedDispatcher.addCallback {
            presenter.onNavigateBack()
        }
    }

    override fun onNewIntent(intent: Intent) {
        intentExtractor.extractUrlFromIntent(intent)?.let(presenter::onNewAction)
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onViewDetached()
    }

    override fun onPause() {
        super.onPause()
        presenter.onViewHidden()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(menu(), menu)
        menuItemShare = menu.findItem(R.id.action_share)
        menuItemCopyLink = menu.findItem(R.id.action_copy)
        menuItemAddToHome = menu.findItem(R.id.action_add_to_homescreen)
        menuItemAddBookmark = menu.findItem(R.id.action_add_bookmark)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return menuItemAdapter.adaptMenuItem(item)?.let(presenter::onMenuClick)?.let { true }
            ?: super.onOptionsItemSelected(item)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return keyEventAdapter.adaptKeyEvent(event)?.let(presenter::onKeyComboClick)?.let { true }
            ?: super.onKeyUp(keyCode, event)
    }

    /**
     * @see BrowserContract.View.renderState
     */
    fun renderState(viewState: PartialBrowserViewState) {
        viewState.isBackEnabled?.let { viewDelegate.actionBack.isEnabled = it }
        viewState.isForwardEnabled?.let { viewDelegate.actionForward.isEnabled = it }
        viewState.displayUrl?.let(viewDelegate.search::setText)
        viewState.sslState?.let {
            viewDelegate.searchSslStatus.setImageDrawable(createSslDrawableForState(it))
            viewDelegate.searchSslStatus.updateVisibilityForDrawable()
        }
        viewState.enableFullMenu?.let {
            menuItemShare?.isVisible = it
            menuItemCopyLink?.isVisible = it
            menuItemAddToHome?.isVisible = it
            menuItemAddBookmark?.isVisible = it
        }
        viewState.themeColor?.value()?.let(::animateColorChange)
        viewState.progress?.let {
            viewDelegate.progressView.isVisible = it != 100
            viewDelegate.progressView.progress = it
        }
        viewState.isRefresh?.let {
            viewDelegate.searchRefresh.setImageResource(
                if (it) {
                    R.drawable.ic_action_refresh
                } else {
                    R.drawable.ic_action_delete
                }
            )
        }
        viewState.isRootFolder?.let {
            viewDelegate.bookmarkBackButton.startAnimation(
                AnimationUtils.createRotationTransitionAnimation(
                    viewDelegate.bookmarkBackButton,
                    if (it) {
                        R.drawable.ic_action_star
                    } else {
                        R.drawable.ic_action_back
                    }
                )
            )
        }
        viewState.findInPage?.let {
            if (it.isEmpty()) {
                viewDelegate.findBar.isVisible = false
            } else {
                viewDelegate.findBar.isVisible = true
                viewDelegate.findQuery.text = it
            }
        }
    }

    /**
     * @see BrowserContract.View.renderTabs
     */
    fun renderTabs(tabListState: List<TabViewState>) {
        viewDelegate.tabCountView.updateCount(tabListState.size)
        val shouldScroll = tabsAdapter.itemCount < tabListState.size
        tabsAdapter.submitList(tabListState)
        val nextSelected = tabListState.indexOfFirst(TabViewState::isSelected)
        if (shouldScroll && nextSelected != -1) {
            mainHandler.post {
                if (tabPager.isBottomTabDrawerOpen()) {
                    activeRecyclerView?.smoothScrollToPosition(nextSelected)
                } else {
                    pendingScroll = nextSelected
                }
            }
        }
    }

    /**
     * @see BrowserContract.View.showAddBookmarkDialog
     */
    fun showAddBookmarkDialog(title: String, url: String, folders: List<String>) {
        lightningDialogBuilder.showAddBookmarkDialog(
            activity = this,
            currentTitle = title,
            currentUrl = url,
            folders = folders,
            onSave = presenter::onBookmarkConfirmed
        )
    }

    /**
     * @see BrowserContract.View.showBookmarkOptionsDialog
     */
    fun showBookmarkOptionsDialog(bookmark: Bookmark.Entry) {
        lightningDialogBuilder.showLongPressedDialogForBookmarkUrl(
            activity = this,
            onClick = {
                presenter.onBookmarkOptionClick(bookmark, it)
            }
        )
    }

    /**
     * @see BrowserContract.View.showEditBookmarkDialog
     */
    fun showEditBookmarkDialog(title: String, url: String, folder: String, folders: List<String>) {
        lightningDialogBuilder.showEditBookmarkDialog(
            activity = this,
            currentTitle = title,
            currentUrl = url,
            currentFolder = folder,
            folders = folders,
            onSave = presenter::onBookmarkEditConfirmed
        )
    }

    /**
     * @see BrowserContract.View.showFolderOptionsDialog
     */
    fun showFolderOptionsDialog(folder: Bookmark.Folder) {
        lightningDialogBuilder.showBookmarkFolderLongPressedDialog(
            activity = this,
            onClick = {
                presenter.onFolderOptionClick(folder, it)
            }
        )
    }

    /**
     * @see BrowserContract.View.showEditFolderDialog
     */
    fun showEditFolderDialog(oldTitle: String) {
        lightningDialogBuilder.showRenameFolderDialog(
            activity = this,
            oldTitle = oldTitle,
            onSave = presenter::onBookmarkFolderRenameConfirmed
        )
    }

    /**
     * @see BrowserContract.View.showDownloadOptionsDialog
     */
    fun showDownloadOptionsDialog(download: DownloadEntry) {
        lightningDialogBuilder.showLongPressedDialogForDownloadUrl(
            activity = this,
            onClick = {
                presenter.onDownloadOptionClick(download, it)
            }
        )
    }

    /**
     * @see BrowserContract.View.showHistoryOptionsDialog
     */
    fun showHistoryOptionsDialog(historyEntry: HistoryEntry) {
        lightningDialogBuilder.showLongPressedHistoryLinkDialog(
            activity = this,
            onClick = {
                presenter.onHistoryOptionClick(historyEntry, it)
            }
        )
    }

    /**
     * @see BrowserContract.View.showFindInPageDialog
     */
    fun showFindInPageDialog() {
        BrowserDialog.showEditText(
            this,
            R.string.action_find,
            R.string.search_hint,
            R.string.search_hint,
            presenter::onFindInPage
        )
    }

    /**
     * @see BrowserContract.View.showLinkLongPressDialog
     */
    fun showLinkLongPressDialog(longPress: LongPress) {
        BrowserDialog.show(
            this, longPress.targetUrl?.replace(HTTP, ""),
            DialogItem(title = R.string.dialog_open_new_tab) {
                presenter.onLinkLongPressEvent(
                    longPress,
                    BrowserContract.LinkLongPressEvent.NEW_TAB
                )
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                presenter.onLinkLongPressEvent(
                    longPress,
                    BrowserContract.LinkLongPressEvent.BACKGROUND_TAB
                )
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = !isIncognito()
            ) {
                presenter.onLinkLongPressEvent(
                    longPress,
                    BrowserContract.LinkLongPressEvent.INCOGNITO_TAB
                )
            },
            DialogItem(title = R.string.action_share) {
                presenter.onLinkLongPressEvent(longPress, BrowserContract.LinkLongPressEvent.SHARE)
            },
            DialogItem(title = R.string.dialog_copy_link) {
                presenter.onLinkLongPressEvent(
                    longPress,
                    BrowserContract.LinkLongPressEvent.COPY_LINK
                )
            })
    }

    /**
     * @see BrowserContract.View.showImageLongPressDialog
     */
    fun showImageLongPressDialog(longPress: LongPress) {
        BrowserDialog.show(
            this, longPress.targetUrl?.replace(HTTP, ""),
            DialogItem(title = R.string.dialog_open_new_tab) {
                presenter.onImageLongPressEvent(
                    longPress,
                    BrowserContract.ImageLongPressEvent.NEW_TAB
                )
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                presenter.onImageLongPressEvent(
                    longPress,
                    BrowserContract.ImageLongPressEvent.BACKGROUND_TAB
                )
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = !isIncognito()
            ) {
                presenter.onImageLongPressEvent(
                    longPress,
                    BrowserContract.ImageLongPressEvent.INCOGNITO_TAB
                )
            },
            DialogItem(title = R.string.action_share) {
                presenter.onImageLongPressEvent(
                    longPress,
                    BrowserContract.ImageLongPressEvent.SHARE
                )
            },
            DialogItem(title = R.string.dialog_copy_link) {
                presenter.onImageLongPressEvent(
                    longPress,
                    BrowserContract.ImageLongPressEvent.COPY_LINK
                )
            },
            DialogItem(title = R.string.dialog_download_image) {
                presenter.onImageLongPressEvent(
                    longPress,
                    BrowserContract.ImageLongPressEvent.DOWNLOAD
                )
            })
    }

    /**
     * @see BrowserContract.View.showCloseBrowserDialog
     */
    fun showCloseBrowserDialog(id: Int) {
        BrowserDialog.show(
            this, R.string.dialog_title_close_browser,
            DialogItem(title = R.string.close_tab) {
                presenter.onCloseBrowserEvent(id, BrowserContract.CloseTabEvent.CLOSE_CURRENT)
            },
            DialogItem(title = R.string.close_other_tabs) {
                presenter.onCloseBrowserEvent(id, BrowserContract.CloseTabEvent.CLOSE_OTHERS)
            },
            DialogItem(title = R.string.close_all_tabs, onClick = {
                presenter.onCloseBrowserEvent(id, BrowserContract.CloseTabEvent.CLOSE_ALL)
            })
        )
    }

    /**
     * @see BrowserContract.View.openBookmarkDrawer
     */
    fun openBookmarkDrawer() {
        viewDelegate.drawerLayout.closeDrawer(viewDelegate.tabDrawer)
        viewDelegate.drawerLayout.openDrawer(viewDelegate.bookmarkDrawer)
    }

    /**
     * @see BrowserContract.View.closeBookmarkDrawer
     */
    fun closeBookmarkDrawer() {
        viewDelegate.drawerLayout.closeDrawer(viewDelegate.bookmarkDrawer)
    }

    /**
     * @see BrowserContract.View.openTabDrawer
     */
    fun openTabDrawer() {
        viewDelegate.drawerLayout.closeDrawer(viewDelegate.bookmarkDrawer)
        if (viewDelegate.browserLayoutContainer == null) {
            viewDelegate.drawerLayout.openDrawer(viewDelegate.tabDrawer)
        } else {
            presenter.onTabDrawerMoved(isOpen = true)
            tabPager.openBottomTabDrawer()
            if (pendingScroll != -1) {
                activeRecyclerView?.scrollToPosition(pendingScroll)
                pendingScroll = -1
            }
        }
    }

    /**
     * @see BrowserContract.View.closeTabDrawer
     */
    fun closeTabDrawer() {
        if (viewDelegate.browserLayoutContainer == null) {
            viewDelegate.drawerLayout.closeDrawer(viewDelegate.tabDrawer)
        } else {
            presenter.onTabDrawerMoved(isOpen = false)
            tabPager.closeBottomTabDrawer()
        }
    }

    /**
     * @see BrowserContract.View.showToolbar
     */
    fun showToolbar() {
        tabPager.showToolbar()
    }

    /**
     * @see BrowserContract.View.showToolsDialog
     */
    fun showToolsDialog(areAdsAllowed: Boolean, shouldShowAdBlockOption: Boolean) {
        val whitelistString = if (areAdsAllowed) {
            R.string.dialog_adblock_enable_for_site
        } else {
            R.string.dialog_adblock_disable_for_site
        }

        BrowserDialog.showWithIcons(
            this, getString(R.string.dialog_tools_title),
            DialogItem(
                icon = drawable(R.drawable.ic_action_desktop),
                title = R.string.dialog_toggle_desktop,
                onClick = presenter::onToggleDesktopAgent
            ),
            DialogItem(
                icon = drawable(R.drawable.ic_block),
                colorTint = color(R.color.error_red).takeIf { areAdsAllowed },
                title = whitelistString,
                isConditionMet = shouldShowAdBlockOption,
                onClick = presenter::onToggleAdBlocking
            )
        )
    }

    /**
     * @see BrowserContract.View.showLocalFileBlockedDialog
     */
    fun showLocalFileBlockedDialog() {
        AlertDialog.Builder(this)
            .setCancelable(true)
            .setTitle(R.string.title_warning)
            .setMessage(R.string.message_blocked_local)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                presenter.onConfirmOpenLocalFile(allow = false)
            }
            .setPositiveButton(R.string.action_open) { _, _ ->
                presenter.onConfirmOpenLocalFile(allow = true)
            }
            .setOnCancelListener { presenter.onConfirmOpenLocalFile(allow = false) }
            .resizeAndShow()
    }

    /**
     * @see BrowserContract.View.showFileChooser
     */
    fun showFileChooser(intent: Intent) {
        launcher.launch(intent)
    }

    /**
     * @see BrowserContract.View.showCustomView
     */
    fun showCustomView(view: View) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        viewDelegate.root.addView(view)
        customView = view
        setFullscreen(enabled = true, immersive = true)
    }

    /**
     * @see BrowserContract.View.hideCustomView
     */
    fun hideCustomView() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        customView?.let(viewDelegate.root::removeView)
        customView = null
        setFullscreen(enabled = false, immersive = false)
    }

    /**
     * @see BrowserContract.View.clearSearchFocus
     */
    fun clearSearchFocus() {
        viewDelegate.search.clearFocus()
    }

    private fun setFullscreen(enabled: Boolean, immersive: Boolean) {
        val window = window
        val controller = WindowCompat.getInsetsController(window, window.decorView)

        if (enabled) {
            if (immersive) {
                controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        }
    }

    private fun animateColorChange(color: Int) {
        if (!userPreferencesDataStore.colorModeEnabled.getUnsafe() || userPreferencesDataStore.useTheme.getUnsafe() != AppTheme.LIGHT || isIncognito()) {
            return
        }
        val adapter = tabsAdapter as? DesktopTabRecyclerViewAdapter
        val colorAnimator = ColorAnimator(defaultColor)
        viewDelegate.toolbar.startAnimation(
            colorAnimator.animateTo(
                color
            ) { mainColor, secondaryColor ->
                if (userPreferencesDataStore.tabConfiguration.getUnsafe() != TabConfiguration.DESKTOP) {
                    backgroundDrawable.color = mainColor
                    window.setBackgroundDrawable(backgroundDrawable)
                } else {
                    adapter?.updateForegroundTabColor(mainColor)
                }
                viewDelegate.toolbar.setBackgroundColor(mainColor)
                viewDelegate.searchContainer.background?.tint(secondaryColor)
            })
    }

    private fun ImageView.updateVisibilityForDrawable() {
        visibility = if (drawable == null) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
}
