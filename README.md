# DNS Changer & DoH Proxy for Android

[English](README.md) • [فارسی](README.fa.md) • [العربية](README.ar.md) • [Русский](README.ru.md)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?logo=android)](https://www.android.com)  [![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg?logo=kotlin)](https://kotlinlang.org)  [![MinSDK](https://img.shields.io/badge/Min%20SDK-24%2B-blue.svg)](https://developer.android.com/about/versions/nougat)  [![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)  [![UI](https://img.shields.io/badge/UI-Glassmorphic%20%2F%20WebView%20M3-darkgreen.svg)]()

[![Latest Release](https://img.shields.io/github/v/release/SFDNSAPP/sfdns-pro?label=Latest%20Release&color=blue)](https://github.com/SFDNSAPP/sfdns-pro/releases/latest)  [![GitHub stars](https://img.shields.io/github/stars/SFDNSAPP/sfdns-pro?style=social)](https://github.com/SFDNSAPP/sfdns-pro/stargazers)  [![Open issues](https://img.shields.io/github/issues/SFDNSAPP/sfdns-pro?color=orange)](https://github.com/SFDNSAPP/sfdns-pro/issues)

An advanced, lightweight, privacy-focused Android application for changing DNS servers and tunneling DNS queries securely via **DNS-over-HTTPS (DoH)**. Built natively with Kotlin and `VpnService` to capture DNS traffic and forward it to chosen resolvers.

---
## 📑 Table of Contents

- [Key Features](#-key-features)
- [Download](#-download)
- [Architecture & How It Works](#️-architecture--how-it-works)
- [Screenshots](#-screenshots)
- [Quick Start](#-quick-start)
- [Building & Installation](#️-building--installation)
- [Security & Privacy](#-security--privacy)
- [Roadmap](#-roadmap)
- [FAQ](#-faq)
- [Contributing](#-contributing)
- [Disclaimer](#️-disclaimer)
- [Support & Contact](#-support--contact)
- [License](#-license)

our website: https://sfdns.dpdns.org 🌐

---

## 🌟 Key Features

- 🛡️ **Privacy First & Secure DoH**: Supports encrypted DNS-over-HTTPS (DoH) queries to prevent DNS spoofing, eavesdropping, and ISP hijacking.
- ⚡ **Lightweight Local VPN Routing**: Captures only DNS traffic (port 53 UDP/TCP). Other internet traffic is not routed through the app.
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
- 🧩 **Home Screen Widgets & Quick Tile**: Toggle DNS connection from the Quick Settings tile or from app widgets.
- 🎨 **Modern Glassmorphic UI**: Smooth animations, dark mode, ping test indicators, and real-time DNS status feedback.
- 🔄 **Auto-Connect on Boot**: Optional background startup after device reboot.

---
## 📥 Download

Get the latest signed APK from the [Releases](https://github.com/SFDNSAPP/sfdns-pro/releases) page — no need to build from source.
or download from our website: https://sfdns.dpdns.org 🌐
---
## 🏗️ Architecture & How It Works

1. **Android VpnService Loop**: The app establishes a local virtual TUN interface (`VpnService`). Instead of routing all traffic, it selectively intercepts DNS destinations.
2. **DNS Packet Draining & DoH Processing**: When DoH is enabled, captured DNS queries are converted into RFC 8484-compliant HTTPS requests sent over TLS to the selected resolver (e.g. `https://1.1.1.1/dns-query`).
3. **Zero Logs & Minimal Footprint**: The app avoids persistent logging and does not transmit telemetry to third parties.

---
## 📱 Screenshots

Below are representative screenshots showing the main screens and settings. Click images to view full-size (hosted via repository attachments).

| Screen | Preview |
|---|---:|
| Dashboard | <img width="240" alt="Dashboard" src="https://github.com/user-attachments/assets/4e748ac6-c996-4091-bb73-fa192b69224c" /> |
| Advanced Settings | <img width="240" alt="Advanced Settings" src="https://github.com/user-attachments/assets/90834c62-2e21-4674-bbe2-c32a94181f77" /> |
| Custom DNS Input | <img width="240" alt="Custom DNS" src="https://github.com/user-attachments/assets/bac20289-0678-482e-9653-5d15260142e0" /> |
| Main DNS Menu | <img width="240" alt="Main DNS Menu" src="https://github.com/user-attachments/assets/bbe9246c-e312-4831-a780-1794462f4ebe" /> |
| Side Menu | <img width="240" alt="Side Menu" src="https://github.com/user-attachments/assets/c903e875-0eba-40fa-8902-a8235fa082e0" /> |
| site <pic1> | <img width="240" height="2340" alt="site" src="https://github.com/user-attachments/assets/c6d3c3e5-5a3b-4d3b-82b4-66f5229ab642" />

---
## 🚀 Quick Start

1. Install the APK from the [Releases page](https://github.com/SFDNSAPP/sfdns-pro/releases).
2. Open the app and grant the VPN permission when prompted.
3. Choose a resolver from the list or enter custom Primary/Secondary DNS addresses.
4. Enable DoH in settings if you want encrypted DNS (recommended).
5. Use the Quick Settings tile or widget for one-tap connect/disconnect.
more information in our website: https://sfdns.dpdns.org 🌐
Notes:
- The app only intercepts DNS queries; normal traffic is not proxied.
- On modern Android versions, the OS may prompt for a persistent VPN notification — this is required for operation.

---
## 🔒 Security & Privacy

- `allowBackup` set to `false` to avoid sensitive app state extraction.
- Cleartext HTTP traffic disabled (`usesCleartextTraffic="false"`).
- Zero user data collection or telemetry analytics by default.

---
## 🗺️ Roadmap

This roadmap is organized into three clear categories so you can see what is completed, in progress, and planned.

### Done ✅
- [x] Core DoH proxy implementation and local VPN-based DNS interception (core functionality implemented and available in releases).
- [x] Pre-configured list of popular resolvers (Cloudflare, Google, Quad9, Shecan, etc.) — curated in-app server list.

### In Progress 🚧
- [ ] Turkish and French language support (localization in progress)

### Future Plans 📋
- [ ] Network-aware optimization — detect active network (Wi-Fi / mobile) and apply per-network profiles
- [ ] Split-tunneling support (per-app DNS/VPN routing)
- [ ] Ping / latency history graph and improved diagnostics
- [ ] Export / import custom DNS profiles (backup & restore)

Have an idea or want to help? Open an [issue](https://github.com/SFDNSAPP/sfdns-pro/issues) and tag it with `enhancement` or submit a PR.

---
## ❓ FAQ

Q: Does this app route all my traffic through a VPN?

A: No. The app only intercepts DNS queries (port 53) and forwards them to the configured resolver. Other traffic uses the device's normal network path.

Q: Is my DNS traffic logged or shared?

A: No. The app aims for minimal footprint and does not persistently log DNS queries or send telemetry to third parties. See the Security & Privacy section for details.

Q: Will this work on rooted devices or older Android versions?

A: The app uses Android's VpnService and works on devices with API level 24+. Root is not required. Some manufacturer-specific ROMs may alter VPN behavior.

Q: How can I contribute translations or patches?

A: Please open an Issue or submit a Pull Request. Follow the contributing steps in this README and include details about the change.

---
## 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m "Add: your feature"`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

Please keep code style consistent with existing Kotlin conventions and include tests where appropriate.

---
## ⚖️ Disclaimer

This application only changes DNS resolution and does not route or inspect general internet traffic. It is provided for privacy, security, and connectivity purposes.

Users are responsible for complying with the laws and regulations of their country regarding internet usage and DNS/VPN tools. The developers assume no liability for misuse of this application.

---
## 💬 Support & Contact

- 🐛 Found a bug? Open an [Issue](https://github.com/SFDNSAPP/sfdns-pro/issues)
- 💡 Feature requests? Use the `enhancement` label on Issues
- 📧 Email: [sfdnsapp@gmail.com](mailto:sfdnsapp@gmail.com)
- 📢 Telegram Channel: [@sfdnsapp](https://t.me/sfdnsapp)
our website: https://sfdns.dpdns.org 🌐
If you find this project useful, please consider starring the repo ⭐

---
## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
