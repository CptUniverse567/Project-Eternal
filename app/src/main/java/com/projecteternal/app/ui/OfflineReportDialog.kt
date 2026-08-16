package com.projecteternal.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.projecteternal.content.Items
import com.projecteternal.content.Monsters
import com.projecteternal.content.Skills
import com.projecteternal.model.OfflineReport

@Composable
fun OfflineReportDialog(report: OfflineReport, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("btn_report_continue")) { Text("Continue") }
        },
        title = {
            Column {
                Text("While you were away", modifier = Modifier.testTag("txt_report_title"))
                Text(
                    if (report.elapsedClamped) "max ${formatDuration(report.elapsedSeconds)} simulated" else formatDuration(report.elapsedSeconds),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                report.activityLabel?.let {
                    LabeledRow("Activity", it)
                }
                if (report.charXpGained > 0) {
                    LabeledRow("Character XP", "+${report.charXpGained}")
                }
                if (report.newLevels.isNotEmpty()) {
                    LabeledRow("Levels up", report.newLevels.entries.joinToString(", ") { (key, value) ->
                        when {
                            key == "character" -> "Character Lv $value"
                            key.startsWith("skill:") -> "${Skills.get(key.removePrefix("skill:")).name} Lv $value"
                            else -> "$key Lv $value"
                        }
                    })
                }
                report.skillXpGained.entries
                    .filter { it.value > 0 }
                    .sortedBy { it.key }
                    .forEach { (skill, xp) ->
                        LabeledRow(Skills.get(skill).name, "+$xp xp")
                    }
                if (report.resourcesGained.isNotEmpty()) {
                    report.resourcesGained.entries.sortedBy { it.key }.forEach { (item, count) ->
                        LabeledRow(Items.get(item).name, "+$count")
                    }
                }
                if (report.marksGained > 0) LabeledRow("Marks", "+${report.marksGained}")
                if (report.kills.isNotEmpty()) {
                    report.kills.entries.sortedBy { it.key }.forEach { (monster, count) ->
                        LabeledRow(Monsters.get(monster).name, "${count} slain")
                    }
                }
                report.retainerOutput.forEach { (retainerId, output) ->
                    output.entries.forEach { (item, count) ->
                        LabeledRow("Retainer ${retainerId.takeLast(4)}", "+$count ${Items.get(item).name}")
                    }
                }
                if (report.notableEvents.isNotEmpty()) {
                    report.notableEvents.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                if (report.itemsBroken.isNotEmpty()) {
                    LabeledRow("Broken", report.itemsBroken.joinToString(", "))
                }
                if (report.questsCompleted.isNotEmpty()) {
                    LabeledRow("Quests", report.questsCompleted.joinToString(", "))
                }
                if (report.isEmpty()) {
                    Text(
                        "The Reach slumbered quietly. No progress was lost.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
    )
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
