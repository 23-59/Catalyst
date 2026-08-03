const fs = require('fs');
let todoContent = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', 'utf8');

const regex = /SettingsScreen\([\s\S]*?bottomPadding = innerPadding\.calculateBottomPadding\(\) \+ 130\.dp\n\s*\)/;
const replacement = `SettingsScreen(
                                viewModel = viewModel,
                                bottomPadding = innerPadding.calculateBottomPadding() + 130.dp,
                                isThemeAnimating = themeRevealState.isAnimating
                            )`;

if (regex.test(todoContent)) {
    todoContent = todoContent.replace(regex, replacement);
    fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', todoContent);
    console.log('TodoApp updated successfully.');
} else {
    console.log('Regex target not found in TodoApp');
}
