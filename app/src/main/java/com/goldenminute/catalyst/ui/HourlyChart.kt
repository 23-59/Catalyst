package com.goldenminute.catalyst.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.data.Category
import com.goldenminute.catalyst.ui.theme.BorderColor
import com.goldenminute.catalyst.ui.theme.SlateDark
import com.goldenminute.catalyst.ui.theme.TextPrimary
import com.goldenminute.catalyst.ui.theme.TextSecondary
import com.goldenminute.catalyst.ui.theme.safeParseColor
import com.goldenminute.catalyst.ui.theme.NeonCyan
import com.goldenminute.catalyst.ui.theme.NeonPurple

@Composable
fun HourlyCategoryChart(
    title: String,
    labels: List<String>,
    hourlyData: List<Map<Int?, Int>>,
    categories: List<Category>
) {
    // Find the peak hour and its total value
    val hourTotals = hourlyData.map { it.values.sum() }
    val maxVal = hourTotals.maxOrNull()?.coerceAtLeast(1) ?: 1
    val peakHourIndex = hourTotals.indexOfFirst { it == maxVal && it > 0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateDark, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    if (peakHourIndex != -1) {
                        val isPersianChart = java.util.Locale.getDefault().language == "fa"
                        val peakText = stringResource(R.string.hourly_peak_format, peakHourIndex, maxVal)
                        Text(
                            text = if (isPersianChart) JalaliCalendarHelper.toPersianDigits(peakText) else peakText,
                            color = NeonPurple,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 1. Draw horizontal grid dashed lines
                        val gridLines = 4
                        val lineSpacing = size.height / gridLines
                        for (i in 0..gridLines) {
                            val y = i * lineSpacing
                            drawLine(
                                color = BorderColor.copy(alpha = 0.3f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }
                        
                        // 2. Draw bars
                        val barSpacing = size.width / (hourlyData.size + 1)
                        val barWidth = 12f // slightly wider for a more modern pill look
                        
                        hourlyData.forEachIndexed { index, hourMap ->
                            val x = barSpacing * (index + 1)
                            val totalVal = hourMap.values.sum().toFloat()
                            
                            // Bar background (thin column silhouette)
                            drawRoundRect(
                                color = BorderColor.copy(alpha = 0.1f),
                                topLeft = Offset(x - (barWidth/2), 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(barWidth/2)
                            )
                            
                            // Draw highlight column background if this is the peak hour
                            if (index == peakHourIndex) {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            NeonPurple.copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    ),
                                    topLeft = Offset(x - (barWidth * 1.5f), 0f),
                                    size = Size(barWidth * 3f, size.height),
                                    cornerRadius = CornerRadius(barWidth)
                                )
                            }
                            
                            // Bar foreground segments (stacked)
                            if (totalVal > 0) {
                                var currentY = size.height
                                hourMap.forEach { (catId, count) ->
                                    val cat = categories.find { it.id == catId }
                                    val catColor = if (cat != null) safeParseColor(cat.colorHex, Color.Gray) else Color.Gray
                                    val segmentHeight = size.height * (count.toFloat() / maxVal)
                                    
                                    // Make sure we cap the drawing within the canvas height
                                    val adjustedY = (currentY - segmentHeight).coerceAtLeast(0f)
                                    val drawHeight = (currentY - adjustedY).coerceAtLeast(1f)
                                    
                                    drawRoundRect(
                                        color = catColor,
                                        topLeft = Offset(x - (barWidth/2), adjustedY),
                                        size = Size(barWidth, drawHeight),
                                        cornerRadius = CornerRadius(barWidth/2)
                                    )
                                    currentY = adjustedY
                                }
                            }
                        }
                        
                        // 3. Draw a solid baseline
                        drawLine(
                            color = BorderColor.copy(alpha = 0.8f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2f
                        )
                    }
                }
                
                // Labels underneath the chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEach { label ->
                        Text(
                            text = label,
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
                
                // Categories Legend
                val activeCategories = categories.filter { cat ->
                    hourlyData.any { hourMap -> hourMap.containsKey(cat.id) }
                }
                if (activeCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeCategories.forEach { cat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(safeParseColor(cat.colorHex, Color.Gray), CircleShape)
                                )
                                Text(
                                    text = cat.name,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
