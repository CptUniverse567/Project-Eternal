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