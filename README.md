# Asgard Pool – Pool & Swimming Lessons Manager

Android app (Java) based on Asgard Challenge 4. **UI is built with XML layouts** (no Jetpack Compose).

## Opening in Android Studio

1. **File → Open** and select the project folder `AsgardChallenge`.
2. **Sync Project with Gradle Files** (or the Gradle elephant button).
3. Connect a device or start an emulator → **Run 'app'**.

## Features

- **Student input (1–30):** First name, last name, swim style (Freestyle, Breaststroke, Butterfly, Backstroke), lesson type (Private only / Group only / Private or group by preference).
- **Constraints:**
  - Private lesson: 45 minutes; group lesson: 60 minutes.
  - Pool closed on weekends.
  - Yotam: Monday & Thursday 16:00–20:00, all styles.
  - Yoni: Tuesday–Friday 08:00–15:00 (not Sunday/Monday), Breaststroke and Butterfly.
  - Johnny: Sunday, Tuesday, Thursday 10:00–19:00 (pool closed Sunday → effectively Tuesday & Thursday); all styles.
- **Output:** Weekly schedule (Gantt-style list) + “Conflicts / Gaps” button with pop-up.

## Project structure

- `app/src/main/java/com/asgard/pool/` – MainActivity, ScheduleActivity, StudentAdapter.
- `model/` – Student, Instructor, Lesson, SwimStyle, LessonType.
- `scheduler/PoolScheduler.java` – Scheduling logic and conflict/gap detection.
- All UI: **XML layouts** in `res/layout/` and `res/values/strings.xml`.
