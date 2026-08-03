const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', 'utf8');

const backgroundCode = `.background(MaterialTheme.colorScheme.background)
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
                }`;

// Fix Scaffold
content = content.replace(/Scaffold\([\s\S]*?modifier = Modifier\.fillMaxSize\(\),[\s\S]*?containerColor = Color\.Transparent,/, `Scaffold(
            modifier = Modifier
                .fillMaxSize()
                ${backgroundCode},
            containerColor = Color.Transparent,`);

// Fix AnimatedContent
content = content.replace(/\.drawWithContent \{[\s\S]*?val gradientColor = backgroundColor[\s\S]*?size = Size\(canvasSize\.width, gradientHeight\)[\s\S]*?\)[\s\S]*?\}[\s\S]*?\.hazeSource\(state = hazeState\)[\s\S]*?\.background\(ObsidianBlack\)[\s\S]*?\.drawBehind \{[\s\S]*?radius = r2[\s\S]*?\)[\s\S]*?\},/, `.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
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
                    .hazeSource(state = hazeState),`);

fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', content);
console.log("Replaced via regex.");
