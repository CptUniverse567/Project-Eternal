package com.projecteternal.feature.economy

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import com.projecteternal.content.Nodes
import com.projecteternal.model.EquipSlot
import com.projecteternal.model.GameState
import com.projecteternal.model.ItemKind
import com.projecteternal.model.NodeType
import com.projecteternal.model.RetainerSpecialization
import com.projecteternal.sim.GameIntent
import com.projecteternal.sim.GameStateRepository
import com.projecteternal.sim.MarketService
import kotlin.math.roundToInt

@Composable
fun EconomyScreen(repository: GameStateRepository, state: GameState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Market", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            Text(
                marketHeaderText(state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { Text("Buy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
        val buyable = Items.allItems().filter { it.stackable && it.buyPrice > 0 && it.kind != ItemKind.CURRENCY_TOKEN }
        items(buyable) { def ->
            val price = MarketService.currentBuyPrice(state, def.id)
            MarketRow(
                icon = def.icon,
                name = def.name,
                price = "$price ◎",
                actionLabel = "Buy",
                enabled = state.character.marks >= price,
                onAction = { repository.dispatch(GameIntent.Buy(def.id, 1)) },
            )
        }

        item {
            Text("Sell", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        }
        val sellable = state.inventory.filter { Items.get(it.defId).sellPrice > 0 }
        if (sellable.isEmpty()) {
            item { Text("Nothing to sell yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(sellable) { stack ->
                val def = Items.get(stack.defId)
                val price = MarketService.currentSellPrice(state, stack.defId)
                val advice = MarketService.sellAdvice(state, stack.defId)
                MarketRow(
                    icon = def.icon,
                    name = def.name,
                    count = "×${stack.count}",
                    price = "$price ◎ each",
                    actionLabel = "Sell 1",
                    enabled = stack.count > 0,
                    onAction = { repository.dispatch(GameIntent.Sell(stack.defId, 1)) },
                    subline = when {
                        advice?.hasBetterPath == true -> "💡 ${advice.betterHint} (else $price◎)"
                        else -> null
                    },
                )
            }
        }

        item { Text("Retainers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)) }
        if (!state.hasUnlock("screen:workers")) {
            item { Text("🔒 Workers unlock once the Deep Quartz Grotto is discovered (main quest).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else if (state.retainers.isEmpty()) {
            item { Text("No retainers yet — discover the Deep Quartz Grotto to recruit workers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.retainers) { retainer ->
                RetainerCard(repository, state, retainer.id)
            }
        }

        val hired = state.retainers.map { it.id }.toSet()
        val hireable = com.projecteternal.content.RetainerRecruits.allRecruits()
            .filter { !hired.contains(it.id) && state.hasUnlock(it.unlockToken) }
        if (state.hasUnlock("screen:workers") && hireable.isNotEmpty()) {
            item {
                Text("Hire Workers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            }
            items(hireable) { recruit ->
                Card {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(recruit.icon, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${recruit.name} · ${recruit.specialization.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "${recruit.description} · ${recruit.costMarks} ◎",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = { repository.dispatch(GameIntent.RecruitRetainer(recruit.id)) },
                            enabled = state.character.marks >= recruit.costMarks,
                            modifier = Modifier.testTag("recruit_${recruit.id}"),
                        ) { Text("Hire", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }
    }
}

/** Plain-language explanation of the market price rules (display only). */
private fun marketHeaderText(state: GameState): String {
    val regionBonus = ((MarketService.bestRegionModifier(state) - 1.0) * 100).roundToInt()
    val premium = MarketService.processedDemandPremium(state)
    val premiumText = "%.2f".format(premium)
    return buildString {
        append(
            if (regionBonus > 0) "Selling earns +$regionBonus% in the best Reach you've toured. "
            else "Selling at home prices — tour farther Reaches to earn more. ",
        )
        append("Refined and crafted goods sell for more than raw (×$premiumText demand). ")
        append("Prices drift as you trade.")
    }
}

@Composable
private fun MarketRow(
    icon: String,
    name: String,
    price: String,
    actionLabel: String,
    enabled: Boolean,
    onAction: () -> Unit,
    count: String? = null,
    subline: String? = null,
) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    if (count != null) "$count · $price" else price,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (subline != null) {
                    Text(
                        subline,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            OutlinedButton(onClick = onAction, enabled = enabled) { Text(actionLabel, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun RetainerCard(repository: GameStateRepository, state: GameState, retainerId: String) {
    val retainer = state.retainer(retainerId) ?: return
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${retainer.name} (Lv ${retainer.level})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    retainer.specialization.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { retainer.staminaFraction.toFloat() },
                modifier = Modifier.fillMaxWidth().height(4.dp),
            )
            Text(
                "Stamina ${retainer.stamina}/${retainer.maxStamina}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (retainer.traitIds.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    retainer.traitIds.forEach { traitId ->
                        val trait = com.projecteternal.content.RetainerTraits.get(traitId)
                        Text(
                            "${trait.icon} ${trait.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                val milesDone = com.projecteternal.sim.RetainerEngine.TRAIT_MILESTONE_LEVELS.count { retainer.level >= it }
                val next = com.projecteternal.sim.RetainerEngine.TRAIT_MILESTONE_LEVELS.getOrNull(milesDone)
                if (next != null) {
                    Text(
                        "Next trait at Lv $next",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val assignable = state.nodes.filter { it.unlocked && compatible(retainer.specialization, it.type) }
            val assignedNodeId = retainer.assignedNodeId
            Spacer(Modifier.height(8.dp))
            if (assignedNodeId != null) {
                Button(
                    onClick = { repository.dispatch(GameIntent.AssignRetainer(retainer.id, null)) },
                    modifier = Modifier.fillMaxWidth().testTag("unassign_${retainer.id}"),
                ) {
                    Text("Unassign from ${Nodes.get(assignedNodeId).name}")
                }
            } else if (assignable.isNotEmpty()) {
                Text("Assign to:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                assignable.forEach { node ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        OutlinedButton(
                            onClick = { repository.dispatch(GameIntent.AssignRetainer(retainer.id, node.id)) },
                            modifier = Modifier.fillMaxWidth().testTag("assign_${retainer.id}_${node.id}"),
                        ) {
                            Text(Nodes.get(node.id).name, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                Text("No compatible nodes discovered.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun compatible(spec: RetainerSpecialization, type: NodeType): Boolean = when (spec) {
    RetainerSpecialization.MINER -> type == NodeType.MINE
    RetainerSpecialization.LUMBERJACK -> type == NodeType.FOREST
    RetainerSpecialization.FARMER -> type == NodeType.FARM
    RetainerSpecialization.FISHER -> type == NodeType.FISHERY
    RetainerSpecialization.FORAGER -> type == NodeType.FOREST || type == NodeType.SPECIAL
    RetainerSpecialization.CRAFTER -> type == NodeType.MINE || type == NodeType.FOREST
}