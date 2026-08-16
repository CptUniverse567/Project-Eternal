package com.projecteternal.content

import com.projecteternal.model.ItemStack
import com.projecteternal.model.RecipeId

/**
 * Static processing/crafting recipe catalog. Processing recipes use the
 * PROCESSING skills; crafting recipes use the CRAFTING skills.
 */
object Recipes {
    private val all: Map<RecipeId, RecipeDefinition> = listOf(
        // ---- processing: ore -> fragments -> ingots -> alloys ----
        RecipeDefinition(
            id = "grind_iron", name = "Grind Iron Ore", skillId = "grinding",
            timeSeconds = 3,
            inputs = listOf(ItemStack("ore_iron", 1)),
            outputs = listOf(ItemStack("iron_fragment", 2)),
            xpPerCraft = 5,
            description = "Crush ore into fragments.",
        ),
        RecipeDefinition(
            id = "smelt_ingot", name = "Smelt Iron Ingot", skillId = "smelting",
            timeSeconds = 5,
            inputs = listOf(ItemStack("iron_fragment", 2)),
            outputs = listOf(ItemStack("iron_ingot", 1)),
            xpPerCraft = 7,
            description = "Fragments into an ingot.",
        ),
        RecipeDefinition(
            id = "refine_refined", name = "Refine Iron", skillId = "refining", skillLevelRequired = 1,
            timeSeconds = 8,
            inputs = listOf(ItemStack("iron_ingot", 2)),
            outputs = listOf(ItemStack("refined_iron", 1)),
            xpPerCraft = 18,
            description = "High-purity iron for advanced work.",
        ),
        RecipeDefinition(
            id = "refine_steel", name = "Forge Steel", skillId = "refining", skillLevelRequired = 2,
            timeSeconds = 12,
            inputs = listOf(ItemStack("refined_iron", 2), ItemStack("ore_coal", 1)),
            outputs = listOf(ItemStack("steel", 1)),
            xpPerCraft = 40,
            description = "Iron, coal and patience.",
        ),
        RecipeDefinition(
            id = "refine_catalyst", name = "Distill Stormbound Catalyst", skillId = "refining", skillLevelRequired = 5,
            timeSeconds = 20,
            inputs = listOf(ItemStack("core_storm", 1), ItemStack("crystal_advanced", 2)),
            outputs = listOf(ItemStack("catalyst_storm", 1)),
            xpPerCraft = 90,
            unlockToken = "recipe:refine_catalyst",
            description = "A stormheart core, stilled in voidforged crystal. The key to transcending.",
        ),
        RecipeDefinition(
            id = "heat_lumber", name = "Dry Lumber", skillId = "heating",
            timeSeconds = 3,
            inputs = listOf(ItemStack("wood", 1)),
            outputs = listOf(ItemStack("lumber", 1)),
            xpPerCraft = 4,
            description = "Seasoned, straight planks.",
        ),
        // ---- processing: harvest -> provisions ----
        RecipeDefinition(
            id = "dry_fish", name = "Dry River Carp", skillId = "drying",
            timeSeconds = 4,
            inputs = listOf(ItemStack("fish_carp", 1)),
            outputs = listOf(ItemStack("dried_fish", 1)),
            xpPerCraft = 6,
            description = "Salt, air and time make the catch last the winter.",
        ),
        RecipeDefinition(
            id = "mill_flour", name = "Mill Grain to Flour", skillId = "milling",
            timeSeconds = 3,
            inputs = listOf(ItemStack("grain", 1)),
            outputs = listOf(ItemStack("flour", 1)),
            xpPerCraft = 5,
            description = "The terraces give grain; the wheel gives flour.",
        ),
        // ---- crafting: tools & gear ----
        RecipeDefinition(
            id = "craft_rope", name = "Twist Hemp Rope", skillId = "carpentry",
            timeSeconds = 3,
            inputs = listOf(ItemStack("wood", 1)),
            outputs = listOf(ItemStack("rope", 1)),
            xpPerCraft = 5,
            description = "Twist, braid and pull until it holds.",
        ),
        RecipeDefinition(
            id = "craft_fishing_rod", name = "Build Stout Fishing Rod", skillId = "carpentry",
            skillLevelRequired = 1, timeSeconds = 6,
            inputs = listOf(ItemStack("wood", 1), ItemStack("rope", 1)),
            outputs = listOf(ItemStack("tool_fishing_rod", 1)),
            xpPerCraft = 10,
            unlockToken = "recipe:craft_fishing_rod",
            description = "A rod with spine enough for the river's best.",
        ),
        RecipeDefinition(
            id = "craft_shield_oak", name = "Band the Oak Shield", skillId = "carpentry",
            skillLevelRequired = 2, timeSeconds = 8,
            inputs = listOf(ItemStack("lumber", 2), ItemStack("rope", 1)),
            outputs = listOf(ItemStack("shield_oak", 1)),
            xpPerCraft = 14,
            unlockToken = "recipe:craft_shield_oak",
            description = "Oak and rope, fitted to your arm.",
        ),
        RecipeDefinition(
            id = "craft_machinery_part", name = "Fabricate Machinery Part", skillId = "engineering",
            skillLevelRequired = 1, timeSeconds = 10,
            inputs = listOf(ItemStack("steel", 1), ItemStack("lumber", 1)),
            outputs = listOf(ItemStack("machinery_part", 1)),
            xpPerCraft = 30,
            description = "Precision tolerances, hold a breath and strike.",
        ),
        RecipeDefinition(
            id = "shape_ward", name = "Shape Ward of Stability", skillId = "engineering",
            skillLevelRequired = 2, timeSeconds = 12,
            inputs = listOf(ItemStack("crystal_advanced", 1), ItemStack("steel", 1)),
            outputs = listOf(ItemStack("ward_of_stability", 1)),
            xpPerCraft = 45,
            unlockToken = "recipe:shape_ward",
            description = "Voidforged crystal bound in steel, stilling chaos itself.",
        ),
        // ---- crafting: consumables ----
        RecipeDefinition(
            id = "cook_roast", name = "Roast Wolf Meat", skillId = "cooking",
            timeSeconds = 3,
            inputs = listOf(ItemStack("meat_wolf", 1)),
            outputs = listOf(ItemStack("roasted_meat", 1)),
            xpPerCraft = 4,
            description = "Tastes like victory, slightly gamey.",
        ),
        RecipeDefinition(
            id = "bake_bread", name = "Bake Bread", skillId = "cooking",
            timeSeconds = 5,
            inputs = listOf(ItemStack("flour", 1)),
            outputs = listOf(ItemStack("bread", 1)),
            xpPerCraft = 8,
            description = "Flour, water and the patience of stone ovens.",
        ),
        // ---- crafting: weapons & armor ----
        RecipeDefinition(
            id = "craft_sword_bronze", name = "Forge Bronze Sword", skillId = "blacksmithing",
            timeSeconds = 6,
            inputs = listOf(ItemStack("iron_ingot", 2), ItemStack("wood", 1)),
            outputs = listOf(ItemStack("sword_bronze", 1)),
            xpPerCraft = 12,
            unlockToken = "recipe:sword_bronze",
            description = "Your first blade.",
        ),
        RecipeDefinition(
            id = "craft_sword_steel", name = "Forge Steel Sword", skillId = "blacksmithing",
            skillLevelRequired = 4, timeSeconds = 15,
            inputs = listOf(ItemStack("steel", 2), ItemStack("lumber", 1)),
            outputs = listOf(ItemStack("sword_steel", 1)),
            xpPerCraft = 60,
            unlockToken = "recipe:sword_steel",
            description = "A real soldier's weapon.",
        ),
        RecipeDefinition(
            id = "craft_armor_hide", name = "Stitch Hide Armor", skillId = "armorcraft",
            timeSeconds = 5,
            inputs = listOf(ItemStack("pelt_wolf", 3), ItemStack("wood", 1)),
            outputs = listOf(ItemStack("armor_hide", 1)),
            xpPerCraft = 10,
            description = "Fur and lumber, held together by willpower.",
        ),
        RecipeDefinition(
            id = "craft_helmet_hide", name = "Stitch Hide Hood", skillId = "armorcraft",
            timeSeconds = 4,
            inputs = listOf(ItemStack("pelt_wolf", 2)),
            outputs = listOf(ItemStack("helmet_hide", 1)),
            xpPerCraft = 8,
            description = "Keeps the rain out of your ears.",
        ),
        RecipeDefinition(
            id = "craft_pickaxe", name = "Forge Journeyman's Pickaxe", skillId = "blacksmithing",
            skillLevelRequired = 1, timeSeconds = 8,
            inputs = listOf(ItemStack("iron_ingot", 2), ItemStack("wood", 2)),
            outputs = listOf(ItemStack("tool_pickaxe", 1)),
            xpPerCraft = 14,
            description = "A proper mining tool.",
        ),
        // ---- crafting: consumables ----
        RecipeDefinition(
            id = "cook_roast", name = "Roast Wolf Meat", skillId = "cooking",
            timeSeconds = 3,
            inputs = listOf(ItemStack("meat_wolf", 1)),
            outputs = listOf(ItemStack("roasted_meat", 1)),
            xpPerCraft = 4,
            description = "Tastes like victory, slightly gamey.",
        ),
        RecipeDefinition(
            id = "brew_potion", name = "Brew Lesser Vitality Draft", skillId = "alchemy",
            timeSeconds = 5,
            inputs = listOf(ItemStack("herb_brightleaf", 2)),
            outputs = listOf(ItemStack("potion_lesser", 1)),
            xpPerCraft = 6,
            description = "Brightleaf, water, fire.",
        ),
    ).associateBy { it.id }

    fun get(id: RecipeId): RecipeDefinition = all[id]
        ?: error("Unknown recipe def '$id' (content gap)")

    fun availableRecipes(unlocks: Set<String>): List<RecipeDefinition> =
        all.values.filter { it.unlockToken.isEmpty() || unlocks.contains(it.unlockToken) }
            .sortedBy { it.skillLevelRequired }
}