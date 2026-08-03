const fs = require('fs');

let content = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', 'utf8');

// 1. Remove background and drawBehind from Scaffold
const scaffoldTarget = /Scaffold\([\s\S]*?modifier = Modifier[\s\S]*?\.fillMaxSize\(\)[\s\S]*?\.background\(MaterialTheme\.colorScheme\.background\)[\s\S]*?\.drawBehind \{[\s\S]*?radius = r2[\s\S]*?\)[\s\S]*?\},[\s\S]*?containerColor = Color\.Transparent,/;

const scaffoldReplacement = `Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,`;

if (scaffoldTarget.test(content)) {
    content = content.replace(scaffoldTarget, scaffoldReplacement);
    console.log("Scaffold reverted.");
} else {
    console.log("Scaffold target not found.");
}

// 2. Add background, drawBehind to AnimatedContent and use backgroundColor for gradient
const animatedTarget = /AnimatedContent\([\s\S]*?targetState = currentScreen,[\s\S]*?modifier = Modifier[\s\S]*?\.fillMaxSize\(\)[\s\S]*?\.graphicsLayer \{ compositingStrategy = androidx\.compose\.ui\.graphics\.CompositingStrategy\.Offscreen \}[\s\S]*?\.drawWithContent \{[\s\S]*?drawRect\([\s\S]*?brush = Brush\.verticalGradient\([\s\S]*?colors = listOf\(Color\.Black, Color\.Transparent\),[\s\S]*?endY = canvasSize\.height[\s\S]*?\),[\s\S]*?blendMode = androidx\.compose\.ui\.graphics\.BlendMode\.DstIn[\s\S]*?\)[\s\S]*?\}[\s\S]*?\.hazeSource\(state = hazeState\),/;

const animatedReplacement = `AnimatedContent(
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
                    .background(backgroundColor)
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

if (animatedTarget.test(content)) {
    content = content.replace(animatedTarget, animatedReplacement);
    console.log("AnimatedContent reverted.");
} else {
    console.log("AnimatedContent target not found.");
}

fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', content);
