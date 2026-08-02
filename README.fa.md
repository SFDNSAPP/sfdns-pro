# DNS Changer & DoH Proxy for Android

[English](README.md) • [فارسی](README.fa.md) • [العربية](README.ar.md) • [Русский](README.ru.md)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?logo=android)](https://www.android.com)  [![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg?logo=kotlin)](https://kotlinlang.org)  [![MinSDK](https://img.shields.io/badge/Min%20SDK-24%2B-blue.svg)](https://developer.android.com/about/versions/nougat)  [![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)  [![UI](https://img.shields.io/badge/UI-Glassmorphic%20%2F%20WebView%20M3-darkgreen.svg)]()

[![Latest Release](https://img.shields.io/github/v/release/SFDNSAPP/sfdns-pro?label=Latest%20Release&color=blue)](https://github.com/SFDNSAPP/sfdns-pro/releases/latest)  [![GitHub stars](https://img.shields.io/github/stars/SFDNSAPP/sfdns-pro?style=social)](https://github.com/SFDNSAPP/sfdns-pro/stargazers)  [![Open issues](https://img.shields.io/github/issues/SFDNSAPP/sfdns-pro?color=orange)](https://github.com/SFDNSAPP/sfdns-pro/issues)

یک نسخهٔ کامل فارسی از README با متن، جدول تصاویر، بخش Quick Start، Roadmap (Done/In Progress/Future Plans)، و FAQ ۴ سؤالی که متن انگلیسی را کاملاً منعکس می‌کند.

---
## 📑 فهرست مطالب

- [ویژگی‌های کلیدی](#-ویژگی‌های-کلیدی)
- [دانلود](#-دانلود)
- [معماری و نحوهٔ کار](#️-معماری-و-نحوهٔ-کار)
- [تصاویر (Screenshots)](#-تصاویر-screenshots)
- [راهنمای سریع](#-راهنمای-سریع)
- [ساخت و نصب](#️-ساخت-و-نصب)
- [امنیت و حریم خصوصی](#-امنیت-و-حریم-خصوصی)
- [نقشهٔ راه](#️-نقشهٔ-راه)
- [پرسش‌های متداول (FAQ)](#-پرسش‌های-متداول-faq)
- [شرکت در توسعه](#-شرکت-در-توسعه)
- [سلب مسئولیت](#️-سلب-مسئولیت)
- [پشتیبانی و تماس](#-پشتیبانی-و-تماس)
- [مجوز](#-مجوز)

---
## 🌟 ویژگی‌های کلیدی

- 🛡️ حفظ حریم خصوصی و پشتیبانی از DoH امن: پشتیبانی از DNS-over-HTTPS برای جلوگیری از جعل DNS، استراق سمع و تغییر مسیر توسط ISP.
- ⚡ مسیر‌یابی سبک وزن مبتنی بر VPN محلی: تنها بسته‌های DNS (پورت 53 UDP/TCP) رهگیری می‌شوند؛ بقیهٔ ترافیک مستقیماً از طریق رابط شبکهٔ پیش‌فرض عبور می‌کند.
- 🚀 فهرست پیش‌فرض از سرورهای ضدتحریم و سریع:
  - Cloudflare (`1.1.1.1`)
  - Google Public DNS (`8.8.8.8`)
  - Quad9 (`9.9.9.9`)
  - Shecan (`178.22.122.100`)
  - Electro (`78.157.42.100`)
  - Radar Game (`10.201.201.201`)
  - 403 Online (`10.202.10.202`)
  - Begzar (`185.55.226.26`)
  - پشتیبانی از وارد کردن DNS سفارشی (Primary & Secondary)
- 🧩 ویجت‌های صفحهٔ اصلی و Quick Tile: قطع/وصل سریع و تغییر سرور از طریق ویجت یا Quick Settings tile.
- 🎨 رابط کاربری مدرن Glassmorphic، انیمیشن‌های روان، حالت تاریک و نشانه‌گرهای وضعیت DNS.
- 🔄 اتصال خودکار هنگام راه‌اندازی (اختیاری).

---
## 📥 دانلود

آخرین APK امضا‌شده را از صفحهٔ [Releases](https://github.com/SFDNSAPP/sfdns-pro/releases) دریافت کنید — نیازی به ساخت از سورس نیست.

---
## 🏗️ معماری و نحوهٔ کار

1. اپ یک رابط مجازی TUN محلی (`VpnService`) ایجاد می‌کند و به‌جای مسیریابی تمام ترافیک، به‌صورت انتخابی مقصدهای DNS را رهگیری می‌کند.
2. زمانی که DoH فعال است، پرس‌وجوهای DNS گرفته‌شده از پورت 53 تبدیل به درخواست‌های HTTPS مطابق RFC 8484 شده و از طریق TLS به resolver انتخاب‌شده ارسال می‌شوند (مثلاً `https://1.1.1.1/dns-query`).
3. اپ مینیمم لاگ را نگه می‌دارد و تلاشی برای ارسال telemetry به اشخاص ثالث نمی‌کند.

---
## 📱 تصاویر (Screenshots)

پیش‌نمایش تصاویر اصلی برنامه (برای مشاهدهٔ سایز کامل روی تصاویر در گیت‌هاب کلیک کنید):

| صفحه | پیش‌نمایش |
|---|---:|
| داشبورد (Dashboard) | <img width="240" alt="Dashboard" src="https://github.com/user-attachments/assets/4e748ac6-c996-4091-bb73-fa192b69224c" /> |
| تنظیمات پیشرفته (Advanced Settings) | <img width="240" alt="Advanced Settings" src="https://github.com/user-attachments/assets/90834c62-2e21-4674-bbe2-c32a94181f77" /> |
| ورودی DNS سفارشی (Custom DNS) | <img width="240" alt="Custom DNS" src="https://github.com/user-attachments/assets/bac20289-0678-482e-9653-5d15260142e0" /> |
| منوی اصلی DNS (Main DNS Menu) | <img width="240" alt="Main DNS Menu" src="https://github.com/user-attachments/assets/bbe9246c-e312-4831-a780-1794462f4ebe" /> |
| منوی کناری (Side Menu) | <img width="240" alt="Side Menu" src="https://github.com/user-attachments/assets/c903e875-0eba-40fa-8902-a8235fa082e0" /> |

---
## 🚀 راهنمای سریع

1. فایل APK را از [Releases](https://github.com/SFDNSAPP/sfdns-pro/releases) دانلود و نصب کنید.
2. برنامه را باز کرده و مجوز VPN را قبول کنید.
3. یک resolver از لیست انتخاب کنید یا آدرس‌های DNS سفارشی وارد کنید.
4. اگر می‌خواهید DNS رمزنگاری‌شده داشته باشید، DoH را فعال کنید.
5. برای اتصال/قطع سریع از Quick Settings tile یا ویجت استفاده کنید.

نکات:
- برنامه تنها پرس‌وجوهای DNS را رهگیری می‌کند؛ ترافیک عادی مسیریابی نمی‌شود.
- در برخی نسخه‌های اندروید اعلان دائمی VPN از طرف سیستم نمایش داده می‌شود که برای عملکرد مورد نیاز است.

---
## 🛠️ ساخت و نصب

### پیش‌نیازها
- Android Studio (Ladybug یا جدیدتر)
- JDK 17 یا 21
- Android SDK 36 (Min SDK 24)

### مراحل
1. مخزن را کلون کنید:
```bash
git clone https://github.com/SFDNSAPP/sfdns-pro.git
```
2. پروژه را در Android Studio باز کنید.
3. APK را با Gradle بسازید:
```bash
./gradlew assembleRelease
```
4. APK تولیدشده را نصب کنید.

---
## 🔒 امنیت و حریم خصوصی

- `allowBackup` روی `false` تنظیم شده است تا از استخراج حالت حساس برنامه جلوگیری شود.
- ترافیک Cleartext به‌صورت پیش‌فرض غیرفعال است (`usesCleartextTraffic="false"`).
- داده‌های کاربران و telemetry به‌صورت پیش‌فرض ذخیره یا ارسال نمی‌شود.

---
## 🗺️ نقشهٔ راه

این نقشهٔ راه به سه دسته تقسیم شده است تا وضعیت هر مورد مشخص باشد.

### انجام‌شده ✅
- [x] پیاده‌سازی اصلی پروکسی DoH و رهگیری DNS مبتنی بر VPN (قابلیت core در نسخه‌های منتشرشده موجود است).
- [x] فهرست پیش‌فرض سرورهای معروف (Cloudflare, Google, Quad9, Shecan و غیره).

### در حال انجام 🚧
- [ ] پشتیبانی از زبان‌های ترکی و فرانسوی (ترجمه و محلی‌سازی در حال انجام)

### برنامه‌های آینده 📋
- [ ] بهینه‌سازی بر اساس شبکه فعال (Wi‑Fi / mobile) و اعمال پروفایل‌های مجزای شبکه
- [ ] پشتیبانی از Split‑tunneling (مسیردهی DNS/برنامه‌ای)
- [ ] نمودار تاریخچهٔ پینگ/لاتنسی و تشخیص‌های بهتر
- [ ] صادرات/واردات پروفایل‌های DNS سفارشی (پشتیبان‌گیری و بازیابی)

برای پیشنهاد یا همکاری یک [issue](https://github.com/SFDNSAPP/sfdns-pro/issues) باز کنید یا PR ارسال کنید.

---
## ❓ پرسش‌های متداول (FAQ)

Q: آیا این اپ تمام ترافیک من را از طریق VPN هدایت می‌کند؟

A: خیر. اپ تنها پرس‌وجوهای DNS (پورت 53) را رهگیری و به resolver تعیین‌شده ارسال می‌کند؛ بقیهٔ ترافیک از مسیر عادی دستگاه عبور می‌کند.

Q: آیا پرس‌وجوهای DNS من لاگ یا به اشتراک گذاشته می‌شوند؟

A: خیر. اپ تلاش می‌کند کمترین اثر را داشته باشد و پرس‌وجوهای DNS را به‌صورت ماندگار لاگ نمی‌کند یا telemetry به اشخاص ثالث ارسال نمی‌کند.

Q: آیا اپ روی دستگاه‌های روت‌شده یا نسخه‌های قدیمی‌تر اندروید کار می‌کند؟

A: اپ از VpnService اندروید استفاده می‌کند و برای API level 24+ کار می‌کند. نیازی به روت نیست. برخی ROM‌های سازنده ممکن است رفتار VPN را تغییر دهند.

Q: چگونه می‌توانم ترجمه‌ها یا تغییرات را مشارکت دهم؟

A: لطفاً یک Issue باز کنید یا Pull Request ارسال کنید. مراحل مشارکت در بخش Contributing توضیح داده شده است.

---
## 🤝 مشارکت

مشارکت خوش‌آمد است!

1. مخزن را فورک کنید
2. یک شاخهٔ 기능 ایجاد کنید (`git checkout -b feature/your-feature`)
3. تغییرات را کامیت کنید (`git commit -m "Add: your feature"`)
4. شاخه را پوش کنید (`git push origin feature/your-feature`)
5. یک Pull Request باز کنید

لطفاً سبک کدنویسی را با قراردادهای Kotlin موجود رعایت کنید و تغییرات را قبل از ارسال تست کنید.

---
## ⚖️ سلب مسئولیت

این اپ تنها رزولوشن DNS را تغییر می‌دهد و ترافیک عمومی اینترنت را مسیریابی یا بازرسی نمی‌کند. این نرم‌افزار برای مقاصد حریم خصوصی، امنیت و دسترسی ارائه می‌شود.

کاربران مسئول رعایت قوانین محلی دربارهٔ استفاده از ابزارهای DNS/VPN هستند. توسعه‌دهندگان مسئولیتی بابت سوءاستفاده ندارند.

---
## 💬 پشتیبانی و تماس

- 🐛 خطا پیدا کردید؟ یک [Issue](https://github.com/SFDNSAPP/sfdns-pro/issues) باز کنید
- 💡 درخواست ویژگی؟ از برچسب `enhancement` استفاده کنید
- 📧 ایمیل: [sfdnsapp@gmail.com](mailto:sfdnsapp@gmail.com)
- 📢 کانال تلگرام: [@sfdnsapp](https://t.me/sfdnsapp)

اگر این پروژه برایتان مفید است، لطفاً ستاره بدهید ⭐

---
## 📄 مجوز

این پروژه با مجوز MIT عرضه شده است — برای جزئیات فایل [LICENSE](LICENSE) را ببینید.
