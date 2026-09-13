package com.goldenminute.catalyst.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.service.SmartTrackerService
import com.goldenminute.catalyst.ui.theme.*
import java.util.Locale

/**
 * Palette of distinct colors for app section breakdowns.
 */
fun sectionColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF06B6D4), // Cyan
        Color(0xFF8B5CF6), // Purple
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFF3B82F6), // Blue
        Color(0xFFEC4899), // Pink
        Color(0xFF14B8A6), // Teal
    )
    return colors[index % colors.size]
}

/**
 * Returns an appropriate icon for each app section.
 */
fun getSectionIcon(section: String): ImageVector {
    return when (section.lowercase(Locale.ROOT)) {
        "home" -> Icons.Default.Home
        "explore", "discover", "popular" -> Icons.Default.Explore
        "reels", "shorts", "watch" -> Icons.Default.PlayCircleOutline
        "messages", "message", "direct", "inbox", "chat" -> Icons.Default.ChatBubbleOutline
        "profile" -> Icons.Default.Person
        "notifications", "notification", "activity" -> Icons.Default.NotificationsNone
        "shop", "marketplace" -> Icons.Default.Storefront
        else -> Icons.Default.Category
    }
}

/**
 * Formats a duration in milliseconds into a long readable string (e.g. 1h 25m 30s).
 */
fun formatDuration(ms: Long, isPersian: Boolean): String {
    val hours = ms / 3600000
    val minutes = (ms % 3600000) / 60000
    val seconds = (ms % 60000) / 1000
    return when {
        hours > 0 -> {
            if (isPersian) {
                val hStr = JalaliCalendarHelper.toPersianDigits(hours.toString())
                val mStr = JalaliCalendarHelper.toPersianDigits(minutes.toString())
                val sStr = JalaliCalendarHelper.toPersianDigits(seconds.toString())
                "$hStr ساعت $mStr دقیقه $sStr ثانیه"
            } else {
                String.format(Locale.US, "%dh %dm %ds", hours, minutes, seconds)
            }
        }
        minutes > 0 -> {
            if (isPersian) {
                val mStr = JalaliCalendarHelper.toPersianDigits(minutes.toString())
                val sStr = JalaliCalendarHelper.toPersianDigits(seconds.toString())
                "$mStr دقیقه $sStr ثانیه"
            } else {
                String.format(Locale.US, "%dm %ds", minutes, seconds)
            }
        }
        else -> {
            if (isPersian) {
                val sStr = JalaliCalendarHelper.toPersianDigits(seconds.toString())
                "$sStr ثانیه"
            } else {
                String.format(Locale.US, "%ds", seconds)
            }
        }
    }
}

/**
 * Formats a duration in milliseconds into a concise readable string,
 * supporting Persian numeral localization.
 */
fun formatDurationShort(ms: Long, isPersian: Boolean): String {
    val hours = ms / 3600000
    val minutes = (ms % 3600000) / 60000
    val seconds = (ms % 60000) / 1000
    return when {
        hours > 0 && minutes > 0 -> {
            if (isPersian) {
                val hStr = JalaliCalendarHelper.toPersianDigits(hours.toString())
                val mStr = JalaliCalendarHelper.toPersianDigits(minutes.toString())
                "$hStr ساعت $mStr دقیقه"
            } else {
                String.format(Locale.US, "%dh %dm", hours, minutes)
            }
        }
        hours > 0 -> {
            if (isPersian) {
                val hStr = JalaliCalendarHelper.toPersianDigits(hours.toString())
                "$hStr ساعت"
            } else {
                String.format(Locale.US, "%dh", hours)
            }
        }
        minutes > 0 -> {
            if (isPersian) {
                val mStr = JalaliCalendarHelper.toPersianDigits(minutes.toString())
                "$mStr دقیقه"
            } else {
                String.format(Locale.US, "%dm", minutes)
            }
        }
        seconds > 0 -> {
            if (isPersian) {
                val sStr = JalaliCalendarHelper.toPersianDigits(seconds.toString())
                "$sStr ثانیه"
            } else {
                String.format(Locale.US, "%ds", seconds)
            }
        }
        else -> if (isPersian) "۰ ثانیه" else "0s"
    }
}

/**
 * Resolves the full display name for an app section, e.g. "Instagram • Profile" or "اینستاگرام • پروفایل".
 */
fun getSectionDisplayName(
    packageName: String?,
    section: String,
    context: Context? = null,
    isPersian: Boolean = false
): String {
    val localizedSection = SmartTrackerService.localizeSectionName(section, isPersian)
    if (packageName.isNullOrBlank()) return localizedSection
    val appName = SmartTrackerService.getAppDisplayName(packageName, context, isPersian)
    return if (appName.isNotBlank()) "$appName • $localizedSection" else localizedSection
}

/**
 * Displays unified sections breakdown for a task:
 * Combines all recorded sessions of an assigned app and renders each section as a single section row
 * with its assigned app name, section name, icon, duration, and proportion progress bar.
 */
@Composable
fun UnifiedSectionsView(
    sections: Map<String, Long>,
    packageName: String?,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    if (sections.isEmpty()) return

    val context = LocalContext.current
    val totalSectionTime = remember(sections) { sections.values.sum().coerceAtLeast(1L) }
    val sortedSections = remember(sections) { sections.entries.sortedByDescending { it.value } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateDark)
            .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.app_sections_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = formatDurationShort(totalSectionTime, isPersian),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Horizontal segmented progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        ) {
            sortedSections.forEachIndexed { index, (_, time) ->
                val fraction = (time.toFloat() / totalSectionTime).coerceIn(0.01f, 1f)
                Box(
                    modifier = Modifier
                        .weight(fraction)
                        .fillMaxHeight()
                        .background(sectionColor(index))
                )
            }
        }

        // Section rows
        sortedSections.forEachIndexed { index, (section, time) ->
            val color = sectionColor(index)
            val displayName = getSectionDisplayName(packageName, section, context, isPersian)
            val icon = getSectionIcon(section)
            val fraction = (time.toFloat() / totalSectionTime).coerceIn(0.01f, 1f)
            val percent = (time * 100 / totalSectionTime).toInt()
            val percentStr = if (isPersian) "${JalaliCalendarHelper.toPersianDigits(percent.toString())}٪" else "$percent%"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateLight.copy(alpha = 0.6f))
                    .border(0.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatDurationShort(time, isPersian),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                maxLines = 1,
                                softWrap = false
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.copy(alpha = 0.14f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = percentStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = color,
                        trackColor = SlateDark
                    )
                }
            }
        }
    }
}
