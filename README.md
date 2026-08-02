# DNS Changer & DoH Proxy for Android

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?logo=android)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![MinSDK](https://img.shields.io/badge/Min%20SDK-24%2B-blue.svg)](https://developer.android.com/about/versions/nougat)
[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)
[![UI](https://img.shields.io/badge/UI-Glassmorphic%20%2F%20WebView%20M3-darkgreen.svg)]()

An advanced, lightweight, privacy-focused Android application for changing DNS servers and tunneling DNS queries securely via **DNS-over-HTTPS (DoH)**. Built natively with Kotlin and `VpnService` to r[...] 

---

## 🌟 Key Features

- 🛡️ **Privacy First & Secure DoH**: Supports encrypted DNS-over-HTTPS (DoH) queries to prevent DNS spoofing, eavesdropping, and ISP hijacking.
- ⚡ **Lightweight Local VPN Routing**: Captures only port 53 UDP/TCP DNS packets. All other internet traffic flows directly through the default network interface.
- 🚀 **Pre-configured Anti-Sanction & Fast Servers**:
  - **Cloudflare** (`1.1.1.1`)
  - **Google Public DNS** (`8.8.8.8`)
  - **Quad9** (`9.9.9.9`)
  - **Shecan** (`178.22.122.100`)
  - **Electro** (`78.157.42.100`)
  - **Radar Game** (`10.201.201.201`)
  - **403 Online** (`10.202.10.202`)
  - **Begzar** (`185.55.226.26`)
  - **Custom Primary & Secondary DNS** input support
- 🧩 **Home Screen Widgets & Quick Tile**:
  - **Quick Settings Tile**: Toggle DNS connection directly from Android's notification shade.
  - **Interactive App Widgets**: One-tap toggle and candidate switching directly from your Home Screen.
- 🎨 **Modern Glassmorphic UI**: Smooth animations, dark mode aesthetic, ping test indicators, and real-time DNS status feedback.
- 🔄 **Auto-Connect on Boot**: Optional background service startup after device reboot.

---

## 🏗️ Architecture & How It Works

1. **Android VpnService Loop**:
   The app establishes a local virtual TUN interface (`VpnService`). Rather than routing global IP traffic (`0.0.0.0/0`), it selectively intercepts DNS server destinations.
2. **DNS Packet Draining & DoH Processing**:
   When DoH is active, DNS queries captured from port 53 are converted into RFC 8484 compliant HTTPS POST messages sent over TLS to the selected DoH resolver (e.g. `https://1.1.1.1/dns-query`). The DN[...]
3. **Zero Logs & Maximum Speed**:
   No data is stored, cached on disk, or transmitted to third-party tracking servers.

---

## 📱 Screenshots

پایین چهار تصویر از رابط برنامه قرار داده شده‌اند. اگر مایل بودی من خودِ تصاویر را هم در مسیر `assets/screenshots/` آپلود می‌کنم؛ فعلاً لینک‌ها به‌صورت مسیرهای محلی داخل مخزن درج شده‌اند.

![Advanced Settings — تنظیمات پیشرفته](assets/screenshots/shot1.png "Advanced Settings")

![Main Dashboard — داشبورد اصلی (Connected)](assets/screenshots/shot2.png "Main Dashboard")

![Add Custom DNS — افزودن DNS سفارشی](assets/screenshots/shot3.png "Add Custom DNS")

![Server Selector — انتخاب سرور‌ها](assets/screenshots/shot4.png "Server Selector")

---

## 🛠️ Building & Installation

### Requirements
- Android Studio Ladybug or newer
- JDK 17 / 21
- Android SDK 36 (Min SDK 24)

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/SFDNSAPP/sfdns-pro.git
   ```
2. Open the project in **Android Studio**.
3. Build the APK using Gradle:
   ```bash
   ./gradlew assembleRelease
   ```
4. Install the generated APK on your device.

---

## 🔒 Security & Privacy

- `allowBackup` set to `false` to avoid sensitive app state extraction.
- Cleartext HTTP traffic disabled (`usesCleartextTraffic="false"`).
- Zero user data collection or telemetry analytics.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
