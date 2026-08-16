package com.projecteternal.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * First-run how-to-play sheet / glossary. Pure educational composable — no game
 * rules live here, it only explains existing mechanics in plain language.
 */
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How to play", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
            ) {
                HelpSection("Your coin") {
                    "◎ is Marks, the realm's currency. Earn it from quests, monster loot, and selling goods at the Market (Economy tab)."
                }
                HelpSection("What to do") {
                    "Pick an activity on the Adventure tab — gather at a node or hunt a monster. You keep working while the game is closed, and a report tells you what happened when you return."
                }
                HelpSection("Tools wear out") {
                    "Gathering equipment has durability and breaks with use. A broken tool stops contributing — repair it on the Character → Gear tab (a repair kit costs 5 Marks) or forge a replacement. Your pickaxe boosts mining speed while equipped."
                }
                HelpSection("Quests guide you") {
                    "Your next goal is in the top bar and on the Character → Quests tab. Completing main quests unlocks new nodes, recipes, and screen features."
                }
                HelpSection("Prices") {
                    "The Market prices goods by the best Reach you've toured — \"region modifier\" — so visiting farther Reaches raises your selling prices. \"Processed-goods demand\" means refined or crafted goods sell for more than raw materials, so processing ore into bars pays better than dumping the ore."
                }
                HelpSection("Gear and enhancements") {
                    "Enhancement is unlocked by the main quest \"Forge Your Blade\". Enhancements boost gear but failures can downgrade your item — or shatter it at high tiers. Protection items can blunt the worst of it."
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("btn_help_close")) {
                Text("Got it")
            }
        },
        dismissButton = {},
    )
}

@Composable
private fun HelpSection(title: String, body: () -> String) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(2.dp))
    Text(body(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
}