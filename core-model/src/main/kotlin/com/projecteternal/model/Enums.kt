package com.projecteternal.model

import kotlinx.serialization.Serializable

/** Type aliases so identifiers are self-documenting. */
typealias ItemId = String
typealias RecipeId = String
typealias MonsterId = String
typealias NodeId = String
typealias RegionId = String
typealias QuestId = String
typealias SkillId = String
typealias RetainerId = String

/** Loot/objective targets etc. also use ItemId/MonsterId aliases above. */

/** Equipment slots available to the player character and retainers. */
@Serializable
enum class EquipSlot {
    MAIN_WEAPON,
    SECONDARY_WEAPON,
    HELMET,
    CHEST,
    GLOVES,
    BOOTS,
    ACCESSORY_1,
    ACCESSORY_2,
    TOOL,
}

/** Broad item taxonomy. */
@Serializable
enum class ItemKind {
    RESOURCE,
    MATERIAL,
    EQUIPMENT,
    CONSUMABLE,
    CURRENCY_TOKEN,
}

/** What a character or retainer is currently doing. */
@Serializable
enum class ActivityType {
    COMBAT,
    GATHERING,
    PROCESSING,
    CRAFTING,
}

/** Node kinds. Monster territories double as combat areas. */
@Serializable
enum class NodeType {
    MINE,
    FOREST,
    FARM,
    FISHERY,
    RUIN,
    MONSTER_TERRITORY,
    SPECIAL,
    CITY,
    TRADE_HUB,
}

/** Retainer (worker) specializations. */
@Serializable
enum class RetainerSpecialization {
    MINER,
    LUMBERJACK,
    FARMER,
    FISHER,
    FORAGER,
    CRAFTER,
}

@Serializable
enum class QuestStatus {
    AVAILABLE,
    ACTIVE,
    COMPLETED,
}

/**
 * Generic quest objective kinds. QuestEngine derives the current value of every
 * objective purely from game state, so quests are content data, not bespoke code.
 */
@Serializable
enum class ObjectiveType {
    HAVE_ITEMS,           // targetId = item defId, targetCount = how many currently held
    GATHER,               // targetId = gathered item defId (cumulative, from StatsTracker)
    CRAFT,                // targetId = crafted item defId (cumulative)
    KILL,                 // targetId = monster defId (cumulative)
    DISCOVER_NODE,        // targetId = nodeId
    REACH_CHAR_LEVEL,     // targetCount = level
    REACH_SKILL_LEVEL,    // targetId = skillId, targetCount = level
    HAVE_MARKS,           // targetCount = marks
    ENHANCE_ANY_TO,       // targetCount = enhancement level ever achieved
    EQUIP_SLOT,           // targetId = slot enum name
    VISIT_REGION,         // targetId = regionId
}

/**
 * Enhancement tier bands. Tiers are DATA (content tables) mapped from the
 * instance's enhancement level, so new bands can be appended without a schema
 * rewrite. This enum enumerates known band ids for failure-consequence logic;
 * the band's exact probability table and materials live in core-content.
 */
@Serializable
enum class EnhancementBand {
    BASE,        // levels +0..+15
    ADVANCED,    // levels +16..+30, requires refined high-tier materials
    TRANSCENDENT,// +31..+45, placeholder architecture for Phase 3
    ASCENDANT,   // soft-prestige layer, placeholder architecture for Phase 3
}
