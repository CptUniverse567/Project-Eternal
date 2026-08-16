package com.projecteternal.feature.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.projecteternal.content.EnhancementTables
import com.projecteternal.content.FailureConsequence
import com.projecteternal.content.Items
import com.projecteternal.content.LevelCurves
import com.projecteternal.content.Quests
import com.projecteternal.content.Skills
import com.projecteternal.model.EquipSlot
import com.projecteternal.model.GameState
import com.projecteternal.model.ItemInstance
import com.projecteternal.model.ItemKind
import com.projecteternal.model.QuestStatus
import com.projecteternal.sim.CombatStatsMath
import com.projecteternal.sim.GameIntent
import com.projecteternal.sim.GameStateRepository
import com.projecteternal.sim.QuestEngine

private enum class CharTab(val label: String) {
    STATS("Stats"), GEAR("Gear"), GOODS("Goods"), QUESTS("Quests"),
}

@Composable
fun CharacterScreen(
    repository: GameStateRepository,
    state: GameState,
    gearTabRequest: Int = 0,
) {
    var tabIndex by remember { mutableIntStateOf(0) }

    // The top bar's "Gear" shortcut deep-links here: bumping gearTabRequest
    // jumps straight to the Gear tab.
    LaunchedEffect(gearTabRequest) {
        if (gearTabRequest > 0) {
            tabIndex = CharTab.GEAR.ordinal
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CharTab.entries.forEachIndexed { index, tab ->
                FilterChip(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
        when (CharTab.entries[tabIndex]) {
            CharTab.STATS -> StatsTab(state)
            CharTab.GEAR -> GearTab(repository, state)
            CharTab.GOODS -> GoodsTab(repository, state)
            CharTab.QUESTS -> QuestsTab(repository, state)
        }
    }
}

@Composable
private fun StatsTab(state: GameState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            val eff = CombatStatsMath.effectiveStats(state)
            Card {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Combat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    StatLine("Attack", "${eff.attack}")
                    StatLine("Defense", "${eff.defense}")
                    StatLine("Accuracy", "${eff.accuracy}")
                    StatLine("Evasion", "${eff.evasion}")
                    StatLine("Crit", "${eff.critChance}% ×${eff.critMultiplier}")
                    StatLine("Attack speed", "${eff.attackSpeed}")
                    StatLine("Max HP", "${eff.maxHp}")
                    StatLine("Resolve", "${state.character.resolve}")
                }
            }
        }
        item {
            Text("Skills", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        val knownSkills = Skills.ofCategory(com.projecteternal.content.SkillCategory.GATHERING) +
            Skills.ofCategory(com.projecteternal.content.SkillCategory.PROCESSING) +
            Skills.ofCategory(com.projecteternal.content.SkillCategory.CRAFTING)
        items(knownSkills) { skill ->
            val level = state.character.skillLevel(skill.id)
            val xp = state.character.skillXp[skill.id] ?: 0
            val toNext = LevelCurves.skillXpToNext(level)
            Card {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(skill.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("Lv $level", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (xp.toFloat() / toNext).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                    )
                    Text(
                        "${xp}/$toNext xp · ${skill.description}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GearTab(repository: GameStateRepository, state: GameState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Equipped", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
        if (state.character.equipped.isEmpty()) {
            item { Text("Nothing equipped.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.character.equipped.toList()) { (slot, inst) ->
                EquippedRow(repository, state, slot, inst)
            }
        }

        item {
            Text("In pack", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        }
        val unequipped = state.equipmentItems.filter { inst ->
            state.character.equipped.values.none { it.uid == inst.uid }
        }
        if (unequipped.isEmpty()) {
            item { Text("No equipment in your pack. Forge some.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(unequipped) { inst -> PackItemRow(repository, state, inst) }
        }
    }
}

@Composable
private fun EquippedRow(repository: GameStateRepository, state: GameState, slot: EquipSlot, inst: ItemInstance) {
    val def = Items.get(inst.defId)
    Card {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(def.icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("${def.name} +${inst.enhancementLevel}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "Slot: ${slot.name.lowercase().replace('_', ' ')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = { repository.dispatch(GameIntent.Unequip(slot)) }) {
                Text("Unequip", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PackItemRow(repository: GameStateRepository, state: GameState, inst: ItemInstance) {
    val def = Items.get(inst.defId)
    val damage = inst.maxDurability > 0 && inst.durability < inst.maxDurability
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(def.icon, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("${def.name} +${inst.enhancementLevel}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "Durability ${inst.durability}/${inst.maxDurability} · ${def.slot?.name?.lowercase()?.replace('_', ' ')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (inst.maxDurability > 0) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { inst.durabilityFraction.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { repository.dispatch(GameIntent.Equip(inst.uid)) },
                    enabled = inst.durability > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Equip", style = MaterialTheme.typography.labelMedium)
                }
                if (def.enhanceable) {
                    val table = EnhancementTables.tableFor(inst.enhancementLevel)
                    val success = table?.baseSuccessPercent?.get(inst.enhancementLevel)
                    val shards = table?.shardsPerAttempt?.get(inst.enhancementLevel)
                    val locked = table != null && table.unlockToken.isNotEmpty() && !state.hasUnlock(table.unlockToken)
                    val material = table?.materialPerAttempt
                    val haveMaterial = material == null || state.inventoryCount(material.defId) >= material.count
                    val canEnhance = state.hasUnlock("screen:enhance") &&
                        !locked && table != null && success != null && shards != null &&
                        state.inventoryCount("shard_resonance") >= shards &&
                        haveMaterial && inst.durability > 0
                    val alternate = table?.alternateMaterialPerAttempt
                    val haveAlternate = alternate == null || state.inventoryCount(alternate.defId) >= alternate.count
                    val canEnhanceAlternate = state.hasUnlock("screen:enhance") &&
                        !locked && table != null && success != null && shards != null &&
                        state.inventoryCount("shard_resonance") >= shards &&
                        haveAlternate && inst.durability > 0
                    val countProtection = state.inventoryCount(table?.protectionItem ?: "")
                    val countWard = state.inventoryCount(table?.fullNegationItem ?: "")
                    if (table != null) {
                        Spacer(Modifier.height(4.dp))
                        val band = table.band.name.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.uppercase() }
                        val details = mutableListOf(band)
                        if (material != null) {
                            details += "${material.count}× ${Items.get(material.defId).name} (owned ${state.inventoryCount(material.defId)})"
                        }
                        if (alternate != null) {
                            details += "or ${alternate.count}× ${Items.get(alternate.defId).name} (owned ${state.inventoryCount(alternate.defId)})"
                        }
                        if (locked) details += "locked"
                        Text(
                            details.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (locked) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (table.failure == FailureConsequence.SHATTER_TO_BAND_FLOOR && inst.enhancementLevel >= table.downgradeThreshold) {
                            Text(
                                "Failure here shatters the item back to +${table.minLevel}.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    if (state.hasUnlock("screen:enhance")) {
                        if (locked) {
                            Text(
                                "Enhancement locked — distill a Stormbound Catalyst first.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            if (canEnhance) {
                                OutlinedButton(
                                    onClick = { repository.dispatch(GameIntent.Enhance(inst.uid, useProtection = false)) },
                                    enabled = true,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Enhance ${success?.let { "($it%)" } ?: ""}", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (alternate != null && canEnhanceAlternate) {
                                OutlinedButton(
                                    onClick = { repository.dispatch(GameIntent.Enhance(inst.uid, useProtection = false, useAlternateMaterial = true)) },
                                    enabled = true,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Enhance w/ ${Items.get(alternate.defId).name} (${success?.let { "($it%)" } ?: ""})", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (countProtection > 0) {
                                OutlinedButton(
                                    onClick = { repository.dispatch(GameIntent.Enhance(inst.uid, useProtection = true)) },
                                    enabled = canEnhance,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Enhance w/ ${Items.get(table!!.protectionItem!!).name} ($countProtection)", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (countWard > 0) {
                                OutlinedButton(
                                    onClick = { repository.dispatch(GameIntent.Enhance(inst.uid, useProtection = false, useFullNegation = true)) },
                                    enabled = canEnhance,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Enhance w/ ${Items.get(table!!.fullNegationItem!!).name} ($countWard)", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    } else {
                        Text(
                            "Enhancement unlocks after the main quest 'Forge Your Blade'.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (damage) {
                    val kits = state.inventoryCount("repair_kit")
                    val canRepair = kits > 0 && state.character.marks >= 5
                    OutlinedButton(
                        onClick = { repository.dispatch(GameIntent.Repair(inst.uid)) },
                        enabled = canRepair,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Repair", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoodsTab(repository: GameStateRepository, state: GameState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("Inventory", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
        if (state.inventory.isEmpty()) {
            item { Text("Your pack is empty.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.inventory) { stack ->
                val def = Items.get(stack.defId)
                Card {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(def.icon, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(def.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                "T${def.tier} · ${def.kind.name.lowercase()} · sells for ${def.sellPrice}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("×${stack.count}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        if (def.kind == ItemKind.CONSUMABLE) {
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { repository.dispatch(GameIntent.UseConsumable(stack.defId)) },
                            ) {
                                Text("Use", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestsTab(repository: GameStateRepository, state: GameState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Active", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
        val active = state.quests.filter { it.status == QuestStatus.ACTIVE }
        if (active.isEmpty()) {
            item { Text("No active quests.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(active) { progress ->
                val def = Quests.get(progress.questId)
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(def.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(def.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        def.objectives.forEach { obj ->
                            val current = QuestEngine.currentValue(state, obj)
                            val text = if (obj.targetCount > 1) "${obj.description} $current/${obj.targetCount}" else obj.description
                            Text(
                                "• $text",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (current >= obj.targetCount) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Available", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        }
        val done = state.quests.filter { it.status == QuestStatus.COMPLETED }.map { it.questId }.toSet()
        val available = Quests.available(done, state.unlocks)
            .filter { state.quest(it.id) == null }
            .filter { !it.autoAccept }
        if (available.isEmpty()) {
            item { Text("Nothing new to take.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(available) { def ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(def.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(def.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { repository.dispatch(GameIntent.AcceptQuest(def.id)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Accept", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        val completed = state.quests.filter { it.status == QuestStatus.COMPLETED }
        if (completed.isNotEmpty()) {
            item {
                Text("Completed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            }
            items(completed) { progress ->
                Text(
                    "✓ ${Quests.get(progress.questId).name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}