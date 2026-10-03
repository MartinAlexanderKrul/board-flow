package cz.nicolsburg.boardflow.data

import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.sync.withLock
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cz.nicolsburg.boardflow.model.PlayerResult
import java.time.LocalDate

class BggPlayPostWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = SecurePreferences(applicationContext)
        val store = CanonicalCollectionStore.getInstance(applicationContext)
        val repository = BggRepository()

        val creds = prefs.getCredentials() ?: return Result.success()
        val unposted = store.getLoggedPlays().filter { !it.postedToBgg }
        if (unposted.isEmpty()) return Result.success()

        repository.login(creds).onFailure { return Result.retry() }

        val roster = store.getPlayers()

        var anyFailed = false
        var postedAny = false
        for (candidate in unposted) {
            // Under the shared lock, and re-read: the app may have posted or deleted this play
            // since the list above was taken.
            val posted = PlayPostLock.mutex.withLock {
                val play = store.getLoggedPlays().firstOrNull { it.id == candidate.id } ?: return@withLock false
                if (play.postedToBgg) return@withLock false
                postOne(play, repository, store, roster)
            }
            if (posted == null) anyFailed = true else if (posted) postedAny = true
        }
        if (postedAny) PlayPostLock.notifyPostedInBackground()
        return if (anyFailed) Result.retry() else Result.success()
    }

    /** True when posted, null when BGG refused it. (The caller passes false for a skipped play.) */
    private suspend fun postOne(
        play: cz.nicolsburg.boardflow.model.LoggedPlay,
        repository: BggRepository,
        store: CanonicalCollectionStore,
        roster: List<cz.nicolsburg.boardflow.model.Player>
    ): Boolean? {
        run {
            val players = play.players.map { pr ->
                val trimmed = pr.name.trim()
                val canonical = roster.firstOrNull { p ->
                    (listOf(p.displayName) + p.aliases).any { it.trim().equals(trimmed, ignoreCase = true) }
                }
                if (canonical != null) pr.copy(name = canonical.displayName) else pr.copy(name = trimmed)
            }
            val usernameMap = buildUsernameMap(players, roster)
            repository.logPlay(
                gameId = play.gameId,
                date = LocalDate.parse(play.date),
                players = players,
                playerBggUsernames = usernameMap,
                durationMinutes = play.durationMinutes,
                location = play.location,
                comments = play.comments,
                quantity = play.quantity,
                incomplete = play.incomplete,
                nowInStats = play.nowInStats
            ).onSuccess { savedPlayId ->
                val posted = play.copy(
                    id = savedPlayId ?: play.id,
                    players = players,
                    postedToBgg = true
                )
                store.saveLoggedPlay(posted)
                if (posted.id != play.id) {
                    // Moods and the quote are keyed by play id: carry them to the BGG id.
                    play.memory?.let { store.savePlayMemory(posted.id, it) }
                    store.deleteLoggedPlay(play.id)
                    PlayPostLock.promotedIds[play.id] = posted.id
                }
                Log.i(TAG, "Posted play ${play.id} -> ${posted.id}")
                return true
            }.onFailure {
                Log.w(TAG, "Failed to post play ${play.id}: ${it.message}")
            }
            return null
        }
    }

    private fun buildUsernameMap(players: List<PlayerResult>, roster: List<cz.nicolsburg.boardflow.model.Player>): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        players.forEachIndexed { index, pr ->
            val match = roster.firstOrNull { p ->
                (listOf(p.displayName) + p.aliases).any { it.trim().equals(pr.name.trim(), ignoreCase = true) }
            }
            if (match != null && match.bggUsername.isNotBlank()) result[index] = match.bggUsername
        }
        return result
    }

    companion object {
        private const val TAG = "BggPlayPostWorker"
        private const val WORK_NAME = "bgg_post_unposted"

        /**
         * Posts every unposted local play once the device is online. Queued at app start and
         * whenever a play is saved without reaching BGG; a run already waiting is kept.
         */
        fun enqueue(context: Context) {
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<BggPlayPostWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            )
        }
    }
}
