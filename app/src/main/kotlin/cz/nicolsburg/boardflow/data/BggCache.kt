package cz.nicolsburg.boardflow.data

import android.content.Context
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.io.File

class BggCache(context: Context) {

    companion object {
        private const val TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    private val filesDir = context.filesDir
    private val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    fun exists(username: String): Boolean {
        val scoped = cacheFile(username)
        val legacy = legacyCacheFile()
        val file = when {
            scoped.exists() -> scoped
            legacy.exists() -> legacy
            else -> return false
        }
        return try {
            val envelope = mapper.readValue(file, Envelope::class.java)
            envelope.enriched && System.currentTimeMillis() - envelope.savedAt < TTL_MS
        } catch (_: Exception) {
            false
        }
    }

    fun save(username: String, games: List<BggApiClient.BggGame>, enriched: Boolean = true) {
        mapper.writeValue(cacheFile(username), Envelope(games = games, enriched = enriched, savedAt = System.currentTimeMillis()))
    }

    fun load(username: String): List<BggApiClient.BggGame> {
        val scoped = cacheFile(username)
        val source = when {
            scoped.exists() -> scoped
            legacyCacheFile().exists() -> legacyCacheFile()
            else -> return emptyList()
        }
        return mapper.readValue(source, Envelope::class.java).games
    }

    fun delete(username: String) {
        cacheFile(username).delete()
    }

    private fun cacheFile(username: String): File {
        val safe = username.trim().lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "_")
            .ifBlank { "default" }
        return File(filesDir, "bgg_collection_$safe.json")
    }

    private fun legacyCacheFile(): File = File(filesDir, "bgg_collection.json")

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Envelope(
        val games: List<BggApiClient.BggGame> = emptyList(),
        val enriched: Boolean = false,
        val savedAt: Long = 0L
    )
}
