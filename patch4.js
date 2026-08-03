const fs = require('fs');

function patchFile(path, replacements) {
    let content = fs.readFileSync(path, 'utf8');
    let originalContent = content;
    
    for (const [target, replacement] of replacements) {
        if (!content.includes(target)) {
            console.error(`Failed to find target in ${path}:\n${target}`);
        } else {
            content = content.replace(target, replacement);
        }
    }
    
    if (content !== originalContent) {
        fs.writeFileSync(path, content, 'utf8');
        console.log(`Successfully patched ${path}`);
    } else {
        console.log(`No changes made to ${path}`);
    }
}

// 1. Patch Theme.kt
const themePath = 'c:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/goldenminute/catalyst/ui/theme/Theme.kt';
const themeTarget = `    val animSpec = tween<Color>(500)
    val aObsidianBlack by animateColorAsState(targetPalette.obsidianBlack, animSpec, label="ObsidianBlack")
    val aSlateDark by animateColorAsState(targetPalette.slateDark, animSpec, label="SlateDark")
    val aSlateLight by animateColorAsState(targetPalette.slateLight, animSpec, label="SlateLight")
    val aNeonCyan by animateColorAsState(targetPalette.neonCyan, animSpec, label="NeonCyan")
    val aNeonPurple by animateColorAsState(targetPalette.neonPurple, animSpec, label="NeonPurple")
    val aNeonBlue by animateColorAsState(targetPalette.neonBlue, animSpec, label="NeonBlue")
    val aTextPrimary by animateColorAsState(targetPalette.textPrimary, animSpec, label="TextPrimary")
    val aTextSecondary by animateColorAsState(targetPalette.textSecondary, animSpec, label="TextSecondary")
    val aTextMuted by animateColorAsState(targetPalette.textMuted, animSpec, label="TextMuted")
    val aBorderColor by animateColorAsState(targetPalette.borderColor, animSpec, label="BorderColor")`;
const themeReplacement = `    val aObsidianBlack = targetPalette.obsidianBlack
    val aSlateDark = targetPalette.slateDark
    val aSlateLight = targetPalette.slateLight
    val aNeonCyan = targetPalette.neonCyan
    val aNeonPurple = targetPalette.neonPurple
    val aNeonBlue = targetPalette.neonBlue
    val aTextPrimary = targetPalette.textPrimary
    val aTextSecondary = targetPalette.textSecondary
    val aTextMuted = targetPalette.textMuted
    val aBorderColor = targetPalette.borderColor`;

patchFile(themePath, [[themeTarget, themeReplacement]]);

// 2. Patch WelcomeScreen.kt
const welcomePath = 'c:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/goldenminute/catalyst/ui/WelcomeScreen.kt';

const wTarget1 = `            .padding(24.dp),`;
const wReplacement1 = `            .padding(16.dp),`;

const wTarget2 = `            Spacer(modifier = Modifier.height(32.dp))

            // Settings Section in GlassCard`;
const wReplacement2 = `            Spacer(modifier = Modifier.height(16.dp))

            // Settings Section in GlassCard`;

const wTarget3 = `            Spacer(modifier = Modifier.height(48.dp))

            // CTA`;
const wReplacement3 = `            Spacer(modifier = Modifier.height(24.dp))

            // CTA`;

const wTarget4 = `            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}`;
const wReplacement4 = `            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}`;

patchFile(welcomePath, [
    [wTarget1, wReplacement1],
    [wTarget2, wReplacement2],
    [wTarget3, wReplacement3],
    [wTarget4, wReplacement4]
]);
