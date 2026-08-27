# MiTension

**MiTension** is an Android app for tracking blood pressure and heart rate. It is designed to be simple, intuitive and accessible, with large controls for people with reduced vision.

> 🇪🇸 **Español:** lee la documentación en español en [README-es.md](README-es.md).

## Features

- 📝 **Record readings** — enter systolic (maximum) and diastolic (minimum) pressure in mmHg (with decimals) plus heart rate (bpm). A note field is available. Values are validated before saving so the user cannot make mistakes.
- 💓 **Automatic classification** — each reading is classified according to international blood pressure categories (normal, elevated, hypertension stage 1/2, low) with a color badge.
- 📈 **History with chart** — all readings for the active user are shown in a line chart (systolic, diastolic and pulse) plus a list with average and latest values.
- ✏️ **Full CRUD** — readings can be created, edited and deleted (with confirmation).
- ⏰ **Reminders** — configurable reminders for the morning, midday and evening. Each can be enabled/disabled, its time changed and its notification sound customized.
- 👥 **Multiple users** — on first launch you create the phone owner. You can add more users, switch the active user, rename or delete them. Each user keeps its own history.
- 🌐 **Bilingual** — Spanish by default, switchable to English in Settings (no device restart needed).
- 🔠 **Text size** — Normal, Large and Extra-large text settings for accessibility.
- 🔔 **Notifications** — request permission to post notifications and allow exact alarms so reminders arrive on time.
- 🎨 **Nice design** — medical theme (red/white) with a custom launcher icon (heart + ECG line).

## Screens

| Screen | Description |
| --- | --- |
| **Nueva / New** | Form to record a reading, last reading summary, active user |
| **Histórico / History** | Chart, statistics and editable/deletable list |
| **Avisos / Alerts** | Enable, schedule and choose sound for the 3 reminders |
| **Usuarios / Users** | Create, switch, rename and delete users |
| **Ajustes / Settings** | Language and text size, notification settings, about |

## Tech stack

- **Kotlin** 2.2 · **Jetpack Compose** (Material 3)
- **Room** for local persistence (users, readings, reminders)
- **DataStore** (Preferences) for settings
- **AlarmManager** + **BroadcastReceivers** for daily reminders (rescheduled after reboot)
- **Navigation-free tab shell** with bottom navigation bar
- Manual DI via the `Application` class

## Project structure

```
app/src/main/java/com/manalejandro/mitension/
├── MiTensionApplication.kt      # Application + DI container
├── MainActivity.kt              # Root composable, theme + locale wiring
├── data/
│   ├── db/                      # Room entities, DAOs and database
│   └── SettingsRepository.kt    # DataStore settings
├── notifications/               # Channels, scheduler, receivers
├── ui/
│   ├── MainViewModel.kt         # Shared state and operations
│   ├── MainScreen.kt            # Bottom navigation shell
│   ├── components/              # Reusable accessible components
│   ├── chart/                   # Canvas line chart
│   ├── screens/                 # onboarding, home, history, reminders, users, settings
│   └── theme/                   # Medical color palette + typography
└── util/                        # Validation, locale, reminder kinds
```

## Build

Requirements: JDK 17+ and the Android SDK (compileSdk 37).

```bash
./gradlew :app:assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

Run the unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

## Permissions used

| Permission | Purpose |
| --- | --- |
| `POST_NOTIFICATIONS` | Show reminder notifications (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | Fire reminders at the exact configured time (Android 12+) |
| `RECEIVE_BOOT_COMPLETED` | Restore reminders after the device restarts |
| `VIBRATE` | Vibration on reminder notifications |

## Data & privacy

All data is stored locally on the device (Room + DataStore). No data leaves the device.

## License

This project is a personal project by **Alejandro Manzanares**. See the GitHub repository: <https://github.com/manalejandro/MiTension>.

**Disclaimer:** MiTension is a tracking tool and does not provide medical advice. Always consult a healthcare professional about your blood pressure.