package com.projecteternal.content

import com.projecteternal.model.ItemStack
import com.projecteternal.model.Objective
import com.projecteternal.model.ObjectiveType
import com.projecteternal.model.QuestId

/**
 * Static quest catalog. The Hollowreach main line implements the §16 example
 * chain: mine iron -> craft sword -> hunt wolves -> discover mine -> defeat
 * guardian -> unlock new region.
 *
 * Gating model: a quest's `prerequisites` are satisfied when every entry is
 * either a completed quest id or a currently-held unlock token. When the
 * quest is `autoAccept`, it activates as soon as prereqs are met.
 */
object Quests {
    private val all: Map<QuestId, QuestDefinition> = listOf(

        QuestDefinition(
            id = "q_intro",
            name = "A Wayfarer's First Step",
            description = "Equip your pickaxe and pull the first ore from Rustrock Quarry.",
            category = QuestCategory.MAIN,
            objectives = listOf(
                Objective("o1", ObjectiveType.EQUIP_SLOT, targetId = "TOOL", description = "Equip a tool"),
                Objective("o2", ObjectiveType.GATHER, targetId = "ore_iron", targetCount = 3, description = "Gather Iron Ore"),
            ),
            reward = QuestReward(marks = 25, charXp = 15),
            autoAccept = true,
        ),

        QuestDefinition(
            id = "q_first_ore",
            name = "A Miner's Share",
            description = "Ten iron ore marks a real day's work.",
            category = QuestCategory.MAIN,
            prerequisites = listOf("q_intro"),
            objectives = listOf(
                Objective("o1", ObjectiveType.GATHER, targetId = "ore_iron", targetCount = 10, description = "Gather Iron Ore"),
            ),
            reward = QuestReward(
                marks = 40, charXp = 40,
                unlocks = listOf("recipe:sword_bronze", "screen:crafting"),
            ),
            autoAccept = true,
        ),

        QuestDefinition(
            id = "q_first_sword",
            name = "Forge Your Blade",
            description = "Smelt ingots, shape a Bronze Sword and equip it.",
            category = QuestCategory.MAIN,
            prerequisites = listOf("q_first_ore"),
            objectives = listOf(
                Objective("o1", ObjectiveType.CRAFT, targetId = "sword_bronze", targetCount = 1, description = "Craft a Bronze Sword"),
                Objective("o2", ObjectiveType.EQUIP_SLOT, targetId = "MAIN_WEAPON", description = "Equip a main weapon"),
            ),
            reward = QuestReward(
                marks = 60, charXp = 60,
                unlocks = listOf("quest:hunt", "screen:enhance"),
            ),
            autoAccept = true,
        ),

        QuestDefinition(
            id = "q_hunt_wolves",
            name = "Thinning the Pack",
            description = "Eight grey wolves. The woods will notice.",
            category = QuestCategory.COMBAT,
            prerequisites = listOf("quest:hunt"),
            objectives = listOf(
                Objective("o1", ObjectiveType.KILL, targetId = "wolf", targetCount = 8, description = "Slay Grey Wolves"),
            ),
            reward = QuestReward(
                marks = 80, charXp = 120,
                unlocks = listOf("quest:deep_mine"),
            ),
            autoAccept = true,
        ),

        QuestDefinition(
            id = "q_deep_mine",
            name = "The Deep Quartz Grotto",
            description = "An old map points to a deeper seam. Find it.",
            category = QuestCategory.DISCOVERY,
            prerequisites = listOf("quest:deep_mine"),
            objectives = listOf(
                Objective("o1", ObjectiveType.DISCOVER_NODE, targetId = "node_deep_quarry", description = "Discover the Deep Quartz Grotto"),
            ),
            reward = QuestReward(
                marks = 60, charXp = 80,
                unlocks = listOf("quest:guardian", "screen:workers"),
            ),
            autoAccept = true,
        ),

        QuestDefinition(
            id = "q_guardian",
            name = "The Sundered Ruin",
            description = "Slay the Hollowreach Guardian and open the road to Emberreach.",
            category = QuestCategory.MAIN,
            prerequisites = listOf("quest:guardian"),
            objectives = listOf(
                Objective("o1", ObjectiveType.KILL, targetId = "guardian_golem", targetCount = 1, description = "Slay the Guardian"),
            ),
            reward = QuestReward(
                marks = 500, charXp = 400,
                items = listOf(ItemStack("shard_resonance", 5)),
                unlocks = listOf("region:emberreach"),
            ),
            autoAccept = true,
        ),

        // ---- side quests (player-accepted manually) ----
        QuestDefinition(
            id = "q_side_armor",
            name = "Dressed for the Woods",
            description = "Stitch a Hide Armor and wear it with pride.",
            category = QuestCategory.LIFESKILL,
            objectives = listOf(
                Objective("o1", ObjectiveType.CRAFT, targetId = "armor_hide", targetCount = 1, description = "Craft Hide Armor"),
                Objective("o2", ObjectiveType.EQUIP_SLOT, targetId = "CHEST", description = "Equip chest armor"),
            ),
            reward = QuestReward(marks = 55, charXp = 50, items = listOf(ItemStack("potion_lesser", 2))),
        ),

        QuestDefinition(
            id = "q_side_boar",
            name = "Boar Appreciation Society",
            description = "Prove your respect with five bristle boar hides.",
            category = QuestCategory.COMBAT,
            objectives = listOf(
                Objective("o1", ObjectiveType.KILL, targetId = "boar", targetCount = 5, description = "Slay Bristle Boars"),
            ),
            reward = QuestReward(marks = 120, charXp = 80, items = listOf(ItemStack("roasted_meat", 3))),
        ),

        QuestDefinition(
            id = "q_side_forger",
            name = "An Apprentice's Anvil",
            description = "Earn level 4 in Blacksmithing. The village smith is watching.",
            category = QuestCategory.LIFESKILL,
            objectives = listOf(
                Objective("o1", ObjectiveType.REACH_SKILL_LEVEL, targetId = "blacksmithing", targetCount = 4, description = "Reach Blacksmithing 4"),
            ),
            reward = QuestReward(marks = 200, charXp = 100, unlocks = listOf("recipe:tool_pickaxe")),
        ),

        // ---- life-skill chain: farm, mill, bake, fish ----
        QuestDefinition(
            id = "q_side_harvest",
            name = "The First Harvest",
            description = "The terraces by the river are in. Bring in the grain and turn it to flour.",
            category = QuestCategory.LIFESKILL,
            prerequisites = listOf("q_intro"),
            objectives = listOf(
                Objective("o1", ObjectiveType.GATHER, targetId = "grain", targetCount = 8, description = "Gather Grain"),
                Objective("o2", ObjectiveType.CRAFT, targetId = "flour", targetCount = 4, description = "Mill Grain to Flour"),
            ),
            reward = QuestReward(
                marks = 90, charXp = 60,
                items = listOf(ItemStack("bread", 2)),
                unlocks = listOf("recipe:bake_bread"),
            ),
        ),

        QuestDefinition(
            id = "q_side_fisher",
            name = "What the River Keeps",
            description = "Six river carp proves your patience. A rod of your own is the next step.",
            category = QuestCategory.LIFESKILL,
            prerequisites = listOf("q_intro"),
            objectives = listOf(
                Objective("o1", ObjectiveType.GATHER, targetId = "fish_carp", targetCount = 6, description = "Gather River Carp"),
            ),
            reward = QuestReward(
                marks = 80, charXp = 60,
                items = listOf(ItemStack("dried_fish", 3)),
                unlocks = listOf("recipe:craft_fishing_rod"),
            ),
        ),

        // ---- Emberreach regional chain into Stormreach ----
        QuestDefinition(
            id = "q_emberreach_ash",
            name = "The Ash-Spawned",
            description = "Ten brutes roam the highland ash. Thin their numbers — the road beyond wants clearing.",
            category = QuestCategory.REGIONAL,
            prerequisites = listOf("region:emberreach"),
            objectives = listOf(
                Objective("o1", ObjectiveType.KILL, targetId = "ash_beast", targetCount = 10, description = "Slay Ash-Spawned Brutes"),
            ),
            reward = QuestReward(
                marks = 350, charXp = 300,
                items = listOf(ItemStack("ward_of_stability", 1)),
                unlocks = listOf("region:stormreach"),
            ),
            autoAccept = true,
        ),

        QuestDefinition(
            id = "q_stormreach_herald",
            name = "Riding the Squall",
            description = "Something old rides the storms of Stormreach. It must not reach the passes.",
            category = QuestCategory.REGIONAL,
            prerequisites = listOf("region:stormreach"),
            objectives = listOf(
                Objective("o1", ObjectiveType.KILL, targetId = "storm_herald", targetCount = 1, description = "Slay the Storm Herald"),
            ),
            reward = QuestReward(
                marks = 900, charXp = 600,
                items = listOf(ItemStack("core_storm", 2)),
                unlocks = listOf("recipe:shape_ward", "recipe:refine_catalyst"),
            ),
            autoAccept = true,
        ),

        QuestDefinition(
            id = "q_light_beyond",
            name = "The Light Beyond the Veil",
            description = "Take a weapon past what mortal metal can hold — and hold the stilled tempest that distills it.",
            category = QuestCategory.REGIONAL,
            prerequisites = listOf("q_stormreach_herald", "recipe:refine_catalyst"),
            objectives = listOf(
                Objective("o1", ObjectiveType.ENHANCE_ANY_TO, targetCount = 31, description = "Reach a +31 enhancement"),
                Objective("o2", ObjectiveType.HAVE_ITEMS, targetId = "catalyst_storm", targetCount = 1, description = "Hold a Stormbound Catalyst"),
            ),
            reward = QuestReward(
                marks = 1500, charXp = 900,
                items = listOf(ItemStack("core_storm", 3)),
                unlocks = listOf("region:dawnreach"),
            ),
            autoAccept = true,
        ),

        // ================= PACK 01 — OUTER REACHES =================

        // ---- Cindervale entry (no circular gate) ----
        QuestDefinition(
            id = "q_cindervale",
            name = "The Glass Country",
            description = "Prove your forge can hold an alloy, and the high road to the ash country opens.",
            category = QuestCategory.MAIN,
            prerequisites = listOf("q_emberreach_ash"),
            objectives = listOf(
                Objective("o1", ObjectiveType.HAVE_ITEMS, targetId = "steel", targetCount = 1, description = "Hold a bar of Steel"),
            ),
            reward = QuestReward(
                marks = 200, charXp = 150,
                unlocks = listOf("region:cindervale"),
            ),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_cindervale_market",
            name = "Ashveil Market",
            description = "Find the market on the high road and learn the ash country's ways.",
            category = QuestCategory.REGIONAL,
            prerequisites = listOf("region:cindervale"),
            objectives = listOf(
                Objective("o1", ObjectiveType.DISCOVER_NODE, targetId = "node_ashveil_market", description = "Discover Ashveil Market"),
            ),
            reward = QuestReward(
                marks = 100, charXp = 80,
                unlocks = listOf("recipe:cinder_gear"),
            ),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_cinder_steel",
            name = "The Warm Furnace",
            description = "Forge Cinder Steel — the backbone of the ash country's craft.",
            category = QuestCategory.LIFESKILL,
            prerequisites = listOf("region:cindervale"),
            objectives = listOf(
                Objective("o1", ObjectiveType.CRAFT, targetId = "cinder_steel", targetCount = 2, description = "Forge Cinder Steel"),
            ),
            reward = QuestReward(
                marks = 150, charXp = 120,
                unlocks = listOf("recipe:sword_cinder"),
            ),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_cinder_king",
            name = "The Ash Sovereign",
            description = "End the thing that guards the Glass-Pass, and the glass country gives up its secrets.",
            category = QuestCategory.MAIN,
            prerequisites = listOf("region:cindervale", "recipe:cinder_gear"),
            objectives = listOf(
                Objective("o1", ObjectiveType.KILL, targetId = "ash_sovereign", targetCount = 1, description = "Slay the Ash Sovereign"),
            ),
            reward = QuestReward(
                marks = 400, charXp = 300,
                items = listOf(ItemStack("cinder_core", 1)),
                unlocks = listOf("recipe:glasswork", "recipe:jewelry"),
            ),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_cinder_regional",
            name = "Walking the Ashes",
            description = "Learn every corner of Cindervale.",
            category = QuestCategory.REGIONAL,
            prerequisites = listOf("region:cindervale"),
            objectives = listOf(
                Objective("o1", ObjectiveType.DISCOVER_NODE, targetId = "node_cinder_quarry", description = "Discover Cinderlode Quarry"),
                Objective("o2", ObjectiveType.DISCOVER_NODE, targetId = "node_sootbark_woods", description = "Discover Sootbark Woods"),
                Objective("o3", ObjectiveType.DISCOVER_NODE, targetId = "node_sunbaked_shallows", description = "Discover Sunbaked Shallows"),
                Objective("o4", ObjectiveType.DISCOVER_NODE, targetId = "node_cinder_terraces", description = "Discover Cindervale Terraces"),
            ),
            reward = QuestReward(marks = 120, charXp = 100),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_cinder_armor",
            name = "Dressed in Ash",
            description = "Stitch the Ashbark gloves and boots and wear them with pride.",
            category = QuestCategory.LIFESKILL,
            prerequisites = listOf("recipe:cinder_gear"),
            objectives = listOf(
                Objective("o1", ObjectiveType.CRAFT, targetId = "gloves_ashbark", targetCount = 1, description = "Craft Ashbark Gloves"),
                Objective("o2", ObjectiveType.CRAFT, targetId = "boots_ashbark", targetCount = 1, description = "Craft Ashbark Boots"),
            ),
            reward = QuestReward(marks = 150, charXp = 120),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_glazer",
            name = "The Glasswright",
            description = "Vitrify emberglass and master the glass country's trade.",
            category = QuestCategory.LIFESKILL,
            prerequisites = listOf("recipe:glasswork"),
            objectives = listOf(
                Objective("o1", ObjectiveType.CRAFT, targetId = "glass_shard", targetCount = 10, description = "Vitrify Glass Shards"),
            ),
            reward = QuestReward(marks = 120, charXp = 100),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_jeweler",
            name = "A Ring of Embers",
            description = "Set the Ember Ring and learn the jeweler's craft.",
            category = QuestCategory.LIFESKILL,
            prerequisites = listOf("recipe:jewelry"),
            objectives = listOf(
                Objective("o1", ObjectiveType.CRAFT, targetId = "ring_ember", targetCount = 1, description = "Craft an Ember Ring"),
            ),
            reward = QuestReward(marks = 150, charXp = 120),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_advance_begin",
            name = "Kindling",
            description = "Push a weapon past +5. The ember catches.",
            category = QuestCategory.LIFESKILL,
            prerequisites = listOf("screen:enhance", "region:cindervale"),
            objectives = listOf(
                Objective("o1", ObjectiveType.ENHANCE_ANY_TO, targetCount = 5, description = "Reach a +5 enhancement"),
            ),
            reward = QuestReward(
                marks = 150, charXp = 120,
                items = listOf(ItemStack("shard_resonance", 5)),
            ),
            autoAccept = true,
        ),

        // ---- Stormreach expedition chain ----
        QuestDefinition(
            id = "q_stormreach_expeditions",
            name = "Into the Squall",
            description = "Map the storm country's new seams.",
            category = QuestCategory.REGIONAL,
            prerequisites = listOf("region:stormreach"),
            objectives = listOf(
                Objective("o1", ObjectiveType.DISCOVER_NODE, targetId = "node_storm_quarry", description = "Discover Stormscale Quarry"),
                Objective("o2", ObjectiveType.DISCOVER_NODE, targetId = "node_thunderwood", description = "Discover Thunderwood"),
                Objective("o3", ObjectiveType.DISCOVER_NODE, targetId = "node_storm_field", description = "Discover Stormfield"),
                Objective("o4", ObjectiveType.DISCOVER_NODE, targetId = "node_deepstorm_spire", description = "Discover Deepstorm Spire"),
            ),
            reward = QuestReward(
                marks = 250, charXp = 180,
                items = listOf(ItemStack("core_storm", 1)),
            ),
            autoAccept = true,
        ),

        // ---- Frostreach entry (no circular gate) ----
        QuestDefinition(
            id = "q_frostreach",
            name = "The High Fells",
            description = "Master the Cindervale forge and hold Cinder Steel — the high road to Frostreach opens.",
            category = QuestCategory.MAIN,
            prerequisites = listOf("q_stormreach_herald"),
            objectives = listOf(
                Objective("o1", ObjectiveType.HAVE_ITEMS, targetId = "cinder_steel", targetCount = 2, description = "Hold Cinder Steel"),
            ),
            reward = QuestReward(
                marks = 300, charXp = 220,
                unlocks = listOf("region:frostreach"),
            ),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_frostvein",
            name = "The Cold Vein",
            description = "Frostvein grows in the ice. Gather enough to feed the forge.",
            category = QuestCategory.REGIONAL,
            prerequisites = listOf("region:frostreach"),
            objectives = listOf(
                Objective("o1", ObjectiveType.GATHER, targetId = "frostvein", targetCount = 10, description = "Gather Frostvein"),
            ),
            reward = QuestReward(
                marks = 250, charXp = 200,
                items = listOf(ItemStack("shard_resonance", 10)),
            ),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_frost_warden",
            name = "The Glacier Warden",
            description = "Wake the old thing under the ice and put it back down. The foundry learns.",
            category = QuestCategory.MAIN,
            prerequisites = listOf("region:frostreach"),
            objectives = listOf(
                Objective("o1", ObjectiveType.KILL, targetId = "glacier_warden", targetCount = 1, description = "Slay the Glacier Warden"),
            ),
            reward = QuestReward(
                marks = 700, charXp = 500,
                items = listOf(ItemStack("frostvein", 3)),
                unlocks = listOf("recipe:frostforge"),
            ),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_herbalist_highland",
            name = "The Tundra Herbalist",
            description = "The highland tundra is a herbalist's first true home. Gather Frostmoss.",
            category = QuestCategory.LIFESKILL,
            prerequisites = listOf("region:frostreach"),
            objectives = listOf(
                Objective("o1", ObjectiveType.GATHER, targetId = "frostmoss", targetCount = 15, description = "Gather Frostmoss"),
            ),
            reward = QuestReward(marks = 150, charXp = 120),
            autoAccept = true,
        ),

        // ---- Enhancement checkpoints ----
        QuestDefinition(
            id = "q_frostvein_enhance",
            name = "The Frostvein Forge",
            description = "Cross into the ADVANCED band — reach +16.",
            category = QuestCategory.REGIONAL,
            prerequisites = listOf("region:frostreach"),
            objectives = listOf(
                Objective("o1", ObjectiveType.ENHANCE_ANY_TO, targetCount = 16, description = "Reach a +16 enhancement"),
            ),
            reward = QuestReward(
                marks = 300, charXp = 250,
                items = listOf(ItemStack("frostvein", 5)),
            ),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_advance_mid",
            name = "Steel Resolve",
            description = "Push past +20. The foundry respects the climb.",
            category = QuestCategory.REGIONAL,
            prerequisites = listOf("q_frostvein_enhance"),
            objectives = listOf(
                Objective("o1", ObjectiveType.ENHANCE_ANY_TO, targetCount = 20, description = "Reach a +20 enhancement"),
            ),
            reward = QuestReward(
                marks = 400, charXp = 300,
                items = listOf(ItemStack("ward_frost", 1)),
            ),
            autoAccept = true,
        ),
        QuestDefinition(
            id = "q_advance_near",
            name = "Threshold",
            description = "Climb to +30. The transcendent gate stands just beyond.",
            category = QuestCategory.REGIONAL,
            prerequisites = listOf("q_advance_mid"),
            objectives = listOf(
                Objective("o1", ObjectiveType.ENHANCE_ANY_TO, targetCount = 30, description = "Reach a +30 enhancement"),
            ),
            reward = QuestReward(
                marks = 600, charXp = 450,
                items = listOf(ItemStack("core_storm", 2)),
            ),
            autoAccept = true,
        ),
    ).associateBy { it.id }

    fun get(id: QuestId): QuestDefinition = all[id]
        ?: error("Unknown quest def '$id' (content gap)")

    /** Quests whose prereqs are satisfied by completed quests + held unlocks. */
    fun available(completedQuestIds: Set<String>, unlocks: Set<String>): List<QuestDefinition> {
        fun sat(entry: String): Boolean =
            completedQuestIds.contains(entry) || unlocks.contains(entry)
        return all.values.filter { q -> q.prerequisites.all(::sat) }
    }
}