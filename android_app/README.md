# PryrTm 🕌

An ultra-modern, battery-efficient, Jetpack Compose Android Application that fetches live accurate prayer times directly from the official Malaysian Jakim E-Solat API.

## ✨ Features

- **Live Jakim API Sync**: Fetches exact mathematical prayer times based on Malaysian Zones seamlessly in the background.
- **Jetpack Compose UI**: Built entirely 100% in Jetpack Compose, featuring a dynamic responsive interface, elegant Dark/Light thematic support, and glowing Next-Prayer highlights.
- **Homescreen Widget (Glance)**: Features a sleek, horizontal 1x4 Profile Widget built using Android Jetpack Glance.
- **True Live Countdown (0% Battery)**: The widget countdown doesn't arbitrarily wake up your CPU. It delegates ticking directly to Android's native underlying hardware (`<Chronometer>`), using virtually 0% battery.
- **Dual-Layer Smart Alarms**: Bypasses unstable WorkManagers. Uses native Android `AlarmManager` to securely wake the phone exactly 6 times a day from deep-sleep Doze mode.
    - Features a customizable **Pre-Prayer Warning Notification** (e.g., 10 minutes before).
    - Fires a primary exact **Call to Prayer Notification** exactly on the minute.
- **Time-Travel Immune**: The app actively listens to OS-level `ACTION_TIME_CHANGED` broadcasts. If you travel across time-zones or manually change your phone's clock, the app invisibly rebuilds all Alarms and Widget UI states instantly.
- **Instant Override Refresh ⟳**: The widget features a rigorous manual override refresh button that cuts straight through Android's aggressive background OS throttling to instantly ping the live server and redraw your homescreen widget in milliseconds.

## 🛠️ Technical Stack
* **Language**: Kotlin
* **UI Engine**: Jetpack Compose API
* **Widget Engine**: Jetpack Glance App Widget API
* **Networking**: Retrofit2 + Gson
* **Background Engine**: Android BroadcastReceivers + AlarmManager (Exact & AllowWhileIdle)
* **Storage**: Encrypted SharedPreferences
* **Visual Identity**: Modern Material 3 Dynamic colors with a custom native Scalable Vector Graphic Adaptive Icon design (`#0F172A` Midnight Blue vs `#38BDF8` Cyan).

## 🔋 The Battery Philosophy
Most prayer applications completely obliterate your phone's battery by:
1. Pinging an internet server constantly in the background.
2. Re-calculating live widget countdown timers every 1000 milliseconds on the main thread CPU.

**PryrTm** takes a completely different path:
1. It downloads a full JSON timezone map exactly *once* locally to Flash Storage, abandoning the network fully entirely.
2. The widget countdown handles no math itself. It hands a secure UTC timestamp to the Android hardware OS and goes completely back to sleep silently.
3. No infinite background notification listener loops. The OS sleeps silently until the absolute fraction of a millisecond it needs to fire the Push Notification. 

## 🚀 How to Run
1. Clone the repository and open it precisely in **Android Studio Minimum Iguana+**.
2. Run standard Gradle Sync. Ensure your system runs **JDK 17**.
3. Plug in an Android Emulator or Physical Android device (SDK 24+ minimum, optimized for Android 13/14).
4. Hit **Play ▶️**. Accept the push notification Android permissions upon opening the Settings ("Tetapan") tab.
