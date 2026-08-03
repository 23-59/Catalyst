package com.goldenminute.catalyst.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

data class AppPalette(
    val obsidianBlack: Color,
    val slateDark: Color,
    val slateLight: Color,
    val neonCyan: Color,
    val neonPurple: Color,
    val neonBlue: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderColor: Color
)

fun getThemePalette(themeKey: String, customColor: Int, customApplyToBg: Boolean): AppPalette {
    return when (themeKey) {
        "slate_light" -> AppPalette(
            obsidianBlack = Color(0xFFF3F4F6),
            slateDark = Color(0xFFFFFFFF),
            slateLight = Color(0xFFE5E7EB),
            neonCyan = Color(0xFF2563EB),
            neonPurple = Color(0xFF4F46E5),
            neonBlue = Color(0xFF3B82F6),
            textPrimary = Color(0xFF111827),
            textSecondary = Color(0xFF4B5563),
            textMuted = Color(0xFF9CA3AF),
            borderColor = Color(0xFFD1D5DB)
        )
        "ocean_dark" -> AppPalette(
            obsidianBlack = Color(0xFF04151F),
            slateDark = Color(0xFF0B253A),
            slateLight = Color(0xFF133654),
            neonCyan = Color(0xFF00E5FF),
            neonPurple = Color(0xFF00B4D8),
            neonBlue = Color(0xFF0077B6),
            textPrimary = Color(0xFFE0FBFC),
            textSecondary = Color(0xFF98C1D9),
            textMuted = Color(0xFF5C8099),
            borderColor = Color(0x3300E5FF)
        )
        "forest_dark" -> AppPalette(
            obsidianBlack = Color(0xFF0D1B13),
            slateDark = Color(0xFF14291D),
            slateLight = Color(0xFF1C3A2A),
            neonCyan = Color(0xFF2DDA8A),
            neonPurple = Color(0xFF20C997),
            neonBlue = Color(0xFF12A078),
            textPrimary = Color(0xFFE8F5E9),
            textSecondary = Color(0xFFA5D6A7),
            textMuted = Color(0xFF63946E),
            borderColor = Color(0x332DDA8A)
        )
        "amber_dark" -> AppPalette(
            obsidianBlack = Color(0xFF1F1300),
            slateDark = Color(0xFF332000),
            slateLight = Color(0xFF4C2F00),
            neonCyan = Color(0xFFFF9F1C),
            neonPurple = Color(0xFFFFBF69),
            neonBlue = Color(0xFFCB7900),
            textPrimary = Color(0xFFFFF3E0),
            textSecondary = Color(0xFFFFCC80),
            textMuted = Color(0xFF9E7E5B),
            borderColor = Color(0x33FF9F1C)
        )
        "rose_dark" -> AppPalette(
            obsidianBlack = Color(0xFF1A0B11),
            slateDark = Color(0xFF2C131D),
            slateLight = Color(0xFF401C2B),
            neonCyan = Color(0xFFFF4D6D),
            neonPurple = Color(0xFFFF758F),
            neonBlue = Color(0xFFC9184A),
            textPrimary = Color(0xFFFFF0F3),
            textSecondary = Color(0xFFFFB3C6),
            textMuted = Color(0xFF936876),
            borderColor = Color(0x33FF4D6D)
        )
        "monochrome_dark" -> AppPalette(
            obsidianBlack = Color(0xFF000000),
            slateDark = Color(0xFF121212),
            slateLight = Color(0xFF242424),
            neonCyan = Color(0xFFFFFFFF),
            neonPurple = Color(0xFFE0E0E0),
            neonBlue = Color(0xFF9E9E9E),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFAAAAAA),
            textMuted = Color(0xFF666666),
            borderColor = Color(0xFF333333)
        )
        else -> if (themeKey.startsWith("custom")) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(customColor, hsv)
            val s = hsv[1]
            val v = hsv[2]
            
            val tNeonCyan = Color(customColor)
            
            hsv[1] = (s * 0.7f).coerceIn(0f, 1f)
            hsv[2] = (v * 1.2f).coerceIn(0f, 1f)
            val tNeonPurple = Color(android.graphics.Color.HSVToColor(hsv))
            
            hsv[1] = (s * 1.2f).coerceIn(0f, 1f)
            hsv[2] = (v * 0.8f).coerceIn(0f, 1f)
            val tNeonBlue = Color(android.graphics.Color.HSVToColor(hsv))
            
            val isLight = themeKey.startsWith("custom_light")
            
            val tObsidianBlack: Color
            val tSlateDark: Color
            val tSlateLight: Color
            val textColorPrimary: Color
            val textColorSecondary: Color
            val textColorMuted: Color

            if (isLight) {
                tObsidianBlack = Color(0xFFF3F4F6)
                tSlateDark = Color(0xFFFFFFFF)
                tSlateLight = Color(0xFFE5E7EB)

                textColorPrimary = Color(0xFF111827)
                textColorSecondary = Color(0xFF4B5563)
                textColorMuted = Color(0xFF9CA3AF)
            } else {
                tObsidianBlack = Color(0xFF121214)
                tSlateDark = Color(0xFF1A1A1E)
                tSlateLight = Color(0xFF26262B)

                textColorPrimary = Color(0xFFF8F9FA)
                textColorSecondary = Color(0xFFB0A8B9)
                textColorMuted = Color(0xFF6B6578)
            }
            
            AppPalette(
                obsidianBlack = tObsidianBlack,
                slateDark = tSlateDark,
                slateLight = tSlateLight,
                neonCyan = tNeonCyan,
                neonPurple = tNeonPurple,
                neonBlue = tNeonBlue,
                textPrimary = textColorPrimary,
                textSecondary = textColorSecondary,
                textMuted = textColorMuted,
                borderColor = if (isLight) Color(0x26000000) else Color(0x1FFFFFFF)
            )
        } else AppPalette( // cosmic_dark
            obsidianBlack = Color(0xFF121214),
            slateDark = Color(0xFF1A1A1E),
            slateLight = Color(0xFF26262B),
            neonCyan = Color(0xFF00ADB5),
            neonPurple = Color(0xFF8B5CF6),
            neonBlue = Color(0xFF3B82F6),
            textPrimary = Color(0xFFF3F4F6),
            textSecondary = Color(0xFF9CA3AF),
            textMuted = Color(0xFF6B7280),
            borderColor = Color(0x1F00ADB5)
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeKey: String = "cosmic_dark",
    customColor: Int = android.graphics.Color.parseColor("#9D4EDD"),
    customApplyToBg: Boolean = true,
    languageCode: String = "en",
    content: @Composable () -> Unit
) {
    val targetPalette = getThemePalette(themeKey, customColor, customApplyToBg)

    val aObsidianBlack = targetPalette.obsidianBlack
    val aSlateDark = targetPalette.slateDark
    val aSlateLight = targetPalette.slateLight
    val aNeonCyan = targetPalette.neonCyan
    val aNeonPurple = targetPalette.neonPurple
    val aNeonBlue = targetPalette.neonBlue
    val aTextPrimary = targetPalette.textPrimary
    val aTextSecondary = targetPalette.textSecondary
    val aTextMuted = targetPalette.textMuted
    val aBorderColor = targetPalette.borderColor

    SideEffect {
        ObsidianBlack = aObsidianBlack
        SlateDark = aSlateDark
        SlateLight = aSlateLight
        NeonCyan = aNeonCyan
        NeonPurple = aNeonPurple
        NeonBlue = aNeonBlue
        TextPrimary = aTextPrimary
        TextSecondary = aTextSecondary
        TextMuted = aTextMuted
        BorderColor = aBorderColor
        SurfaceElevation = if (themeKey == "slate_light" || themeKey.startsWith("custom_light")) 2.dp else 0.dp
        
        PriorityHigh = Color(0xFFFF2D55)
        PriorityMedium = Color(0xFFFF9500)
        PriorityLow = Color(0xFF30D158)
    }

    val colorScheme = if (themeKey == "slate_light" || themeKey.startsWith("custom_light")) {
        lightColorScheme(
            primary = aNeonCyan,
            onPrimary = Color.White,
            secondary = aNeonPurple,
            onSecondary = Color.White,
            tertiary = aNeonBlue,
            background = aObsidianBlack,
            onBackground = aTextPrimary,
            surface = aSlateDark,
            onSurface = aTextPrimary,
            surfaceVariant = aSlateLight,
            onSurfaceVariant = aTextPrimary,
            outline = aBorderColor,
            error = PriorityHigh
        )
    } else {
        darkColorScheme(
            primary = aNeonCyan,
            onPrimary = aObsidianBlack,
            secondary = aNeonPurple,
            onSecondary = Color.White,
            tertiary = aNeonBlue,
            background = aObsidianBlack,
            onBackground = aTextPrimary,
            surface = aSlateDark,
            onSurface = aTextPrimary,
            surfaceVariant = aSlateLight,
            onSurfaceVariant = aTextPrimary,
            outline = aBorderColor,
            error = PriorityHigh
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val isLight = themeKey == "slate_light" || themeKey.startsWith("custom_light")
            
            if (isLight) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = android.graphics.Color.BLACK
            } else {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
            
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = isLight
                isAppearanceLightNavigationBars = false
            }
        }
    }

    val typography = if (languageCode == "fa") {
        getTypography(Vazirmatn)
    } else {
        getTypography(FontFamily.Default)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
