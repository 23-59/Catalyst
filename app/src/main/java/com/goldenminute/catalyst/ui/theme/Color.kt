package com.goldenminute.catalyst.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

var ObsidianBlack by mutableStateOf(Color(0xFF121214)) // Deep charcoal background
var SlateDark by mutableStateOf(Color(0xFF1A1A1E))     // Slightly lighter charcoal card base
var SlateLight by mutableStateOf(Color(0xFF26262B))    // Active console base / lighter charcoal
var SurfaceElevation: Dp by mutableStateOf(0.dp)

var NeonCyan by mutableStateOf(Color(0xFF00ADB5))      // Vibrant cyan core accent
var NeonPurple by mutableStateOf(Color(0xFF8B5CF6))    // Vibrant purple secondary
var NeonBlue by mutableStateOf(Color(0xFF3B82F6))      // Modern blue third accent

var PriorityHigh by mutableStateOf(Color(0xFFFF2D55))   // Vibrant Neon Red
var PriorityMedium by mutableStateOf(Color(0xFFFF9500)) // Vibrant Neon Orange
var PriorityLow by mutableStateOf(Color(0xFF30D158))    // Vibrant Neon Green

var TextPrimary by mutableStateOf(Color(0xFFF3F4F6))    // White/slate 50
var TextSecondary by mutableStateOf(Color(0xFF9CA3AF))  // Slate 400
var TextMuted by mutableStateOf(Color(0xFF6B7280))      // Slate 500
var BorderColor by mutableStateOf(Color(0x1F00ADB5))    // Subtle glowing vibrant cyan border

fun safeParseColor(colorHex: String, fallback: Color = Color.Gray): Color {
    if (colorHex.isBlank()) return fallback
    return try {
        val cleaned = if (colorHex.startsWith("#")) colorHex else "#$colorHex"
        Color(android.graphics.Color.parseColor(cleaned))
    } catch (e: Exception) {
        try {
            val hexVal = colorHex.removePrefix("#")
            if (hexVal.length == 6) {
                Color((hexVal.toLong(16) or 0xFF000000).toInt())
            } else if (hexVal.length == 8) {
                Color(hexVal.toLong(16).toInt())
            } else {
                fallback
            }
        } catch (ex: Exception) {
            fallback
        }
    }
}
