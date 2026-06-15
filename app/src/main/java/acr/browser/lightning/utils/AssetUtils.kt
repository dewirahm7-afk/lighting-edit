package acr.browser.lightning.utils

import android.content.res.AssetManager
import java.io.IOException

object AssetUtils {
    fun readAssetToString(assetManager: AssetManager, fileName: String): String {
        return try {
            assetManager.open(fileName).use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: IOException) {
            ""
        }
    }
}
