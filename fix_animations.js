const fs = require('fs');

function removeAnimateColorAsState(filePath) {
    let content = fs.readFileSync(filePath, 'utf8');
    const original = content;

    // We want to match:
    // val animName by animateColorAsState(
    //     targetValue = SOMETHING,
    //     animationSpec = tween(...),
    //     label = "..."
    // )
    // And replace it with: val animName = SOMETHING

    // We can use a regex that matches `val XXX by animateColorAsState(` up to the closing `)`
    // Since Kotlin parenthesis can be nested, we can manually parse it or use a simpler regex.
    // Let's use a regex that matches the block.

    const regex = /val\s+([a-zA-Z0-9_]+)\s+by\s+animateColorAsState\s*\(\s*targetValue\s*=\s*([^,]+?)(?:,\s*animationSpec[^)]*\))?[^)]*\)/gs;
    
    // Wait, the targetValue could be an `if (...) { ... } else { ... }` or `if (...) A else B`
    // Let's use a custom parser to be safe, because regex with nested parentheses is dangerous.

    let index = 0;
    while ((index = content.indexOf('by animateColorAsState', index)) !== -1) {
        // find the start of 'val '
        let start = content.lastIndexOf('val ', index);
        let name = content.substring(start + 4, content.indexOf('by', start)).trim();

        // find targetValue
        let targetStart = content.indexOf('targetValue = ', index);
        if (targetStart === -1 || targetStart > index + 200) {
            index += 20;
            continue;
        }
        
        targetStart += 'targetValue = '.length;
        
        // now extract the expression for targetValue. It ends at the comma before animationSpec or label.
        let commaIndex = content.indexOf(',', targetStart);
        let targetValue = content.substring(targetStart, commaIndex).trim();
        
        // find the closing parenthesis of animateColorAsState
        let pCount = 0;
        let parenStart = content.indexOf('(', index);
        let end = -1;
        for (let i = parenStart; i < content.length; i++) {
            if (content[i] === '(') pCount++;
            if (content[i] === ')') {
                pCount--;
                if (pCount === 0) {
                    end = i;
                    break;
                }
            }
        }
        
        if (end !== -1) {
            let replacement = `val ${name} = ${targetValue}`;
            content = content.substring(0, start) + replacement + content.substring(end + 1);
            index = start + replacement.length;
        } else {
            index += 20;
        }
    }

    if (content !== original) {
        fs.writeFileSync(filePath, content, 'utf8');
        console.log(`Updated ${filePath}`);
    } else {
        console.log(`No changes made to ${filePath}`);
    }
}

const files = [
  'c:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/goldenminute/catalyst/ui/WelcomeScreen.kt',
  'c:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/goldenminute/catalyst/ui/SettingsScreen.kt',
  'c:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt'
];

files.forEach(removeAnimateColorAsState);
