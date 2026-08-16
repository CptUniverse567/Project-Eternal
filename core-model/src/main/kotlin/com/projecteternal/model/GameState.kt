package com.projecteternal.model

import kotlinx.serialization.Serializable

/** Tracks the current save schema. Migration happens in core-persistence. */
object SaveSchema {
    const val CURRENT: Int = 1
}

/**
 * The entire mutable game state. Immutable; all mutations produce a new
 * instance. Persisted as a single atomic JSON blob by core-persistence.
 */
@Serializable
data class GameState(
    val schemaVersion: Int = SaveSchema.CURRENT,
    val saveId: String = "",
    val character: Character = Character(name = ""),
    val inventory: List<ItemStack> = emptyList(),
    val equipmentItems: List<ItemInstance> = emptyList(),
    val retainers: List<Retainer> = emptyList(),
    val nodes: List<WorldNode> = emptyList(),
    val quests: List<QuestProgress> = emptyList(),
    val market: MarketState = MarketState(),
    val stats: StatsTracker = StatsTracker(),
    val unlocks: Set<String> = emptySet(),
    val rngSeed: Long = 0,
    val rngAdvance: Long = 0,
    val createdEpochMs: Long = 0,
    val lastSavedEpochMs: Long = 0,
    val totalPlaySeconds: Long = 0,
    val pendingOfflineReport: OfflineReport? = null,
) {

    // ---- inventory helpers (pure) ----

    fun inventoryCount(defId: ItemId): Long =
        inventory.firstOrNull { it.defId == defId }?.count ?: 0

    fun addItem(defId: ItemId, count: Long): GameState {
        if (count <= 0) return this
        val existing = inventory.firstOrNull { it.defId == defId }
        val next = if (existing != null) {
            inventory.map { if (it.defId == defId) it.copy(count = it.count + count) else it }
        } else {
            inventory + ItemStack(defId, count)
        }
        return copy(inventory = next)
    }

    fun removeItem(defId: ItemId, count: Long): GameState {
        if (count <= 0) return this
        val existing = inventory.firstOrNull { it.defId == defId } ?: return this
        val remaining = existing.count - count
        val next = if (remaining <= 0) {
            inventory.filter { it.defId != defId }
        } else {
            inventory.map { if (it.defId == defId) it.copy(count = remaining) else it }
        }
        return copy(inventory = next)
    }

    fun addMarks(amount: Long): GameState =
        copy(character = character.copy(marks = character.marks + amount))

    fun spendMarks(amount: Long): GameState {
        require(amount >= 0)
        return copy(character = character.copy(marks = character.marks - amount))
    }

    fun addResolve(amount: Long): GameState =
        copy(character = character.copy(resolve = character.resolve + amount))

    fun addXp(amount: Long): GameState =
        copy(character = character.copy(xp = character.xp + amount))

    fun addSkillXp(skill: SkillId, amount: Long): GameState =
        copy(character = character.copy(
            skillXp = character.skillXp + (skill to (character.skillXp[skill] ?: 0) + amount)
        ))

    // ---- unlocks ----

    fun hasUnlock(token: String): Boolean = unlocks.contains(token)

    fun unlock(vararg tokens: String): GameState = copy(unlocks = unlocks + tokens)

    // ---- nodes ----

    fun node(id: NodeId): WorldNode? = nodes.firstOrNull { it.id == id }

    fun unlockedNodes(): List<WorldNode> = nodes.filter { it.unlocked }

    fun updateNode(id: NodeId, transform: (WorldNode) -> WorldNode): GameState {
        val target = node(id) ?: return this
        return copy(nodes = nodes.map { if (it.id == id) transform(it) else it })
    }

    fun discoverNode(id: NodeId): GameState {
        val updated = updateNode(id) { it.copy(unlocked = true) }
        val region = updated.node(id)?.regionId ?: return updated
        return updated.copy(
            stats = updated.stats.visitRegion(region),
        )
    }

    // ---- retainers ----

    fun retainer(id: RetainerId): Retainer? = retainers.firstOrNull { it.id == id }

    fun updateRetainer(id: RetainerId, transform: (Retainer) -> Retainer): GameState {
        val target = retainer(id) ?: return this
        return copy(retainers = retainers.map { if (it.id == id) transform(it) else it })
    }

    // ---- equipment ----

    fun itemInstance(uid: String): ItemInstance? = equipmentItems.firstOrNull { it.uid == uid }

    fun equippedIn(slot: EquipSlot): ItemInstance? = character.equipped[slot]

    fun addEquipmentItem(item: ItemInstance): GameState =
        copy(equipmentItems = equipmentItems + item)

    fun updateEquipmentItem(uid: String, transform: (ItemInstance) -> ItemInstance): GameState =
        copy(equipmentItems = equipmentItems.map {
            if (it.uid == uid) transform(it) else it
        })

    // ---- quests ----

    fun quest(questId: QuestId): QuestProgress? = quests.firstOrNull { it.questId == questId }

    fun updateQuest(questId: QuestId, transform: (QuestProgress) -> QuestProgress): GameState {
        val target = quest(questId) ?: return this
        return copy(quests = quests.map { if (it.questId == questId) transform(it) else it })
    }

    fun acceptQuests(ids: List<QuestId>, nowEpochMs: Long): GameState {
        val newOnes = ids.filter { quest(it) == null }
        if (newOnes.isEmpty()) return this
        val toAdd = newOnes.map { QuestProgress(questId = it, status = QuestStatus.ACTIVE, acceptedAtEpochMs = nowEpochMs) }
        return copy(quests = quests + toAdd)
    }

    fun completeQuest(questId: QuestId, nowEpochMs: Long): GameState =
        updateQuest(questId) {
            it.copy(status = QuestStatus.COMPLETED, completedAtEpochMs = nowEpochMs)
        }.copy(
            stats = stats.copy(completedQuestCount = stats.completedQuestCount + 1)
        )

    // ---- activity ----

    fun withActivity(activity: ActivityState?): GameState = copy(character = character.copy(currentActivity = activity))

    // ---- rng ----

    /** Advance the deterministic seed counter and return the next seed. */
    fun nextSeed(): Pair<GameState, Long> {
        val next = rngAdvance + 1
        return Pair(copy(rngAdvance = next), mix64(rngSeed, next))
    }

    private fun mix64(seed: Long, advance: Long): Long {
        var h = seed xor advance
        h *= -0x61c8864680b583ebL
        h = h xor (h ushr 31)
        h *= -0x7ee3623f09785b6bL
        h = h xor (h ushr 33)
        return h
    }
}
