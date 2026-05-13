package cz.nicolsburg.boardflow.data

import android.content.Context
import cz.nicolsburg.boardflow.model.GameItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object BggImageCache {

    private const val DIR = "bgg_thumbs"
    private const val MAX_CACHE_BYTES = 40L * 1024L * 1024L
    private const val MAX_CACHE_FILES = 500
    private const val PRELOAD_CONCURRENCY = 4

    private val http = OkHttpClient()

    private fun cacheDir(context: Context): File = File(context.filesDir, DIR).also { it.mkdirs() }

    fun localFile(context: Context, objectId: String): File = File(cacheDir(context), "$objectId.jpg")

    fun isCached(context: Context, objectId: String) = localFile(context, objectId).exists()

    fun download(context: Context, objectId: String, url: String): File? {
        val result = downloadImage(context, objectId, url)
        if (result != null) prune(context)
        return result
    }

    suspend fun preloadAll(context: Context, games: List<GameItem>) {
        val semaphore = Semaphore(PRELOAD_CONCURRENCY)
        coroutineScope {
            for (game in games) {
                val url = game.thumbnailUrl?.takeIf { it.isNotBlank() } ?: continue
                if (game.objectId.isBlank()) continue
                launch(Dispatchers.IO) {
                    semaphore.withPermit { downloadImage(context, game.objectId, url) }
                }
            }
        }
        prune(context)
    }

    fun clearAll(context: Context) {
        cacheDir(context).listFiles()?.forEach { it.delete() }
    }

    fun prune(context: Context) {
        val files = cacheDir(context).listFiles()?.filter { it.isFile } ?: return
        var totalBytes = files.sumOf { it.length() }
        // Fast path: nothing to do
        if (files.size <= MAX_CACHE_FILES && totalBytes <= MAX_CACHE_BYTES) return
        val sorted = files.sortedBy { it.lastModified() }.toMutableList()
        while (sorted.size > MAX_CACHE_FILES || totalBytes > MAX_CACHE_BYTES) {
            val oldest = sorted.removeFirstOrNull() ?: break
            totalBytes -= oldest.length()
            oldest.delete()
        }
    }

    private fun downloadImage(context: Context, objectId: String, url: String): File? {
        val file = localFile(context, objectId)
        if (file.exists()) {
            file.setLastModified(System.currentTimeMillis())
            return file
        }
        return try {
            val response = http.newCall(Request.Builder().url(url).build()).execute()
            if (response.code != 200) return null
            val bytes = response.body?.bytes() ?: return null
            file.writeBytes(bytes)
            file
        } catch (_: Exception) { null }
    }
}
