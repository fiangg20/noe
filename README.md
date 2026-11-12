# Noe

A notes app for Android, built with Kotlin, Jetpack Compose and Material 3 Expressive.
The APK is built by GitHub Actions, so Android Studio is not needed.

## Features

- Text notes and checklists (press Enter to add the next item, completed items collapse, "Clear" removes them)
- Text styling in text notes: bold, italic, underline, strikethrough and four text sizes (tap the Aa button in the editor's bottom bar; select text first, or pick a style and keep typing)
- Tick checklist items straight from the notes grid; wavy progress bars show how far along a list is
- Filter chips on the home screen: All, Notes, Checklists, Reminders, Colored
- Greeting header with your name (set it in Settings) and a short summary of your notes
- Pin, archive, trash (auto-deleted after 30 days) and undo
- 10 note colors and labels, with a label list in the side drawer
- Search across titles, text and labels
- Reminders that show a notification (quick picks or a date and time picker), can repeat daily or weekly, with a Snooze 1 hour button
- Launcher shortcuts (long-press the app icon): New note and New checklist
- Word count and Copy to clipboard in the editor
- Grid or list view, sort by edited, created or title
- Multi-select: pin, archive, delete, color, label, duplicate, share
- Share text from other apps into a new note
- Backup: export all notes to a JSON file and import them back
- Expressive look on stable Material 3 1.4.0: dynamic color on Android 12+, 6 accent palettes,
  light, dark or system mode, rounded shapes, bold type, spring-animated FAB menu, morphing checkboxes, press-scale cards, wavy progress and cookie-shaped shapes.
  (The official Material 3 Expressive component APIs are not public in 1.4.0, so these are built by hand.)

## Get the APK

1. Create a new GitHub repository and upload everything in this folder.
   The web uploader skips folders that start with a dot, so the `.github` folder will be missing.
2. In the repository choose **Add file > Create new file**, type `.github/workflows/build.yml`
   as the name (typing the slashes creates the folders) and paste the contents of
   `build.yml.txt` from this folder. Commit it to `main`.
3. Open the **Actions** tab. The build starts by itself and takes a few minutes.
4. Open the finished run and download **noe-debug-apk** from Artifacts. Unzip it and install `app-debug.apk`.

## Font (Google Sans Flex)

1. Download Google Sans Flex from https://fonts.google.com/specimen/Google+Sans+Flex (open source, OFL license).
2. Take the variable font `.ttf` from the download and rename it to `google_sans_flex.ttf` (lowercase, underscores).
3. Put it in the repo at `app/src/main/res/font/google_sans_flex.ttf`.
   On GitHub: open `app/src/main/res`, choose **Add file > Upload files**, and drag in a folder named `font` that contains the file.

Without the file the app still builds and uses the system font.

## Notes

- Notes are stored on the device in a private JSON file. Use Export in Settings to back them up.
- Reminders use inexact alarms, so they can fire a little late when the phone is in deep sleep.
  Android 13+ asks for notification permission the first time you set a reminder.
- Debug build only. It is signed with a debug key, which is fine for personal use.
