package com.projecteternal.model

import kotlinx.serialization.Serializable

/**
 * A stack of a stackable item (resources, materials, consumables) in the
 * inventory. Non-stackable items (equipment) are represented by ItemInstance
 * and kept in [GameState.equipmentItems].
 */
@Serializable
data class ItemStack(
    val defId: ItemId,
    val count: Long,
) {
    companion object {
        fun of(defId: ItemId, count: Long): ItemStack = ItemStack(defId, count)
    }
}

/**
 * A physical item instance. Only used for non-stackable equipment.
 * [durability] 0 means "not applicable".
 */
@Serializable
data class ItemInstance(
    val uid: String,
    val defId: ItemId,
    val enhancementLevel: Int = 0,
    val durability: Int = 0,
    val maxDurability: Int = 0,
    val bound: Boolean = false,
) {
    val durabilityFraction: Double
        get() = if (maxDurability <= 0) 1.0 else durability.toDouble() / maxDurability
}
