const fs = require('fs');
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
