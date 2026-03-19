# PryrTm

A simple, battery-friendly Android application built with Jetpack Compose that displays and notifies users of prayer times using the official Malaysian Jakim E-Solat API.

## Features

- **Prayer Times Integration**: Fetches daily prayer times based on Malaysian zones from the e-solat API.
- **Jetpack Compose UI**: A clean, modern user interface built using Jetpack Compose with light and dark mode support.
- **Home Screen Widget**: Includes a simple Jetpack Glance widget with a live countdown timer.
- **Notifications**: Uses Android's `AlarmManager` to provide reliable notifications without draining battery in the background. Features both pre-prayer reminders and exact-time notifications.
- **Offline Caching**: Saves prayer times locally so the app doesn't need to constantly check the internet.
- **Manual Widget Refresh**: A refresh button on the widget to manually sync the data if needed.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Widget**: Jetpack Glance
- **Networking**: Retrofit2 + Gson
- **Background Tasks**: AlarmManager, BroadcastReceivers
- **Storage**: SharedPreferences

## How to Run

1. Clone this repository to your local machine.
2. Open the `android_app` folder in Android Studio.
3. Sync the Gradle project.
4. Run the app on an Android emulator or a physical device.
