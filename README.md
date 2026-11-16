# HealthMate – Personal Health & Wellness Tracker

A native Android app written in **Kotlin** with **Jetpack Compose** for logging
daily steps, calories burned and water intake. Records are stored on the phone
in a local **SQLite** database.

## Features

- **Dashboard** – today's steps, calories and water with progress toward daily goals
- **Add / edit records** – form with validation (numbers only, no future dates, limits)
- **Records list** – newest first, filter by Today / Last 7 days / Last 30 days / custom dates
- **Delete** – from the edit screen (with confirmation) or by swiping left (with Undo)
- **Weekly chart** – last 7 days for any metric, with the daily goal line
- **Streak badge** – how many days in a row you have logged something
- **Settings** – your name, height, weight (with BMI) and your own daily goals
- **Calories from steps** – one tap on the form estimates calories burned from
  your steps, height and weight (formula below)
- Light and dark theme that follows the phone setting
- Dummy records are added on first launch for testing

## Architecture (MVVM)

```
app/src/main/java/com/healthmate/app/
├── HealthMateApplication.kt     # creates the database helper and repository once
├── MainActivity.kt              # sets the Compose content
├── navigation/HealthMateApp.kt  # bottom navigation, Add button, NavHost
├── core/
│   ├── constants/               # daily goals, input limits, metric colours/icons
│   ├── database/                # HealthDatabaseHelper – SQLiteOpenHelper, table, dummy data
│   ├── settings/                # UserSettings, Goals, SettingsRepository (SharedPreferences)
│   ├── theme/                   # Material 3 light/dark theme
│   ├── ui/                      # view model factory, error banner, date rules
│   └── utils/                   # formatting, validators, CalorieCalculator
└── features/
    ├── records/
    │   ├── data/                # HealthRecord, DailyTotal, repository (all SQL / CRUD)
    │   ├── form/                # RecordFormViewModel + add/edit screen
    │   ├── RecordsViewModel.kt  # list state, filters, delete/undo
    │   └── RecordListScreen.kt
    ├── dashboard/               # DashboardViewModel, screen, SummaryCard, WeeklyChart
    └── settings/                # SettingsViewModel + profile/goals screen
```

Data flows one way: **Screen → ViewModel → Repository → SQLiteOpenHelper → SQLite**.

- Each ViewModel keeps its screen state in a `StateFlow`; screens collect it with
  `collectAsStateWithLifecycle()` and redraw when it changes.
- The repository runs every query on `Dispatchers.IO` so the UI never freezes.
- After every insert/update/delete the repository bumps a `changes` counter.
  Both list and dashboard view models collect it and reload, so they always
  match the database.

## Database

Table `health_records`

| Column   | Type    | Notes                          |
|----------|---------|--------------------------------|
| id       | INTEGER | Primary key, auto increment    |
| date     | TEXT    | `yyyy-MM-dd`, indexed          |
| steps    | INTEGER | steps walked                   |
| calories | INTEGER | kcal burned                    |
| water    | INTEGER | ml drunk                       |

Settings (name, height, weight, goals) are small key-value data, so they are
kept in **SharedPreferences**, separate from the health records in SQLite.
The `health_records` table is unchanged.

## Calories from steps

| Step | Formula |
|------|---------|
| 1. Stride length | height (cm) × 0.414 ÷ 100 = metres |
| 2. Distance | steps × stride length ÷ 1000 = km |
| 3. Calories | weight (kg) × distance (km) × 0.5 = kcal |

Example: 175 cm, 70 kg, 7,850 steps → stride 0.72 m → 5.7 km → **199 kcal**.
It is an estimate; the user can still type their own value.

## Libraries

| Library                         | Why                                               |
|---------------------------------|---------------------------------------------------|
| Jetpack Compose + Material 3    | UI toolkit and Material design components         |
| Lifecycle ViewModel / Compose   | ViewModels and lifecycle-aware state collection   |
| Navigation Compose              | Moving between dashboard, list and form           |
| Kotlin Coroutines               | Background database work                          |
| Material Icons Extended         | Walk, fire and water drop icons                   |
| Android SQLite (built in)       | Local database through `SQLiteOpenHelper`         |
| JUnit, Robolectric, Compose UI test, coroutines-test | (test) unit, database and UI tests |

The weekly chart is drawn by hand on a Compose `Canvas`, so no chart library is needed.

## Running

Open the folder in Android Studio and press Run, or from a terminal:

```bash
./gradlew installDebug        # install on a running emulator or phone
./gradlew testDebugUnitTest   # 52 unit, database and UI tests
./gradlew assembleRelease     # APK in app/build/outputs/apk/release/
```
