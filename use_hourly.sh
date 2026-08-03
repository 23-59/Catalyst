#!/bin/bash
sed -i 's/val hourlyData = remember(todos)/val hourlyData = remember(todos, categories)/g' app/src/main/java/com/example/ui/ProgressScreen.kt
sed -i 's/val hours = IntArray(24) { 0 }/val hours = Array(24) { mutableMapOf<Int?, Int>() }/g' app/src/main/java/com/example/ui/ProgressScreen.kt
sed -i 's/hours\[hour\]++/hours[hour][todo.categoryId] = (hours[hour][todo.categoryId] ?: 0) + 1/g' app/src/main/java/com/example/ui/ProgressScreen.kt
sed -i 's/if (hourlyData.any { it > 0 })/if (hourlyData.any { it.isNotEmpty() })/g' app/src/main/java/com/example/ui/ProgressScreen.kt
sed -i 's/ChartBox(/HourlyCategoryChart(/g' app/src/main/java/com/example/ui/ProgressScreen.kt
sed -i 's/values = hourlyData.map { it.toFloat() },/hourlyData = hourlyData,\n                categories = categories/g' app/src/main/java/com/example/ui/ProgressScreen.kt
sed -i '/isHourly = true/d' app/src/main/java/com/example/ui/ProgressScreen.kt
