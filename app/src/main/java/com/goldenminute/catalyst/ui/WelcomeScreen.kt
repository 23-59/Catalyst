package com.goldenminute.catalyst.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap

@Composable
fun AnimatedCosmicBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "angle"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        translate(left = center.x, top = center.y) {
            rotate(degrees = angle) {
                // Draw glowing orbs
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(-w * 0.3f, -h * 0.3f),
                        radius = w * 0.8f * scale
                    ),
                    radius = w * 0.8f * scale,
                    center = Offset(-w * 0.3f, -h * 0.3f)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonPurple.copy(alpha = 0.2f), Color.Transparent),
                        center = Offset(w * 0.4f, h * 0.2f),
                        radius = w * 0.9f * scale
                    ),
                    radius = w * 0.9f * scale,
                    center = Offset(w * 0.4f, h * 0.2f)
                )
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val animSurface = SlateDark
    val animBorder = BorderColor

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(animSurface.copy(alpha = 0.85f))
            .border(1.dp, animBorder, RoundedCornerShape(32.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun WelcomeScreen(
    viewModel: TodoViewModel,
    onGetStarted: () -> Unit
) {
    val currentLang by viewModel.appLanguage.collectAsState()
    val currentTheme by viewModel.appTheme.collectAsState()

    var startAnimation by remember { mutableStateOf(false) }
    var isStarting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val animatedBgColor = ObsidianBlack
    val animatedPrimaryColor = NeonCyan
    val animatedSecondaryColor = NeonPurple

    var revealCenter by remember { mutableStateOf(Offset.Zero) }
    val themeRevealState = LocalThemeRevealState.current
    var isInitialThemeLoad by remember { mutableStateOf(true) }

    val overallScale by animateFloatAsState(
        targetValue = if (isStarting) 1.5f else 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "overallScale"
    )
    val overallAlpha by animateFloatAsState(
        targetValue = if (isStarting) 0f else 1f,
        animationSpec = tween(500, easing = LinearEasing),
        label = "overallAlpha"
    )

    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor)
    ) {
        AnimatedCosmicBackground()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .graphicsLayer {
                        scaleX = overallScale
                        scaleY = overallScale
                        alpha = overallAlpha
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
            
            // Header Section
            AnimatedVisibility(
                visible = startAnimation,
                enter = scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(tween(700))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.mipmap.ic_launcher_foreground),
                            contentDescription = "Logo",
                            modifier = Modifier.requiredSize(160.dp),
                            contentScale = ContentScale.Fit
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .border(2.dp, BorderColor, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.welcome_title),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 38.sp,
                        modifier = Modifier.testTag("welcome_title")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.welcome_subtitle),
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag("welcome_subtitle")
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Settings Section in GlassCard
            AnimatedVisibility(
                visible = startAnimation,
                enter = slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(700, delayMillis = 300)) + fadeIn(tween(700, delayMillis = 300))
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    // Language Selection
                    Text(
                        text = stringResource(R.string.choose_language),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LanguageCard(
                                languageCode = "en",
                                label = stringResource(R.string.lang_en),
                                isSelected = (currentLang == "en"),
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setLanguage("en") }
                            )

                            LanguageCard(
                                languageCode = "fa",
                                label = stringResource(R.string.lang_fa),
                                isSelected = (currentLang == "fa"),
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setLanguage("fa") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Theme Selection
                    Text(
                        text = stringResource(R.string.choose_theme),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 4x2 Grid for Themes using Column/Row
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ThemeCard(
                                themeKey = "cosmic_dark",
                                label = stringResource(R.string.theme_cosmic_dark),
                                isSelected = (currentTheme == "cosmic_dark"),
                                bgColor = Color(0xFF121214),
                                primaryColor = Color(0xFF00ADB5),
                                secondaryColor = Color(0xFF8B5CF6),
                                modifier = Modifier.weight(1f),
                                onClick = { offset, color -> revealCenter = offset; themeRevealState.requestReveal(offset); viewModel.setTheme("cosmic_dark") }
                            )
                            ThemeCard(
                                themeKey = "slate_light",
                                label = stringResource(R.string.theme_slate_light),
                                isSelected = (currentTheme == "slate_light"),
                                bgColor = Color(0xFFF3F4F6),
                                primaryColor = Color(0xFF2563EB),
                                secondaryColor = Color(0xFF4F46E5),
                                modifier = Modifier.weight(1f),
                                onClick = { offset, color -> revealCenter = offset; themeRevealState.requestReveal(offset); viewModel.setTheme("slate_light") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ThemeCard(
                                themeKey = "ocean_dark",
                                label = stringResource(R.string.theme_ocean_dark),
                                isSelected = (currentTheme == "ocean_dark"),
                                bgColor = Color(0xFF04151F),
                                primaryColor = Color(0xFF00E5FF),
                                secondaryColor = Color(0xFF00B4D8),
                                modifier = Modifier.weight(1f),
                                onClick = { offset, color -> revealCenter = offset; themeRevealState.requestReveal(offset); viewModel.setTheme("ocean_dark") }
                            )
                            ThemeCard(
                                themeKey = "forest_dark",
                                label = stringResource(R.string.theme_forest_dark),
                                isSelected = (currentTheme == "forest_dark"),
                                bgColor = Color(0xFF0D1B13),
                                primaryColor = Color(0xFF2DDA8A),
                                secondaryColor = Color(0xFF20C997),
                                modifier = Modifier.weight(1f),
                                onClick = { offset, color -> revealCenter = offset; themeRevealState.requestReveal(offset); viewModel.setTheme("forest_dark") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ThemeCard(
                                themeKey = "amber_dark",
                                label = stringResource(R.string.theme_amber_dark),
                                isSelected = (currentTheme == "amber_dark"),
                                bgColor = Color(0xFF1F1300),
                                primaryColor = Color(0xFFFF9F1C),
                                secondaryColor = Color(0xFFFFBF69),
                                modifier = Modifier.weight(1f),
                                onClick = { offset, color -> revealCenter = offset; themeRevealState.requestReveal(offset); viewModel.setTheme("amber_dark") }
                            )
                            ThemeCard(
                                themeKey = "rose_dark",
                                label = stringResource(R.string.theme_rose_dark),
                                isSelected = (currentTheme == "rose_dark"),
                                bgColor = Color(0xFF1A0B11),
                                primaryColor = Color(0xFFFF4D6D),
                                secondaryColor = Color(0xFFFF758F),
                                modifier = Modifier.weight(1f),
                                onClick = { offset, color -> revealCenter = offset; themeRevealState.requestReveal(offset); viewModel.setTheme("rose_dark") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ThemeCard(
                                themeKey = "monochrome_dark",
                                label = stringResource(R.string.theme_monochrome_dark),
                                isSelected = (currentTheme == "monochrome_dark"),
                                bgColor = Color(0xFF000000),
                                primaryColor = Color(0xFFFFFFFF),
                                secondaryColor = Color(0xFFE0E0E0),
                                modifier = Modifier.weight(1f),
                                onClick = { offset, color -> revealCenter = offset; themeRevealState.requestReveal(offset); viewModel.setTheme("monochrome_dark") }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // CTA
            AnimatedVisibility(
                visible = startAnimation,
                enter = slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(700, delayMillis = 600)) + fadeIn(tween(700, delayMillis = 600))
            ) {
                Button(
                    onClick = {
                        isStarting = true
                        scope.launch {
                            delay(450)
                            onGetStarted()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("get_started_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.get_started),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTheme == "monochrome_dark") Color.Black else Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (currentTheme == "monochrome_dark") Color.Black else Color.White
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
}

@Composable
fun LanguageCard(
    languageCode: String,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val alpha by animateFloatAsState(targetValue = if (isSelected) 1f else 0.5f, label = "alpha")

    val animCardBg = if (isSelected) SlateLight else SlateDark
    val animBorderColor = BorderColor
    val animPrimaryColor = NeonCyan
    val animSecondaryColor = NeonPurple
    val animTextPrimary = TextPrimary
    val animTextSecondary = TextSecondary

    Box(
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(animCardBg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) Brush.linearGradient(listOf(animPrimaryColor, animSecondaryColor)) else SolidColor(animBorderColor),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag("lang_card_$languageCode"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) animTextPrimary else animTextSecondary.copy(alpha = alpha)
            )
        }
        if (isSelected) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = animSecondaryColor,
                    modifier = Modifier.padding(end = 16.dp).size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ThemeCard(
    themeKey: String,
    label: String,
    isSelected: Boolean,
    bgColor: Color = Color.Transparent,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    onClick: (Offset, Color) -> Unit
) {
    var layoutCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val animCardBg = if (isSelected) SlateLight else SlateDark
    val animBorderColor = BorderColor
    val animPrimaryColor = NeonCyan
    val animSecondaryColor = NeonPurple
    val animTextPrimary = TextPrimary
    val animTextSecondary = TextSecondary
    
    Box(
        modifier = modifier
            .scale(scale)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(animCardBg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) Brush.linearGradient(listOf(animPrimaryColor, animSecondaryColor)) else SolidColor(animBorderColor),
                shape = RoundedCornerShape(16.dp)
            )
            .onGloballyPositioned { layoutCoordinates = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { localOffset ->
                        val globalOffset = layoutCoordinates?.localToWindow(localOffset) ?: Offset.Zero
                        onClick(globalOffset, bgColor)
                    }
                )
            }
            .testTag("theme_card_$themeKey"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) animTextPrimary else animTextSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).padding(end = 4.dp)
            )

            // Color palette preview (Background tone, Primary accent, Secondary accent)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (bgColor != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(secondaryColor)
                )
            }
        }
        
        if (isSelected) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopEnd) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(animPrimaryColor))
            }
        }
    }
}
