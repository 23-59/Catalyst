package com.goldenminute.catalyst.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.ui.theme.*
import java.util.Calendar
import java.util.Date

@Composable
fun JalaliDatePickerDialog(
    initialDate: Date?,
    onDismissResult: () -> Unit,
    onDateSelectedResult: (Date) -> Unit
) {
    val initialJalali = remember(initialDate) {
        val cal = Calendar.getInstance()
        if (initialDate != null) {
            cal.time = initialDate
        }
        JalaliCalendarHelper.gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    var viewYear by remember { mutableStateOf(initialJalali.year) }
    var viewMonth by remember { mutableStateOf(initialJalali.month) }

    var selectedYear by remember { mutableStateOf(initialJalali.year) }
    var selectedMonth by remember { mutableStateOf(initialJalali.month) }
    var selectedDay by remember { mutableStateOf(initialJalali.day) }

    val weekdays = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    AppDialog(onDismissRequest = onDismissResult) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
                border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                elevation = CardDefaults.cardElevation(defaultElevation = SurfaceElevation)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Status
                    Text(
                        text = stringResource(R.string.select_date),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val formattedSelectedDate = remember(selectedYear, selectedMonth, selectedDay) {
                        val mName = JalaliCalendarHelper.JalaliDate.getMonthName(selectedMonth)
                        val dStr = JalaliCalendarHelper.toPersianDigits(selectedDay.toString())
                        val yStr = JalaliCalendarHelper.toPersianDigits(selectedYear.toString())
                        "$dStr $mName $yStr"
                    }

                    Text(
                        text = formattedSelectedDate,
                        color = NeonPurple,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Month & Year Selector Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (viewMonth == 1) {
                                    viewMonth = 12
                                    viewYear--
                                } else {
                                    viewMonth--
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.previous_month),
                                tint = TextPrimary
                            )
                        }

                        val monthYearLabel = remember(viewYear, viewMonth) {
                            val mName = JalaliCalendarHelper.JalaliDate.getMonthName(viewMonth)
                            val yStr = JalaliCalendarHelper.toPersianDigits(viewYear.toString())
                            "$mName $yStr"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = monthYearLabel,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                if (viewMonth == 12) {
                                    viewMonth = 1
                                    viewYear++
                                } else {
                                    viewMonth++
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.next_month),
                                tint = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Weekday Titles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        weekdays.forEach { day ->
                            Text(
                                text = day,
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Days Grid
                    val totalDays = JalaliCalendarHelper.getJalaliMonthDays(viewYear, viewMonth)
                    val firstDayGregorian = JalaliCalendarHelper.jalaliToGregorian(viewYear, viewMonth, 1)
                    val startDayCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, firstDayGregorian.year)
                        set(Calendar.MONTH, firstDayGregorian.month - 1)
                        set(Calendar.DAY_OF_MONTH, firstDayGregorian.day)
                    }
                    val dayOfWeek = startDayCal.get(Calendar.DAY_OF_WEEK)
                    val weekdayOffset = JalaliCalendarHelper.getJalaliWeekdayIndex(dayOfWeek)

                    val gridList = remember(viewYear, viewMonth, totalDays, weekdayOffset) {
                        mutableListOf<Int?>().apply {
                            for (i in 0 until weekdayOffset) {
                                add(null)
                            }
                            for (i in 1..totalDays) {
                                add(i)
                            }
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(gridList.size) { index ->
                            val day = gridList[index]
                            if (day != null) {
                                val isSelected = selectedYear == viewYear &&
                                        selectedMonth == viewMonth &&
                                        selectedDay == day

                                val isFriday = (index % 7 == 6)

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) NeonCyan else Color.Transparent)
                                        .clickable {
                                            selectedYear = viewYear
                                            selectedMonth = viewMonth
                                            selectedDay = day
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = JalaliCalendarHelper.toPersianDigits(day.toString()),
                                        color = when {
                                            isSelected -> ObsidianBlack
                                            isFriday -> PriorityHigh
                                            else -> TextPrimary
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.aspectRatio(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Actions Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismissResult) {
                            Text(stringResource(R.string.cancel), color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = {
                                val gDate = JalaliCalendarHelper.jalaliToGregorian(
                                    selectedYear,
                                    selectedMonth,
                                    selectedDay
                                )
                                val resultCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, gDate.year)
                                    set(Calendar.MONTH, gDate.month - 1)
                                    set(Calendar.DAY_OF_MONTH, gDate.day)
                                }
                                onDateSelectedResult(resultCal.time)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.ok), color = ObsidianBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
