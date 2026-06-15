# StedNet-Lock

A Shizuku-powered network switcher and profile manager for Android.

`StedNet-Lock` allows you to toggle network modes (such as forcing 5G, 4G, or Auto-mode) and manage customized network profiles. Utilizing the **Shizuku** API, it performs system-level network configurations securely and efficiently without requiring root access.

---

## Download

You can download the pre-compiled, signed release APK directly from the [Releases](https://github.com/OmerKurdi79/StedNet-Lock/releases) section.

---

## Features

- **Network Mode Toggle**: Easily switch between Auto, 5G, and 4G network modes.
- **Customized Network Profiles**: Create, save, and apply personalized profiles for different network conditions and SIM cards.
- **Home Screen Widgets**: Instantly switch profiles or trigger network modes directly from your launcher.
- **Shizuku Powered**: Runs commands in a privileged context securely, removing the need for a rooted device.
- **Modern Jetpack Compose UI**: A fully responsive interface designed with Material 3 guidelines, featuring a default immersive dark theme.
- **Dual SIM Support**: Automatically detects active SIM configurations and applies profiles to the selected subscription.

---

## Technical Stack

- **Framework**: Jetpack Compose (Kotlin)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room Database (for saving network profiles)
- **API Integration**: Shizuku API (for privileged network state manipulation)
- **Asynchrony**: Kotlin Coroutines & StateFlow

---

## Prerequisites

1. **Android Device**: Android 7.0 (API level 24) or higher.
2. **Shizuku**: The Shizuku app must be installed and running on the device. You can download it from [Google Play](https://play.google.com/store/apps/details?id=rikka.shizuku) or [GitHub](https://github.com/RikkaApps/Shizuku).
   - Once installed, enable Developer Options on your device, start Shizuku via Wireless Debugging (or ADB), and authorize `StedNet-Lock`.

---

## Running and Building Locally

To build the project yourself:

1. Clone the repository:
   ```bash
   git clone https://github.com/OmerKurdi79/StedNet-Lock.git
   cd StedNet-Lock
   ```
2. Open the project in **Android Studio** (Koala or newer recommended).
3. Connect your Android device or emulator with Shizuku running.
4. Build and run the app from Android Studio.

---

## License

This project is licensed under the [MIT License](LICENSE) - see the [LICENSE](LICENSE) file for details.
