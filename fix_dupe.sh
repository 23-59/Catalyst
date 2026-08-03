#!/bin/bash
awk '/@Composable/{
    if (in_composable) {
        next
    } else {
        in_composable = 1
        print
    }
}
!/@Composable/{
    in_composable = 0
    print
}' app/src/main/java/com/example/ui/ProgressScreen.kt > temp.kt
mv temp.kt app/src/main/java/com/example/ui/ProgressScreen.kt
