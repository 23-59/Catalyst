const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', 'utf8');

// 1. Remove drawWithContent from AnimatedContent modifier
const drawWithContentRegex = /\.drawWithContent \{[\s\S]*?val gradientColor = backgroundColor[\s\S]*?size = Size\(canvasSize\.width, gradientHeight\)[\s\S]*?\)[\s\S]*?\}/;
if (drawWithContentRegex.test(content)) {
    content = content.replace(drawWithContentRegex, '');
    console.log("Removed drawWithContent from AnimatedContent.");
} else {
    console.log("Could not find drawWithContent block.");
}

// 2. Wrap the when(screen) inside Box
const whenRegex = /\) \{ screen ->\s*when \(screen\) \{/;
const whenReplacement = `) { screen ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val canvasSize = size
                            val gradientHeight = bottomGapDp.toPx()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startY = canvasSize.height - gradientHeight,
                                    endY = canvasSize.height
                                ),
                                topLeft = Offset(0f, canvasSize.height - gradientHeight),
                                size = Size(canvasSize.width, gradientHeight),
                                blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                            )
                        }
                ) {
                    when (screen) {`;
if (whenRegex.test(content)) {
    content = content.replace(whenRegex, whenReplacement);
    console.log("Wrapped when(screen) in Box.");
} else {
    console.log("Could not find when(screen) block.");
}

// 3. Add closing brace for Box
const endRegex = /\} \/\/ End of AnimatedContent/;
const endReplacement = `    }
            } // End of AnimatedContent`;
if (endRegex.test(content)) {
    content = content.replace(endRegex, endReplacement);
    console.log("Added closing brace for Box.");
} else {
    console.log("Could not find End of AnimatedContent.");
}

fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', content);
