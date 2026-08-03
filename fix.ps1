$filePath = "c:\Users\amirh\Downloads\hypertask_v2\app\src\main\java\com\goldenminute\catalyst\ui\TodoApp.kt"
$lines = Get-Content $filePath -Encoding UTF8
$keepLines = $lines[0..6868]
$ending = @"
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                border = BorderStroke(1.dp, BorderColor),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                            Button(
                                onClick = { onConfirm(title, content, selectedColorIndex) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
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

fun Context.findActivity(): android.app.Activity? {
    var cur = this
    while (cur is ContextWrapper) {
        if (cur is android.app.Activity) {
            return cur
        }
        cur = cur.baseContext
    }
    return null
}
"@
$keepLines | Out-File -FilePath $filePath -Encoding UTF8
Add-Content -Path $filePath -Value $ending -Encoding UTF8
