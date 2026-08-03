const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/FocusScreen.kt', 'utf8');

const target = `                .border(
                    BorderStroke(
                        width = 1.5.dp,
                        color = if (isTimerRunning) TextPrimary.copy(alpha = 0.4f * glowPulse) else BorderColor
                    ),
                    shape = RoundedCornerShape(32.dp)
                )`;

const replace = `                .border(
                    BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(32.dp)
                )`;

if (content.includes(target)) {
    content = content.replace(target, replace);
    fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/FocusScreen.kt', content);
    console.log('FocusScreen action capsule border updated');
} else {
    console.log('Target not found in FocusScreen');
}
