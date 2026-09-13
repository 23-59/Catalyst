const fs = require('fs');
const file = 'c:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt';
let content = fs.readFileSync(file, 'utf8');
const index = content.indexOf('if (associatedPackageName?.isNotEmpty() == true) {');
console.log(content.substring(index, index + 500));
