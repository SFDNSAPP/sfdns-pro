# DNS Changer & DoH Proxy for Android

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?logo=android)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![MinSDK](https://img.shields.io/badge/Min%20SDK-24%2B-blue.svg)](https://developer.android.com/about/versions/nougat)
[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)
[![UI](https://img.shields.io/badge/UI-Glassmorphic%20%2F%20WebView%20M3-darkgreen.svg)]()

An advanced, lightweight, privacy-focused Android application for changing DNS servers and tunneling DNS queries securely via **DNS-over-HTTPS (DoH)**. Built natively with Kotlin and `VpnService` to route only DNS traffic without slowing down overall network bandwidth or intercepting general device traffic.

---
## 📑 Table of Contents

- [Key Features](#-key-features)
- [Download](#-download)
- [Architecture & How It Works](#️-architecture--how-it-works)
- [Screenshots](#-screenshots)
- [Building & Installation](#️-building--installation)
- [Security & Privacy](#-security--privacy)
- [Roadmap](#️-roadmap)
- [Contributing](#-contributing)
- [Disclaimer](#️-disclaimer)
- [Support & Contact](#-support--contact)
- [License](#-license)

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
## 📥 Download

Get the latest signed APK from the [Releases](https://github.com/SFDNSAPP/sfdns-pro/releases) page — no need to build from source.

[![Latest Release](https://img.shields.io/github/v/release/SFDNSAPP/sfdns-pro?label=Latest%20Release&color=blue)](https://github.com/SFDNSAPP/sfdns-pro/releases/latest)

---
## 🏗️ Architecture & How It Works

1. **Android VpnService Loop**:
   The app establishes a local virtual TUN interface (`VpnService`). Rather than routing global IP traffic (`0.0.0.0/0`), it selectively intercepts DNS server destinations.
2. **DNS Packet Draining & DoH Processing**:
   When DoH is active, DNS queries captured from port 53 are converted into RFC 8484 compliant HTTPS POST messages sent over TLS to the selected DoH resolver (e.g. `https://1.1.1.1/dns-query`). The DNS response payload is packed back into an IPv4 UDP packet and returned to the application requestor.
3. **Zero Logs & Maximum Speed**:
   No data is stored, cached on disk, or transmitted to third-party tracking servers.

---

## 📱 Screenshots

dashboard: <img width="1080" height="2340" alt="1000027453" src="https://github.com/user-attachments/assets/4e748ac6-c996-4091-bb73-fa192b69224c" />
advance setting: <img width="1080" height="2340" alt="1000027461" src="https://github.com/user-attachments/assets/90834c62-2e21-4674-bbe2-c32a94181f77" />
custom DNS: <img width="1080" height="2340" alt="1000027457" src="https://github.com/user-attachments/assets/bac20289-0678-482e-9653-5d15260142e0" />
main DNS menu: <img width="1080" height="2340" alt="1000027455" src="https://github.com/user-attachments/assets/bbe9246c-e312-4831-a780-1794462f4ebe" />
side menu : <img width="1080" height="2340" alt="1000027459" src="https://github.com/user-attachments/assets/c903e875-0eba-40fa-8902-a8235fa082e0" />


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
## 🗺️ Roadmap

- [ ] Network-aware optimization — detect the active network (Wi-Fi, mobile data, etc.) and apply a tailored profile/behavior for each
- [ ] Turkish and French language support added to the UI
- [ ] Stronger and more reliable DNS servers added to the default list
- [ ] Split-tunneling support (per-app VPN routing)
- [ ] Ping/latency history graph
- [ ] Export/import custom DNS profiles

Have an idea? Open an [issue](https://github.com/SFDNSAPP/sfdns-pro/issues) with the `enhancement` label.

---
## 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m "Add: your feature"`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

Please keep code style consistent with the existing Kotlin conventions and test your changes before submitting.

---
## ⚖️ Disclaimer

This application only changes DNS resolution and does not route or inspect general internet traffic. It is provided for privacy, security, and connectivity purposes.

Users are responsible for complying with the laws and regulations of their country regarding internet usage and DNS/VPN tools. The developers assume no liability for misuse of this application.

---
## 💬 Support & Contact

- 🐛 Found a bug? Open an [Issue](https://github.com/SFDNSAPP/sfdns-pro/issues)
- 💡 Have a feature request? Use the `enhancement` label on Issues
- 📧 Email: [sfdnsapp@gmail.com](mailto:sfdnsapp@gmail.com)
- 📢 Telegram Channel: [@sfdnsapp](https://t.me/sfdnsapp)

⭐ If you find this project useful, consider giving it a star!

---
## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
