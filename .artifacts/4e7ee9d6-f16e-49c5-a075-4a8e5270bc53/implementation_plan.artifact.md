# Implementation Plan - Rollback System Bar Fixes (Second Rollback)

The user wants to rollback the latest attempt to fix the system bar theme sync issue. I will restore the app to its original state (including the custom splash screen and opaque system bars) while preserving the task description height increase.

## User Review Required

> [!WARNING]
> This rollback will restore the custom splash screen and its 2.5-second animation delay. The system bars will return to being opaque and may lose sync during language changes, as per the initial bug report.

## Proposed Changes

### [Component Name] UI & Theme Rollback

#### [MODIFY] [MainActivity.kt](file:///C:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/example/MainActivity.kt)
- Restore `import com.example.ui.SplashScreen`.
- Restore the original `enableEdgeToEdge` with hardcoded light style.
- Remove manual contrast enforcement overrides.
- Restore `showSplash` collection and `AnimatedContent` logic for switching between `SplashScreen` and main content.

#### [MODIFY] [Theme.kt](file:///C:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/example/ui/theme/Theme.kt)
- Remove `WindowCompat.setDecorFitsSystemWindows`.
- Restore `window.statusBarColor = colorScheme.background.toArgb()`.
- Remove manual contrast enforcement overrides.

#### [MODIFY] [TodoApp.kt](file:///C:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/example/ui/TodoApp.kt)
- Move the `drawBehind` gradient logic from the outermost `Box` back to the `AnimatedContent` inside the `Scaffold`.
- Remove `contentWindowInsets = WindowInsets(0, 0, 0, 0)` from the `Scaffold`.
- Remove `background(ObsidianBlack)` from the outermost `Box`.
- **PRESERVE**: Keep the updated `heightIn(min = 310.dp, max = 600.dp)` for the task description box.

#### [MODIFY] [TodoViewModel.kt](file:///C:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/example/ui/TodoViewModel.kt)
- Restore `showSplash` state and `dismissSplash()` function.

#### [NEW] [SplashScreen.kt](file:///C:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/example/ui/SplashScreen.kt)
- Recreate the file with its original high-fidelity content.

#### [MODIFY] [themes.xml](file:///C:/Users/amirh/Downloads/hypertask_v2/app/src/main/res/values/themes.xml) & [values-v31/themes.xml](file:///C:/Users/amirh/Downloads/hypertask_v2/app/src/main/res/values-v31/themes.xml)
- Remove the transparent bar color items.

## Verification Plan

### Manual Verification
1. Deploy the app.
2. Verify the custom splash screen is back.
3. Verify the task description height increase is preserved.
4. Verify system bars return to their original behavior.
