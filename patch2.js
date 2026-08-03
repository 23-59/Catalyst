const fs = require('fs');
const path = 'c:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/goldenminute/catalyst/ui/TodoApp.kt';
let content = fs.readFileSync(path, 'utf8');

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

const isWin = content.includes('\r\n');
const fix = s => isWin ? s.replace(/\n/g, '\r\n') : s;

let c4 = content.replace(fix(t4), fix(r4));
if (c4 === content) {
    console.log("Failed to match t4!");
} else {
    fs.writeFileSync(path, c4, 'utf8');
    console.log("File updated successfully.");
}
