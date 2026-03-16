# Goals Feature — End-to-End Test

Manual verification steps for the Goals flow.

## Prerequisites

- App installed and user logged in
- Network available (for music playback from Jamendo API)

## Test Steps

### 1. Generate goals

1. Launch the app
2. Tap the **Goals** tab in the bottom navigation
3. **Verify:** Two mission cards appear in the DAILY GOALS tab with different modes (e.g. ZEN, SYNC, OVERDRIVE) and target times (10/15/20/25/30 min)
4. **Verify:** Progress bars show 0:00 and 0% progress

### 2. Play music and watch progress advance

1. Switch to the **Terminal** tab
2. Select a mode that matches one of today's goals (e.g. if today has a ZEN goal, switch to ZEN)
3. Tap **Play** to start music
4. **Verify:** Music plays
5. Switch back to the **Goals** tab
6. **Verify:** The matching goal's progress bar advances every second (timer increments: 0:01, 0:02, …)
7. **Verify:** Progress bar fill increases proportionally

### 3. Complete goal and check achievement

1. Keep music playing until the goal's accumulated time reaches the target (e.g. 10 min = 10:00)
2. **Verify:** The goal shows a completion crown (trophy icon) when done
3. **Verify:** Progress bar is full and timer shows target time
4. Switch to the **ACHIEVEMENTS** tab
5. **Verify:** Current month's card shows updated completed count (e.g. `1 / 2 goals`)

### 4. Navigation and Hilt

- **Goals tab:** Bottom nav item with "Goals" title and goals icon shows GoalsScreen
- **GoalsViewModel:** Injected via Hilt; no crash when opening Goals tab
