# QRScannerCreator

A modern Android app for QR code generation and scanning, supporting WiFi quick connect, logo QR, and more. (Min SDK: Android 11)

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
