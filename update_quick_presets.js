const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/ProgressScreen.kt', 'utf8');

const regex = /\/\/ Quick Preset Chips[\s\S]*?Row\([\s\S]*?modifier = Modifier\.fillMaxWidth\(\),[\s\S]*?horizontalArrangement = Arrangement\.SpaceBetween,[\s\S]*?verticalAlignment = Alignment\.CenterVertically[\s\S]*?\) \{[\s\S]*?Text\([\s\S]*?text = stringResource\(R\.string\.quick_presets\),[\s\S]*?color = TextSecondary,[\s\S]*?style = MaterialTheme\.typography\.labelSmall[\s\S]*?\)[\s\S]*?Row\(horizontalArrangement = Arrangement\.spacedBy\(6\.dp\)\) \{[\s\S]*?listOf\(1\.0f, 2\.0f, 3\.0f, 4\.0f, 6\.0f, 8\.0f\)\.forEach \{ preset ->[\s\S]*?val isSelected = dailyFocusGoalHours == preset[\s\S]*?Box\([\s\S]*?modifier = Modifier[\s\S]*?\.clip\(RoundedCornerShape\(10\.dp\)\)[\s\S]*?\.background\(if \(isSelected\) NeonCyan else SlateLight\.copy\(alpha = 0\.3f\)\)[\s\S]*?\.border\(1\.dp, if \(isSelected\) NeonCyan else BorderColor, RoundedCornerShape\(10\.dp\)\)[\s\S]*?\.clickable \{ viewModel\.setDailyFocusGoalHours\(preset\) \}[\s\S]*?\.padding\(horizontal = 8\.dp, vertical = 4\.dp\)[\s\S]*?\) \{[\s\S]*?Text\([\s\S]*?text = stringResource\(R\.string\.hours_short, preset\.toInt\(\)\),[\s\S]*?color = if \(isSelected\) ObsidianBlack else TextPrimary,[\s\S]*?style = MaterialTheme\.typography\.labelMedium\.copy\(fontWeight = FontWeight\.Bold\)[\s\S]*?\)[\s\S]*?\}[\s\S]*?\}[\s\S]*?\}[\s\S]*?\}/;

const replacement = `// Quick Preset Chips
            Column(
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

if (regex.test(content)) {
    content = content.replace(regex, replacement);
    fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/ProgressScreen.kt', content);
    console.log("Quick Presets updated via regex.");
} else {
    console.log("Regex for Quick Presets not matched.");
}
