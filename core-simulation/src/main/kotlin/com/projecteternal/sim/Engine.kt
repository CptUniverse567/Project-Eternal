package com.projecteternal.sim

import com.projecteternal.content.Items
import com.projecteternal.content.Monsters
import com.projecteternal.content.Nodes
import com.projecteternal.content.Quests
import com.projecteternal.content.Recipes
import com.projecteternal.model.ActivityType
import com.projecteternal.model.EquipSlot
import com.projecteternal.model.GameState
import com.projecteternal.model.ItemKind
import com.projecteternal.model.NodeType
import com.projecteternal.model.RetainerSpecialization
import com.projecteternal.model.QuestStatus
import kotlin.math.ceil

/**
 * Pure game engine: every intent handler maps (state, intent) -> new state.
 * No Android or persistence dependencies — fully unit-testable. The app layer
 * (GameController) wires this to storage and a StateFlow.
 */
class GameEngine {

    data class Result(val state: GameState, val events: List<String>)

    fun newGame(name: String, now: Long): GameState = GameFactory.newGame(name, now)

    fun apply(state: GameState, intent: GameIntent, now: Long): Result {
        val base = when (intent) {
            is GameIntent.StartGame -> if (state.saveId.isBlank()) GameFactory.newGame(intent.name, now) else state
            is GameIntent.StartActivity -> startActivity(state, intent, now)
            GameIntent.StopActivity -> state.withActivity(null)
            is GameIntent.Enhance -> enhance(state, intent)
            is GameIntent.Equip -> equip(state, intent.itemUid)
            is GameIntent.Unequip -> unequip(state, intent.slot)
            is GameIntent.Repair -> repair(state, intent.itemUid)
            is GameIntent.Buy -> buy(state, intent)
            is GameIntent.Sell -> sell(state, intent)
            is GameIntent.SellAll -> sellAll(state, intent)
            is GameIntent.SellEquipment -> sellEquipment(state, intent)
            is GameIntent.SellAllEquipment -> sellAllEquipment(state, intent)
            is GameIntent.AssignRetainer -> assignRetainer(state, intent)
            is GameIntent.RecruitRetainer -> recruitRetainer(state, intent)
            is GameIntent.AcceptQuest -> acceptQuest(state, intent.questId, now)
            is GameIntent.UseConsumable -> useConsumable(state, intent.defId)
            GameIntent.DismissOfflineReport -> state.copy(pendingOfflineReport = null)
            is GameIntent.Tick -> tick(state, intent.nowEpochMs)
            GameIntent.OnResume -> state
        }
        val resolved = resolveQuestsAndNodes(base, now)
        return Result(resolved, eventsFor(state, resolved))
    }

    /**
     * Quest rewards may unlock nodes, which complete more quests, which unlock
     * more nodes — iterate to a fixpoint so the whole chain resolves atomically
     * in a single `apply`.
     */
    private fun resolveQuestsAndNodes(start: GameState, now: Long): GameState {
        var s = start
        while (true) {
            val afterQuests = QuestEngine.process(s, now).state
            val afterNodes = autoDiscoverNodes(afterQuests)
            if (afterNodes == s) return afterNodes
            s = afterNodes
        }
    }

    /** Nodes whose unlock token is held become available automatically. */
    private fun autoDiscoverNodes(state: GameState): GameState {
        var s = state
        for (node in s.nodes) {
            val def = Nodes.get(node.id)
            if (!node.unlocked && def.unlockToken.isNotEmpty() && s.hasUnlock(def.unlockToken)) {
                s = s.discoverNode(node.id)
            }
        }
        return s
    }

    /** Foreground heartbeat — advances the current activity and retainers. */
    private fun tick(state: GameState, now: Long): GameState {
        val (s0, rng) = RandomSource.next(state)
        val elapsedSeconds = ((now - s0.lastSavedEpochMs).coerceAtLeast(0)) / 1000.0
        if (elapsedSeconds <= 0) return state.copy(lastSavedEpochMs = now)
        val (s1, _) = ActivityEngine.advance(s0, elapsedSeconds, rng)
        val (s2, _) = RetainerEngine.advance(s1, elapsedSeconds, rng)
        return s2.copy(
            lastSavedEpochMs = now,
            totalPlaySeconds = s2.totalPlaySeconds + elapsedSeconds.toLong(),
        )
    }

    private fun startActivity(state: GameState, intent: GameIntent.StartActivity, now: Long): GameState {
        when (intent.type) {
            ActivityType.GATHERING -> {
                val node = Nodes.get(intent.targetId)
                val current = state.node(intent.targetId)
                if (current == null || !current.unlocked) return state
                if (node.yields.isEmpty()) return state
            }
            ActivityType.COMBAT -> {
                val monster = Monsters.get(intent.targetId)
                val unlockedInRegion = state.unlockedNodes().any { it.regionId == monster.regionId }
                if (!unlockedInRegion) return state
            }
            ActivityType.PROCESSING, ActivityType.CRAFTING -> {
                val recipe = Recipes.get(intent.targetId)
                if (recipe.unlockToken.isNotEmpty() && !state.hasUnlock(recipe.unlockToken)) return state
                if (state.character.skillLevel(recipe.skillId) < recipe.skillLevelRequired) return state
                // A recipe job must be fundable at start. Without this, starting
                // with no materials creates an activity that the very first tick
                // immediately clears (advanceRecipe's input cap) — a start that
                // never observably runs, and that the UI's disabled "Missing
                // materials" button is supposed to make impossible.
                val fundable = recipe.inputs.none { it.count > 0 && state.inventoryCount(it.defId) < it.count }
                if (!fundable) return state
            }
        }
        return state.withActivity(
            com.projecteternal.model.ActivityState(
                type = intent.type,
                targetId = intent.targetId,
                startedAtEpochMs = now,
            )
        )
    }

    private fun equip(state: GameState, itemUid: String): GameState {
        val inst = state.itemInstance(itemUid) ?: return state
        if (inst.durability <= 0) return state
        val def = Items.get(inst.defId)
        val slot = def.slot ?: return state
        if (state.character.equipped.values.any { it.uid == itemUid }) return state
        return state.copy(
            character = state.character.copy(equipped = state.character.equipped + (slot to inst)),
        )
    }

    private fun unequip(state: GameState, slot: EquipSlot): GameState =
        state.copy(character = state.character.copy(equipped = state.character.equipped - slot))

    private fun repair(state: GameState, itemUid: String): GameState {
        val inst = state.itemInstance(itemUid) ?: return state
        if (inst.maxDurability <= 0 || inst.durability >= inst.maxDurability) return state
        val deficit = inst.maxDurability - inst.durability
        val kitsNeeded = ceil(deficit / 20.0).toInt()
        val marksNeeded = kitsNeeded * 5L
        if (state.inventoryCount("repair_kit") < kitsNeeded) return state
        if (state.character.marks < marksNeeded) return state
        return state
            .removeItem("repair_kit", kitsNeeded.toLong())
            .spendMarks(marksNeeded)
            .updateEquipmentItem(itemUid) { it.copy(durability = it.maxDurability) }
    }

    private fun enhance(state: GameState, intent: GameIntent.Enhance): GameState {
        val inst = state.itemInstance(intent.itemUid) ?: return state
        val level = inst.enhancementLevel
        val table = com.projecteternal.content.EnhancementTables.tableFor(level) ?: return state
        if (inst.durability <= 0) return state
        val shards = table.shardsPerAttempt[level] ?: return state
        if (state.inventoryCount("shard_resonance") < shards) return state
        // Choose the material: explicit alternate request falls back to primary if absent.
        val primary = table.materialPerAttempt
        val alternate = table.alternateMaterialPerAttempt
        val material = when {
            intent.useAlternateMaterial && alternate != null -> alternate
            else -> primary
        }
        if (material != null && state.inventoryCount(material.defId) < material.count) return state
        if (table.unlockToken.isNotEmpty() && !state.hasUnlock(table.unlockToken)) return state
        val hasProtection = state.inventoryCount(table.protectionItem ?: "") >= 1
        val useProtection = intent.useProtection && hasProtection
        val hasFullNegation = state.inventoryCount(table.fullNegationItem ?: "") >= 1
        val useFullNegation = intent.useFullNegation && hasFullNegation

        val (s0, rng) = RandomSource.next(state)
        val outcome = EnhancementResolver.attempt(
            inst, s0.character.resolve, useProtection, hasProtection, rng,
            useFullNegation = useFullNegation, hasFullNegation = hasFullNegation,
        )

        var s = s0
            .removeItem("shard_resonance", outcome.consumedShards)
            .updateEquipmentItem(intent.itemUid) { it.copy(enhancementLevel = outcome.newLevel) }
        if (outcome.consumedMaterial && material != null) {
            s = s.removeItem(material.defId, material.count)
        }
        if (outcome.consumedProtection) {
            s = s.removeItem(table.protectionItem!!, 1)
        }
        if (outcome.consumedFullNegation) {
            s = s.removeItem(table.fullNegationItem!!, 1)
        }
        s = s.copy(character = s.character.copy(resolve = outcome.newResolve))
        s = s.copy(stats = s.stats.recordEnhance(outcome.newLevel))
        if (outcome.durabilityLoss > 0) {
            s = EquipmentHelper.damage(s, intent.itemUid, outcome.durabilityLoss.toDouble())
        }
        return s
    }

    private fun buy(state: GameState, intent: GameIntent.Buy): GameState {
        if (!Items.get(intent.defId).stackable) return state
        if (!MarketService.canBuy(state, intent.defId, intent.count)) return state
        val price = MarketService.currentBuyPrice(state, intent.defId) * intent.count
        return state
            .spendMarks(price)
            .addItem(intent.defId, intent.count)
            .let { MarketService.recordTrade(it, intent.defId, intent.count, MarketService.TradeKind.BUY) }
    }

    private fun sell(state: GameState, intent: GameIntent.Sell): GameState {
        if (!Items.get(intent.defId).stackable) return state
        if (!MarketService.canSell(state, intent.defId, intent.count)) return state
        val price = MarketService.currentSellPrice(state, intent.defId) * intent.count
        return state
            .removeItem(intent.defId, intent.count)
            .addMarks(price)
            .let { MarketService.recordTrade(it, intent.defId, intent.count, MarketService.TradeKind.SELL) }
    }

    private fun sellAll(state: GameState, intent: GameIntent.SellAll): GameState {
        val def = Items.get(intent.defId)
        if (def.sellPrice <= 0) return state
        val count = state.inventoryCount(intent.defId)
        if (count <= 0) return state
        val price = MarketService.currentSellPrice(state, intent.defId) * count
        return state
            .removeItem(intent.defId, count)
            .addMarks(price)
            .let { MarketService.recordTrade(it, intent.defId, count, MarketService.TradeKind.SELL) }
    }

    private fun sellEquipment(state: GameState, intent: GameIntent.SellEquipment): GameState {
        val inst = state.itemInstance(intent.itemUid) ?: return state
        val def = Items.get(inst.defId)
        if (def.sellPrice <= 0) return state
        // Equipped gear is protected from accidental sale.
        if (state.character.equipped.values.any { it.uid == inst.uid }) return state
        val price = MarketService.currentSellPrice(state, inst.defId)
        return state
            .copy(equipmentItems = state.equipmentItems.filter { it.uid != inst.uid })
            .addMarks(price)
            .let { MarketService.recordTrade(it, inst.defId, 1, MarketService.TradeKind.SELL) }
    }

    private fun sellAllEquipment(state: GameState, intent: GameIntent.SellAllEquipment): GameState {
        val def = Items.get(intent.defId)
        if (def.sellPrice <= 0) return state
        val equippedUids = state.character.equipped.values.map { it.uid }.toSet()
        val sellable = state.equipmentItems.filter { it.defId == intent.defId && it.uid !in equippedUids }
        if (sellable.isEmpty()) return state
        val price = MarketService.currentSellPrice(state, intent.defId) * sellable.size
        val removed = sellable.map { it.uid }.toSet()
        return state
            .copy(equipmentItems = state.equipmentItems.filter { it.uid !in removed })
            .addMarks(price)
            .let { MarketService.recordTrade(it, intent.defId, sellable.size.toLong(), MarketService.TradeKind.SELL) }
    }

    private fun recruitRetainer(state: GameState, intent: GameIntent.RecruitRetainer): GameState {
        val def = com.projecteternal.content.RetainerRecruits.get(intent.defId)
        if (state.retainer(def.id) != null) return state
        if (def.unlockToken.isNotEmpty() && !state.hasUnlock(def.unlockToken)) return state
        if (state.character.marks < def.costMarks) return state
        val hire = com.projecteternal.model.Retainer(
            id = def.id,
            name = def.name,
            specialization = def.specialization,
            gatheringSpeed = def.gatheringSpeed,
            productionSpeed = def.productionSpeed,
            luck = def.luck,
        )
        return state.spendMarks(def.costMarks).copy(retainers = state.retainers + hire)
    }

    private fun assignRetainer(state: GameState, intent: GameIntent.AssignRetainer): GameState {
        val retainer = state.retainer(intent.retainerId) ?: return state
        val nodeId = intent.nodeId
        if (nodeId == null) {
            val current = retainer.assignedNodeId ?: return state
            val updatedNode = state.updateNode(current) { n ->
                n.copy(assignedRetainerIds = n.assignedRetainerIds - retainer.id)
            }
            return updatedNode.updateRetainer(retainer.id) { it.withAssignment(null) }
        }
        val node = state.node(nodeId) ?: return state
        if (!node.unlocked) return state
        if (!nodeCompatible(retainer.specialization, node.type)) return state
        val withRetainer = state.updateRetainer(retainer.id) { it.withAssignment(nodeId) }
        return withRetainer.updateNode(nodeId) { n ->
            n.copy(assignedRetainerIds = (n.assignedRetainerIds + retainer.id).distinct())
        }
    }

    private fun nodeCompatible(spec: RetainerSpecialization, type: NodeType): Boolean = when (spec) {
        RetainerSpecialization.MINER -> type == NodeType.MINE
        RetainerSpecialization.LUMBERJACK -> type == NodeType.FOREST
        RetainerSpecialization.FARMER -> type == NodeType.FARM
        RetainerSpecialization.FISHER -> type == NodeType.FISHERY
        RetainerSpecialization.FORAGER -> type == NodeType.FOREST || type == NodeType.SPECIAL
        RetainerSpecialization.CRAFTER -> type == NodeType.MINE || type == NodeType.FOREST
        RetainerSpecialization.PROSPECTOR -> type == NodeType.MINE || type == NodeType.FOREST || type == NodeType.SPECIAL
        RetainerSpecialization.FORGER -> type == NodeType.MINE || type == NodeType.SPECIAL
    }

    private fun acceptQuest(state: GameState, questId: String, now: Long): GameState {
        val def = Quests.get(questId)
        val completed = state.quests.filter { it.status == QuestStatus.COMPLETED }.map { it.questId }.toSet()
        if (state.quest(questId) != null) return state
        if (!def.prerequisites.all { completed.contains(it) || state.hasUnlock(it) }) return state
        return state.acceptQuests(listOf(questId), now)
    }

    private fun useConsumable(state: GameState, defId: String): GameState {
        if (state.inventoryCount(defId) <= 0) return state
        val def = Items.get(defId)
        if (def.kind != ItemKind.CONSUMABLE) return state
        val heal = def.healAmount
        if (heal <= 0) return state
        val maxHp = CombatStatsMath.effectiveStats(state).maxHp
        val hp = (state.character.health + heal).coerceAtMost(maxHp)
        return state.removeItem(defId, 1).copy(character = state.character.copy(health = hp))
    }

    private fun eventsFor(before: GameState, after: GameState): List<String> {
        val events = mutableListOf<String>()
        val beforeDone = before.quests.filter { it.status == QuestStatus.COMPLETED }.map { it.questId }.toSet()
        for (q in after.quests) {
            if (q.status == QuestStatus.COMPLETED && !beforeDone.contains(q.questId)) {
                events.add("Quest complete: ${Quests.get(q.questId).name}")
            }
        }
        return events
    }
}
