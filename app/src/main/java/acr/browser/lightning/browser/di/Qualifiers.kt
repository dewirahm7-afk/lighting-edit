package acr.browser.lightning.browser.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IncognitoMode

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InitialIntent

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InitialUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SuggestionsClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HostsClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainHandler

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FilesDir

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DataDir

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CacheDir
