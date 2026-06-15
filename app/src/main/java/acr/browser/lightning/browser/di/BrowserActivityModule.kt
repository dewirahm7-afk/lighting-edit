package acr.browser.lightning.browser.di

import acr.browser.lightning.browser.BrowserActivity
import acr.browser.lightning.browser.view.ViewDelegate
import acr.browser.lightning.databinding.BrowserBottomTabsBinding
import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
object BrowserActivityModule {

    @Provides
    fun provideBrowserActivity(activity: Activity): BrowserActivity = activity as BrowserActivity

    @Provides
    @IncognitoMode
    fun provideIncognitoMode(activity: BrowserActivity): Boolean = activity.isIncognito()

    @Provides
    @InitialIntent
    fun provideInitialIntent(activity: BrowserActivity): Intent? = activity.intent
}
