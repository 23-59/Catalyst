const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', 'utf8');

const target = `                        } // End of AnimatedVisibility
            }
        ) { innerPadding ->`;

const replacement = `                        } // End of AnimatedVisibility
                    }
                }
            }
        ) { innerPadding ->`;

if (content.includes(target)) {
    content = content.replace(target, replacement);
    fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt', content);
    console.log('Fixed braces successfully');
} else {
    console.log('Target not found');
}
