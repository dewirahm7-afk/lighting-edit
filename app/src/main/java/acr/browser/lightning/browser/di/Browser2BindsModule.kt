package acr.browser.lightning.browser.di

import acr.browser.lightning.browser.BrowserContract
import acr.browser.lightning.browser.BrowserNavigator
import acr.browser.lightning.browser.cleanup.DelegatingExitCleanup
import acr.browser.lightning.browser.cleanup.ExitCleanup
import acr.browser.lightning.browser.image.FaviconImageLoader
import acr.browser.lightning.browser.image.ImageLoader
import acr.browser.lightning.browser.tab.TabsRepository
import acr.browser.lightning.browser.theme.DefaultThemeProvider
import acr.browser.lightning.browser.theme.ThemeProvider
import android.app.Activity
import androidx.fragment.app.FragmentActivity
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * Binds implementations to interfaces for the browser scope.
 */
@Module
@InstallIn(ActivityComponent::class)
interface Browser2BindsModule {

    @Binds
    fun bindsBrowserModel(tabsRepository: TabsRepository): BrowserContract.Model

    @Binds
    fun bindsFaviconImageLoader(faviconImageLoader: FaviconImageLoader): ImageLoader

    @Binds
    fun bindsBrowserNavigator(browserNavigator: BrowserNavigator): BrowserContract.Navigator

    @Binds
    fun bindsExitCleanup(delegatingExitCleanup: DelegatingExitCleanup): ExitCleanup

    @Binds
    fun bindsThemeProvider(legacyThemeProvider: DefaultThemeProvider): ThemeProvider
}
