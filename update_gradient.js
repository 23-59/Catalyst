const fs = require('fs');

let content = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', 'utf8');

// 1. Move background and drawBehind from AnimatedContent to Scaffold
const scaffoldTarget = `        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,`;

const scaffoldReplacement = `        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .drawBehind {
                    val r1 = maxOf(1f, size.maxDimension * 0.45f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x246366F1), Color.Transparent),
                            center = Offset(x = 0f, y = 0f),
                            radius = r1
                        ),
                        center = Offset(x = 0f, y = 0f),
                        radius = r1
                    )
                    val r2 = maxOf(1f, size.maxDimension * 0.55f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x2B8B5CF6), Color.Transparent),
                            center = Offset(x = size.width, y = size.height * 0.85f),
                            radius = r2
                        ),
                        center = Offset(x = size.width, y = size.height * 0.85f),
                        radius = r2
                    )
                },
            containerColor = Color.Transparent,`;

if (content.includes(scaffoldTarget)) {
    content = content.replace(scaffoldTarget, scaffoldReplacement);
    console.log("Scaffold updated successfully.");
} else {
    console.log("Scaffold target not found.");
}

// 2. Change AnimatedContent's drawWithContent to use BlendMode.DstIn and remove background/drawBehind
const animatedTarget = `            AnimatedContent(
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
                    .hazeSource(state = hazeState)
                    .background(ObsidianBlack)
                    .drawBehind {
                        val r1 = maxOf(1f, size.maxDimension * 0.45f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x246366F1), Color.Transparent),
                                center = Offset(x = 0f, y = 0f),
                                radius = r1
                            ),
                            center = Offset(x = 0f, y = 0f),
                            radius = r1
                        )
                        val r2 = maxOf(1f, size.maxDimension * 0.55f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x2B8B5CF6), Color.Transparent),
                                center = Offset(x = size.width, y = size.height * 0.85f),
                                radius = r2
                            ),
                            center = Offset(x = size.width, y = size.height * 0.85f),
                            radius = r2
                        )
                    },`;

const animatedReplacement = `            AnimatedContent(
                targetState = currentScreen,
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
                    .hazeSource(state = hazeState),`;

if (content.includes(animatedTarget)) {
    content = content.replace(animatedTarget, animatedReplacement);
    console.log("AnimatedContent updated successfully.");
} else {
    console.log("AnimatedContent target not found.");
}

fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', content);
