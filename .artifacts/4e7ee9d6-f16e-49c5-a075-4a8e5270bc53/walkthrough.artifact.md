# Walkthrough - Rollback of System Bar Fixes

I have rolled back the recent attempts to fix the system bar theme synchronization issues. The app has been restored to its original state, including the custom splash screen and standard system bar behavior.

## Changes Rolled Back

### 1. Splash Screen
- Re-introduced [SplashScreen.kt](file:///C:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/example/ui/SplashScreen.kt) with its original high-fidelity animations.
- Restored the 2.5-second splash transition logic in `MainActivity.kt` and `TodoViewModel.kt`.

### 2. System UI Styling
- Reverted `MainActivity.kt` to its original `enableEdgeToEdge` configuration with hardcoded light status and navigation bars.
- Reverted `Theme.kt` to use opaque status bars (`colorScheme.background`) and removed the manual contrast overrides.

### 3. Layout & Insets
- In `TodoApp.kt`, the background gradient and `drawBehind` logic were moved back from the outermost container to the inner `AnimatedContent`.
- Removed the zero window insets enforcement from the `Scaffold`.

### 4. XML Themes
- Removed transparency properties from `themes.xml` and `values-v31/themes.xml`.

## Preserved Changes
- **Task Details Height**: The 150dp increase for the task description box has been maintained (`min = 310.dp`, `max = 600.dp`) as requested.

## Verification Results
- **Build**: Successfully ran `:app:assembleDebug`.
- **UI**: Verified that the custom splash screen is active upon app launch.
