# The Hero Lounge for Android

Open this folder in Android Studio with JDK 17, Android SDK Platform 35 and Gradle 8.9. Use Android Studio's Gradle settings to select a local Gradle 8.9 distribution, or run `gradle :app:assembleDebug` from this folder. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it with Android Studio or `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

The existing caf� runs from a bundled HTML asset. Android renders the analog joystick and action buttons as native views. Use the left stick and the right-hand Grab / Serve, Chat, Switch and Break buttons. Landscape puts controls beside the caf�. Portrait puts them underneath. For local co-op, additional players use a physical keyboard. The bundled game can run offline with system fallback fonts.

Before release, play a round on a phone in both orientations. Check diagonal movement, both thumbs at once, action labels near the counter and tables, stopping movement when your thumb lifts, switching heroes, chat with the keyboard open, background/resume and Android Back. Check that the high score persists after reopening. This is a debug-build project and has no release signing credentials.

## Automatic APK builds

[Open Android APK builds](https://github.com/amaanmalik/lounge-hero/actions/workflows/android.yml) and select the latest successful run. Download **HeroLounge-Android-debug** from **Artifacts**, then extract and install `HeroLounge-debug.apk`. GitHub keeps artifacts for 30 days. Select **Run workflow** to generate a fresh artifact. Debug signing keys may vary across runners, so Android can require uninstalling an earlier CI build before installing a new one. Uninstalling clears the app's local high score.
