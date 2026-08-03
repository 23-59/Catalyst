import re

with open('app/src/main/java/com/example/ui/TodoApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_logic = """                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    themes.chunked(2).forEach { rowThemes ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowThemes.forEach { (themeId, themeName, colorAccent) ->
                                val isSelected = currentTheme == themeId
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            color = if (isSelected) colorAccent.copy(alpha = 0.15f) else SlateDark,
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) colorAccent else BorderColor,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { viewModel.setTheme(themeId) }
                                        .padding(horizontal = 12.dp)
                                        .testTag("theme_${themeId}_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(colorAccent)
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = if (themeId == "monochrome_dark" || themeId == "amber_dark" || themeId == "slate_light") Color.Black else Color.White,
                                                modifier = Modifier.align(Alignment.Center).size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = themeName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (rowThemes.size < 2) {
                                repeat(2 - rowThemes.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }"""

new_logic = """                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    themes.forEach { (themeId, themeName, colorAccent) ->
                        val isSelected = currentTheme == themeId
                        val isLight = themeId == "slate_light"
                        
                        Column(
                            modifier = Modifier
                                .width(96.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) colorAccent.copy(alpha = 0.1f) else SlateDark)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) colorAccent else BorderColor,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.setTheme(themeId) }
                                .padding(12.dp)
                                .testTag("theme_${themeId}_button"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(
                                                colorAccent,
                                                colorAccent.copy(alpha = 0.6f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isLight) Color.White else ObsidianBlack,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (themeId == "monochrome_dark" || themeId == "amber_dark" || themeId == "slate_light" || themeId == "forest_dark") Color.Black else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = themeName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) TextPrimary else TextSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                minLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }"""

content = content.replace(old_logic, new_logic)

with open('app/src/main/java/com/example/ui/TodoApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
