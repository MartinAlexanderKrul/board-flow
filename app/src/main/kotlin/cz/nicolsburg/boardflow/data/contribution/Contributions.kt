package cz.nicolsburg.boardflow.data.contribution

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cz.nicolsburg.boardflow.BuildConfig
import cz.nicolsburg.boardflow.data.scoresheet.ScoreSheetJson
import cz.nicolsburg.boardflow.model.GuideOrigin
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.SetupGuide
import cz.nicolsburg.boardflow.model.SheetOrigin
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * What goes to the contribution relay (`relay/` in the repo), which opens a pull request with it.
 * Only the guide or sheet itself is sent, stripped of anything about the user: no author, no
 * players, no account. Pure, so it is unit-tested.
 */
object ContributionPayload {
    const val KIND_SCORE_SHEET = "score-sheet"
    const val KIND_SETUP_GUIDE = "setup-guide"

    /** A reviewed sheet as the catalog will hold it: id `community-<gameId>`, shown as shared. */
    fun scoreSheet(sheet: ScoreSheet, appVersion: String): JSONObject {
        val gameId = sheet.gameIds.first()
        val shared = sheet.copy(id = "community-$gameId", origin = SheetOrigin.COMMUNITY, reviewed = true)
        return envelope(KIND_SCORE_SHEET, gameId, sheet.gameName, sheet.gameIds, ScoreSheetJson.write(shared), appVersion)
    }

    /** A reviewed guide as a community guide; the author is left out (the catalog's guides carry none). */
    fun setupGuide(guide: SetupGuide, appVersion: String): JSONObject {
        val shared = guide.copy(provenance = guide.provenance.copy(origin = GuideOrigin.COMMUNITY, author = null, reviewed = true))
        return envelope(
            KIND_SETUP_GUIDE, guide.gameId, guide.gameName, guide.allGameIds,
            JSONObject(SetupGuideJson.toJsonString(shared)), appVersion
        )
    }

    private fun envelope(kind: String, gameId: Int, gameName: String, gameIds: Set<Int>, document: JSONObject, appVersion: String) =
        JSONObject().apply {
            put("kind", kind)
            put("gameId", gameId)
            put("gameName", gameName)
            put("gameIds", JSONArray(gameIds.sorted()))
            put("document", document)
            put("appVersion", appVersion)
        }

    /** A stable fingerprint of what is shared, so the same content is not sent twice. */
    fun fingerprint(payload: JSONObject): String {
        val doc = payload.getJSONObject("document").toString()
        val bytes = MessageDigest.getInstance("SHA-256").digest("${payload.getString("kind")}:$doc".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Shares the user's reviewed guides and score sheets with every BoardFlow user, with no button:
 * marking a draft as reviewed (or saving a guide in the editor) queues [ContributionWorker], which
 * sends it to the relay once online; the relay opens a pull request that the maintainer reviews,
 * and once merged the catalog delivers it to everyone. Off when the build has no relay URL.
 */
class ContributionService(
    private val context: Context,
    private val relayUrl: String = BuildConfig.CONTRIBUTION_RELAY_URL
) {
    val enabled: Boolean get() = relayUrl.isNotBlank()

    fun shareScoreSheet(sheet: ScoreSheet) {
        if (sheet.gameIds.isEmpty()) return
        enqueue(ContributionPayload.scoreSheet(sheet, BuildConfig.VERSION_NAME))
    }

    fun shareSetupGuide(guide: SetupGuide) = enqueue(ContributionPayload.setupGuide(guide, BuildConfig.VERSION_NAME))

    private fun enqueue(payload: JSONObject) {
        if (!enabled) return
        val key = "${payload.getString("kind")}-${payload.getInt("gameId")}"
        val fingerprint = ContributionPayload.fingerprint(payload)
        if (prefs().getString(key, null) == fingerprint) return
        // The payload goes in a file: work input is capped at 10 KB and guides are larger.
        val file = File(context.filesDir, "contributions/$key.json").apply {
            parentFile?.mkdirs()
            writeText(payload.toString())
        }
        val request = OneTimeWorkRequestBuilder<ContributionWorker>()
            .setInputData(workDataOf(ContributionWorker.KEY_FILE to file.absolutePath, ContributionWorker.KEY_ID to key))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(ContributionWorker.TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("contribution_$key", ExistingWorkPolicy.REPLACE, request)
    }

    /** Sends one payload; true when the relay took it (or refused it for good), false to try again later. */
    internal suspend fun send(key: String, payload: String): Boolean = withContext(Dispatchers.IO) {
        val response = runCatching {
            client.newCall(
                Request.Builder().url(relayUrl).post(payload.toRequestBody("application/json".toMediaType())).build()
            ).execute().use { it.code to it.body?.string().orEmpty() }
        }.onFailure { Log.w(TAG, "Relay unreachable: ${it.message}") }.getOrNull() ?: return@withContext false
        val (code, body) = response
        Log.d(TAG, "Relay $key -> $code $body")
        when {
            code in 200..299 -> {
                prefs().edit().putString(key, ContributionPayload.fingerprint(JSONObject(payload))).apply()
                true
            }
            // Rate limited or the relay is down: later. Anything else it refused will not change by retrying.
            code == 429 || code >= 500 -> false
            else -> true
        }
    }

    private fun prefs() = context.getSharedPreferences("contributions", Context.MODE_PRIVATE)

    private val client by lazy {
        OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    }

    private companion object {
        const val TAG = "Contributions"
    }
}

/** Sends a queued contribution to the relay; retried with backoff while offline or rate limited. */
class ContributionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val file = inputData.getString(KEY_FILE)?.let(::File)?.takeIf { it.exists() } ?: return Result.success()
        val key = inputData.getString(KEY_ID).orEmpty()
        val done = cz.nicolsburg.boardflow.core.di.AppContainer.get(applicationContext).contributionService.send(key, file.readText())
        if (!done) return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        file.delete()
        return Result.success()
    }

    companion object {
        const val TAG = "contribution"
        const val KEY_FILE = "file"
        const val KEY_ID = "id"
        private const val MAX_ATTEMPTS = 8
    }
}
