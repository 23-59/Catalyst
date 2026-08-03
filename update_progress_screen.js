const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/ProgressScreen.kt', 'utf8');

// 1. Fix overall progress high priority bug
const filterTarget = `        val filteredTodos = todos.filter {
            (it.completedDate != null && it.completedDate in startMs..endMs) ||
            (it.dueDate != null && it.dueDate in startMs..endMs)
        }`;
const filterReplacement = `        val filteredTodos = todos.filter {
            (it.completedDate != null && it.completedDate in startMs..endMs) ||
            (it.dueDate != null && it.dueDate in startMs..endMs) ||
            (!it.isCompleted && it.priority == 2)
        }`;

if (content.includes(filterTarget)) {
    content = content.replace(filterTarget, filterReplacement);
} else {
    console.log("Filter target not found!");
}

// 2. Fix the layout of Set Daily Goals dialog (lines ~1941)
const dialogRowTarget = `                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(1.0f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f).forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (tempGoal == preset) NeonCyan else SlateDark)
                                    .border(1.dp, if (tempGoal == preset) NeonCyan else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { tempGoal = preset }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.hours_short, preset.toInt()),
                                    color = if (tempGoal == preset) ObsidianBlack else TextPrimary,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }`;

const dialogRowReplacement = `                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1.0f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f).chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { preset ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (tempGoal == preset) NeonCyan else SlateDark)
                                            .border(1.dp, if (tempGoal == preset) NeonCyan else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { tempGoal = preset }
                                            .padding(horizontal = 4.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.hours_short, preset.toInt()),
                                            color = if (tempGoal == preset) ObsidianBlack else TextPrimary,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }`;

if (content.includes(dialogRowTarget)) {
    content = content.replace(dialogRowTarget, dialogRowReplacement);
} else {
    console.log("Dialog Row target not found!");
}

// 3. Fix the Quick Preset Chips in InsightsScreen body (lines ~2178)
const quickPresetsTarget = `            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.quick_presets),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1.0f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f).forEach { preset ->
                        val isSelected = dailyFocusGoalHours == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan else SlateLight.copy(alpha = 0.3f))
                                .border(1.dp, if (isSelected) NeonCyan else BorderColor, RoundedCornerShape(10.dp))
                                .clickable { viewModel.setDailyFocusGoalHours(preset) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hours_short, preset.toInt()),
                                color = if (isSelected) ObsidianBlack else TextPrimary,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }`;

const quickPresetsReplacement = `            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.quick_presets),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1.0f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f).chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { preset ->
                                val isSelected = dailyFocusGoalHours == preset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NeonCyan else SlateLight.copy(alpha = 0.3f))
                                        .border(1.dp, if (isSelected) NeonCyan else BorderColor, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.setDailyFocusGoalHours(preset) }
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.hours_short, preset.toInt()),
                                        color = if (isSelected) ObsidianBlack else TextPrimary,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }`;

if (content.includes(quickPresetsTarget)) {
    content = content.replace(quickPresetsTarget, quickPresetsReplacement);
} else {
    console.log("Quick Presets target not found!");
}

fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/ProgressScreen.kt', content);
console.log("Done.");
