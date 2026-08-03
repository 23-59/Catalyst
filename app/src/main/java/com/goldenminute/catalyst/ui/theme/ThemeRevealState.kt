package com.goldenminute.catalyst.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * Holds the state for a circular-reveal theme transition.
 *
 * When a theme card is tapped:
 *  1. The tap handler calls [requestReveal] with the global (window) offset.
 *  2. MainActivity captures the current View as a bitmap ("old" screenshot).
 *  3. The theme is changed, so the composable tree re-renders with the new palette.
 *  4. An expanding circle clips a hole in the old-screenshot overlay, progressively
 *     revealing the new theme underneath.
 */
class ThemeRevealState {
    /** Centre of the reveal circle in window coordinates. */
    var center: Offset = Offset.Zero
        private set

    var onRevealRequested: ((Offset) -> Unit)? = null

    /** Indicates whether the theme reveal animation is currently running. */
    var isAnimating: Boolean by androidx.compose.runtime.mutableStateOf(false)

    /** Called from theme-card tap handlers. */
    fun requestReveal(windowOffset: Offset) {
        center = windowOffset
        isAnimating = true
        onRevealRequested?.invoke(windowOffset)
    }
}

/** CompositionLocal so every screen can access the shared reveal state. */
val LocalThemeRevealState = compositionLocalOf { ThemeRevealState() }
