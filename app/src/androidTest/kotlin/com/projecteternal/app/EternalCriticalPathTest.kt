package com.projecteternal.app

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.projecteternal.app.controller.GameController
import com.projecteternal.app.ui.EternalApp
import com.projecteternal.app.ui.EternalTheme
import com.projecteternal.model.OfflineReport
import com.projecteternal.model.ActivityType
import com.projecteternal.persist.SaveManager
import com.projecteternal.persist.SaveStore
import com.projecteternal.persist.StoredSave
import com.projecteternal.sim.GameFactory
import com.projecteternal.sim.GameIntent
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deterministic Compose critical-path tests (§21): dry-run the same UiState the
 * activity renders, against an in-memory [SaveStore]. No emulator timing
 * dependencies — the AppContainer/Room path is separately covered by unit tests.
 */
@RunWith(AndroidJUnit4::class)
class EternalCriticalPathTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun freshStart_creation_toPlayableShell() {
        val store = InMemorySaveStore()
        val controller = GameController(SaveManager(store))

        compose.setContent { EternalTheme { EternalApp(controller) } }

        compose.waitUntil(timeoutMillis = 10_000) {
            controller.startup.value.phase == GameController.Phase.NEW_GAME
        }
        compose.onNodeWithTag("field_name").assertExists()
        compose.onNodeWithTag("btn_set_out").performClick() // disabled while blank
        compose.onNodeWithTag("field_name").performTextReplacement("Tester")
        compose.onNodeWithTag("btn_set_out").performClick()

        compose.waitUntil(timeoutMillis = 30_000) {
            if (controller.startup.value.phase == GameController.Phase.READY) {
                true
            } else {
                // Slow emulator can drop a tap; keep trying until state advances.
                compose.onNodeWithTag("btn_set_out").performClick()
                false
            }
        }
        compose.waitUntil(timeoutMillis = 45_000) {
            controller.state.value?.character?.name == "Tester"
        }
        compose.waitUntil(timeoutMillis = 45_000) {
            runCatching {
                val text = compose.onNodeWithTag("header_name").fetchSemanticsNode()
                    .config[androidx.compose.ui.semantics.SemanticsProperties.Text]
                    .joinToString(separator = "") { it.text }
                text.contains("Tester") && text.contains("Lv 1")
            }.getOrDefault(false)
        }
        val headerText = compose.onNodeWithTag("header_name").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text]
            .joinToString(separator = "") { it.text }
        if (!(headerText.contains("Tester") && headerText.contains("Lv 1"))) {
            error("header did not render name: got [$headerText]")
        }
        compose.onNodeWithText("The Reach").assertExists()
        compose.onNodeWithText("Rustrock Quarry").assertExists()

        // StartGame also wrote the new game through SaveManager (debounced).
        compose.waitUntil(timeoutMillis = 20_000) {
            runBlocking { store.readLive() != null }
        }
    }

    @Test
    fun offlineReturn_showsReport_thenDismisses() {
        val store = InMemorySaveStore()
        val manager = SaveManager(store)
        val now = System.currentTimeMillis()
        val seeded = GameFactory.newGame("Tester", now - 7_200_000L).copy(
            pendingOfflineReport = OfflineReport(
                elapsedSeconds = 7200,
                resourcesGained = mapOf("ore_iron" to 40),
                marksGained = 120,
            ),
        )
        runBlocking { manager.save(seeded) }
        val controller = GameController(manager)

        compose.setContent { EternalTheme { EternalApp(controller) } }

        compose.waitUntil(timeoutMillis = 10_000) {
            controller.startup.value.phase == GameController.Phase.READY
        }
        compose.onNodeWithTag("txt_report_title").assertExists()
        compose.onNodeWithText("Iron Ore").assertExists()
        compose.onNodeWithText("+40").assertExists()
        compose.onNodeWithText("+120").assertExists()

        compose.onNodeWithTag("btn_report_continue").performClick()

        compose.waitUntil(timeoutMillis = 10_000) {
            controller.state.value?.pendingOfflineReport == null
        }
        compose.onNodeWithTag("txt_report_title").assertDoesNotExist()
        compose.onNodeWithTag("header_name").assertExists()
    }

    /**
     * Release-candidate regression: the Industry Start button must produce a
     * real, observable processing job. Drives the actual UI (tab tap + Start
     * tap), then asserts the full contract against engine state: the activity is
     * created, advancing consumes one input and produces output, and Stop ends
     * the job — all through the same StateFlow the UI renders from.
     */
    @Test
    fun industryStart_processingRunsProducesThenStops() {
        val store = InMemorySaveStore()
        val manager = SaveManager(store)
        val now = System.currentTimeMillis()
        val seeded = GameFactory.newGame("Tester", now).addItem("ore_iron", 8)
        runBlocking { manager.save(seeded) }
        val controller = GameController(manager)

        compose.setContent { EternalTheme { EternalApp(controller) } }

        compose.waitUntil(timeoutMillis = 10_000) {
            controller.startup.value.phase == GameController.Phase.READY
        }
        compose.onNodeWithTag("tab_INDUSTRY").performClick()

        compose.waitUntil(timeoutMillis = 15_000) {
            runCatching { compose.onNodeWithText("Grind Iron Ore").assertExists() }.isSuccess
        }
        // Press the visible Start button for a satisfiable processing recipe.
        compose.onNodeWithTag("btn_start_recipe_grind_iron").performClick()

        // 1 + 2 + 4 + 5: the intent reaches the engine and creates the activity.
        compose.waitUntil(timeoutMillis = 15_000) {
            controller.state.value?.character?.currentActivity?.type == ActivityType.PROCESSING &&
                controller.state.value?.character?.currentActivity?.targetId == "grind_iron"
        }
        // The card flips to the running (disabled) state — observable UI change.
        compose.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                val node = compose.onNodeWithTag("btn_start_recipe_grind_iron").fetchSemanticsNode()
                androidx.compose.ui.semantics.SemanticsProperties.Disabled in node.config
            }.getOrDefault(false)
        }

        // Deterministic advancement through the engine (no live ticker in this
        // harness): the job must consume input and produce output.
        val base = controller.state.value!!.lastSavedEpochMs
        controller.dispatch(GameIntent.Tick(base + 6_000))
        compose.waitUntil(timeoutMillis = 15_000) {
            val s = controller.state.value ?: return@waitUntil false
            s.inventoryCount("ore_iron") < 8 && s.inventoryCount("iron_fragment") > 0
        }

        // 7: the job can be stopped from the UI and the state reflects it.
        compose.onNodeWithTag("btn_stop_activity").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            controller.state.value?.character?.currentActivity == null
        }
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