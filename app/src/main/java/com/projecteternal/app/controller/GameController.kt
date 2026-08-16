package com.projecteternal.app.controller

import com.projecteternal.model.GameState
import com.projecteternal.persist.SaveManager
import com.projecteternal.sim.GameEngine
import com.projecteternal.sim.GameIntent
import com.projecteternal.sim.GameStateRepository
import com.projecteternal.sim.OfflineSimulator
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * App-level game controller. Owns the single source of truth ([state]), routes
 * intents through the pure [GameEngine], persists via [SaveManager], and wires
 * the foreground ticker plus offline-on-resume.
 */
class GameController(
    private val saveManager: SaveManager,
    private val engine: GameEngine = GameEngine(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val clock: () -> Long = { System.currentTimeMillis() },
) : GameStateRepository {

    enum class Phase { LOADING, NEW_GAME, READY, CORRUPT }

    data class Startup(val phase: Phase, val warning: String? = null)

    private val _state = MutableStateFlow<GameState?>(null)
    private val _startup = MutableStateFlow(Startup(Phase.LOADING))
    private val _events = MutableStateFlow<List<String>>(emptyList())
    private val _showFirstRunHelp = MutableStateFlow(false)

    override val state: StateFlow<GameState?> = _state.asStateFlow()
    val startup: StateFlow<Startup> = _startup.asStateFlow()

    /** One-shot narrative/quest events for the UI to surface then dismiss. */
    val events: StateFlow<List<String>> = _events.asStateFlow()

    /** Set once when a brand-new game is created; the UI surfaces the how-to sheet. */
    val showFirstRunHelp: StateFlow<Boolean> = _showFirstRunHelp.asStateFlow()

    private val dirty = AtomicBoolean(false)
    private var saveJob: Job? = null
    private var tickerJob: Job? = null

    init {
        scope.launch { load() }
    }

    private suspend fun load() {
        // Resolve the save first, THEN check whether a game already exists.
        // The initial guard used to run BEFORE the suspension in load(), so a
        // StartGame dispatched while load() was in flight could be clobbered by
        // the late result (its NoSave branch overwrote a READY startup phase).
        // Re-checking after the suspend makes this deterministic: whichever
        // creates state first wins, and every branch is prevented from regressing
        // past an already-created game.
        val result = saveManager.load()
        if (_state.value != null) return
        when (result) {
            is SaveManager.LoadResult.NoSave -> {
                _startup.value = Startup(Phase.NEW_GAME)
            }
            is SaveManager.LoadResult.Success -> {
                // Cold start of an existing save: replay the closed gap through
                // the offline simulator so the player keeps the idle gains on
                // app open (with a report). Skip when a report is already
                // pending — that save is mid-handoff between onResume and the
                // dismissal dialog, and re-simulating would clobber it.
                val loaded = if (result.state.pendingOfflineReport == null) {
                    OfflineSimulator.simulate(result.state, clock()).state
                } else {
                    result.state
                }
                _state.value = loaded
                _startup.value = Startup(Phase.READY, result.warnings.firstOrNull())
                persistDirty()
            }
            is SaveManager.LoadResult.Failure -> {
                _startup.value = Startup(Phase.CORRUPT, result.reason)
            }
        }
    }

    override fun dispatch(intent: GameIntent) {
        // StartGame is the one intent valid before any state exists.
        if (intent is GameIntent.StartGame) {
            val fresh = engine.newGame(intent.name, clock())
            _state.value = fresh
            _startup.value = Startup(Phase.READY)
            _events.value = emptyList()
            _showFirstRunHelp.value = true
            requestSave(fresh)
            return
        }
        val current = _state.value ?: return
        if (intent is GameIntent.Tick) {
            val result = engine.apply(current, intent, intent.nowEpochMs)
            _state.value = result.state
            requestSave(result.state)
            return
        }
        val now = clock()
        val result = engine.apply(current, intent, now)
        _state.value = result.state
        if (result.events.isNotEmpty()) _events.value = result.events
        requestSave(result.state)
    }

    /** App returned to foreground: resolve the offline gap, then resume the ticker. */
    fun onResume(nowEpochMs: Long) {
        val s = _state.value
        if (s != null) {
            val offline = OfflineSimulator.simulate(s, nowEpochMs)
            if (offline.state != s || offline.report.elapsedSeconds > 0) {
                _state.value = offline.state
                requestSave(offline.state)
            }
        }
        // Start the ticker unconditionally. On a cold start the save is loaded
        // asynchronously AFTER onResume, so _state.value is still null here and
        // the old early-return left the ticker dead: every activity froze at
        // "Gathering…" until a later pause/resume cycle. The loop itself no-ops
        // on a null state, so starting it early is harmless and guarantees
        // foreground advancement the moment state arrives (new game or loaded).
        startTicker()
    }

    /** App left foreground: stop ticking and flush any pending save. */
    fun onPause() {
        tickerJob?.cancel()
        tickerJob = null
        scope.launch { persistDirty() }
    }

    /** Foreground heartbeat driving the active activity. */
    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                delay(1_000)
                dispatch(GameIntent.Tick(clock()))
            }
        }
    }

    /** Re-run the initial load (from a corrupt-screen retry). */
    fun retryLoad() {
        _startup.value = Startup(Phase.LOADING)
        scope.launch { load() }
    }

    fun dismissEvents() {
        if (_events.value.isNotEmpty()) _events.value = emptyList()
    }

    fun dismissFirstRunHelp() {
        _showFirstRunHelp.value = false
    }

    /** Debounced, self-coalescing save (never more often than ~every few seconds). */
    private fun requestSave(state: GameState) {
        dirty.set(true)
        if (saveJob?.isActive != true) {
            saveJob = scope.launch {
                delay(SAVE_DEBOUNCE_MS)
                persistDirty()
            }
        }
    }

    private suspend fun persistDirty() {
        if (dirty.compareAndSet(true, false)) {
            val s = _state.value ?: return
            saveManager.save(s)
        }
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 3_000L
    }
}
