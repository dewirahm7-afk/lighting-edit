package acr.browser.lightning.browser.di

import acr.browser.lightning.adblock.allowlist.AllowListModel
import acr.browser.lightning.adblock.allowlist.SessionAllowListModel
import acr.browser.lightning.adblock.source.AssetsHostsDataSource
import acr.browser.lightning.adblock.source.HostsDataSource
import acr.browser.lightning.adblock.source.HostsDataSourceProvider
import acr.browser.lightning.adblock.source.PreferencesHostsDataSourceProvider
import acr.browser.lightning.database.adblock.HostsDatabase
import acr.browser.lightning.database.adblock.HostsRepository
import acr.browser.lightning.database.allowlist.AdBlockAllowListDatabase
import acr.browser.lightning.database.allowlist.AdBlockAllowListRepository
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.database.room.RoomBookmarkRepository
import acr.browser.lightning.database.room.RoomDownloadsRepository
import acr.browser.lightning.database.room.RoomHistoryRepository
import acr.browser.lightning.resources.DefaultResourceProvider
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.settings.adblock.DefaultHostsFileUpdater
import acr.browser.lightning.settings.adblock.HostsFileUpdater
import acr.browser.lightning.ssl.SessionSslWarningPreferences
import acr.browser.lightning.ssl.SslWarningPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Dependency injection module used to bind implementations to interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
interface AppBindsModule {

    @Binds
    fun bindsBookmarkModel(roomBookmarkRepository: RoomBookmarkRepository): BookmarkRepository

    @Binds
    fun bindsDownloadsModel(roomDownloadsRepository: RoomDownloadsRepository): DownloadsRepository

    @Binds
    fun bindsHistoryModel(roomHistoryRepository: RoomHistoryRepository): HistoryRepository

    @Binds
    fun bindsAdBlockAllowListModel(adBlockAllowListDatabase: AdBlockAllowListDatabase): AdBlockAllowListRepository

    @Binds
    fun bindsAllowListModel(sessionAllowListModel: SessionAllowListModel): AllowListModel

    @Binds
    fun bindsSslWarningPreferences(sessionSslWarningPreferences: SessionSslWarningPreferences): SslWarningPreferences

    @Binds
    fun bindsHostsDataSource(assetsHostsDataSource: AssetsHostsDataSource): HostsDataSource

    @Binds
    fun bindsHostsRepository(hostsDatabase: HostsDatabase): HostsRepository

    @Binds
    fun bindsHostsDataSourceProvider(preferencesHostsDataSourceProvider: PreferencesHostsDataSourceProvider): HostsDataSourceProvider

    @Binds
    fun bindsResourceProvider(defaultResourceProvider: DefaultResourceProvider): ResourceProvider

    @Binds
    fun bindsHostsFileUpdater(hostsFileUpdater: DefaultHostsFileUpdater): HostsFileUpdater
}
