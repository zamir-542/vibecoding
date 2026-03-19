# PryrTm

A simple, battery-friendly Android application built with Jetpack Compose that displays and notifies users of prayer times using the official Malaysian Jakim E-Solat API.

## Features

- **Prayer Times Integration**: Fetches daily prayer times based on Malaysian zones from the e-solat API.
- **Hijri Calendar**: Displays the daily Islamic (Hijri) date on the main app and the home screen widget, automatically formatted with traditional Malay month names.
- **GPS Auto-Location**: Automatically detects the user's Jakim Zone based on their GPS coordinates and updates prayer times accordingly.
- **Jetpack Compose UI**: A clean, modern user interface built using Jetpack Compose with light and dark mode support.
- **Home Screen Widget**: Includes a simple Jetpack Glance widget with a live countdown timer and Hijri date.
- **Notifications**: Uses Android's `AlarmManager` to provide reliable notifications without draining battery in the background. Features both pre-prayer reminders and exact-time notifications.
- **Offline Caching**: Saves prayer times locally so the app doesn't need to constantly check the internet.
- **Manual Widget Refresh**: A refresh button on the widget to manually sync the data and location if needed.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Widget**: Jetpack Glance
- **Networking**: Retrofit2 + Gson
- **Background Tasks**: AlarmManager, BroadcastReceivers
- **Storage**: SharedPreferences

## How to Install

1. Navigate to the **Releases** tab on the right side of this GitHub repository.
2. Download the latest `PryrTm.apk` file to your Android device.
3. Tap the downloaded file to install it. (You may need to enable "Install from unknown sources" in your security settings).
4. Launch the app and enjoy!
