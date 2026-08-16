package com.projecteternal.feature.industry

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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.projecteternal.content.Items
import com.projecteternal.content.Recipes
import com.projecteternal.content.Skills
import com.projecteternal.model.ActivityType
import com.projecteternal.model.GameState
import com.projecteternal.model.ItemStack
import com.projecteternal.model.RecipeId
import com.projecteternal.sim.GameIntent
import com.projecteternal.sim.GameStateRepository

@Composable
fun IndustryScreen(repository: GameStateRepository, state: GameState) {
    val recipeDefs = Recipes.availableRecipes(state.unlocks)
    val currentSkill = { skillId: String -> state.character.skillLevel(skillId) }
    val startedId = state.character.currentActivity?.let {
        if (it.type == ActivityType.PROCESSING || it.type == ActivityType.CRAFTING) it.targetId else null
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Industry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        val available = recipeDefs.filter { currentSkill(it.skillId) >= it.skillLevelRequired }
        val locked = recipeDefs.filter { currentSkill(it.skillId) < it.skillLevelRequired }

        if (available.isEmpty() && locked.isEmpty()) {
            Text("No recipes known yet.", style = MaterialTheme.typography.bodyMedium)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(available) { recipe ->
                RecipeCard(
                    recipeId = recipe.id,
                    started = recipe.id == startedId,
                    canRun = true,
                    hasMaterials = hasMaterials(state, recipe.inputs),
                    onStart = {
                        repository.dispatch(GameIntent.StartActivity(if (isProcessing(recipe.skillId)) ActivityType.PROCESSING else ActivityType.CRAFTING, recipe.id))
                    },
                )
            }
            if (locked.isNotEmpty()) {
                item {
                    Text(
                        "Requires higher skill",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(locked) { recipe ->
                    RecipeCard(
                        recipeId = recipe.id,
                        started = recipe.id == startedId,
                        canRun = false,
                        hasMaterials = hasMaterials(state, recipe.inputs),
                        onStart = {},
                    )
                }
            }
        }
    }
}

// Processing skills produce materials; crafting skills produce goods.
private fun isProcessing(skillId: String): Boolean =
    Skills.get(skillId).category == com.projecteternal.content.SkillCategory.PROCESSING

@Composable
private fun RecipeCard(
    recipeId: RecipeId,
    started: Boolean,
    canRun: Boolean,
    hasMaterials: Boolean,
    onStart: () -> Unit,
) {
    val recipe = Recipes.get(recipeId)
    val skill = Skills.get(recipe.skillId)
    val cardContent: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(recipe.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${recipe.timeSeconds}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${skill.name} · lv ${recipe.skillLevelRequired} required · +${recipe.xpPerCraft} xp",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                recipe.inputs.joinToString { "${Items.get(it.defId).icon}${Items.get(it.defId).name}×${it.count}" } +
                    " → " +
                    recipe.outputs.joinToString { "${Items.get(it.defId).icon}${Items.get(it.defId).name}×${it.count}" },
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            val tag = Modifier.fillMaxWidth().testTag("btn_start_recipe_$recipeId")
            when {
                started -> Button(onClick = onStart, enabled = false, modifier = tag) { Text("Running…") }
                !canRun -> Button(onClick = onStart, enabled = false, modifier = tag) { Text("Learn level ${recipe.skillLevelRequired} ${skill.name} first") }
                !hasMaterials -> Button(onClick = onStart, enabled = false, modifier = tag) { Text("Missing materials") }
                else -> Button(onClick = onStart, modifier = tag) { Text("Start") }
            }
        }
    }
    if (canRun) {
        Card { cardContent() }
    } else {
        OutlinedCard { cardContent() }
    }
}

private fun hasMaterials(state: GameState, inputs: List<ItemStack>): Boolean =
    inputs.all { state.inventoryCount(it.defId) >= it.count }