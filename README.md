# Khoroch Tracker Android v3.0

## Build APK locally
Open this project in Android Studio, let Gradle Sync complete, then choose:
Build > Build Bundle(s) / APK(s) > Build APK(s).

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Build APK using GitHub Actions
This project includes `.github/workflows/build-apk.yml`.
1. Create a GitHub repository and upload/push the contents of this project to its `main` branch.
2. Open the repository's Actions tab and run **Build Android APK** (or wait for the push workflow).
3. Open the successful workflow run, download the `khoroch-tracker-debug-apk` artifact, and extract the APK.

## Features
Add money, add expenses, categories, custom dates and descriptions, edit/delete, history filters, dashboard balance, monthly/category reports, category chart, PDF report export, dark mode, Bengali/English switch, offline Room database, JSON backup/restore.

## Important
This ZIP contains source code and a cloud-build workflow, not a prebuilt APK. Backup before restoring; restore replaces current records. Date format is YYYY-MM-DD.
