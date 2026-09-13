const fs = require('fs');
const file = 'c:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt';
let content = fs.readFileSync(file, 'utf8');

const startIndex = content.indexOf('if (associatedPackageName?.isNotEmpty() == true) {');
const endIndex = content.indexOf('// Daily task switch row');

if (startIndex !== -1 && endIndex !== -1) {
    const sectionToReplace = content.substring(startIndex, endIndex);
    const newSection = `if (associatedPackageName?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Tracking Mode",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            val modes = listOf(
                                "Strict" to "No distractions. Leaving the app triggers an alert.",
                                "Creative" to "Relaxed tracking. Gather inspiration freely."
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                modes.forEach { (mode, desc) ->
                                    val isSelected = trackingMode == mode
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { trackingMode = mode },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) NeonCyan.copy(alpha = 0.15f) else SlateLight,
                                        ),
                                        border = BorderStroke(1.5.dp, if (isSelected) NeonCyan else BorderColor)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = mode,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSelected) NeonCyan else TextPrimary
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = NeonCyan,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    `;
    content = content.replace(sectionToReplace, newSection);
    fs.writeFileSync(file, content);
    console.log("Replaced successfully!");
} else {
    console.log("Could not find start or end index.");
}
