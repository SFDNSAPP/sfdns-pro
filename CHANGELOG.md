# 🚀 SFDNS Pro - Release v2.5

We are excited to announce **SFDNS Pro v2.5**! This release brings major stability improvements, critical bug fixes, enhanced security validations, and new navigation links for open-source contributors.

---

### 🛠️ Key Improvements & Bug Fixes

#### 🔒 Security & Engine Hardening
* **JavaScript Bridge Validation:** Added strict security and URL pattern validation across Native-JS bridge interfaces (`openGitHub`, `openGmail`) to prevent untrusted URI execution.
* **WebView Lifecycle & Memory Management:** Resolved potential memory leaks by properly releasing WebView resources and event receivers during activity teardown.

#### ⚡ VPN & DNS Packet Processing
* **IPv4 & IPv6 Packet Handler:** Upgraded IP header inspection and UDP port 53 extraction for both IPv4 and IPv6 traffic in DoH (DNS-over-HTTPS) mode.
* **Graceful Tunnel Teardown:** Fixed stream closing and file descriptor handling during VPN shutdown, preventing unexpected background socket drops.

#### 🎨 UI & Community Integration
* **GitHub Integration:** Added a direct GitHub link in the navigation drawer and the **About** modal to easily access the source code repository.
* **Changelog & Notification System:** Integrated the updated v2.5 changelog into the in-app notification bell with automatic badge updates.

---

### 🔗 Project Repository
* **GitHub:** https://github.com/SFDNSAPP/sfdns-pro

---

Note: The signed APK for v2.5 is pending and will be uploaded tonight (EOD). This changelog entry is posted now so contributors and users can see the v2.5 release notes; the installable binary will be attached to a release once available.