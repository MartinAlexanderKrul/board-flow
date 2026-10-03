package cz.nicolsburg.boardflow.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared by every path that posts a local play to BGG: the app's own posts (Log Play, History's
 * Post / Post all) and [BggPlayPostWorker]. One post runs at a time, and each poster re-reads the
 * play under [mutex] first, so a play already posted or deleted by the other path is skipped and
 * never reaches BGG twice.
 */
object PlayPostLock {
    val mutex = Mutex()

    /** Local play id -> the BGG id it was promoted to, for callers still holding the local copy. */
    val promotedIds = ConcurrentHashMap<String, String>()

    private val _postedInBackground = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fires after [BggPlayPostWorker] posted plays, so an open app reloads its history. */
    val postedInBackground: SharedFlow<Unit> = _postedInBackground.asSharedFlow()

    fun notifyPostedInBackground() {
        _postedInBackground.tryEmit(Unit)
    }
}
