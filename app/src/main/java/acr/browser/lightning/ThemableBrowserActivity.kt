package acr.browser.lightning

import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.preference.datastore.getUnsafe
import acr.browser.lightning.utils.ThemeUtils
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.iterator
import acr.browser.lightning.di.PrelaunchEntryPoint
import dagger.hilt.EntryPoints
import javax.inject.Inject

/**
 * A theme aware activity that updates its theme based on the user preferences.
 */
abstract class ThemableBrowserActivity : ThemableActivity() {

    @Inject
    internal lateinit var userPreferencesDataStore: UserPreferencesDataStore

    private var themeId: AppTheme = AppTheme.LIGHT
    private var tabConfiguration: TabConfiguration = TabConfiguration.DRAWER_BOTTOM
    private var shouldRunOnResumeActions = false

    /**
     * Override this to provide an alternate theme that should be set for every instance of this
     * activity regardless of the user's preference.
     */
    @StyleRes
    protected open fun provideThemeOverride(): Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val entryPoint = EntryPoints.get(applicationContext, PrelaunchEntryPoint::class.java)
        val userPrefs = entryPoint.userPreferencesDataStore()
        
        themeId = userPrefs.useTheme.getUnsafe()
        tabConfiguration = userPrefs.tabConfiguration.getUnsafe()

        setTheme(
            provideThemeOverride() ?: when (themeId) {
                AppTheme.LIGHT -> R.style.Theme_LightTheme
                AppTheme.DARK -> R.style.Theme_DarkTheme
                AppTheme.BLACK -> R.style.Theme_BlackTheme
            }
        )
        
        super.onCreate(savedInstanceState)
        resetPreferences()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        withStyledAttributes(attrs = intArrayOf(R.attr.iconColorState)) {
            val iconTintList = getColorStateList(0)
            menu.iterator().forEach { menuItem ->
                menuItem.icon?.let {
                    DrawableCompat.setTintList(
                        DrawableCompat.wrap(it),
                        iconTintList
                    )
                }
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    private fun resetPreferences() {
        val window = window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        
        if (userPreferencesDataStore.useBlackStatusBar.getUnsafe() ||
            userPreferencesDataStore.tabConfiguration.getUnsafe() == TabConfiguration.DESKTOP
        ) {
            window.statusBarColor = Color.BLACK
            controller.isAppearanceLightStatusBars = false
        } else {
            window.statusBarColor = ThemeUtils.getStatusBarColor(this)
            // Auto-detect based on brightness
            controller.isAppearanceLightStatusBars = ThemeUtils.isColorLight(window.statusBarColor)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && shouldRunOnResumeActions) {
            shouldRunOnResumeActions = false
            onWindowVisibleToUserAfterResume()
        }
    }

    /**
     * Called after the activity is resumed
     * and the UI becomes visible to the user.
     * Called by onWindowFocusChanged only if
     * onResume has been called.
     */
    protected open fun onWindowVisibleToUserAfterResume() = Unit

    override fun onResume() {
        super.onResume()
        resetPreferences()
        shouldRunOnResumeActions = true
        val nextTabConfiguration = userPreferencesDataStore.tabConfiguration.getUnsafe()
        if (themeId != userPreferencesDataStore.useTheme.getUnsafe() || tabConfiguration != nextTabConfiguration) {
            restart()
        }
    }

    protected fun restart() {
        finish()
        startActivity(Intent(this, javaClass))
    }
}
