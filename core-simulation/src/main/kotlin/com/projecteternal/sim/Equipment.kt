package com.projecteternal.sim

import com.projecteternal.model.EquipSlot
import com.projecteternal.model.GameState
import com.projecteternal.model.ItemInstance

/** Shared equipment mutation helpers. */
object EquipmentHelper {

    /** Apply durability damage; broken items are unequipped and recorded. */
    fun damage(state: GameState, uid: String, amount: Double): GameState {
        if (amount <= 0) return state
        val loss = kotlin.math.floor(amount).toInt()
        if (loss <= 0) return state
        val inst = state.itemInstance(uid) ?: return state
        val newDur = (inst.durability - loss).coerceAtLeast(0)
        val updated = state.updateEquipmentItem(uid) { it.copy(durability = newDur) }
        if (newDur > 0) return updated
        return updated.copy(
            character = updated.character.copy(
                equipped = updated.character.equipped.filterValues { it.uid != uid }
            ),
        )
    }

    fun damageTool(state: GameState, amount: Double): GameState {
        val tool = state.character.equipped[EquipSlot.TOOL] ?: return state
        return damage(state, tool.uid, amount)
    }

    /** Damage the main weapon by [perKill] per kill. */
    fun damageWeaponForKills(state: GameState, kills: Long): GameState {
        val weapon = state.character.equipped[EquipSlot.MAIN_WEAPON] ?: return state
        return damage(state, weapon.uid, kills * SimConfig.DURABILITY_PER_KILL_WEAPON)
    }

    /** Damage all armor by [perKill] per kill. */
    fun damageArmorForKills(state: GameState, kills: Long): GameState {
        var s = state
        for ((slot, inst) in state.character.equipped) {
            if (slot == EquipSlot.MAIN_WEAPON || slot == EquipSlot.TOOL) continue
            if (inst.maxDurability <= 0) continue
            s = damage(s, inst.uid, kills * SimConfig.DURABILITY_PER_KILL_ARMOR)
        }
        return s
    }

    fun brokenUids(before: Map<String, ItemInstance>, after: GameState): List<String> =
        before.values.filter { it.durability > 0 }
            .mapNotNull { inst -> after.itemInstance(inst.uid)?.takeIf { it.durability <= 0 }?.uid }
}
