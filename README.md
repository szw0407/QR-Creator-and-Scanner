# QRScannerCreator

A modern Android app for QR code generation and scanning, supporting WiFi quick connect, logo QR, and more. (Min SDK: Android 11)

## Background

这原本是我在 2025 年的“移动互联网开发课程”的一次作业而已。但是我意识到这个功能，如果全部实现了，甚至比很多现有的应用都要好用，所以就决定把它开源出来，以供大家使用。

曾经（以及现在）我使用的扫描二维码的软件（除了微信、支付宝之类），是[QR Scanner](https://fxedel.gitlab.io/fdroid-website/en/packages/com.secuso.privacyFriendlyCodeScanner/)（F-Droid 上的某款开源应用），对比一看功能上都差不多了（没实现的以后可以实现，无所谓），UI和依赖库倒是都现代化了很多，功能也会有一些改进和增强。

## Features

- **QR Code Generation**
  - Generate QR codes from any text
  - Support for error correction levels (L/M/Q/H)
  - Add custom logo to the center of QR code (PNG transparency supported)
  - Save QR code as PNG to file or directly to gallery

- **QR Code Scanning**
  - Scan QR codes using camera (with permission handling)
  - Scan QR codes from images in gallery
  - Decodes and displays QR content instantly
  - Click decoded result to perform smart actions (open URL, connect WiFi, etc.)
  - Long-press decoded result to copy to clipboard

- **WiFi QR Code Support**
  - Recognize WiFi QR codes and suggest system-level WiFi connection (WPA1/2/3 Personal, open network)
  - Permission check and user guidance for WiFi connection
  - Enterprise WiFi/EAP is not supported yet as I have no access to an EAP network for testing

- **QR Vcard Support**
  - Recognize QR codes containing vCard data
  - Display contact information with options to save to contacts or copy to clipboard
  - Save into the device's contacts app

- **Barcode Support**
  - Custom scan activity: scans all major 1D/2D barcodes (QR, Code128, EAN, PDF417, DataMatrix, etc.)
  - Orientation-aware: landscape for barcodes, portrait for QR
  - Flashlight toggle in scan UI

- **User Experience**
  - Material 3 UI, responsive and modern
  - Toast feedback for all actions
  - All features available offline

## How to Build

1. Open in Android Studio (Arctic Fox or newer recommended)
2. Sync Gradle and build the project
3. Run on a device (Android 11+ required)

---

**Note:**
- WiFi connection uses Android's official suggestion API, user confirmation is required for security reasons.
- Enterprise WiFi (EAP) is not supported yet due to lack of testing environment. If anyone can provide an EAP network QR codes for testing, please let me know via issues.
