# Implementation Plan - Reuse Glow Pulse in Focus Capsule

This plan involves wiring up the existing `glowPulse` animation to the "Space-age Focus Control Capsule/Dock" in the Focus screen so it visually pulses when the timer is active.

## Proposed Changes

### UI Components

#### [MODIFY] [FocusScreen.kt](file:///C:/Users/amirh/Downloads/hypertask_v2/app/src/main/java/com/goldenminute/catalyst/ui/FocusScreen.kt)
- Update the `Box` modifier for the action capsule to use `glowPulse`.
- Apply `glowPulse` to the `shadow`'s `ambientColor` and `spotColor` when `isTimerRunning` is true.
- Apply `glowPulse` to the `border` color's alpha to create a breathing effect during active focus sessions.

## Verification Plan

### Manual Verification
- Start the timer (Stopwatch or Pomodoro) on the Focus screen.
- Observe the action capsule to see if it has a breathing glow effect (shadow and border pulsing).
- Stop the timer and verify the glow returns to the static default state.
