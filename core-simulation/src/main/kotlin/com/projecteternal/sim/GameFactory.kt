package com.projecteternal.sim

import com.projecteternal.content.Nodes
import com.projecteternal.model.GameState
import com.projecteternal.model.ItemInstance
import com.projecteternal.model.Retainer
import com.projecteternal.model.RetainerSpecialization
import com.projecteternal.model.WorldNode

/** Default unlock tokens granted on character creation. */
object GameFactory {

    val DEFAULT_UNLOCKS: Set<String> = setOf(
        "screen:character", "screen:inventory", "screen:combat",
        "screen:gathering", "screen:processing", "screen:world",
        "screen:market", "screen:quests",
    )

    private fun nodeFrom(def: com.projecteternal.content.NodeDefinition): WorldNode =
        WorldNode(
            id = def.id,
            regionId = def.regionId,
            type = def.type,
            tier = def.tier,
            unlocked = def.unlockToken.isEmpty(),
        )

    private fun starterRetainer(): Retainer = Retainer(
        id = "retainer_aldo",
        name = "Aldo",
        specialization = RetainerSpecialization.MINER,
        gatheringSpeed = 1.0,
        productionSpeed = 1.0,
    )

    fun newGame(name: String, now: Long): GameState {
        val pickaxe = ItemInstance(
            uid = "uid_start_pickaxe",
            defId = "tool_pickaxe",
            durability = 100,
            maxDurability = 100,
            bound = true,
        )

        val nodes = listOf(
            "hollowreach", "emberreach", "cindervale", "stormreach", "frostreach", "dawnreach",
        ).flatMap { region ->
            Nodes.inRegion(region).map(::nodeFrom)
        }

        var s = GameState(
            saveId = "sv_" + java.util.UUID.randomUUID().toString().substring(0, 8),
            character = com.projecteternal.model.Character(
                name = name.ifBlank { "Wayfarer" },
                marks = 50,
                health = 100,
                equipped = mapOf(com.projecteternal.model.EquipSlot.TOOL to pickaxe),
            ),
            equipmentItems = listOf(pickaxe),
            inventory = listOf(
                com.projecteternal.model.ItemStack("potion_lesser", 2),
                com.projecteternal.model.ItemStack("shard_resonance", 3),
                com.projecteternal.model.ItemStack("repair_kit", 1),
                com.projecteternal.model.ItemStack("oil_preservation", 1),
            ),
            retainers = listOf(starterRetainer()),
            nodes = nodes,
            unlocks = DEFAULT_UNLOCKS,
            rngSeed = now xor name.hashCode().toLong() xor 0x9E3779B97F4A7C1L,
            createdEpochMs = now,
            lastSavedEpochMs = now,
        )
        s = s.acceptQuests(listOf("q_intro"), now)
        return s
    }
}
