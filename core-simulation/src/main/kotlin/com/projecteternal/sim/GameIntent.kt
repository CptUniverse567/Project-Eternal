package com.projecteternal.sim

import com.projecteternal.model.ActivityType
import com.projecteternal.model.EquipSlot
import com.projecteternal.model.GameState
import kotlinx.coroutines.flow.StateFlow

/**
 * All user actions and lifecycle events. UI never mutates state directly; it
 * dispatches these to the [GameStateRepository].
 */
sealed interface GameIntent {
    /** Create the character (first launch only). */
    data class StartGame(val name: String) : GameIntent

    /** Begin/switch the character's activity (gathering/combat/processing/crafting). */
    data class StartActivity(val type: ActivityType, val targetId: String) : GameIntent

    data object StopActivity : GameIntent

    data class Enhance(
        val itemUid: String,
        val useProtection: Boolean,
        val useFullNegation: Boolean = false,
        /** Use the table's alternate enhancement material (e.g. Frostvein for ADVANCED). */
        val useAlternateMaterial: Boolean = false,
    ) : GameIntent

    data class Equip(val itemUid: String) : GameIntent

    data class Unequip(val slot: EquipSlot) : GameIntent

    data class Repair(val itemUid: String) : GameIntent

    data class Buy(val defId: String, val count: Long) : GameIntent

    data class Sell(val defId: String, val count: Long) : GameIntent

    /** null nodeId unassigns the retainer. */
    data class AssignRetainer(val retainerId: String, val nodeId: String?) : GameIntent

    /** Hire a documented-retainer into the roster for Marks (requires its unlock token). */
    data class RecruitRetainer(val defId: String) : GameIntent

    data class AcceptQuest(val questId: String) : GameIntent

    data class UseConsumable(val defId: String) : GameIntent

    data object DismissOfflineReport : GameIntent

    /** Foreground heartbeat (~1 Hz) advancing the active activity. */
    data class Tick(val nowEpochMs: Long) : GameIntent

    /** App returned to foreground — run the offline simulation. */
    data object OnResume : GameIntent
}

/** Central state holder the UI reads from and dispatches intents to. */
interface GameStateRepository {
    /** Null while the controller is still loading or no save exists yet. */
    val state: StateFlow<GameState?>
    fun dispatch(intent: GameIntent)
}
