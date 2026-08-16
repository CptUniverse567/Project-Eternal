package com.projecteternal.content

import com.projecteternal.model.CombatStats
import com.projecteternal.model.EquipSlot
import com.projecteternal.model.ItemId
import com.projecteternal.model.ItemKind

/** Static item/resource catalog. */
object Items {
    private val all: Map<ItemId, ItemDefinition> = listOf(
        // ---- raw resources ----
        ItemDefinition("ore_iron", "Iron Ore", ItemKind.RESOURCE, tier = 1,
            buyPrice = 6, sellPrice = 3, icon = "🪨",
            description = "Raw iron ore. Feed it through a grinding wheel."),
        ItemDefinition("wood", "Hardwood Log", ItemKind.RESOURCE, tier = 1,
            buyPrice = 5, sellPrice = 2, icon = "🪵",
            description = "A sturdy log from the old woods."),
        ItemDefinition("herb_brightleaf", "Brightleaf", ItemKind.RESOURCE, tier = 1,
            buyPrice = 8, sellPrice = 4, icon = "🌿",
            description = "A faintly glowing leaf; base of many brews."),
        ItemDefinition("ore_coal", "Coal", ItemKind.RESOURCE, tier = 2,
            buyPrice = 20, sellPrice = 10, icon = "⚫",
            description = "Dense black coal, only found in deep seams."),
        ItemDefinition("meat_wolf", "Wolf Meat", ItemKind.RESOURCE, tier = 1,
            buyPrice = 9, sellPrice = 4, icon = "🍖",
            description = "Tough but edible."),
        ItemDefinition("pelt_wolf", "Wolf Pelt", ItemKind.RESOURCE, tier = 1,
            buyPrice = 7, sellPrice = 3, icon = "🧶",
            description = "Coarse grey fur."),
        ItemDefinition("meat_boar", "Boar Meat", ItemKind.RESOURCE, tier = 1,
            buyPrice = 11, sellPrice = 5, icon = "🥩"),
        ItemDefinition("pelt_boar", "Boar Hide", ItemKind.RESOURCE, tier = 1,
            buyPrice = 9, sellPrice = 4, icon = "🟤"),
        ItemDefinition("grain", "Grain", ItemKind.RESOURCE, tier = 1,
            buyPrice = 5, sellPrice = 2, icon = "🌾",
            description = "Golden grain from the terraces. Mill it for flour."),
        ItemDefinition("fish_carp", "River Carp", ItemKind.RESOURCE, tier = 1,
            buyPrice = 10, sellPrice = 4, icon = "🐟",
            description = "A quiet river catch. Perfect for drying."),

        // ---- processed materials ----
        ItemDefinition("iron_fragment", "Iron Fragment", ItemKind.MATERIAL, tier = 1,
            buyPrice = 12, sellPrice = 6, icon = "⚙️",
            description = "Ground ore ready for the smelter."),
        ItemDefinition("iron_ingot", "Iron Ingot", ItemKind.MATERIAL, tier = 1,
            buyPrice = 26, sellPrice = 13, icon = "🟦",
            description = "A cast iron ingot. Weapon-ready."),
        ItemDefinition("refined_iron", "Refined Iron", ItemKind.MATERIAL, tier = 2,
            buyPrice = 58, sellPrice = 29, icon = "🔷",
            description = "High-purity iron. Upgrades are forged from this."),
        ItemDefinition("steel", "Steel", ItemKind.MATERIAL, tier = 2,
            buyPrice = 130, sellPrice = 65, icon = "⚪"),
        ItemDefinition("lumber", "Lumber", ItemKind.MATERIAL, tier = 1,
            buyPrice = 14, sellPrice = 7, icon = "🟫"),
        ItemDefinition("flour", "Flour", ItemKind.MATERIAL, tier = 1,
            buyPrice = 12, sellPrice = 6, icon = "🫓",
            description = "Finely milled grain, ready for the oven."),
        ItemDefinition("rope", "Hemp Rope", ItemKind.MATERIAL, tier = 1,
            buyPrice = 10, sellPrice = 5, icon = "🪢",
            description = "Twisted cord. Fishing rods and fine craft trade on it."),
        ItemDefinition("machinery_part", "Machinery Part", ItemKind.MATERIAL, tier = 2,
            buyPrice = 240, sellPrice = 120, icon = "⚙️",
            description = "Precision fittings only an engineer could love."),
        ItemDefinition("core_storm", "Stormheart Core", ItemKind.MATERIAL, tier = 3,
            buyPrice = 700, sellPrice = 350, icon = "🌩️",
            description = "The still-warm heart of a storm herald."),
        ItemDefinition("roasted_meat", "Roasted Meat", ItemKind.CONSUMABLE, tier = 1,
            buyPrice = 20, sellPrice = 8, icon = "🍗",
            description = "Restores 20 health. Cooked at a campfire."),

        // ---- special resources ----
        ItemDefinition("shard_resonance", "Resonance Shard", ItemKind.MATERIAL, tier = 1,
            buyPrice = 40, sellPrice = 20, icon = "🔮",
            description = "Amplifies equipment. The heart of enhancement."),
        ItemDefinition("crystal_advanced", "Voidforged Crystal", ItemKind.MATERIAL, tier = 3,
            buyPrice = 400, sellPrice = 200, icon = "💠",
            description = "Required for ADVANCED enhancement tiers. Emberreach reagent."),
        ItemDefinition("crystal_guardian", "Guardian Core", ItemKind.MATERIAL, tier = 2,
            buyPrice = 250, sellPrice = 120, icon = "🟣",
            description = "Held by the ruin guardian. Proof of a great hunt."),

        ItemDefinition("catalyst_storm", "Stormbound Catalyst", ItemKind.MATERIAL, tier = 4,
            buyPrice = 1500, sellPrice = 750, icon = "🌠",
            description = "Stormheart fused with a voidforged crystal. Required for TRANSCENDENT enhancement."),

        // ---- equipment: tools ----
        ItemDefinition("tool_pickaxe", "Journeyman's Pickaxe", ItemKind.EQUIPMENT, slot = EquipSlot.TOOL,
            tier = 1, baseStats = CombatStats(attack = 1), maxDurability = 100,
            stackable = false, buyPrice = 80, sellPrice = 30, icon = "⛏️",
            description = "Mines 25% faster. Equip to your tool slot.", enhanceable = true),
        ItemDefinition("tool_fishing_rod", "Stout Fishing Rod", ItemKind.EQUIPMENT, slot = EquipSlot.TOOL,
            tier = 1, baseStats = CombatStats(attack = 1), maxDurability = 90,
            stackable = false, buyPrice = 0, sellPrice = 40, icon = "🎣",
            description = "Fishes 25% faster. A reassuring bend in every cast.", enhanceable = true),

        // ---- equipment: weapons ----
        ItemDefinition("sword_bronze", "Bronze Sword", ItemKind.EQUIPMENT, slot = EquipSlot.MAIN_WEAPON,
            tier = 1, baseStats = CombatStats(attack = 8, accuracy = 5), maxDurability = 80,
            stackable = false, buyPrice = 0, sellPrice = 30, icon = "⚔️",
            description = "Your first real blade, forged from iron you mined.", enhanceable = true),
        ItemDefinition("sword_steel", "Steel Sword", ItemKind.EQUIPMENT, slot = EquipSlot.MAIN_WEAPON,
            tier = 2, baseStats = CombatStats(attack = 18, accuracy = 8), maxDurability = 110,
            stackable = false, buyPrice = 900, sellPrice = 350, icon = "🗡️",
            description = "Tempered steel. A genuine upgrade.", enhanceable = true),
        ItemDefinition("shield_oak", "Oak Shield", ItemKind.EQUIPMENT, slot = EquipSlot.SECONDARY_WEAPON,
            tier = 1, baseStats = CombatStats(defense = 6, accuracy = -2), maxDurability = 120,
            stackable = false, buyPrice = 0, sellPrice = 50, icon = "🪓",
            description = "Oak banded with rope. Blocks what the sword cannot.", enhanceable = true),

        // ---- equipment: armor ----
        ItemDefinition("armor_hide", "Hide Armor", ItemKind.EQUIPMENT, slot = EquipSlot.CHEST,
            tier = 1, baseStats = CombatStats(defense = 4, maxHp = 20), maxDurability = 70,
            stackable = false, buyPrice = 0, sellPrice = 25, icon = "🦺",
            description = "Wolf hide stitched over a lumber frame.", enhanceable = true),
        ItemDefinition("helmet_hide", "Hide Hood", ItemKind.EQUIPMENT, slot = EquipSlot.HELMET,
            tier = 1, baseStats = CombatStats(defense = 2, maxHp = 8), maxDurability = 60,
            stackable = false, buyPrice = 0, sellPrice = 18, icon = "🎩",
            description = "Warm and vaguely intimidating.", enhanceable = true),

        // ---- potions ----
        ItemDefinition("potion_lesser", "Lesser Vitality Draft", ItemKind.CONSUMABLE,
            buyPrice = 50, sellPrice = 20, icon = "🧪",
            description = "Restores 50 health."),

        // ---- enhancement & repair consumables ----
        ItemDefinition("oil_preservation", "Oil of Preservation", ItemKind.CONSUMABLE,
            buyPrice = 150, sellPrice = 60, icon = "🫗",
            description = "Prevents an enhancement failure from downgrading your item."),
        ItemDefinition("ward_of_stability", "Ward of Stability", ItemKind.CONSUMABLE,
            buyPrice = 420, sellPrice = 170, icon = "🛡️",
            description = "Fully negates an enhancement failure — no downgrade, no durability loss."),
        ItemDefinition("repair_kit", "Repair Kit", ItemKind.CONSUMABLE,
            buyPrice = 40, sellPrice = 15, icon = "🔧",
            description = "Restores durability to a damaged item."),
        ItemDefinition("bread", "Bread", ItemKind.CONSUMABLE,
            buyPrice = 30, sellPrice = 12, icon = "🍞",
            description = "Restores 25 health. Warmed by the oven and simple faith."),
        ItemDefinition("dried_fish", "Dried River Fish", ItemKind.CONSUMABLE,
            buyPrice = 24, sellPrice = 10, icon = "🥓",
            description = "Restores 18 health. Keeps for the trail."),
    ).associateBy { it.id }

    fun get(id: ItemId): ItemDefinition = all[id]
        ?: error("Unknown item def '$id' (content gap)")

    fun contains(id: ItemId): Boolean = all.containsKey(id)
    fun allItems(): List<ItemDefinition> = all.values.sortedBy { it.tier }
}
