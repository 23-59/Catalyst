const fs = require('fs');

const path = 'c:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt';
let content = fs.readFileSync(path, 'utf8');

const t1 = `    ) {
        CompositionLocalProvider(
            LocalContext provides currentLocalContext,
            LocalLayoutDirection provides currentLayoutDirection
        ) {
            Card(`;
const r1 = `    ) {
        val typography = MaterialTheme.typography
        CompositionLocalProvider(
            LocalContext provides currentLocalContext,
            LocalLayoutDirection provides currentLayoutDirection
        ) {
            MaterialTheme(typography = typography) {
                Card(`;

const t2 = `                                    R.string.create
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}`;
const r2 = `                                    R.string.create
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            }
        }
    }
}`;

const t3 = `        ) {
            CompositionLocalProvider(
                LocalContext provides currentLocalContext,
                LocalLayoutDirection provides currentLayoutDirection
            ) {
                Card(`;
const r3 = `        ) {
            val typography = MaterialTheme.typography
            CompositionLocalProvider(
                LocalContext provides currentLocalContext,
                LocalLayoutDirection provides currentLayoutDirection
            ) {
                MaterialTheme(typography = typography) {
                    Card(`;

const t4 = `                                Text(
                                    stringResource(R.string.save_note),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }`;
const r4 = `                                Text(
                                    stringResource(R.string.save_note),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }`;

// Normalize line endings in strings to match file if file uses \r\n
const isWin = content.includes('\r\n');
const fix = s => isWin ? s.replace(/\n/g, '\r\n') : s;

let c1 = content.replace(fix(t1), fix(r1));
if (c1 === content) console.log("Failed to match t1");
let c2 = c1.replace(fix(t2), fix(r2));
if (c2 === c1) console.log("Failed to match t2");
let c3 = c2.replace(fix(t3), fix(r3));
if (c3 === c2) console.log("Failed to match t3");
let c4 = c3.replace(fix(t4), fix(r4));
if (c4 === c3) console.log("Failed to match t4");

fs.writeFileSync(path, c4, 'utf8');
console.log("File updated successfully.");
