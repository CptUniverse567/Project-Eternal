package com.projecteternal.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.projecteternal.app.controller.GameController
import com.projecteternal.content.Monsters
import com.projecteternal.content.Nodes
import com.projecteternal.content.Recipes
import com.projecteternal.model.ActivityType
import com.projecteternal.model.GameState
import com.projecteternal.model.ObjectiveType
import com.projecteternal.sim.GameIntent
import com.projecteternal.sim.GameStateRepository
import com.projecteternal.feature.adventure.AdventureScreen
import com.projecteternal.feature.character.CharacterScreen
import com.projecteternal.feature.economy.EconomyScreen
import com.projecteternal.feature.industry.IndustryScreen

private enum class AppTab(val label: String, val icon: String) {
    ADVENTURE("Adventure", "⚔️"),
    INDUSTRY("Industry", "⚒️"),
    ECONOMY("Economy", "💰"),
    CHARACTER("Character", "🧙"),
    SETTINGS("Settings", "⚙️"),
}

@Composable
fun EternalApp(controller: GameController) {
    val startup by controller.startup.collectAsState()
    when (startup.phase) {
        GameController.Phase.LOADING -> LoadingScreen()
        GameController.Phase.NEW_GAME -> CreationScreen(
            onCreate = { name -> controller.dispatch(GameIntent.StartGame(name)) },
        )
        GameController.Phase.CORRUPT -> CorruptScreen(
            reason = startup.warning,
            onRetry = controller::retryLoad,
        )
        GameController.Phase.READY -> {
            val state by controller.state.collectAsState()
            state?.let { GameShell(controller, it) }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Loading the Reach…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CorruptScreen(reason: String?, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Save corrupted", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    reason ?: "The save file could not be read.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onRetry) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun GameShell(controller: GameController, state: GameState) {
    var selectedTab by remember { mutableIntStateOf(AppTab.ADVENTURE.ordinal) }
    var gearRequest by remember { mutableIntStateOf(0) }
    var helpOpen by remember { mutableStateOf(false) }
    val events by controller.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(events) {
        for (event in events) {
            snackbarHostState.showSnackbar(event)
        }
        if (events.isNotEmpty()) controller.dismissEvents()
    }

    // One-shot first-run sheet: shown after a brand-new game is created.
    LaunchedEffect(controller.showFirstRunHelp.value) {
        if (controller.showFirstRunHelp.value) helpOpen = true
    }

    fun goToGear() {
        selectedTab = AppTab.CHARACTER.ordinal
        gearRequest++
    }

    Scaffold(
        topBar = {
            GameTopBar(
                state = state,
                onStopActivity = { controller.dispatch(GameIntent.StopActivity) },
                onOpenHelp = { helpOpen = true },
                onGearShortcut = { goToGear() },
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(tab.icon, style = MaterialTheme.typography.titleMedium) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("tab_${tab.name}"),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val tab = AppTab.entries[selectedTab]
            when (tab) {
                AppTab.ADVENTURE -> AdventureScreen(controller, state)
                AppTab.INDUSTRY -> IndustryScreen(controller, state)
                AppTab.ECONOMY -> EconomyScreen(controller, state)
                AppTab.CHARACTER -> CharacterScreen(controller, state, gearTabRequest = gearRequest)
                AppTab.SETTINGS -> SettingsScreen(state)
            }
        }
        if (state.pendingOfflineReport != null) {
            OfflineReportDialog(state.pendingOfflineReport!!) {
                controller.dispatch(GameIntent.DismissOfflineReport)
            }
        }
        if (helpOpen) {
            HelpDialog(onDismiss = {
                helpOpen = false
                controller.dismissFirstRunHelp()
            })
        }
    }
}

@Composable
private fun GameTopBar(state: GameState, onStopActivity: () -> Unit, onOpenHelp: () -> Unit, onGearShortcut: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${state.character.name}  ·  Lv ${state.character.level}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).testTag("header_name"),
                )
                Text(
                    "${state.character.marks} Marks",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                TextButton(
                    onClick = onOpenHelp,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.testTag("btn_help"),
                ) {
                    Text("?", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(6.dp))
            val maxHp = if (state.character.maxHp > 0) state.character.maxHp else 1
            LinearProgressIndicator(
                progress = { state.character.health.toFloat() / maxHp },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Spacer(Modifier.height(6.dp))
            val activity = state.character.currentActivity
            val goal = nextGoal(state)
            val gearHint = goal.first
            val needsGear = goal.second
            if (activity != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        activityLabel(state, activity.type, activity.targetId),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f).testTag("activity_label"),
                    )
                    TextButton(onClick = onStopActivity, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp), modifier = Modifier.testTag("btn_stop_activity")) {
                        Text("Stop", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { activityProgress(state, activity.type, activity.targetId) },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(4.dp))
                HintRow(
                    activityRateHint(state, activity.type, activity.targetId) + gearHint,
                    needsGear,
                    onGearShortcut,
                )
            } else {
                Text(
                    "Idle — choose an activity below",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (gearHint.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    HintRow(gearHint, needsGear, onGearShortcut)
                }
            }
        }
    }
}

@Composable
private fun HintRow(text: String, needsGear: Boolean, onGearShortcut: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (needsGear) {
            TextButton(
                onClick = onGearShortcut,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                modifier = Modifier.testTag("btn_gear_shortcut"),
            ) {
                Text("⚒ Gear", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Fraction of the next action completed, derived from the carry counter so the
 * progress bar is real, not decorative.
 */
private fun activityProgress(state: GameState, type: ActivityType, targetId: String): Float {
    val carry = state.character.currentActivity?.carry ?: 0.0
    return (carry.coerceIn(0.0, 1.0)).toFloat()
}

/** Live per-hour rate for the current activity, plus the next-goal hint. */
private fun activityRateHint(state: GameState, type: ActivityType, targetId: String): String = when (type) {
    ActivityType.GATHERING -> {
        val node = Nodes.get(targetId)
        val rate = com.projecteternal.sim.Rates.gatheringActionsPerHour(state, node)
        "~${rate.toInt()}/hr · "
    }
    ActivityType.COMBAT -> {
        val rate = com.projecteternal.sim.Rates.killsPerHour(state, targetId)
        "~${rate.toInt().coerceAtLeast(1)} kills/hr · "
    }
    ActivityType.PROCESSING, ActivityType.CRAFTING -> {
        val rate = com.projecteternal.sim.Rates.recipeActionsPerHour(state, Recipes.get(targetId))
        "~${rate.toInt()}/hr · "
    }
}

/**
 * First incomplete objective of the first active quest, as a short hint.
 * The boolean is true when that objective is an equip/gear objective, so the
 * top bar can offer a deep link straight to the Gear tab.
 */
internal fun nextGoal(state: GameState): Pair<String, Boolean> {
    val active = state.quests.filter { it.status == com.projecteternal.model.QuestStatus.ACTIVE }
    for (progress in active) {
        val def = com.projecteternal.content.Quests.get(progress.questId)
        for (obj in def.objectives) {
            val current = com.projecteternal.sim.QuestEngine.currentValue(state, obj)
            if (current < obj.targetCount) {
                val detail = if (obj.targetCount > 1) " $current/${obj.targetCount}" else ""
                return "Next: ${obj.description}$detail" to (obj.type == ObjectiveType.EQUIP_SLOT)
            }
        }
    }
    return "" to false
}

internal fun nextGoalHint(state: GameState): String = nextGoal(state).first

internal fun activityLabel(state: GameState, type: ActivityType, targetId: String): String = when (type) {
    ActivityType.GATHERING -> "Gathering at ${Nodes.get(targetId).name}"
    ActivityType.COMBAT -> "Fighting ${Monsters.get(targetId).name}"
    ActivityType.PROCESSING -> "Processing: ${Recipes.get(targetId).name}"
    ActivityType.CRAFTING -> "Crafting: ${Recipes.get(targetId).name}"
}

@Composable
private fun SettingsScreen(state: GameState) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("About")
        InfoRow("Version", "0.1.0-slice")
        InfoRow("Save ID", state.saveId.take(8))
        InfoRow("Created", android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", state.createdEpochMs).toString())
        InfoRow("Playtime", formatPlayTime(state.totalPlaySeconds))
        HorizontalDivider()
        SectionTitle("Progress")
        InfoRow("Region", regionProgress(state))
        InfoRow("Unlocks", state.unlocks.size.toString())
        InfoRow("Max enhancement", "+${state.stats.maxEnhanceAchieved}")
        Spacer(Modifier.height(8.dp))
        Text(
            "Project Eternal — Phase 1 vertical slice. Fully offline; saves live in a Room database.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
internal fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

internal fun regionProgress(state: GameState): String {
    val visited = state.stats.visitedRegions
    return when {
        visited.contains("dawnreach") -> "Hollowreach → Emberreach → Stormreach → Dawnreach"
        visited.contains("stormreach") -> "Hollowreach → Emberreach → Stormreach"
        visited.contains("emberreach") -> "Hollowreach → Emberreach"
        else -> "Hollowreach"
    }
}

internal fun formatPlayTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

internal fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

/** Compact icon+name for an item stack / equipment def. */
@Composable
internal fun ItemChip(icon: String, name: String, count: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(icon, style = MaterialTheme.typography.titleMedium)
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (count != null) {
            Text(count, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    )
}
