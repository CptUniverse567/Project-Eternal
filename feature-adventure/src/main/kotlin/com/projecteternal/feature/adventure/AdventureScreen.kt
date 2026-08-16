package com.projecteternal.feature.adventure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.projecteternal.content.Items
import com.projecteternal.content.Monsters
import com.projecteternal.content.Nodes
import com.projecteternal.content.Regions
import com.projecteternal.model.ActivityType
import com.projecteternal.model.GameState
import com.projecteternal.model.MonsterId
import com.projecteternal.model.NodeId
import com.projecteternal.sim.GameIntent
import com.projecteternal.sim.GameStateRepository
import com.projecteternal.sim.Rates

@Composable
fun AdventureScreen(repository: GameStateRepository, state: GameState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("The Reach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

        // Regions with at least one discovered node
        val discoveredNodes = state.nodes.filter { it.unlocked && Nodes.get(it.id).actionsPerHour > 0 }
        val visibleRegions = state.nodes.filter { it.unlocked }.map { it.regionId }.distinct()
            .sortedBy { Regions.get(it).tier }

        visibleRegions.forEach { regionId ->
            val region = Regions.get(regionId)
            item {
                Column {
                    Text(
                        region.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        region.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    region.rareYield?.let { rare ->
                        Text(
                            "Rare find: ${Items.get(rare.defId).icon} ${Items.get(rare.defId).name} ${rare.chancePercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (region.hazardPerAction > 0) {
                        Text(
                            "⚠ ${region.name} saps your health — every action costs health, and no regen while gathering.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            items(discoveredNodes.filter { it.regionId == regionId }) { node ->
                GatheringNodeCard(repository, state, node.id)
            }
            val monsters = Monsters.inRegion(regionId)
            if (monsters.isNotEmpty()) {
                items(monsters) { monster ->
                    MonsterCard(repository, state, monster.id)
                }
            }
        }

        if (visibleRegions.isEmpty()) {
            item { Text("No regions discovered yet.", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun GatheringNodeCard(repository: GameStateRepository, state: GameState, nodeId: NodeId) {
    val def = Nodes.get(nodeId)
    val currentlyMining = state.character.currentActivity?.let {
        it.type == ActivityType.GATHERING && it.targetId == nodeId
    } == true
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(def.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                val effective = Rates.gatheringActionsPerHour(state, def).toInt()
                val base = def.actionsPerHour.toInt()
                Text(
                    if (effective == base) "$base/hr" else "~$effective/hr · base $base/hr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                def.yields.joinToString(" · ") { "${Items.get(it.defId).icon} ${Items.get(it.defId).name} ${it.chancePercent}%" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (com.projecteternal.content.Regions.get(def.regionId).hazardPerAction > 0) {
                Text(
                    "⚠ Hazardous — costs health",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { repository.dispatch(GameIntent.StartActivity(ActivityType.GATHERING, nodeId)) },
                enabled = !currentlyMining,
                modifier = Modifier.fillMaxWidth().testTag("start_gathering_$nodeId"),
            ) {
                Text(if (currentlyMining) "Gathering…" else "${def.type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }} here")
            }
        }
    }
}

@Composable
private fun MonsterCard(repository: GameStateRepository, state: GameState, monsterId: MonsterId) {
    val def = Monsters.get(monsterId)
    val currentlyFighting = state.character.currentActivity?.let {
        it.type == ActivityType.COMBAT && it.targetId == monsterId
    } == true
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    def.name + if (def.boss) "  ⚠️" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "T${def.tier} · ${def.stats.maxHp} HP · ${def.xpReward} xp",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(def.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                def.lootTable.joinToString(" · ") { "${Items.get(it.defId).icon} ${Items.get(it.defId).name} ${it.chancePercent}%" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { repository.dispatch(GameIntent.StartActivity(ActivityType.COMBAT, monsterId)) },
                enabled = !currentlyFighting,
                modifier = Modifier.fillMaxWidth().testTag("start_combat_$monsterId"),
            ) {
                Text(if (currentlyFighting) "Fighting…" else "Hunt")
            }
        }
    }
}