package com.goldenminute.catalyst.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.ui.theme.*

/**
 * A wrapper composable that adds a swipe-down-from-top gesture to enter full screen mode.
 *
 * When the inner scrollable content is at the top (cannot scroll backward), pulling down
 * beyond a threshold triggers [onEnterFullScreen]. A visual indicator shows the pull progress
 * and threshold state.
 *
 * @param isAlreadyFullScreen Whether full screen mode is already active (disables the gesture).
 * @param onEnterFullScreen Callback invoked when the swipe threshold is met and released.
 * @param content The scrollable content to wrap.
 */
@Composable
fun SwipeToFullScreenContainer(
    isAlreadyFullScreen: Boolean,
    canScrollBackward: Boolean,
    onEnterFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorTopPadding: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 120.dp.toPx() }

    // Accumulated pull distance (in px)
    var pullOffset by remember { mutableFloatStateOf(0f) }
    // Whether threshold has been reached (for haptic/visual feedback)
    val thresholdReached = pullOffset >= thresholdPx
    // Normalized progress 0..1
    val progress = (pullOffset / thresholdPx).coerceIn(0f, 1.5f)

    // Animate the indicator offset smoothly when releasing
    val animatedOffset by animateFloatAsState(
        targetValue = pullOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pullOffset"
    )

    val currentCanScrollBackward by rememberUpdatedState(canScrollBackward)
    val currentIsFullScreen by rememberUpdatedState(isAlreadyFullScreen)
    val currentOnEnterFullScreen by rememberUpdatedState(onEnterFullScreen)

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If we're already in fullscreen mode, don't consume any scroll
                if (currentIsFullScreen) return Offset.Zero

                // If user is scrolling UP and we have accumulated pull offset, reduce it first
                if (available.y < 0f && pullOffset > 0f) {
                    val consumed = available.y.coerceAtLeast(-pullOffset)
                    pullOffset += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If already fullscreen or content can still scroll backward, don't intercept
                if (currentIsFullScreen) return Offset.Zero

                // Only intercept downward overscroll when at the top of the list
                if (available.y > 0f && !currentCanScrollBackward) {
                    // Apply rubber-band damping effect
                    val dampingFactor = (1f - (pullOffset / (thresholdPx * 3f)).coerceIn(0f, 0.7f))
                    pullOffset += available.y * dampingFactor * 0.5f
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (currentIsFullScreen) return Velocity.Zero

                if (pullOffset >= thresholdPx) {
                    currentOnEnterFullScreen()
                }
                pullOffset = 0f
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .nestedScroll(nestedScrollConnection)
    ) {
        // Main content offset downward by the pull amount
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = animatedOffset * 0.3f  // Subtle parallax shift
                }
        ) {
            content()
        }

        if (pullOffset > 0f && !isAlreadyFullScreen) {
            SwipeToFullScreenIndicator(
                progress = progress,
                thresholdReached = thresholdReached,
                offsetY = animatedOffset,
                topPadding = indicatorTopPadding
            )
        }
    }
}

@Composable
private fun SwipeToFullScreenIndicator(
    progress: Float,
    thresholdReached: Boolean,
    offsetY: Float,
    topPadding: androidx.compose.ui.unit.Dp
) {
    val density = LocalDensity.current
    val indicatorOffsetDp = with(density) { (offsetY * 0.5f).toDp() }

    // Pulse animation when threshold reached
    val infiniteTransition = rememberInfiniteTransition(label = "thresholdPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Chevron rotation (rotates as user pulls)
    val chevronRotation by animateFloatAsState(
        targetValue = if (thresholdReached) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chevronRotation"
    )

    // Indicator scale when threshold is reached
    val indicatorScale by animateFloatAsState(
        targetValue = if (thresholdReached) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicatorScale"
    )

    val textToShow = if (thresholdReached) {
        stringResource(R.string.release_for_fullscreen)
    } else {
        stringResource(R.string.pull_down_fullscreen)
    }

    val accentColor = if (thresholdReached) NeonCyan else TextSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = topPadding - 20.dp + indicatorOffsetDp.coerceAtMost(60.dp)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = indicatorScale
                    scaleY = indicatorScale
                    alpha = if (thresholdReached) pulseAlpha else progress.coerceIn(0f, 1f)
                }
        ) {
            // Glowing pill bar that fills as progress increases
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SlateLight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = if (thresholdReached) {
                                    listOf(NeonCyan, NeonBlue)
                                } else {
                                    listOf(TextSecondary, TextMuted)
                                }
                            )
                        )
                )
            }

            // Icon + text row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateDark.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (thresholdReached) Icons.Default.Fullscreen else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
                Text(
                    text = textToShow,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (thresholdReached) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.3.sp,
                        fontSize = 11.sp
                    ),
                    color = accentColor
                )
            }
        }
    }
}
