const fs = require('fs');
let settingsContent = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/SettingsScreen.kt', 'utf8');

const targetSignature = `fun SettingsScreen(
    viewModel: TodoViewModel,
    bottomPadding: androidx.compose.ui.unit.Dp = 130.dp
) {`;
const replaceSignature = `fun SettingsScreen(
    viewModel: TodoViewModel,
    bottomPadding: androidx.compose.ui.unit.Dp = 130.dp,
    isThemeAnimating: Boolean = false
) {`;

const targetButton = `            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { 
                    newThemeName = ""
                    newThemeHue = 260f
                    isNewThemeLightMode = false
                    isNewThemeApplyToBg = true
                    showAddThemeDialog = true 
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(stringResource(R.string.add_custom_theme), fontWeight = FontWeight.SemiBold)
            }`;

const replaceButton = `            Spacer(modifier = Modifier.height(20.dp))
            AnimatedVisibility(
                visible = !isThemeAnimating,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 600, easing = LinearOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(50))
            ) {
                Button(
                    onClick = { 
                        newThemeName = ""
                        newThemeHue = 260f
                        isNewThemeLightMode = false
                        isNewThemeApplyToBg = true
                        showAddThemeDialog = true 
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(stringResource(R.string.add_custom_theme), fontWeight = FontWeight.SemiBold)
                }
            }`;

if (settingsContent.includes(targetSignature) && settingsContent.includes(targetButton)) {
    settingsContent = settingsContent.replace(targetSignature, replaceSignature);
    settingsContent = settingsContent.replace(targetButton, replaceButton);
    fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/SettingsScreen.kt', settingsContent);
    console.log('SettingsScreen updated successfully.');
} else {
    console.log('Targets not found in SettingsScreen');
}

let todoContent = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', 'utf8');

const targetTodoCall = `                            SettingsScreen(
                                viewModel = viewModel,
                                bottomPadding = innerPadding.calculateBottomPadding() + 130.dp
                            )`;

const replaceTodoCall = `                            SettingsScreen(
                                viewModel = viewModel,
                                bottomPadding = innerPadding.calculateBottomPadding() + 130.dp,
                                isThemeAnimating = themeRevealState.isAnimating
                            )`;

if (todoContent.includes(targetTodoCall)) {
    todoContent = todoContent.replace(targetTodoCall, replaceTodoCall);
    fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', todoContent);
    console.log('TodoApp updated successfully.');
} else {
    console.log('Target not found in TodoApp');
}
