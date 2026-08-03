#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/HourlyChart.kt
package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.example.data.Category
import com.example.ui.theme.BorderColor
import com.example.ui.theme.SlateDark
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.safeParseColor

@Composable
fun HourlyCategoryChart(
    title: String,
    labels: List<String>,
    hourlyData: List<Map<Int?, Int>>,
    categories: List<Category>
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(SlateDark, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(title, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
                
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridLines = 4
                        val lineSpacing = size.height / gridLines
                        for (i in 0..gridLines) {
                            drawLine(
                                color = BorderColor.copy(alpha = 0.5f),
                                start = Offset(0f, i * lineSpacing),
                                end = Offset(size.width, i * lineSpacing),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }
                        
                        val maxVal = hourlyData.maxOfOrNull { it.values.sum() }?.coerceAtLeast(1) ?: 1
                        val barSpacing = size.width / (hourlyData.size + 1)
                        val barWidth = 8f
                        
                        hourlyData.forEachIndexed { index, hourMap ->
                            val x = barSpacing * (index + 1)
                            val totalVal = hourMap.values.sum().toFloat()
                            val valHeight = totalVal / maxVal
                            val barHeight = size.height * valHeight
                            
                            // Bar background
                            drawRoundRect(
                                color = BorderColor,
                                topLeft = Offset(x - (barWidth/2), 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(barWidth/2)
                            )
                            
                            // Bar foreground (stacked)
                            if (totalVal > 0) {
                                var currentY = size.height
                                hourMap.forEach { (catId, count) ->
                                    val cat = categories.find { it.id == catId }
                                    val catColor = if (cat != null) safeParseColor(cat.colorHex, Color.Gray) else Color.Gray
                                    val segmentHeight = size.height * (count.toFloat() / maxVal)
                                    drawRoundRect(
                                        color = catColor,
                                        topLeft = Offset(x - (barWidth/2), currentY - segmentHeight),
                                        size = Size(barWidth, segmentHeight),
                                        cornerRadius = CornerRadius(barWidth/2) // Simplify corner radius
                                    )
                                    currentY -= segmentHeight
                                }
                            }
                        }
                    }
                }
                
                // Labels
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEach { label ->
                        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
INNER_EOF
chmod +x fix_chart.sh

