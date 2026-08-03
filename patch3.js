const fs = require('fs');
const path = 'c:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt';
let content = fs.readFileSync(path, 'utf8');

const t4 = `fun Context.findActivity(): android.app.Activity? {`;
const r4 = `    }

fun Context.findActivity(): android.app.Activity? {`;

let c4 = content.replace(t4, r4);
if (c4 === content) {
    console.log("Failed to match t4!");
} else {
    fs.writeFileSync(path, c4, 'utf8');
    console.log("File updated successfully.");
}
