# TravelVista v1.0.0 Release Notes

**Version**: `1.0.0` (Version Code `1`)  
**Package**: `com.snj.travelvista`  
**Target Platform**: Android 7.0+ (API 24 to 36)  
**Release Date**: November 15, 2026  

---

## 🚀 Release Summary

TravelVista v1.0.0 is the production-ready release of the native Android travel discovery and itinerary planning application. Built strictly using **Kotlin and XML Views (No Jetpack Compose)**, the app delivers a fluid, modern travel exploration experience adhering to Android Architecture Components and Google Material 3 design guidelines.

This release integrates the **Wikimedia Foundation's Wikivoyage REST API** for live travel guides, real geographical coordinates, and authentic high-resolution photography from **Wikimedia Commons**, supported by a robust offline-first Room database cache, live debounced search, interactive trip planning tools, and persistent favorites.

---

## 🌟 Key Highlights & Changelog

### 1. Real API Integration & Live Data
* **Wikivoyage REST API**: Connected to `https://en.wikivoyage.org/api/rest_v1/page/summary/` to fetch real traveler descriptions, summaries, and coordinates.
* **Real Photography**: Replaced placeholder artwork with authentic, high-resolution photographs hosted on Wikimedia Commons.
* **Remote Search Fallback**: When searching for destinations outside the initial catalog, the app dynamically queries the live Wikivoyage search API.
* **Policy Compliance**: Configured compliant `User-Agent` (`TravelVista/1.0 (Android; contact@travelvista.app)`) via OkHttp interceptor without exposing private API keys or secrets.
* **Polite Batching & Retries**: Concurrently fetches data in polite chunks of 4 with exponential backoff retry to eliminate HTTP 429 rate-limiting.

### 2. Comprehensive Image Attribution & Provenance
* Extracted and preserved verified photographer credits and exact individual licenses (CC BY-SA 1.0, CC BY-SA 2.0, CC BY-SA 2.5, CC BY-SA 3.0, CC BY-SA 4.0, CC BY 2.0, and Public domain) for every photograph.
* Added an **Inline Photo Attribution Pill** on the Destination Details screen.
* Added an interactive **Photo Attribution Dialog** with a one-tap link opening the original Wikimedia Commons file page.
* Added a **Credits Chip** on the Destinations screen displaying full photo attribution details.

### 3. Offline-First Architecture & Database Safety
* **Dual-Table Room Database (v2)**: Maintains `cached_destinations` for fast offline loading and `favorite_destinations` for user wishlists.
* **Safe Non-Destructive Migration (`MIGRATION_1_2`)**: Upgrades database schema safely without wiping user favorites.
* **Zero Duplicates**: Enforced `OnConflictStrategy.REPLACE` on all cache insertions.
* **Instant Start**: Serves cached destinations from Room immediately on launch so users never experience blank screens.

### 4. Premium Material 3 UI/UX Design
* **Curated Color Palette**: Royal Violet (`#6750A4`), Soft Accent (`#9B87F5`), Clean Off-White canvas (`#F7F7FA`), and Deep OLED Dark mode canvas (`#121218`).
* **Explore Dashboard**: Brand header badge, 260dp Snap-carousel featured cards, 210dp popular cards, category pills, and trending vertical cards.
* **Destination Details**: 360dp `CollapsingToolbarLayout` with multi-stop scrims, frosted glass circular action buttons, structured highlights cards, and travel tips.
* **Interactive Trip Planner**: Bottom sheet featuring traveler and day steppers, live daily budget calculations, and itinerary confirmation feedback.

### 5. Production Optimization & R8 Shrinking
* **R8 Minification & Code Shrinking**: Enabled `isMinifyEnabled = true` and `isShrinkResources = true`.
* **ProGuard Compatibility**: Added comprehensive keep rules for Gson DTOs, Retrofit interfaces, Room database entities, and Glide modules.
* **Drastic APK Size Reduction**: Reduced APK size from **9.92 MB** (Debug) down to **3.72 MB** (Release) — a **62.5% decrease**.
* **Digitally Signed**: Output signed with the standard Android SDK certificate for immediate sideloading and testing.

---

## 🧪 Verification & Test Results

* **Unit Test Status**: 100% Passed (`BUILD SUCCESSFUL`)
* **Test Suites (8/8 Passed)**:
  1. `CachedDestinationEntityTest`: Entity serialization, lists, and attribution roundtrip.
  2. `DestinationEntityTest`: Saved favorites entity mapping.
  3. `DestinationModelTest`: Model formatting, price, rating, and location properties.
  4. `MockInterceptorTest`: Preserved demo/fallback network interceptor verification.
  5. `SortAndFilterLogicTest`: Rating, popularity, and price sorting algorithms.
  6. `TravelRepositoryTest`: Offline cache fallback, duplicate prevention, and favorites persistence.
  7. `WikivoyageApiParsingTest`: JSON DTO deserialization and null-safety.
  8. `WikivoyageLiveApiTest`: Live HTTP 200 verification against production Wikivoyage REST API.

---

## 📦 APK Artifacts

| Build Type | File Name | File Size | Output Location |
|---|---|---|---|
| **Optimized Release APK** | `app-release.apk` | **3,726,444 bytes** (~3.72 MB) | `app/build/outputs/apk/release/app-release.apk` |
| **Debug APK** | `app-debug.apk` | **9,921,222 bytes** (~9.92 MB) | `app/build/outputs/apk/debug/app-debug.apk` |

---

## 📥 Installation Instructions

### Via ADB
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Manual Sideloading
1. Transfer `app-release.apk` to your Android device via USB or cloud drive.
2. Open the file in your device's File Manager.
3. Allow *"Install unknown apps"* if prompted, and complete the installation.
