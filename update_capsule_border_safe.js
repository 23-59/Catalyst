const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', 'utf8');

const regex = /width = 0\.5\.dp,\s*color = BorderColor\.copy\(alpha = 0\.6f\),\s*shape = RoundedCornerShape\(20\.dp\)/;
const replacement = `width = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(20.dp)`;

if (regex.test(content)) {
    content = content.replace(regex, replacement);
    fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', content);
    console.log('Successfully updated action capsule border color in TodoApp.kt via short regex');
} else {
    console.log('Target not found in TodoApp.kt');
}
