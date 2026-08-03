const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', 'utf8');

const target = `                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                SlateLight.copy(alpha = 0.25f),
                                                SlateDark.copy(alpha = 0.75f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = BorderColor.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                // 1. Tactile Edit Button with Cyan Halo`;

const replacement = `                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                SlateLight.copy(alpha = 0.25f),
                                                SlateDark.copy(alpha = 0.75f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                // 1. Tactile Edit Button with Cyan Halo`;

if (content.includes(target)) {
    content = content.replace(target, replacement);
    fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', content);
    console.log('Successfully updated action capsule border color in TodoApp.kt via exact string match');
} else {
    console.log('Target block not found in TodoApp.kt. Let me print out what is exactly there:');
    const index = content.indexOf('// 1. Tactile Edit Button with Cyan Halo');
    console.log(content.substring(index - 500, index));
}
