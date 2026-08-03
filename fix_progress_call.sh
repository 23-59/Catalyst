#!/bin/bash
sed -i 's/\/\/ AI Insights Toggle/\/\/ AI Insights Toggle\n        CategoryDistributionSection(filteredTodos, categories)\n        Spacer(modifier = Modifier.height(16.dp))\n\n        \/\/ AI Insights Toggle/g' app/src/main/java/com/example/ui/ProgressScreen.kt
