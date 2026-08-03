#!/bin/bash
sed -i '/fun HourlyCategoryChart(title: String, labels: List<String>, values: List<Float>/,/^@Composable/d' app/src/main/java/com/example/ui/ProgressScreen.kt
