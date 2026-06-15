package acr.browser.lightning

import acr.browser.lightning.browser.BrowserActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * The default browsing experience.
 */
@AndroidEntryPoint
class DefaultBrowserActivity : BrowserActivity() {
    override fun isIncognito(): Boolean = false

    override fun menu(): Int = R.menu.main

    override fun homeIcon(): Int = R.drawable.ic_action_home
}
