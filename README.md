# Krishna Travels Android App

Kotlin + Jetpack Compose Android app for Krishna Travels.

## Features included in the source
- Sign-in flow using Firebase Authentication
- Firestore-backed fleet and trip management
- Bus, trip, staff, passenger booking, attendance and expense screens
- English, Hindi and Gujarati language resources
- GitHub Actions workflow to build a debug APK

## Build APK on GitHub
1. Upload the **contents of this folder** to the top level of your GitHub repository. Do not upload only this ZIP.
2. Keep the `.github/workflows/android.yml` file and its folders.
3. Open the repository's **Actions** tab.
4. Select **Build Krishna Travels APK** and run the workflow (or push a commit to `main`).
5. When the run succeeds, download `krishna-travels-debug-apk` from the run's **Artifacts** section.

## Firebase setup
The project includes `app/google-services.json` from the supplied project. Confirm in Firebase Console that the Android app package `com.aistudio.busflow.kqzmtw` exists, Authentication providers are enabled, and the configured Firestore database is available. Review Firestore security rules before using real passenger or staff data.

## Important
This package preserves the supplied Android project source and adds a GitHub Actions build workflow. The APK is built by GitHub Actions; this ZIP itself is source code, not an APK. The workflow uses Gradle 9.3.1 installed on the runner because the supplied ZIP did not include the standard Gradle wrapper JAR. The project has not been independently compiled in this chat environment.
