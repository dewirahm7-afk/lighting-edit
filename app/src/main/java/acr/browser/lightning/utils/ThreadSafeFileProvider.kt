package acr.browser.lightning.utils

import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.concurrency.CoroutineDispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.io.File

class ThreadSafeFileProvider constructor(
    appCoroutineScope: AppCoroutineScope,
    coroutineDispatchers: CoroutineDispatchers,
    private val fileProducer: () -> File
) {

    interface Factory {
        fun create(fileProducer: () -> File): ThreadSafeFileProvider
    }

    val file: Deferred<File> = appCoroutineScope.async(coroutineDispatchers.io) {
        fileProducer()
    }
}
