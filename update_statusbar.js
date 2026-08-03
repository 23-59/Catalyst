const fs = require('fs');

// Update Theme.kt
let themeContent = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/theme/Theme.kt', 'utf8');

const themeTarget = `            if (isLight) {
                window.statusBarColor = android.graphics.Color.BLACK
                window.navigationBarColor = android.graphics.Color.BLACK
            } else {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
            
            WindowCompat.getInsetsController(window, view).apply {
                // We use white foreground (false) for both dark mode and the new black system bars in light mode
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }`;

const themeReplacement = `            if (isLight) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = android.graphics.Color.BLACK
            } else {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
            
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = isLight
                isAppearanceLightNavigationBars = false
            }`;

if (themeContent.includes(themeTarget)) {
    themeContent = themeContent.replace(themeTarget, themeReplacement);
    fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/theme/Theme.kt', themeContent);
    console.log('Successfully updated Theme.kt');
} else {
    console.log('Target block not found in Theme.kt');
}

// Update MainActivity.kt
let mainContent = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/MainActivity.kt', 'utf8');

const mainTarget = `                enableEdgeToEdge(
                    statusBarStyle = if (isLight) {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK)
                    } else {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    },`;

const mainReplacement = `                enableEdgeToEdge(
                    statusBarStyle = if (isLight) {
                        androidx.activity.SystemBarStyle.light(palette.obsidianBlack.toArgb(), palette.obsidianBlack.toArgb())
                    } else {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    },`;

if (mainContent.includes(mainTarget)) {
    mainContent = mainContent.replace(mainTarget, mainReplacement);
    fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/MainActivity.kt', mainContent);
    console.log('Successfully updated MainActivity.kt');
} else {
    console.log('Target block not found in MainActivity.kt');
}
