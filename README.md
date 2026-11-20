# TravelVista - Worldwide Travel & Booking Platform

![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20%28API%2024%2B%29-green.svg)
![Language](https://img.shields.io/badge/Language-Kotlin%202.x-blue.svg)
![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Repository-orange.svg)
![UI](https://img.shields.io/badge/UI-XML%20Views%20%2B%20Material%203-purple.svg)
![Database](https://img.shields.io/badge/Database-Room%20%28Offline--First%29-red.svg)
![Network](https://img.shields.io/badge/Network-Retrofit%202%20%2B%20OkHttp%204-teal.svg)
![APIs](https://img.shields.io/badge/Live%20APIs-Wikivoyage%20%7C%20Nominatim%20%7C%20Open--Meteo%20%7C%20REST%20Countries%20%7C%20Frankfurter-brightgreen.svg)
![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)

**TravelVista** is a comprehensive, production-grade native Android travel exploration and booking platform built strictly with **Kotlin and XML Views (NO Jetpack Compose)**, following modern Android Architecture Components and clean **MVVM architecture**.

The application empowers travelers to discover destinations worldwide, inspect authentic local culinary specialties and top-visited landmarks, check live weather, view country travel advisories, plan itineraries, manage interactive packing checklists, convert currencies, and initiate bookings through verified external travel providers (Hotels, Flights, Activities).

---

## 🚀 Advance Mode & External Features

### 1. 🏨 Multi-Provider Booking Hub (Trips & Details)
* **Hotels**: Search and compare real accommodations across **Booking.com**, **Google Hotels**, **Agoda**, and **Expedia**.
* **Flights**: Deep search routes via **Google Flights**, **Skyscanner**, and **Kayak**.
* **Tours & Activities**: Explore excursions via **GetYourGuide**, **Viator**, and **Airbnb Experiences**.
* **Directions & Navigation**: Instant one-tap navigation to destination coordinates via **Google Maps Directions**.
* *Transparent User Experience*: Includes explicit provider badges and transparency notices informing travelers that bookings are completed on legitimate provider platforms.

### 2. 🎒 Smart Packing Checklist & Essentials Generator (Trips Tab 5)
* **Pre-Loaded Packing Presets**: Filter items dynamically by travel scenario:
  * 🏖️ **Beach**: Swimwear, reef-safe sunscreen, sunglasses, flip-flops, beach towel, dry bag.
  * 🥾 **Trek / Outdoor**: Hiking boots, moisture-wicking layers, rain jacket, trekking poles, headlamp, hydration pack.
  * 🏙️ **City**: Casual sneakers, daypack, umbrella, portable power bank, offline maps.
  * ❄️ **Winter**: Thermal base layers, insulated winter coat, wool socks, gloves/beanie, lip balm.
  * 📄 **Essential Documents**: Passport/ID, flight tickets, hotel reservations, travel insurance, international credit cards.
* **Interactive Checklist**: Real-time progress bar with live completion percentage indicator.
* **Custom Item Addition**: Add personalized items with a Material Dialog.
* **Persistent Storage**: Saved in local app preferences so checklist state survives app closures and device reboots.

### 3. 💱 Live Multi-Currency Converter & Budget Calculator (Profile & Details)
* **Live Exchange Rates**: Fetched from the European Central Bank via the **Frankfurter API**.
* **Real-Time Currency Switching**: Switch between USD ($), EUR (€), GBP (£), INR (₹), JPY (¥), AUD (A$), and CAD (C$).
* **Dynamic Budgeting**: Detail screens and trip planners automatically convert daily destination expenses into your preferred currency with live formatted amounts.

### 4. 🗺️ Interactive OpenStreetMap Discovery with Category Filtering
* Powered by **osmdroid** and OpenStreetMap tiles.
* **Category Filtering Chips**: Filter map pins in real-time by *All*, *Mountains*, *Beaches*, *Cities*, *Historical*, and *Nature*.
* Interactive marker popups navigate straight to the destination detail guide.

### 5. 🍲 Worldwide Culinary Specialties & Most-Visited Sights Knowledge Base
* Powered by `GlobalCulinaryAndSightsCatalog` and Wikivoyage extraction:
* **Must-Try Regional Food**: Authentic local dishes with high-resolution photography, cultural origin notes, and "Must-Try" highlight badges.
* **Top-Visited Sights & Landmarks**: Iconic regional attractions with category tags and one-tap "📍 Search on Map" actions.
* **Dining Culture & Etiquette**: Practical tips for navigating local markets, street food, and tipping norms.

### 6. ⛅ Real-Time Live Weather & Country Essentials
* **Open-Meteo API**: Live temperature, high/low daily range, wind speed, humidity, and weather condition without requiring API keys.
* **REST Countries API**: Local currency, official languages, capital, region, time zones, and international dialing codes.

---

## 🌟 Core App Features

### 1. Explore Home Dashboard
* **Curated Greeting Header**: "✨ Discover the World" pill badge with clean typography.
* **Category Bar**: *Mountains*, *Beaches*, *Cities*, *Nature*, and *Historical Places*.
* **Featured Destinations Carousel**: 260dp widescreen cards with smooth `PagerSnapHelper` snapping, frosted category badges, star ratings, and budget indicators.
* **Popular Destinations**: Horizontal card layout highlighting top-rated tourist favorites with instant one-tap favorite bookmarking.
* **Trending Destinations**: Refined vertical list with rounded photography containers, category tags, and location markers.
* **Pull-to-Refresh**: Seamless `SwipeRefreshLayout` pulling fresh live data from the Wikivoyage REST API.

### 2. Destination Listing & Live Global Search
* **Worldwide Discovery**: Combining live **Wikivoyage Search** and **OpenStreetMap Nominatim Geocoding** to search any country, city, region, island, or tourist attraction on Earth.
* **Intelligent Image Sanitizer (`ImageLoader`)**:
  * Detects and filters out administrative district maps (`Map-India-Goa01.png`, `locator_map`, etc.) and flag SVGs, replacing them with curated, high-resolution scenic photography.
  * Rewrites Wikimedia Commons thumbnail URLs to standard `/500px-` CDN specifications, eliminating HTTP 400 Bad Request responses.
* **Category Filters & Sorting Dialog**: Filter by categories and sort by *Top Rated*, *Most Popular*, *Budget Friendly*, and *Alphabetical*.

### 3. Destination Details & Interactive Trip Planner
* **Robust Loading & Error States**: Centered indeterminate ProgressBar and retry layout ensure the detail screen is never blank or stuck in a loading state.
* **Collapsing Toolbar**: 360dp hero photography with frosted action buttons and smooth scroll collapse.
* **Highlights & Travel Tips**: Dynamic Material 3 cards with iconography detailing what to see and essential advice.
* **Interactive Trip Planner Bottom Sheet**:
  * Steppers for traveler count (1–12) and trip duration (1–30 days).
  * Live daily budget calculation with clear estimate labels.
  * Persisted directly to Room database (`trips` table) for offline access.
* **Native Share**: Android `ACTION_SEND` intent sharing travel highlights and itineraries.

### 4. Saved Favorites (Room Database v3)
* **Persistent Wishlist**: Stored locally in a dedicated Room database table (`favorite_destinations`) with reactive Kotlin Flow updates.
* **Offline Access**: Favorites remain fully accessible without an active internet connection.
* **Removal with Undo**: Quick removal action accompanied by an "Undo" Snackbar allowing instant restoration.
* **Zero Duplicate Guarantee**: Database operations use `OnConflictStrategy.REPLACE` to ensure duplicate-free caching.

---

## 🏛️ Architecture & Tech Stack

```
com.snj.travelvista/
├── data/
│   ├── local/
│   │   ├── DestinationDao.kt               # Room DAO with reactive Flows & sync lookups
│   │   ├── GlobalTravelDao.kt              # Room DAO for trips, searches, and recent views
│   │   ├── TravelDatabase.kt               # Room Database v3 with safe non-destructive migration
│   │   └── entity/                         # Room entities: Cached, Favorites, Trips, Searches
│   ├── model/
│   │   ├── Destination.kt                  # Core destination domain model
│   │   ├── DestinationFoodAndSights.kt     # Culinary specialties & top visited places models
│   │   ├── GlobalDestinationEnrichment.kt  # Weather & country metadata models
│   │   └── SortOption.kt                   # Sort enumeration
│   ├── remote/
│   │   ├── GlobalCulinaryAndSightsCatalog.kt # Worldwide culinary & sights knowledge engine
│   │   ├── RetrofitClient.kt               # Retrofit singleton with OkHttp & User-Agent interceptor
│   │   ├── WikivoyageApiService.kt         # Production Retrofit service for Wikivoyage API
│   │   ├── provider/                       # Deep link builders for Hotels, Flights, Activities
│   │   └── service/                        # Weather, Geocoding, Country, and Currency API services
│   └── repository/
│       ├── TravelRepository.kt             # Primary repository for destinations, cache, & favorites
│       └── GlobalTravelRepository.kt       # Repository for weather, country, currency, trips & maps
├── ui/
│   ├── common/                             # DiffCallbacks, Resource sealed class, ViewModelFactory
│   ├── destinations/                       # Search, filters, sort, and credits dialog
│   ├── details/                            # Collapsing toolbar, food, sights, weather, trip planner
│   ├── explore/                            # Main dashboard, featured carousel, categories
│   ├── favorites/                          # Saved favorites screen & Room DB observers
│   ├── map/                                # OpenStreetMap discovery with category filter chips
│   ├── profile/                            # Currency switcher, home airport, measurement units
│   ├── trips/                              # Trip planner, bookings hub & smart packing checklist
│   └── MainActivity.kt                     # Hosts NavHostFragment & BottomNavigationView
└── utils/
    ├── ImageLoader.kt                      # Glide loading, thumbnail upgrading & map filtering
    ├── GeoUtils.kt                         # Distance, coordinate & currency formatting utilities
    └── UserPreferences.kt                  # SharedPreferences for currency, units, and airport
```

### Technical Specifications

| Component | Technology |
|---|---|
| **Architecture** | MVVM (Model-View-ViewModel) + Repository Pattern |
| **Language** | Kotlin 2.x + Coroutines + StateFlow / Flow |
| **UI Framework** | Native Android XML (ConstraintLayout, CoordinatorLayout, NestedScrollView) — **NO Jetpack Compose** |
| **Design System** | Google Material 3 (Color Tokens, Shape Appearances, Light & Dark themes) |
| **List Performance** | RecyclerView + `ListAdapter` + `DiffUtil.ItemCallback` |
| **View Binding** | Enabled (`buildFeatures.viewBinding = true`) |
| **Navigation** | Android Jetpack Navigation Component with single Activity architecture |
| **Remote APIs** | Wikivoyage REST API, OpenStreetMap Nominatim, Open-Meteo, REST Countries, Frankfurter ECB |
| **Networking** | Retrofit 2.11 + OkHttp 4.12 + Gson 2.11 + Logging Interceptor |
| **Local Cache** | AndroidX Room 2.8 with SQLite + KSP (Database v3) |
| **Image Loading** | Glide 4.16 with disk caching, memory cache, and crossfade animations |
| **Build System** | Gradle 9.4.1 + Android Gradle Plugin 9.0.1 + R8 ProGuard |
| **SDK Levels** | Min SDK: 24 (Android 7.0) • Compile SDK: 36 • Target SDK: 36 |

---

## 🚀 Building & Testing

### Run Unit Tests
```bash
# Execute all 13 unit test suites
.\gradlew.bat testDebugUnitTest
```

### Build Production Signed Release APK (Optimized with R8)
```bash
# Compiles optimized, shrunk, and signed release APK
.\gradlew.bat assembleRelease
```
* **Release APK Path**: `app/build/outputs/apk/release/app-release.apk`
* **Release APK Size**: `~4.33 MB` (Optimized with R8 minification and resource shrinking)

### Build Debug APK
```bash
.\gradlew.bat assembleDebug
```
* **Debug APK Path**: `app/build/outputs/apk/debug/app-debug.apk`
* **Debug APK Size**: `~10.81 MB`

---

## 📱 Sideload / Install Instructions

To install the signed release APK onto an Android phone or emulator via ADB:
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Or copy `app-release.apk` directly to your Android device's storage and open it in your device's File Manager (enable *"Install Unknown Apps"* if prompted).

---

## 📄 License & Attribution
This project is licensed under the Apache License 2.0. Travel guide content and images are copyrighted by their respective contributors on Wikivoyage and Wikimedia Commons and used under Creative Commons and Public Domain licenses. Live weather data provided by Open-Meteo under CC BY 4.0. Geocoding data © OpenStreetMap contributors under ODbL.
