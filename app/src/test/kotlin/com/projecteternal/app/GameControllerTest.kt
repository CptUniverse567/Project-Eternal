package com.projecteternal.app

import com.projecteternal.app.controller.GameController
import com.projecteternal.model.ActivityType
import com.projecteternal.persist.SaveManager
import com.projecteternal.persist.SaveStore
import com.projecteternal.persist.StoredSave
import com.projecteternal.sim.GameFactory
import com.projecteternal.sim.GameIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression guards for the app-layer controller (the thin Android-aware shell
 * over the pure engine). These run on the JVM with a fake main dispatcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameControllerTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startGame_withNoSave_createsCharacter_andPersists() {
        val store = InMemorySaveStore()
        val controller = GameController(SaveManager(store))

        controller.dispatch(GameIntent.StartGame("Tester"))

        // Regression: StartGame must work before any state exists (the old guard
        // returned early because state was null, silently no-op'ing creation).
        assertEquals(GameController.Phase.READY, controller.startup.value.phase)
        assertEquals("Tester", controller.state.value?.character?.name)
        assertNotNull(controller.state.value?.saveId)
    }

    @Test
    fun slowLoad_doesNotClobberJustStartedGame() {
        val store = BlockingStartStore(InMemorySaveStore())
        val controller = GameController(SaveManager(store))

        // load() is stalled inside the store; create the game anyway.
        controller.dispatch(GameIntent.StartGame("Tester"))
        assertEquals(GameController.Phase.READY, controller.startup.value.phase)

        // Now let the (empty) store resolve: a late NoSave must not reset the game.
        store.allow()
        val phaseAfter = controller.startup.value.phase
        assertEquals(GameController.Phase.READY, phaseAfter)
        assertEquals("Tester", controller.state.value?.character?.name)
    }

    @Test
    fun dismissOfflineReport_clearsPendingReport_stateNonNull() {
        val store = InMemorySaveStore()
        val seeded = GameFactory.newGame("T", 0L).copy(
            pendingOfflineReport = com.projecteternal.model.OfflineReport(elapsedSeconds = 60),
        )
        runBlocking { SaveManager(store).save(seeded) }
        val controller = GameController(SaveManager(store))

        awaitPhase(controller, GameController.Phase.READY)

        controller.dispatch(GameIntent.DismissOfflineReport)
        assertNull(controller.state.value?.pendingOfflineReport)
    }

    /**
     * Cold start: onResume() fires while the save is still loading (state is
     * null), so it must still start the ticker. Regression: the old early
     * return skipped startTicker() on cold starts, freezing every activity at
     * "Gathering…" with no production until a later pause/resume cycle.
     */
    @Test
    fun coldStart_withoutLoadedState_stillStartsTicker_soActivitiesAdvance() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        var fakeNow = 0L
        val store = InMemorySaveStore()
        // Advance the injected clock ~1s per call (a tick fires once per second).
        val controller = GameController(SaveManager(store), clock = { fakeNow += 1_000; fakeNow })

        // onResume runs before any state exists (async load not finished yet).
        controller.onResume(10_000_000L)

        // The player then creates a character and starts gathering.
        controller.dispatch(GameIntent.StartGame("Tester"))
        controller.dispatch(GameIntent.StartActivity(ActivityType.GATHERING, "node_quarry"))

        // Let the foreground ticker run ~45 virtual seconds.
        dispatcher.scheduler.advanceTimeBy(45_000)
        dispatcher.scheduler.runCurrent()

        val state = controller.state.value
        assertNotNull(state)
        assertTrue("total=${state?.totalPlaySeconds}", state!!.totalPlaySeconds >= 40)
        assertEquals(ActivityType.GATHERING, state.character.currentActivity?.type)
        assertTrue(state.character.skillXp["mining"] ?: 0 > 0)
    }

    private fun awaitPhase(controller: GameController, phase: GameController.Phase) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline && controller.startup.value.phase != phase) {
            Thread.sleep(5)
        }
        assertEquals(phase, controller.startup.value.phase)
    }
}

private class InMemorySaveStore : SaveStore {
    private var live: StoredSave? = null
    private var backup: StoredSave? = null
    override suspend fun readLive(): StoredSave? = live
    override suspend fun readBackup(): StoredSave? = backup
    override suspend fun writeLive(save: StoredSave) {
        live = save
    }
    override suspend fun writeBackup(save: StoredSave) {
        backup = save
    }
    override suspend fun clearAll() {
        live = null
        backup = null
    }
}

/** Holds the first readLive() open until [allow]; then behaves like [delegate]. */
private class BlockingStartStore(private val delegate: SaveStore) : SaveStore {
    private val gate = CompletableDeferred<Unit>()
    private var passed = false

    fun allow() {
        gate.complete(Unit)
    }

    override suspend fun readLive(): StoredSave? {
        if (!passed) {
            passed = true
            gate.await()
        }
        return delegate.readLive()
    }

    override suspend fun readBackup(): StoredSave? = delegate.readBackup()
    override suspend fun writeLive(save: StoredSave) = delegate.writeLive(save)
    override suspend fun writeBackup(save: StoredSave) = delegate.writeBackup(save)
    override suspend fun clearAll() = delegate.clearAll()
}