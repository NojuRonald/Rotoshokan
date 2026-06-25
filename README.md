# ROtoshokanD 📚

[![Platform: Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![UI Framework: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20M3-ff69b4.svg)](https://developer.android.com/jetpack/compose)
[![OCR: Google ML Kit](https://img.shields.io/badge/OCR-Google%20ML%20Kit-blue.svg)](https://developers.google.com/ml-kit)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**ROtoshokanD** is a native Android application built with Kotlin and
Jetpack Compose that modernizes your physical library browsing experience. 

Using your device's camera, the app captures text layouts from book covers,
extracts them into structured individual lines using on-device machine
learning (Google ML Kit), and allows you to dynamically select specific
lines to compile a clean search query. This query is then sent to a
remote, upcycled server endpoint running on an old smartphone to fetch
digital eBook links from the Google Books library database.

---

## 🚀 Key Features

* **Interactive OCR Query Builder:** Instead of sending unpolished or
  chaotic raw text data, the app splits parsed text blocks into clean,
  horizontal lines. These are fed into a scrollable `LazyRow` layout as
  individual `SuggestionChip` components. You tap exactly which text
  fragments (like specific Title or Author names) to bundle into your
  final search query.
* **Low-Latency CameraX Integration:** Utilizes `androidx.camera` with an
  optimization profile (`CAPTURE_MODE_MINIMIZE_LATENCY`) bound directly
  to the Composable component's active lifecycle context.
* **Material Design 3 Dashboard:** Designed completely with modern Material
  3 assets including a real-time targeting scanner overlay, conditional
  loading states, and an isolated `SelectionContainer` layout scope so
  you can easily copy returned digital URLs.
* **Asynchronous Network Resiliency:** Implements non-blocking background
  queue execution using `OkHttpClient` to handle payload delivery and
  catch transmission exceptions smoothly.

---

## 🛠️ Tech Stack & Dependencies

| Category | Technology Used | Version / Details |
| :--- | :--- | :--- |
| **Language** | Kotlin | Modern Android Native |
| **UI Framework** | Jetpack Compose | Material 3 Components |
| **Camera API** | CameraX | v1.3.1 (Core, Camera2, Lifecycle, View) |
| **OCR Engine** | Google ML Kit | v16.0.0 (Text Recognition - Latin) |
| **Networking** | OkHttp | v4.12.0 (Asynchronous Requests) |

---

## 🔗 Architecture & System Data Flow
