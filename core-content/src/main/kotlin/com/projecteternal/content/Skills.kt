package com.projecteternal.content

import com.projecteternal.model.SkillId

/** Static skill catalog. */
object Skills {
    private val all: Map<SkillId, SkillDefinition> = listOf(
        // Gathering
        SkillDefinition("mining", "Mining", SkillCategory.GATHERING,
            "Swing, hit, break. Iron doesn't move on its own.",
            perkPerLevel = PerkType.YIELD_PERCENT, perkValuePerLevel = 0.5),
        SkillDefinition("logging", "Logging", SkillCategory.GATHERING,
            "The old woods are patient. So are you.",
            perkPerLevel = PerkType.YIELD_PERCENT, perkValuePerLevel = 0.5),
        SkillDefinition("herbalism", "Herbalism", SkillCategory.GATHERING,
            "Sight, smell, and a careful hand.",
            perkPerLevel = PerkType.YIELD_PERCENT, perkValuePerLevel = 0.5),
        SkillDefinition("farming", "Farming", SkillCategory.GATHERING,
            "Sow, tend, and trust the soil to keep its promises.",
            perkPerLevel = PerkType.YIELD_PERCENT, perkValuePerLevel = 0.5),
        SkillDefinition("fishing", "Fishing", SkillCategory.GATHERING,
            "Wait by quiet water. The river gives what it chooses.",
            perkPerLevel = PerkType.YIELD_PERCENT, perkValuePerLevel = 0.5),

        // Processing
        SkillDefinition("grinding", "Grinding", SkillCategory.PROCESSING,
            "Stone against stone. Ore gives up its shape.",
            perkPerLevel = PerkType.EFFICIENCY_PERCENT, perkValuePerLevel = 1.0),
        SkillDefinition("heating", "Heating", SkillCategory.PROCESSING,
            "Fire prepares what hammering cannot.",
            perkPerLevel = PerkType.EFFICIENCY_PERCENT, perkValuePerLevel = 1.0),
        SkillDefinition("smelting", "Smelting", SkillCategory.PROCESSING,
            "Metal sheds its impurities in the crucible.",
            perkPerLevel = PerkType.EFFICIENCY_PERCENT, perkValuePerLevel = 1.0),
        SkillDefinition("refining", "Refining", SkillCategory.PROCESSING,
            "Purity is a choice made one batch at a time.",
            perkPerLevel = PerkType.EFFICIENCY_PERCENT, perkValuePerLevel = 1.0),
        SkillDefinition("drying", "Drying", SkillCategory.PROCESSING,
            "Air, salt, and patience preserve what water can take.",
            perkPerLevel = PerkType.EFFICIENCY_PERCENT, perkValuePerLevel = 1.0),
        SkillDefinition("milling", "Milling", SkillCategory.PROCESSING,
            "Turn the wheel. The harvest becomes something storable.",
            perkPerLevel = PerkType.EFFICIENCY_PERCENT, perkValuePerLevel = 1.0),

        // Crafting
        SkillDefinition("blacksmithing", "Blacksmithing", SkillCategory.CRAFTING,
            "The anvil remembers every mistake.",
            perkPerLevel = PerkType.EFFICIENCY_PERCENT, perkValuePerLevel = 1.0),
        SkillDefinition("armorcraft", "Armorcraft", SkillCategory.CRAFTING,
            "Layers, stitches, and no sharp edges inward.",
            perkPerLevel = PerkType.EFFICIENCY_PERCENT, perkValuePerLevel = 1.0),
        SkillDefinition("cooking", "Cooking", SkillCategory.CRAFTING,
            "Salt, fire, time. Everything else is flourish.",
            perkPerLevel = PerkType.EFFICIENCY_PERCENT, perkValuePerLevel = 1.0),
        SkillDefinition("alchemy", "Alchemy", SkillCategory.CRAFTING,
            "Measure twice, distill once.",
            perkPerLevel = PerkType.EFFICIENCY_PERCENT, perkValuePerLevel = 1.0),
        SkillDefinition("carpentry", "Carpentry", SkillCategory.CRAFTING,
            "Measure twice, cut once. Wood forgives, barely.",
            perkPerLevel = PerkType.EFFICIENCY_PERCENT, perkValuePerLevel = 1.0),
        SkillDefinition("engineering", "Engineering", SkillCategory.CRAFTING,
            "Every machine is a promise that parts will fit.",
            perkPerLevel = PerkType.EFFICIENCY_PERCENT, perkValuePerLevel = 1.0),
    ).associateBy { it.id }

    fun get(id: SkillId): SkillDefinition = all[id]
        ?: error("Unknown skill def '$id' (content gap)")

    fun ofCategory(category: SkillCategory): List<SkillDefinition> =
        all.values.filter { it.category == category }.sortedBy { it.id }
}