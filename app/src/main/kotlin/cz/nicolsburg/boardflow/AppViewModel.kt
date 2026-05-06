package cz.nicolsburg.boardflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.nicolsburg.boardflow.BuildConfig
import cz.nicolsburg.boardflow.core.di.AppContainer
import cz.nicolsburg.boardflow.model.BggGame
import cz.nicolsburg.boardflow.model.ExtractedPlay
import cz.nicolsburg.boardflow.model.GameItem
import cz.nicolsburg.boardflow.model.GameRelations
import cz.nicolsburg.boardflow.model.LogPlayPrefill
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.model.PlayerResult
import cz.nicolsburg.boardflow.model.RecordMoment
import cz.nicolsburg.boardflow.model.SessionContext
import cz.nicolsburg.boardflow.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDate
import java.util.UUID

class AppViewModel(private val container: AppContainer) : ViewModel() {
    companion object {
        private const val CANONICAL_SNAPSHOT_ID = "__canonical_collection__"

        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>) = AppViewModel(container) as T
        }
    }

    val prefs get() = container.securePreferences

    // --- Theme ---
    private val _appTheme = MutableStateFlow(
        try { AppTheme.valueOf(container.securePreferences.appTheme) }
        catch (_: Exception) { AppTheme.DARK }
    )
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    fun setAppTheme(theme: AppTheme) { _appTheme.value = theme; prefs.appTheme = theme.name }

    // --- Settings save callback ---
    var settingsSaveCallback: (() -> Unit)? = null

    // --- Network ---
    fun isOnline(): Boolean = container.isOnline()

    // --- Game search ---
    private val _recentGames = MutableStateFlow<List<BggGame>>(emptyList())
    private val _allGames = MutableStateFlow<List<BggGame>>(emptyList())
    val collection: StateFlow<List<BggGame>> = _allGames.asStateFlow()
    private val _searchResults = MutableStateFlow<List<BggGame>>(emptyList())
    val searchResults: StateFlow<List<BggGame>> = _searchResults.asStateFlow()
    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()
    private val _collectionLoaded = MutableStateFlow(false)
    val collectionLoaded: StateFlow<Boolean> = _collectionLoaded.asStateFlow()
    private val _logPlayHasUnsavedChanges = MutableStateFlow(false)
    val logPlayHasUnsavedChanges: StateFlow<Boolean> = _logPlayHasUnsavedChanges.asStateFlow()

    fun loadRecentGames() {
        _recentGames.value = prefs.getRecentGames()
        if (_allGames.value.isNotEmpty()) return
        viewModelScope.launch {
            val cachedCollection = container.canonicalCollectionStore.getLightweightGames()
            if (cachedCollection.isNotEmpty()) {
                _allGames.value = cachedCollection
                _searchResults.value = cachedCollection
                _collectionLoaded.value = true
            } else {
                _searchResults.value = _recentGames.value
            }
        }
    }

    fun loadCollection() {
        val username = prefs.bggUsername
        if (username.isBlank()) { _searchError.value = "Please set your BGG username in Settings first"; return }
        viewModelScope.launch {
            _searchLoading.value = true; _searchError.value = null
            val creds = prefs.getCredentials()
            val result = if (creds != null) container.bggRepository.getUserCollectionAuthenticated(creds)
                         else container.bggRepository.getUserCollection(username)
            result.onSuccess { games ->
                _allGames.value = games.sortedBy { it.name }
                _searchResults.value = _allGames.value
                _collectionLoaded.value = true
            }.onFailure { _searchError.value = it.message; _collectionLoaded.value = false }
            _searchLoading.value = false
        }
    }

    fun updateFromCollection(games: List<GameItem>) {
        if (games.isEmpty()) return
        val bggGames = games.mapNotNull { item ->
            val id = item.objectId.toIntOrNull() ?: return@mapNotNull null
            BggGame(
                id = id,
                name = item.identity.name,
                yearPublished = item.yearPublished?.toString(),
                thumbnailUrl = item.thumbnailUrl
            )
        }.sortedBy { it.name }
        if (bggGames.isEmpty()) return
        _allGames.value = bggGames
        _searchResults.value = bggGames
        _collectionLoaded.value = true
    }

    fun filterGames(query: String) {
        if (query.isBlank()) {
            _searchError.value = null
            _searchResults.value = if (_collectionLoaded.value) _allGames.value else _recentGames.value
            return
        }

        if (_collectionLoaded.value) {
            val localMatches = _allGames.value.filter { it.name.contains(query, ignoreCase = true) }
            if (localMatches.isNotEmpty()) {
                _searchError.value = null
                _searchResults.value = localMatches
            } else {
                _searchResults.value = emptyList()
                searchGames(query)
            }
            return
        }

        _searchResults.value = emptyList()
        searchGames(query)
    }

    fun searchGames(query: String) {
        if (query.isBlank()) { _searchResults.value = if (_collectionLoaded.value) _allGames.value else _recentGames.value; return }
        viewModelScope.launch {
            _searchLoading.value = true; _searchError.value = null
            container.bggRepository.searchGames(query, BuildConfig.BGG_XML_API_TOKEN)
                .onSuccess { _searchResults.value = it }
                .onFailure { _searchError.value = it.message }
            _searchLoading.value = false
        }
    }

    fun selectGame(game: BggGame) {
        selectedGame = game
        _logPlayHasUnsavedChanges.value = false
        prefs.addRecentGame(game)
        _recentGames.value = prefs.getRecentGames()
        _extractedPlay.value = null
        _additionalGames.value = emptyList()
        _gameRelations.value = findRelatedGames(game, _allGames.value)

        val pending = _changeGameSession?.takeIf { it.isActive() }
        _changeGameSession = null
        _changeGameSessionActive.value = false
        when {
            pending != null -> {
                _editablePlayers.value = pending.players.map { it.copy(score = "0", isWinner = false) }
                _logPlayPrefill = LogPlayPrefill(location = pending.location)
            }
            _sessionContext.value?.isRecent() == true && _sessionContext.value?.gameId == game.id -> {
                val session = _sessionContext.value!!
                _editablePlayers.value = session.players.map { it.copy(score = "0", isWinner = false) }
                _logPlayPrefill = LogPlayPrefill(location = session.location)
            }
            else -> _editablePlayers.value = emptyList()
        }
    }

    // --- Game relations ---
    private val _gameRelations = MutableStateFlow<GameRelations?>(null)
    val gameRelations: StateFlow<GameRelations?> = _gameRelations.asStateFlow()

    // --- Additional games ---
    private val _additionalGames = MutableStateFlow<List<BggGame>>(emptyList())
    val additionalGames: StateFlow<List<BggGame>> = _additionalGames.asStateFlow()

    fun toggleAdditionalGame(game: BggGame) {
        val current = _additionalGames.value
        _additionalGames.value = if (current.any { it.id == game.id }) current.filter { it.id != game.id } else current + game
    }

    // --- Scan / extraction ---
    private val _extractedPlay = MutableStateFlow<ExtractedPlay?>(null)
    val extractedPlay: StateFlow<ExtractedPlay?> = _extractedPlay.asStateFlow()
    private val _scanLoading = MutableStateFlow(false)
    val scanLoading: StateFlow<Boolean> = _scanLoading.asStateFlow()
    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    var selectedGame: BggGame? = null

    fun extractScores(imageFile: File) {
        viewModelScope.launch {
            _scanLoading.value = true; _scanError.value = null; _extractedPlay.value = null
            container.geminiRepo.extractScoresFromImage(
                imageFile = imageFile, apiKey = prefs.geminiApiKey,
                modelName = prefs.geminiModelEndpoint, availableModels = prefs.getAvailableModels(),
                onModelChanged = { newModel -> prefs.geminiModelEndpoint = newModel }
            ).onSuccess { _extractedPlay.value = it }
             .onFailure { _scanError.value = it.message }
            _scanLoading.value = false
        }
    }

    fun checkAvailableModels(onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            container.geminiRepo.listAvailableModels(prefs.geminiApiKey)
                .onSuccess { models -> prefs.saveAvailableModels(models); onResult(models) }
                .onFailure { onResult(emptyList()) }
        }
    }

    // --- Review / edit ---
    private val _editablePlayers = MutableStateFlow<List<PlayerResult>>(emptyList())
    val editablePlayers: StateFlow<List<PlayerResult>> = _editablePlayers.asStateFlow()

    fun initEditablePlayers(players: List<PlayerResult>) { _editablePlayers.value = players.toMutableList() }
    fun updatePlayer(index: Int, updated: PlayerResult) { _editablePlayers.value = _editablePlayers.value.toMutableList().also { it[index] = updated } }
    fun addPlayer() { _editablePlayers.value = _editablePlayers.value + PlayerResult("", "0", false) }
    fun removePlayer(index: Int) { _editablePlayers.value = _editablePlayers.value.toMutableList().also { it.removeAt(index) } }

    // --- Player roster ---
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    fun loadPlayers() { _players.value = prefs.getPlayers() }

    fun getPlayerSuggestions(input: String): List<Player> {
        if (input.length < 2) return emptyList()
        val lower = input.lowercase().trim(); val threshold = maxOf(2, lower.length / 3)
        return _players.value
            .filter { p -> (listOf(p.displayName) + p.aliases).any { levenshtein(lower, it.lowercase()) <= threshold } }
            .sortedBy { p -> (listOf(p.displayName) + p.aliases).minOf { levenshtein(lower, it.lowercase()) } }
            .take(5)
    }

    fun recordPlayerName(name: String) {
        if (name.isBlank()) return
        val lower = name.lowercase().trim(); val threshold = maxOf(2, lower.length / 3)
        val list = _players.value.toMutableList()
        val match = list.firstOrNull { p -> (listOf(p.displayName) + p.aliases).any { levenshtein(lower, it.lowercase()) <= threshold } }
        if (match != null) {
            val alreadyKnown = (listOf(match.displayName) + match.aliases).any { it.lowercase() == lower }
            if (!alreadyKnown) {
                val idx = list.indexOfFirst { it.id == match.id }
                list[idx] = match.copy(aliases = match.aliases + name.trim())
                _players.value = list; prefs.savePlayers(list)
            }
        } else {
            list.add(Player(UUID.randomUUID().toString(), name.trim(), emptyList()))
            _players.value = list; prefs.savePlayers(list)
        }
    }

    fun addNewPlayer(displayName: String) {
        if (displayName.isBlank()) return
        val list = (_players.value + Player(UUID.randomUUID().toString(), displayName.trim(), emptyList())).sortedBy { it.displayName.lowercase() }
        _players.value = list; prefs.savePlayers(list)
    }

    fun updatePlayerDisplayName(id: String, displayName: String) {
        if (displayName.isBlank()) return
        val list = _players.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) list[idx] = list[idx].copy(displayName = displayName.trim())
        _players.value = list.sortedBy { it.displayName.lowercase() }
        prefs.savePlayers(_players.value)
    }

    fun addPlayerAlias(id: String, alias: String) {
        val trimmed = alias.trim(); if (trimmed.isEmpty()) return
        val currentList = _players.value; val idx = currentList.indexOfFirst { it.id == id }; if (idx < 0) return
        val player = currentList[idx]
        if (player.aliases.any { it.lowercase() == trimmed.lowercase() }) return
        val newList = currentList.toMutableList(); newList[idx] = player.copy(aliases = player.aliases + trimmed)
        _players.value = newList.toList(); prefs.savePlayers(_players.value)
    }

    fun removePlayerAlias(id: String, alias: String) {
        val currentList = _players.value; val idx = currentList.indexOfFirst { it.id == id }; if (idx < 0) return
        val player = currentList[idx]
        val newList = currentList.toMutableList(); newList[idx] = player.copy(aliases = player.aliases.filter { it != alias })
        _players.value = newList.toList(); prefs.savePlayers(_players.value)
    }

    fun updatePlayerBggUsername(id: String, bggUsername: String) {
        val list = _players.value.toMutableList(); val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) list[idx] = list[idx].copy(bggUsername = bggUsername.trim())
        _players.value = list.toList(); prefs.savePlayers(_players.value)
    }

    fun deletePlayer(id: String) { _players.value = _players.value.filter { it.id != id }; prefs.savePlayers(_players.value) }

    // --- Play history (local) ---
    private val _playHistory = MutableStateFlow<List<LoggedPlay>>(emptyList())
    val playHistory: StateFlow<List<LoggedPlay>> = _playHistory.asStateFlow()
    private val _bggPlaysCacheAgeMinutes = MutableStateFlow(Long.MAX_VALUE)

    fun loadPlayHistory() {
        viewModelScope.launch {
            _playHistory.value = container.canonicalCollectionStore.getLoggedPlays()
        }
    }
    fun clearPlayHistory() {
        viewModelScope.launch {
            container.canonicalCollectionStore.clearLoggedPlays()
            prefs.clearLegacyLoggedPlayArtifacts()
            _playHistory.value = emptyList()
        }
    }

    // --- Play history (from BGG) ---
    private val _bggPlays = MutableStateFlow<List<LoggedPlay>>(emptyList())
    val bggPlays: StateFlow<List<LoggedPlay>> = _bggPlays.asStateFlow()
    private val _bggPlaysLoading = MutableStateFlow(false)
    val bggPlaysLoading: StateFlow<Boolean> = _bggPlaysLoading.asStateFlow()
    private val _bggPlaysError = MutableStateFlow<String?>(null)
    val bggPlaysError: StateFlow<String?> = _bggPlaysError.asStateFlow()
    private val _deletingBggPlayId = MutableStateFlow<String?>(null)
    val deletingBggPlayId: StateFlow<String?> = _deletingBggPlayId.asStateFlow()
    val historyPlays: StateFlow<List<LoggedPlay>> = combine(_playHistory, _bggPlays) { local, remote ->
        mergeHistorySources(local, remote)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun fetchBggPlays() {
        viewModelScope.launch {
            _bggPlaysLoading.value = true; _bggPlaysError.value = null
            cz.nicolsburg.boardflow.data.refreshBggPlayCache(prefs, container.canonicalCollectionStore, container.bggRepository)
                .onSuccess {
                    _bggPlays.value = mergeBggPlayLists(it)
                    reconcilePendingLocalPlays(_bggPlays.value)
                    pruneLocalPlaysDeletedOnBgg(it)
                    _bggPlaysCacheAgeMinutes.value = container.canonicalCollectionStore.getBggPlaysCacheAgeMinutes()
                }
                .onFailure { _bggPlaysError.value = it.message }
            _bggPlaysLoading.value = false
        }
    }

    fun loadCachedBggPlays() {
        viewModelScope.launch {
            val cached = container.canonicalCollectionStore.getBggPlaysCache()
            _bggPlaysCacheAgeMinutes.value = container.canonicalCollectionStore.getBggPlaysCacheAgeMinutes()
            if (cached.isNotEmpty()) {
                _bggPlays.value = mergeBggPlayLists(_bggPlays.value, cached)
                reconcilePendingLocalPlays(_bggPlays.value)
            }
        }
    }
    fun isBggPlaysCacheStale(): Boolean = _bggPlaysCacheAgeMinutes.value > 4 * 60
    fun bggPlaysCacheAgeLabel(): String {
        val minutes = _bggPlaysCacheAgeMinutes.value
        return when { minutes == Long.MAX_VALUE -> ""; minutes < 60 -> "updated ${minutes}m ago"; else -> "updated ${minutes / 60}h ago" }
    }

    private fun addOptimisticBggPlays(plays: List<LoggedPlay>) {
        if (plays.isEmpty()) return
        viewModelScope.launch {
            _bggPlays.value = mergeBggPlayLists(plays, _bggPlays.value)
            container.canonicalCollectionStore.saveBggPlaysCache(_bggPlays.value)
            reconcilePendingLocalPlays(_bggPlays.value)
            _bggPlaysCacheAgeMinutes.value = 0L
        }
    }

    private suspend fun reconcilePendingLocalPlays(remote: List<LoggedPlay>) {
        if (remote.isEmpty()) return
        val remoteSignatures = remote.mapTo(mutableSetOf()) { it.signatureKey() }
        val local = container.canonicalCollectionStore.getLoggedPlays()
        val matchingPendingIds = local
            .filter { !it.postedToBgg && it.signatureKey() in remoteSignatures }
            .map { it.id }
        if (matchingPendingIds.isEmpty()) return
        matchingPendingIds.forEach { playId ->
            container.canonicalCollectionStore.updateLoggedPlay(playId) { it.copy(postedToBgg = true) }
        }
        _playHistory.value = container.canonicalCollectionStore.getLoggedPlays()
    }

    // After a full BGG play refresh, remove local plays that carry a real BGG ID but no
    // longer exist on BGG — they were deleted externally (e.g. via the BGG website).
    private suspend fun pruneLocalPlaysDeletedOnBgg(freshBggPlays: List<LoggedPlay>) {
        val bggIds = freshBggPlays.mapTo(hashSetOf()) { it.id }
        val local = container.canonicalCollectionStore.getLoggedPlays()
        val pruned = local.filter { play ->
            play.postedToBgg && !play.id.isLikelyLocalUuid() && play.id !in bggIds
        }
        if (pruned.isEmpty()) return
        pruned.forEach { container.canonicalCollectionStore.deleteLoggedPlay(it.id) }
        _playHistory.value = container.canonicalCollectionStore.getLoggedPlays()
    }

    fun deleteBggPlay(playId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        if (!isOnline()) { onError("Go online to delete plays from BGG"); return }
        val creds = prefs.getCredentials() ?: run { onError("BGG credentials not set"); return }
        val username = prefs.bggUsername.trim()
        if (username.isBlank()) { onError("BGG username not set"); return }
        _deletingBggPlayId.value = playId
        viewModelScope.launch {
            val deletedPlay = (_bggPlays.value + container.canonicalCollectionStore.getBggPlaysCache())
                .firstOrNull { it.id == playId }
            container.bggRepository.login(creds)
                .onFailure {
                    _deletingBggPlayId.value = null
                    onError(it.message ?: "Login failed")
                    return@launch
                }
            val deleteResult = container.bggRepository.deletePlay(playId)

            // If delete "failed" with an unexpected-response error, the play may have already
            // been deleted on BGG externally. Verify by re-fetching and treat as success if gone.
            if (deleteResult.isFailure) {
                val mightBeAlreadyGone = deleteResult.exceptionOrNull()?.message
                    ?.contains("Unexpected BGG confirm-delete") == true
                if (mightBeAlreadyGone) {
                    container.bggRepository.getPlays(username)
                        .onSuccess { refreshed ->
                            if (!refreshed.any { it.id == playId }) {
                                _bggPlays.value = refreshed
                                container.canonicalCollectionStore.saveBggPlaysCache(refreshed)
                                removeLocalCopyOfDeletedBggPlay(playId, deletedPlay)
                                pruneLocalPlaysDeletedOnBgg(refreshed)
                                _bggPlaysCacheAgeMinutes.value = 0L
                                _deletingBggPlayId.value = null
                                onSuccess()
                                return@launch
                            }
                        }
                }
                _deletingBggPlayId.value = null
                onError(deleteResult.exceptionOrNull()?.message ?: "Failed to delete play")
                return@launch
            }

            // Delete was accepted — verify BGG confirms it's gone, then clean up locally.
            container.bggRepository.getPlays(username)
                .onSuccess { refreshed ->
                    if (refreshed.any { it.id == playId }) {
                        _deletingBggPlayId.value = null
                        onError("BGG did not confirm the delete yet. Please refresh and try again.")
                    } else {
                        _bggPlays.value = refreshed
                        container.canonicalCollectionStore.saveBggPlaysCache(refreshed)
                        removeLocalCopyOfDeletedBggPlay(playId, deletedPlay)
                        _bggPlaysCacheAgeMinutes.value = 0L
                        _deletingBggPlayId.value = null
                        onSuccess()
                    }
                }
                .onFailure { error ->
                    _deletingBggPlayId.value = null
                    onError(error.message ?: "Deleted on BGG, but failed to refresh history")
                }
        }
    }

    private suspend fun removeLocalCopyOfDeletedBggPlay(playId: String, deletedPlay: LoggedPlay?) {
        val deletedSignature = deletedPlay?.signatureKey()
        container.canonicalCollectionStore.getLoggedPlays()
            .filter { localPlay ->
                localPlay.id == playId || (deletedSignature != null && localPlay.signatureKey() == deletedSignature)
            }
            .forEach { container.canonicalCollectionStore.deleteLoggedPlay(it.id) }
        _playHistory.value = container.canonicalCollectionStore.getLoggedPlays()
    }

    // --- Post to BGG ---
    private val _postLoading = MutableStateFlow(false)
    val postLoading: StateFlow<Boolean> = _postLoading.asStateFlow()
    private val _postResult = MutableStateFlow<String?>(null)
    val postResult: StateFlow<String?> = _postResult.asStateFlow()

    fun postPlay(date: LocalDate, durationMinutes: Int, location: String, comments: String, quantity: Int = 1, incomplete: Boolean = false, nowInStats: Boolean = true, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val game = selectedGame ?: run { onError("No game selected"); return }
        val normalizedPlayers = normalizePlayersForPosting(_editablePlayers.value)
        if (!isOnline()) {
            val playersSnapshot = normalizedPlayers
            playersSnapshot.forEach { recordPlayerName(it.name) }
            val mainPlay = LoggedPlay(id = UUID.randomUUID().toString(), gameId = game.id, gameName = game.name, date = date.toString(), players = playersSnapshot, durationMinutes = durationMinutes, location = location, postedToBgg = false, comments = comments, quantity = quantity, incomplete = incomplete, nowInStats = nowInStats)
            viewModelScope.launch {
                container.canonicalCollectionStore.saveLoggedPlay(mainPlay)
                val extras = _additionalGames.value; _additionalGames.value = emptyList()
                extras.forEach { extra ->
                    container.canonicalCollectionStore.saveLoggedPlay(
                        LoggedPlay(
                            id = UUID.randomUUID().toString(),
                            gameId = extra.id,
                            gameName = extra.name,
                            date = date.toString(),
                            players = playersSnapshot,
                            durationMinutes = durationMinutes,
                            location = location,
                            postedToBgg = false,
                            comments = comments,
                            quantity = quantity,
                            incomplete = incomplete,
                            nowInStats = nowInStats
                        )
                    )
                }
                prefs.addRecentGame(game)
                _playHistory.value = container.canonicalCollectionStore.getLoggedPlays()
                _logPlayHasUnsavedChanges.value = false
                onSuccess()
            }
            return
        }
        val creds = prefs.getCredentials() ?: run { onError("BGG credentials not set"); return }
        viewModelScope.launch {
            _postLoading.value = true
            container.bggRepository.login(creds).onFailure { _postLoading.value = false; onError(it.message ?: "Login failed"); return@launch }
            val playerBggUsernames = buildBggUsernameMap(normalizedPlayers)
            container.bggRepository.logPlay(gameId = game.id, date = date, players = normalizedPlayers, playerBggUsernames = playerBggUsernames, durationMinutes = durationMinutes, location = location, comments = comments, quantity = quantity, incomplete = incomplete, nowInStats = nowInStats)
                .onSuccess { savedPlayId ->
                    normalizedPlayers.forEach { recordPlayerName(it.name) }
                    val postedPlays = mutableListOf<LoggedPlay>()
                    val locallySavedExtras = mutableListOf<LoggedPlay>()
                    val mainPlay = LoggedPlay(
                        id = savedPlayId ?: UUID.randomUUID().toString(),
                        gameId = game.id,
                        gameName = game.name,
                        date = date.toString(),
                        players = normalizedPlayers,
                        durationMinutes = durationMinutes,
                        location = location,
                        postedToBgg = true,
                        comments = comments,
                        quantity = quantity,
                        incomplete = incomplete,
                        nowInStats = nowInStats
                    )
                    container.canonicalCollectionStore.saveLoggedPlay(mainPlay)
                    postedPlays += mainPlay
                    val extras = _additionalGames.value
                    extras.forEach { extra ->
                        container.bggRepository.logPlay(gameId = extra.id, date = date, players = normalizedPlayers, playerBggUsernames = playerBggUsernames, durationMinutes = durationMinutes, location = location, comments = comments, quantity = quantity, incomplete = incomplete, nowInStats = nowInStats)
                            .onSuccess { extraPlayId ->
                                val extraPlay = LoggedPlay(
                                    id = extraPlayId ?: UUID.randomUUID().toString(),
                                    gameId = extra.id,
                                    gameName = extra.name,
                                    date = date.toString(),
                                    players = normalizedPlayers,
                                    durationMinutes = durationMinutes,
                                    location = location,
                                    postedToBgg = true,
                                    comments = comments,
                                    quantity = quantity,
                                    incomplete = incomplete,
                                    nowInStats = nowInStats
                                )
                                container.canonicalCollectionStore.saveLoggedPlay(extraPlay)
                                postedPlays += extraPlay
                            }
                            .onFailure {
                                val localExtraPlay = LoggedPlay(
                                    id = UUID.randomUUID().toString(),
                                    gameId = extra.id,
                                    gameName = extra.name,
                                    date = date.toString(),
                                    players = normalizedPlayers,
                                    durationMinutes = durationMinutes,
                                    location = location,
                                    postedToBgg = false,
                                    comments = comments,
                                    quantity = quantity,
                                    incomplete = incomplete,
                                    nowInStats = nowInStats
                                )
                                container.canonicalCollectionStore.saveLoggedPlay(localExtraPlay)
                                locallySavedExtras += localExtraPlay
                            }
                    }
                    _additionalGames.value = emptyList()
                    prefs.addRecentGame(game)
                    _playHistory.value = container.canonicalCollectionStore.getLoggedPlays()
                    addOptimisticBggPlays(postedPlays)
                    if (locallySavedExtras.isNotEmpty()) {
                        _postResult.value = "Logged main play. Saved ${locallySavedExtras.size} extra game(s) locally for later BGG sync."
                    }
                    _logPlayHasUnsavedChanges.value = false
                    _postLoading.value = false
                    onSuccess()
                }.onFailure { _postLoading.value = false; onError(it.message ?: "Failed to log play") }
        }
    }

    private fun buildBggUsernameMap(players: List<PlayerResult>): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        players.forEachIndexed { index, pr ->
            val match = resolveRosterPlayer(pr.name) ?: return@forEachIndexed
            if (match.bggUsername.isNotBlank()) result[index] = match.bggUsername
        }
        return result
    }

    // --- Edit existing play ---
    private val _editPlayLoading = MutableStateFlow(false)
    val editPlayLoading: StateFlow<Boolean> = _editPlayLoading.asStateFlow()

    fun editPlay(
        play: LoggedPlay,
        date: String,
        durationMinutes: Int,
        location: String,
        comments: String,
        players: List<PlayerResult>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _editPlayLoading.value = true
            try {
                val normalizedPlayers = normalizePlayersForPosting(players)
                val updatedPlay = play.copy(
                    date = date,
                    durationMinutes = durationMinutes,
                    location = location,
                    comments = comments,
                    players = normalizedPlayers
                )
                if (play.postedToBgg) {
                    if (!isOnline()) { onError("No internet connection"); return@launch }
                    val creds = prefs.getCredentials() ?: run { onError("BGG credentials not set"); return@launch }
                    container.bggRepository.login(creds).getOrThrow()
                    val bggPlayId = resolveBggPlayIdForEdit(play) ?: run {
                        onError("Refresh BGG history before editing this posted play.")
                        return@launch
                    }
                    val savedPlayId = container.bggRepository.logPlay(
                        gameId = play.gameId,
                        date = LocalDate.parse(date),
                        players = normalizedPlayers,
                        playerBggUsernames = buildBggUsernameMap(normalizedPlayers),
                        durationMinutes = durationMinutes,
                        location = location,
                        comments = comments,
                        quantity = play.quantity,
                        incomplete = play.incomplete,
                        nowInStats = play.nowInStats,
                        playId = bggPlayId
                    ).getOrThrow()
                    updateCachedBggPlay(play, updatedPlay.copy(id = savedPlayId ?: bggPlayId, postedToBgg = true))
                }
                container.canonicalCollectionStore.updateLoggedPlay(play.id) {
                    it.copy(
                        date = date,
                        durationMinutes = durationMinutes,
                        location = location,
                        comments = comments,
                        players = normalizedPlayers
                    )
                }
                _playHistory.value = container.canonicalCollectionStore.getLoggedPlays()
                normalizedPlayers.forEach { recordPlayerName(it.name) }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update play")
            } finally {
                _editPlayLoading.value = false
            }
        }
    }

    private suspend fun resolveBggPlayIdForEdit(play: LoggedPlay): String? {
        if (!play.id.isLikelyLocalUuid()) return play.id

        val originalSignature = play.signatureKey()
        val cachedMatch = (_bggPlays.value + container.canonicalCollectionStore.getBggPlaysCache())
            .firstOrNull { !it.id.isLikelyLocalUuid() && it.signatureKey() == originalSignature }
        if (cachedMatch != null) return cachedMatch.id

        val username = prefs.bggUsername.trim()
        if (username.isBlank()) return null

        val refreshed = container.bggRepository.getPlays(username).getOrNull() ?: return null
        if (refreshed.isNotEmpty()) {
            val merged = mergeBggPlayLists(refreshed, _bggPlays.value)
            _bggPlays.value = merged
            container.canonicalCollectionStore.saveBggPlaysCache(merged)
            _bggPlaysCacheAgeMinutes.value = 0L
        }

        return refreshed.firstOrNull {
            !it.id.isLikelyLocalUuid() && it.signatureKey() == originalSignature
        }?.id
    }

    private suspend fun updateCachedBggPlay(originalPlay: LoggedPlay, updatedPlay: LoggedPlay) {
        val originalSignature = originalPlay.signatureKey()
        val cached = container.canonicalCollectionStore.getBggPlaysCache()
        val remoteWithoutOriginal = _bggPlays.value.filterNot {
            it.id == originalPlay.id || it.signatureKey() == originalSignature
        }
        val cachedWithoutOriginal = cached.filterNot {
            it.id == originalPlay.id || it.signatureKey() == originalSignature
        }
        val merged = mergeBggPlayLists(listOf(updatedPlay), remoteWithoutOriginal, cachedWithoutOriginal)
        _bggPlays.value = merged
        container.canonicalCollectionStore.saveBggPlaysCache(merged)
        _bggPlaysCacheAgeMinutes.value = 0L
    }

    // --- Export / Import ---
    fun exportData(includeSensitiveData: Boolean = false): String {
        val collection = runBlocking { container.canonicalCollectionStore.getAllGames() }
        val loggedPlays = runBlocking { container.canonicalCollectionStore.getLoggedPlays() }
        val bggPlays = runBlocking { container.canonicalCollectionStore.getBggPlaysCache() }
        return prefs.exportAll(
            includeSensitiveData = includeSensitiveData,
            collectionSnapshot = collection,
            loggedPlays = loggedPlays,
            cachedBggPlays = bggPlays
        )
    }

    // --- Sync unposted plays ---
    private val _postingPlayId = MutableStateFlow<String?>(null)
    val postingPlayId: StateFlow<String?> = _postingPlayId.asStateFlow()

    fun postSinglePlay(playId: String) {
        if (!isOnline()) return
        val creds = prefs.getCredentials() ?: return
        viewModelScope.launch {
            val play = container.canonicalCollectionStore.getLoggedPlays().firstOrNull { it.id == playId } ?: run {
                _postingPlayId.value = null
                return@launch
            }
            _postingPlayId.value = playId
            container.bggRepository.login(creds).onFailure { _postingPlayId.value = null; return@launch }
            val normalizedPlayers = normalizePlayersForPosting(play.players)
            container.bggRepository.logPlay(gameId = play.gameId, date = LocalDate.parse(play.date), players = normalizedPlayers, playerBggUsernames = buildBggUsernameMap(normalizedPlayers), durationMinutes = play.durationMinutes, location = play.location, comments = play.comments, quantity = play.quantity, incomplete = play.incomplete, nowInStats = play.nowInStats)
                .onSuccess { savedPlayId ->
                    container.canonicalCollectionStore.updateLoggedPlay(play.id) { it.copy(postedToBgg = true, players = normalizedPlayers) }
                    addOptimisticBggPlays(
                        listOf(
                            play.copy(
                                id = savedPlayId ?: play.id,
                                players = normalizedPlayers,
                                postedToBgg = true
                            )
                        )
                    )
                }
            _postingPlayId.value = null; _playHistory.value = container.canonicalCollectionStore.getLoggedPlays()
        }
    }

    fun syncUnpostedPlays() {
        if (!isOnline()) return
        val creds = prefs.getCredentials() ?: return
        viewModelScope.launch {
            val unposted = container.canonicalCollectionStore.getLoggedPlays().filter { !it.postedToBgg }
            if (unposted.isEmpty()) return@launch
            container.bggRepository.login(creds).onFailure { return@launch }
            val postedPlays = mutableListOf<LoggedPlay>()
            for (play in unposted) {
                val normalizedPlayers = normalizePlayersForPosting(play.players)
                container.bggRepository.logPlay(gameId = play.gameId, date = LocalDate.parse(play.date), players = normalizedPlayers, playerBggUsernames = buildBggUsernameMap(normalizedPlayers), durationMinutes = play.durationMinutes, location = play.location, comments = play.comments, quantity = play.quantity, incomplete = play.incomplete, nowInStats = play.nowInStats)
                    .onSuccess { savedPlayId ->
                        container.canonicalCollectionStore.updateLoggedPlay(play.id) { it.copy(postedToBgg = true, players = normalizedPlayers) }
                        postedPlays += play.copy(id = savedPlayId ?: play.id, players = normalizedPlayers, postedToBgg = true)
                    }
            }
            addOptimisticBggPlays(postedPlays)
            _playHistory.value = container.canonicalCollectionStore.getLoggedPlays()
        }
    }

    fun importData(json: String) {
        viewModelScope.launch {
            val imported = prefs.importAll(json)
            val importedCollection = imported.collectionSnapshot
            if (importedCollection.isEmpty()) {
                container.canonicalCollectionStore.clearAllGames()
                _allGames.value = emptyList()
                _searchResults.value = _recentGames.value
                _collectionLoaded.value = false
            } else {
                container.canonicalCollectionStore.replaceAllGames(importedCollection)
                val lightweightGames = container.canonicalCollectionStore.getLightweightGames()
                _allGames.value = lightweightGames
                _searchResults.value = lightweightGames
                _collectionLoaded.value = true
            }
            container.canonicalCollectionStore.replaceLoggedPlays(imported.loggedPlays)
            _playHistory.value = imported.loggedPlays
            container.canonicalCollectionStore.saveBggPlaysCache(imported.cachedBggPlays)
            _bggPlays.value = mergeBggPlayLists(imported.cachedBggPlays)
            _bggPlaysCacheAgeMinutes.value = container.canonicalCollectionStore.getBggPlaysCacheAgeMinutes()

            try {
                _appTheme.value = AppTheme.valueOf(prefs.appTheme)
            } catch (_: Exception) {
                _appTheme.value = AppTheme.DARK
            }
            loadPlayers()
            loadRecentGames()
        }
    }

    fun setExtractedPlayManual() {
        _extractedPlay.value = ExtractedPlay(players = emptyList(), rawText = "Manual entry", date = java.time.LocalDate.now().toString())
    }

    fun clearExtractedPlay() { _extractedPlay.value = null; _editablePlayers.value = emptyList(); _additionalGames.value = emptyList(); _scanError.value = null }

    fun clearLogPlayFlow() {
        selectedGame = null
        _gameRelations.value = null
        _logPlayHasUnsavedChanges.value = false
        _logPlayPrefill = null
        clearExtractedPlay()
    }

    fun setLogPlayHasUnsavedChanges(hasChanges: Boolean) {
        _logPlayHasUnsavedChanges.value = hasChanges
    }

    // --- Session context ---
    private val _sessionContext = MutableStateFlow<SessionContext?>(null)
    val sessionContext: StateFlow<SessionContext?> = _sessionContext.asStateFlow()

    private val _sessionBannerDismissed = MutableStateFlow(false)
    val sessionBannerVisible: StateFlow<Boolean> = combine(_sessionContext, _sessionBannerDismissed) { ctx, dismissed ->
        ctx != null && ctx.isActive() && !dismissed
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Consumed by selectGame to pre-populate players when user changes game mid-session.
    private var _changeGameSession: SessionContext? = null
    private val _changeGameSessionActive = MutableStateFlow(false)
    val changeGameSessionActive: StateFlow<Boolean> = _changeGameSessionActive.asStateFlow()

    // Consumed once by LogPlayScreen on first composition for location/duration prefill.
    private var _logPlayPrefill: LogPlayPrefill? = null

    // Captured before postPlay so detectRecord can compare against prior history.
    private var _historySnapshot: List<LoggedPlay>? = null

    fun loadSessionContext() {
        val ctx = prefs.loadSessionContext()
        _sessionContext.value = if (ctx != null && ctx.isActive()) ctx else null
    }

    fun saveSession(game: BggGame, players: List<PlayerResult>, location: String) {
        val ctx = SessionContext(
            gameId            = game.id,
            gameName          = game.name,
            players           = players,
            location          = location,
            lastPlayTimestamp = System.currentTimeMillis()
        )
        _sessionContext.value = ctx
        prefs.saveSessionContext(ctx)
    }

    fun clearSession() {
        _sessionContext.value = null
        _sessionBannerDismissed.value = false
        _changeGameSession = null
        _changeGameSessionActive.value = false
        prefs.clearSessionContext()
    }

    fun dismissSessionBannerForSession() {
        _sessionBannerDismissed.value = true
    }

    fun takePrefill(): LogPlayPrefill? {
        val result = _logPlayPrefill
        _logPlayPrefill = null
        return result
    }

    fun setupPlayAgain(ctx: SessionContext) {
        val game = BggGame(ctx.gameId, ctx.gameName, null, null)
        selectedGame = game
        _editablePlayers.value = ctx.players.map { it.copy(score = "0", isWinner = false) }
        _extractedPlay.value = null
        _additionalGames.value = emptyList()
        _gameRelations.value = findRelatedGames(game, _allGames.value)
        _logPlayPrefill = LogPlayPrefill(location = ctx.location)
        _logPlayHasUnsavedChanges.value = false
    }

    fun setupChangeGameSession(ctx: SessionContext) {
        _changeGameSession = ctx
        _changeGameSessionActive.value = true
    }

    fun setupPlayAgainFromPlay(play: LoggedPlay) {
        val game = BggGame(play.gameId, play.gameName, null, null)
        selectedGame = game
        _editablePlayers.value = play.players.map { it.copy(score = "0", isWinner = false) }
        _extractedPlay.value = null
        _additionalGames.value = emptyList()
        _gameRelations.value = findRelatedGames(game, _allGames.value)
        _logPlayPrefill = LogPlayPrefill(location = play.location)
        _logPlayHasUnsavedChanges.value = false
    }

    fun addPlayerFromRoster(player: Player) {
        _editablePlayers.value = _editablePlayers.value + PlayerResult(player.displayName, "0", false)
    }

    fun captureHistorySnapshot() {
        _historySnapshot = historyPlays.value.toList()
    }

    fun detectRecord(gameId: Int, gameName: String, savedPlayers: List<PlayerResult>): RecordMoment? {
        val snapshot = _historySnapshot ?: return null
        val gamePlays = snapshot.filter { it.gameId == gameId }

        // Priority 1: first win (player had 0 wins in snapshot → this is their first)
        for (pr in savedPlayers) {
            if (!pr.isWinner) continue
            val lower = pr.name.trim().lowercase()
            val hadPriorWin = gamePlays.any { play ->
                play.players.any { it.name.trim().lowercase() == lower && it.isWinner }
            }
            if (!hadPriorWin) return RecordMoment.FirstWin(pr.name.trim(), gameName)
        }

        // Priority 2: new high score (score > prior max in snapshot)
        for (pr in savedPlayers) {
            val newScore = pr.score.trim().toDoubleOrNull() ?: continue
            if (newScore <= 0) continue
            val lower = pr.name.trim().lowercase()
            val priorMax = gamePlays
                .flatMap { it.players }
                .filter { it.name.trim().lowercase() == lower }
                .mapNotNull { it.score.trim().toDoubleOrNull() }
                .maxOrNull()
            if (priorMax == null || newScore > priorMax) {
                return RecordMoment.NewHighScore(pr.name.trim(), gameName)
            }
        }

        // Priority 3: win streak of 2+ (prior consecutive wins + current win)
        for (pr in savedPlayers) {
            if (!pr.isWinner) continue
            val lower = pr.name.trim().lowercase()
            val playerGamingHistory = gamePlays
                .filter { play -> play.players.any { it.name.trim().lowercase() == lower } }
                .sortedByDescending { it.date }
            var priorStreak = 0
            for (play in playerGamingHistory) {
                val p = play.players.firstOrNull { it.name.trim().lowercase() == lower }
                if (p?.isWinner == true) priorStreak++ else break
            }
            val totalStreak = priorStreak + 1  // +1 for the current win
            if (totalStreak >= 2) return RecordMoment.WinStreak(pr.name.trim(), totalStreak)
        }

        return null
    }

    fun getRecentPlayers(excludeNames: Set<String>): List<Player> {
        val lowerExclude = excludeNames.map { it.lowercase().trim() }.toSet()
        val seen = hashSetOf<String>()
        return historyPlays.value
            .take(20)
            .flatMap { it.players }
            .mapNotNull { pr ->
                val trimmed = pr.name.trim()
                if (trimmed.isBlank() || trimmed.lowercase() in lowerExclude) return@mapNotNull null
                resolveRosterPlayer(trimmed)
            }
            .filter { seen.add(it.id) }
            .take(4)
    }

    fun getFrequentPlayers(gameId: Int, excludeNames: Set<String>): List<Player> {
        val gamePlays = (historyPlays.value).filter { it.gameId == gameId }
        if (gamePlays.isEmpty()) return emptyList()

        val counts = mutableMapOf<String, Int>()
        gamePlays.forEach { play ->
            play.players.forEach { pr ->
                val trimmed = pr.name.trim()
                if (trimmed.isNotBlank()) counts[trimmed] = (counts[trimmed] ?: 0) + 1
            }
        }
        val lowerExclude = excludeNames.map { it.lowercase().trim() }.toSet()
        return counts.entries
            .sortedByDescending { it.value }
            .mapNotNull { (name, _) ->
                if (name.lowercase().trim() in lowerExclude) return@mapNotNull null
                resolveRosterPlayer(name)
            }
            .distinctBy { it.id }
            .take(5)
    }

    private fun normalizePlayersForPosting(players: List<PlayerResult>): List<PlayerResult> {
        return players.map { player ->
            val trimmedName = player.name.trim()
            if (trimmedName.isBlank()) {
                player.copy(name = trimmedName)
            } else {
                val match = resolveRosterPlayer(trimmedName)
                if (match != null) player.copy(name = match.displayName) else player.copy(name = trimmedName)
            }
        }
    }

    private fun resolveRosterPlayer(name: String): Player? {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return null
        val lower = trimmed.lowercase()
        _players.value.firstOrNull { player ->
            (listOf(player.displayName) + player.aliases).any { it.lowercase().trim() == lower }
        }?.let { return it }

        val threshold = maxOf(2, lower.length / 3)
        return _players.value
            .map { player ->
                val bestDistance = (listOf(player.displayName) + player.aliases)
                    .minOf { alias -> levenshtein(lower, alias.lowercase().trim()) }
                player to bestDistance
            }
            .filter { (_, distance) -> distance <= threshold }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

}

private fun mergeHistorySources(local: List<LoggedPlay>, remote: List<LoggedPlay>): List<LoggedPlay> {
    val remoteSignatures = remote.mapTo(hashSetOf()) { it.signatureKey() }
    val localOnly = local.filter { it.signatureKey() !in remoteSignatures }
    val combined = (localOnly + remote).sortedByDescending { it.date }
    // Guarantee unique IDs — LazyColumn keys must not repeat
    val seenIds = hashSetOf<String>()
    return combined.filter { seenIds.add(it.id) }
}

private fun mergeBggPlayLists(vararg lists: List<LoggedPlay>): List<LoggedPlay> {
    val bySignature = linkedMapOf<String, LoggedPlay>()
    lists.asSequence()
        .flatMap { it.asSequence() }
        .forEach { play ->
            val signature = play.signatureKey()
            val existing = bySignature[signature]
            bySignature[signature] = when {
                existing == null -> play
                existing.prefersRemoteIdentityOver(play) -> existing
                play.prefersRemoteIdentityOver(existing) -> play
                else -> existing
            }
        }
    // Secondary dedup by ID: two plays can share an ID with different signatures
    // (e.g. BGG re-uses an ID, or normalization diverges). Keep the remote/posted one.
    val byId = linkedMapOf<String, LoggedPlay>()
    bySignature.values.forEach { play ->
        val existing = byId[play.id]
        byId[play.id] = when {
            existing == null -> play
            existing.prefersRemoteIdentityOver(play) -> existing
            play.prefersRemoteIdentityOver(existing) -> play
            else -> existing
        }
    }
    return byId.values.sortedByDescending { it.date }
}

private fun LoggedPlay.prefersRemoteIdentityOver(other: LoggedPlay): Boolean {
    val thisRemote = !id.isLikelyLocalUuid()
    val otherRemote = !other.id.isLikelyLocalUuid()
    return when {
        thisRemote && !otherRemote -> true
        !thisRemote && otherRemote -> false
        postedToBgg && !other.postedToBgg -> true
        !postedToBgg && other.postedToBgg -> false
        else -> false
    }
}

private fun LoggedPlay.signatureKey(): String {
    val normalizedPlayers = players.joinToString("|") { player ->
        listOf(
            player.name.trim().lowercase(),
            player.score.trim(),
            player.isWinner.toString(),
            player.rating.trim()
        ).joinToString("~")
    }
    return listOf(
        gameId.toString(),
        gameName.trim().lowercase(),
        date,
        durationMinutes.toString(),
        location.trim().lowercase(),
        comments.trim().lowercase(),
        quantity.toString(),
        incomplete.toString(),
        nowInStats.toString(),
        normalizedPlayers
    ).joinToString("||")
}

private fun String.isLikelyLocalUuid(): Boolean {
    return Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        .matches(this)
}

private fun findRelatedGames(game: BggGame, collection: List<BggGame>): GameRelations {
    val name = game.name.trim()
    fun separatorIndex(s: String): Int? = listOf(s.indexOf(':').takeIf { it > 0 }, s.indexOf(" \u2013 ").takeIf { it > 0 }, s.indexOf(" \u2014 ").takeIf { it > 0 }, s.indexOf(" - ").takeIf { it > 0 }).filterNotNull().minOrNull()
    fun rootOf(s: String) = separatorIndex(s)?.let { s.substring(0, it).trim() } ?: s.trim()
    val mySepIdx = separatorIndex(name)
    return if (mySepIdx != null) {
        val root = name.substring(0, mySepIdx).trim()
        val baseGames = collection.filter { other -> other.id != game.id && separatorIndex(other.name.trim()) == null && other.name.trim().equals(root, ignoreCase = true) }
        val siblings = collection.filter { other -> other.id != game.id && separatorIndex(other.name.trim()) != null && rootOf(other.name).equals(root, ignoreCase = true) }
        GameRelations(isExpansion = true, baseGames = baseGames, expansions = siblings)
    } else {
        val expansions = collection.filter { other -> other.id != game.id && rootOf(other.name).equals(name, ignoreCase = true) }
        GameRelations(isExpansion = false, baseGames = emptyList(), expansions = expansions)
    }
}

private fun levenshtein(a: String, b: String): Int {
    val m = a.length; val n = b.length
    val dp = Array(m + 1) { i -> IntArray(n + 1) { j -> if (i == 0) j else if (j == 0) i else 0 } }
    for (i in 1..m) for (j in 1..n) {
        dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1] else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
    }
    return dp[m][n]
}
