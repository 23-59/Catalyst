# Walkthrough - Focus Capsule Glow Pulse

I have integrated the `glowPulse` animation into the Focus Control Capsule on the Focus screen.

## Changes

### [FocusScreen.kt](file:///C:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/goldenminute/catalyst/ui/FocusScreen.kt)
- **Dynamic Shadow**: The shadow elevation and color now pulse when the timer is active, using the `NeonCyan` accent color to indicate the focus state.
- **Breathing Border**: The capsule's border now has a breathing alpha effect when the timer is running, transitioning between 0.3 and 0.9 alpha synchronized with the `glowPulse` animation.
- **Visual Feedback**: The border color switches to `NeonCyan` during active focus, providing immediate visual confirmation that the timer is tracking time.

## Verification Results

### Manual Verification
- Verified that the capsule remains static when the timer is stopped.
- Confirmed that starting the timer (Stopwatch or Pomodoro) triggers the synchronized breathing effect on both the shadow and the border.
- The transition between states is smooth and aligns with the existing timer design language.
