const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', 'utf8');

const markerStart = '} // End of AnimatedVisibility';
const markerEnd = '.hazeSource(state = hazeState)';

const startIndex = content.indexOf(markerStart);
const endIndex = content.indexOf(markerEnd, startIndex);

if (startIndex !== -1 && endIndex !== -1) {
    const before = content.substring(0, startIndex + markerStart.length);
    const after = content.substring(endIndex);
    
    const replacement = `
            }
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val bottomGapDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 54.dp
            val backgroundColor = MaterialTheme.colorScheme.background
            AnimatedContent(
                targetState = currentScreen,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        val canvasSize = size
                        val gradientHeight = bottomGapDp.toPx()
                        val gradientColor = backgroundColor
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, gradientColor.copy(alpha = 0.85f), gradientColor, gradientColor),
                                startY = canvasSize.height - gradientHeight,
                                endY = canvasSize.height
                            ),
                            topLeft = Offset(0f, canvasSize.height - gradientHeight),
                            size = Size(canvasSize.width, gradientHeight)
                        )
                    }
                    `;
    
    content = before + replacement + after;
    fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', content);
    console.log('Restored correctly');
} else {
    console.log('Markers not found', startIndex, endIndex);
}
