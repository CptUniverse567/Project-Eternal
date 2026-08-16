package com.projecteternal.content

import com.projecteternal.model.NodeId
import com.projecteternal.model.NodeType
import com.projecteternal.model.RegionId

/** Static regions (Reaches) catalog. */
object Regions {
    private val all: Map<RegionId, RegionDefinition> = listOf(
        RegionDefinition(
            id = "hollowreach", name = "Hollowreach", tier = 1,
            description = "A sheltered valley. Your home and first proving ground.",
            priceModifier = 1.0,
        ),
        RegionDefinition(
            id = "emberreach", name = "Emberreach", tier = 2,
            description = "A smouldering highland Reach. Guarded by the Hollowreach Guardian.",
            unlockToken = "region:emberreach",
            priceModifier = 1.5,
        ),
        RegionDefinition(
            id = "stormreach", name = "Stormreach", tier = 3,
            description = "Weather-wracked coastlands. Storms churn soil and sea alike.",
            unlockToken = "region:stormreach",
            priceModifier = 2.0,
            yieldMultiplier = 1.5,
        ),
        RegionDefinition(
            id = "dawnreach", name = "Dawnreach", tier = 4,
            description = "A high pass above the storm-lines, where the sky is clear enough to burn.",
            unlockToken = "region:dawnreach",
            priceModifier = 2.5,
            yieldMultiplier = 1.6,
        ),
    ).associateBy { it.id }

    fun get(id: RegionId): RegionDefinition = all[id]
        ?: error("Unknown region def '$id' (content gap)")
}

/** Static node catalog. */
object Nodes {
    private val all: Map<NodeId, NodeDefinition> = listOf(
        NodeDefinition(
            id = "node_quarry", regionId = "hollowreach", type = NodeType.MINE, tier = 1,
            name = "Rustrock Quarry",
            unlockToken = "",
            skills = listOf("mining"),
            xpPerAction = 5,
            actionsPerHour = 120.0,
            yields = listOf(
                YieldEntry("ore_iron", chancePercent = 78),
                YieldEntry("shard_resonance", chancePercent = 12),
                YieldEntry("herb_brightleaf", chancePercent = 10),
            ),
            description = "A crumbling quarry where iron veins poke through grey rock.",
        ),
        NodeDefinition(
            id = "node_wolf_woods", regionId = "hollowreach", type = NodeType.FOREST, tier = 1,
            name = "Wolf's Woods",
            unlockToken = "",
            skills = listOf("logging", "herbalism"),
            xpPerAction = 4,
            actionsPerHour = 100.0,
            yields = listOf(
                YieldEntry("wood", chancePercent = 70),
                YieldEntry("herb_brightleaf", chancePercent = 30),
            ),
            description = "Old-growth woods that creak and growl at dusk.",
        ),
        NodeDefinition(
            id = "node_farmlands", regionId = "hollowreach", type = NodeType.FARM, tier = 1,
            name = "Riverfield Terraces",
            unlockToken = "",
            skills = listOf("farming"),
            xpPerAction = 4,
            actionsPerHour = 100.0,
            yields = listOf(
                YieldEntry("grain", chancePercent = 70),
                YieldEntry("herb_brightleaf", chancePercent = 20),
                YieldEntry("fish_carp", chancePercent = 10),
            ),
            description = "Stepped fields fed by a winding irrigation ditch.",
        ),
        NodeDefinition(
            id = "node_river_fishery", regionId = "hollowreach", type = NodeType.FISHERY, tier = 1,
            name = "Brindlebrook Shallows",
            unlockToken = "",
            skills = listOf("fishing"),
            xpPerAction = 4,
            actionsPerHour = 95.0,
            yields = listOf(
                YieldEntry("fish_carp", chancePercent = 75),
                YieldEntry("shard_resonance", chancePercent = 10),
                YieldEntry("herb_brightleaf", chancePercent = 15),
            ),
            description = "Clear shallows where carp dart between sunlit stones.",
        ),
        NodeDefinition(
            id = "node_deep_quarry", regionId = "hollowreach", type = NodeType.MINE, tier = 2,
            name = "Deep Quartz Grotto",
            unlockToken = "quest:deep_mine",
            skills = listOf("mining"),
            xpPerAction = 14,
            actionsPerHour = 90.0,
            yields = listOf(
                YieldEntry("ore_iron", chancePercent = 50),
                YieldEntry("ore_coal", chancePercent = 45),
                YieldEntry("shard_resonance", chancePercent = 5),
            ),
            description = "A deeper, colder seam. Coal glitters in the dark.",
        ),
        NodeDefinition(
            id = "node_guardian_ruin", regionId = "hollowreach", type = NodeType.RUIN, tier = 2,
            name = "The Sundered Ruin",
            unlockToken = "quest:guardian",
            skills = listOf("mining"),
            xpPerAction = 0,
            actionsPerHour = 0.0,
            yields = emptyList(),
            description = "An old gatehouse. Something heavy walks the perimeter.",
        ),
        NodeDefinition(
            id = "node_emberreach_mine", regionId = "emberreach", type = NodeType.MINE, tier = 3,
            name = "Emberreach Forge-Seam",
            unlockToken = "region:emberreach",
            skills = listOf("mining"),
            xpPerAction = 40,
            actionsPerHour = 80.0,
            yields = listOf(
                YieldEntry("ore_coal", chancePercent = 60),
                YieldEntry("crystal_advanced", chancePercent = 25),
                YieldEntry("ore_iron", chancePercent = 15),
            ),
            description = "A molten seam. Voidforged crystals grow in the heat.",
        ),
        NodeDefinition(
            id = "node_village", regionId = "hollowreach", type = NodeType.CITY, tier = 1,
            name = "Hollowreach Village",
            unlockToken = "",
            skills = listOf(),
            xpPerAction = 0,
            actionsPerHour = 0.0,
            yields = emptyList(),
            description = "Market, smithy and rumours.",
        ),
        NodeDefinition(
            id = "node_emberreach_city", regionId = "emberreach", type = NodeType.TRADE_HUB, tier = 2,
            name = "Emberhold Market",
            unlockToken = "region:emberreach",
            skills = listOf(),
            xpPerAction = 0,
            actionsPerHour = 0.0,
            yields = emptyList(),
            description = "A trading hub on the high road.",
        ),
        NodeDefinition(
            id = "node_emberreach_forest", regionId = "emberreach", type = NodeType.FOREST, tier = 3,
            name = "Scorchpine Grove",
            unlockToken = "region:emberreach",
            skills = listOf("logging", "farming"),
            xpPerAction = 30,
            actionsPerHour = 90.0,
            yields = listOf(
                YieldEntry("wood", chancePercent = 45),
                YieldEntry("grain", chancePercent = 25),
                YieldEntry("crystal_advanced", chancePercent = 30),
            ),
            description = "Blackened pines growing in ash-dark soil that burns rich.",
        ),
        NodeDefinition(
            id = "node_stormreach_coast", regionId = "stormreach", type = NodeType.FISHERY, tier = 4,
            name = "Roaring Coast",
            unlockToken = "region:stormreach",
            skills = listOf("fishing"),
            xpPerAction = 70,
            actionsPerHour = 75.0,
            yields = listOf(
                YieldEntry("core_storm", chancePercent = 25),
                YieldEntry("fish_carp", chancePercent = 40),
                YieldEntry("shard_resonance", chancePercent = 20),
                YieldEntry("crystal_advanced", chancePercent = 15),
            ),
            description = "Storm-fed waters. Even the fish wear lightning.",
        ),
        NodeDefinition(
            id = "node_dawnreach_spire", regionId = "dawnreach", type = NodeType.SPECIAL, tier = 4,
            name = "Skybreak Spire",
            unlockToken = "region:dawnreach",
            skills = listOf("mining"),
            xpPerAction = 90,
            actionsPerHour = 70.0,
            yields = listOf(
                YieldEntry("core_storm", chancePercent = 35),
                YieldEntry("crystal_advanced", chancePercent = 30),
                YieldEntry("shard_resonance", chancePercent = 35),
            ),
            description = "A needle of rock above the storm-lines, feeding the catalyst stiller.",
        ),
        NodeDefinition(
            id = "node_dawnreach_abyss", regionId = "dawnreach", type = NodeType.RUIN, tier = 4,
            name = "The Starfall Abyss",
            unlockToken = "region:dawnreach",
            skills = listOf("mining"),
            xpPerAction = 0,
            actionsPerHour = 0.0,
            yields = emptyList(),
            description = "A wound in the ridge. Something fell here, and it is not done falling.",
        ),
    ).associateBy { it.id }

    fun get(id: NodeId): NodeDefinition = all[id]
        ?: error("Unknown node def '$id' (content gap)")

    fun inRegion(regionId: RegionId): List<NodeDefinition> =
        all.values.filter { it.regionId == regionId }.sortedBy { it.tier }
}
