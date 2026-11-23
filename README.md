<div align="center">
TezBazar
Nationwide Trilingual Express Marketplace, Multimodal AI Concierge & Live Courier Platform for Uzbekistan
![Image](https://img.shields.io/badge/Platform-Android%20(API%2024%E2%80%9336)-3DDC84?style=flat-square&logo=android&logoColor=white)
![Image](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Image](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Image](https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth%20%7C%20App%20Check-FFCA28?style=flat-square&logo=firebase&logoColor=black)
![Image](https://img.shields.io/badge/Firebase%20AI%20Logic-Gemini%203.8%20Flash-8E75B2?style=flat-square&logo=googlegemini&logoColor=white)
![Image](https://img.shields.io/badge/i18n-UZ%20%7C%20RU%20%7C%20EN-059669?style=flat-square)
</div>
Overview
TezBazar is a production-structured Android e-commerce and express delivery application tailored for Uzbekistan. Built with 100% Kotlin, Jetpack Compose (Material Design 3), Cloud Firestore, Jetpack Credential Manager, and Firebase AI Logic (Gemini), the app unifies curated grocery/tech shopping, multimodal AI discovery, local payment workflows (Uzcard / Humo / Cash), and real-time order telemetry across 14 regions and 47 cities/districts of Uzbekistan.
Key Highlights
Nationwide Geo-Logistics (14 Regions / 47 Cities): Full coverage from Tashkent City districts to Samarkand, Bukhara, Fergana Valley, Khorezm, and Karakalpakstan with express courier ETAs, interactive coordinate-mapped address picking (AddressMapPicker), and regional PVZ (Pickup Point) hubs.
Multimodal Gemini Concierge (Firebase.ai): Zero-API-key client AI secured by Firebase App Check, delivering:
Structured JSON personalized product bundles (Schema.obj) with 12-hour on-disk TTL caching and automatic fallback (gemini-3.5-flash-lite ↔ gemini-3.8-flash).
Visual Search: Camera capture or zero-permission Android Photo Picker (PickVisualMedia) dish/product recognition.
Voice & Audio Search: On-device audio capture (MediaRecorder) analyzed directly via inline multimodal Gemini payloads.
Streaming Chat: Real-time generateContentStream() shopping & culinary assistant with catalog item extraction.
Zero-Trust Multi-User Backend: Named Cloud Firestore database (R.string.firestore_database_id) paired with strict owner-isolated security rules (firestore.rules), server-side timestamp invariants, and 5/5 automated emulator rule verification tests (firestore.test.js).
One-Tap Google Sign-In: Exclusive Jetpack Credential Manager integration (GetGoogleIdOption silent sign-in + GetSignInWithGoogleOption interactive flow) with key(uid) ViewModel/session isolation.
Architecture
TezBazar follows MVVM and Unidirectional Data Flow (UDF) with strict separation between UI state, reactive Firestore listeners, and AI inference pipelines.
Text
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Presentation Layer                             │
│  MainActivity (App Check Init + Edge-to-Edge)                               │
│   └─ TezBazarApp (Auth-Gated Root + key(uid) Session Scope)                 │
│       ├─ AuthScreen (Credential Manager Google Sign-In)                     │
│       ├─ Adaptive Shell (NavigationBar < 600dp │ NavigationRail >= 600dp)   │
│       │   ├─ HomeScreen          ├─ AiAssistantScreen   ├─ TrackingScreen   │
│       │   ├─ CartCheckoutScreen  └─ ProfileScreen                           │
│       └─ ViewModels (MarketplaceViewModel │ AppSettingsViewModel)           │
│           └─ StateFlow<UiState<T>> (Loading / Success / Error)              │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │ Coroutines / Flow / Result<T>
┌──────────────────────────────────▼──────────────────────────────────────────┐
│                           Data & Service Layer                              │
│  ├─ MarketplaceRepository       (Named Firestore CRUD, Batch Orders, Flow)  │
│  ├─ AiRecommendationRepository  (Firebase.ai, Schema.obj, Stream, Cache)    │
│  ├─ LanguagePreferences         (Jetpack DataStore: Locale, Theme, Favs)    │
│  ├─ CatalogSeedData             (24 Localized Curated Products, 6 Cats)     │
│  └─ UzbekistanGeoCatalog        (14 Regions, 47 Cities, PVZs, Geo-Math)     │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼──────────────────────────────────────────┐
│                        Firebase Cloud & Security                            │
│  ├─ Firebase Auth               (Google Sign-In Provider Only)              │
│  ├─ Cloud Firestore             (Named Enterprise DB + firestore.rules)     │
│  ├─ Firebase App Check          (Debug Secret Sync Commit / reCAPTCHA Ent.) │
│  └─ Firebase AI Logic           (Gemini 3.8 Flash & 3.5 Flash-Lite)         │
└─────────────────────────────────────────────────────────────────────────────┘
Architectural Invariants
App Check Before UI (MainActivity.onCreate): configureAppCheck(this, intent) runs prior to setContent and ViewModel creation, synchronously persisting FIREBASE_APPCHECK_DEBUG_TOKEN via .commit() in debug builds and loading recaptcha_site_key in release builds.
Session-Isolated ViewModels: Authenticated routes are wrapped in key(uid) and viewModel(key = currentUserId) so switching accounts tears down and recreates Firestore snapshot listeners cleanly without cross-user state leakage.
Two-Tier Flow Error Unwrapping: Firestore snapshot streams unwrap nested CancellationException causes to surface FirebaseFirestoreException as UiState.Error while preserving cooperative coroutine cancellation.
Structured Fault Observability: All Firestore failures route through handleFirestoreError(...) (FirestoreErrorHandler.kt) emitting structured JSON diagnostics with operation type, document path, and auth context.
Core Features
1. Trilingual Localization & Adaptive UI
Instant Runtime Language Switching: Full support for Uzbek (Latin — uz), Russian (ru), and English (en) persisted in DataStore (LanguagePreferences.kt) and synchronized to /users/{uid}.
Material 3 Emerald & Bukhara Gold Theme: Custom color system (EmeraldPrimary, BukharaGold, NightEmeraldBg) supporting System / Light / Dark modes with bundled Montserrat and Inter typefaces (res/font/).
Responsive Form Factors: BoxWithConstraints dynamically adapts between compact mobile (NavigationBar + modal bottom sheet) and tablet/foldable layouts (NavigationRail + split wide detail pane).
2. Nationwide Catalog & Smart Cart
6 Curated Categories & 24 Seed Products + Remote Overlay: Supports cursor-paginated /catalog overlay loading (startAfter) merged seamlessly with CatalogSeedData.
Uzum-Style Installment Preview: Automatic 12-month installment calculation (monthlyInstallmentUzs) alongside discount badges and favorites filtering.
Checkout & Local Payments:
Delivery Modes: Door-to-door Express Courier (12,000 UZS, free over 120,000 UZS) or Free TezBazar PVZ Pickup Point.
Payment Vault: Validates and stores masked Uzcard (8600) and Humo (9860) card metadata (lastFour, maskedNumber, expiry, holder name — never raw PAN/CVV) plus Cash on Delivery.
Promo Engine & Loyalty: Supports promo codes (TEZ2026 — 15% off, SALOM10 — 10,000 UZS off, UZUMFREE / TEZFREE — free delivery) and atomic batch loyalty point rewards on checkout.
3. Live 5-Stage Order Tracking
Tracks orders across PLACED → PREPARING → ON_THE_WAY → ARRIVING → DELIVERED (plus CANCELLED) with interactive canvas courier route visualization (LiveTashkentCourierMap), courier contact card, one-tap reorder, and optional auto-progression demo toggle.
Firestore Schema & Security Rules
All entities are formally defined in firebase-blueprint.json and enforced in firestore.rules under a global default-deny policy.
Collection Path	Access Control	Key Validations & Invariants
/catalog/{productId}	Authenticated read-only (write: false)	Valid document ID check; client mutations prohibited.
/users/{userId}	Owner get / create / update / delete (list: false)	userId == request.auth.uid; immutable createdAt; strict allowlisted affectedKeys; languageCode in ['uz', 'ru', 'en'].
/users/{userId}/cart/{productId}	Owner CRUD (isOwner(userId))	quantity bounded 1..99; unitPriceUzs > 0; productId matches document ID.
/users/{userId}/orders/{orderId}	Owner CRUD (isOwner(userId))	Must initialize as status == 'PLACED'; routeProgress in 0.0..1.0; terminal state lock blocks edits once DELIVERED or CANCELLED.
/users/{userId}/addresses/{addressId}	Owner CRUD (isOwner(userId))	Coordinate bounds enforced (lat in -90..90, lng in -180..180).
/users/{userId}/payment_methods/{cardId}	Owner CRUD (isOwner(userId))	brand in ['UZCARD', 'HUMO']; masked metadata only (lastFour.size() == 4).
/users/{userId}/ai_profiles/{profileId}	Owner CRUD (isOwner(userId))	Bounded lists (<= 10 items) for topCategories and recentQueries.
Tech Stack
Layer	Technology / Library	Version / Notes
Language	Kotlin	2.4.21
Build System	Android Gradle Plugin (Kotlin DSL)	AGP 9.1.1 / compileSdk 36 / minSdk 24
UI Framework	Jetpack Compose + Material 3	Compose BOM 2024.09.00
Navigation	Jetpack Navigation Compose	2.8.9
State & Lifecycle	ViewModel, StateFlow, Lifecycle Compose	2.8.7
Cloud Backend	Firebase BoM (firestore, auth, ai, appcheck)	35.1.0
Authentication	Jetpack Credential Manager + Google Identity	credentials 1.5.0, googleid 1.1.1
Local Storage	Jetpack DataStore Preferences	1.1.7
Location	Google Play Services Location	21.3.0
Testing	JUnit 4, Robolectric, Coroutines Test, Firebase Rules Unit Testing	Robolectric 4.16.1, node:test
Project Structure
Text
.
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── firebase/
│       │   │   │   │   ├── AiRecommendationRepository.kt
│       │   │   │   │   ├── AppCheckInit.kt
│       │   │   │   │   ├── AuthHelper.kt
│       │   │   │   │   ├── FirestoreErrorHandler.kt
│       │   │   │   │   └── MarketplaceRepository.kt
│       │   │   │   └── model/
│       │   │   │       ├── CatalogModels.kt
│       │   │   │       ├── FirestoreModels.kt
│       │   │   │       └── UzbekistanGeoCatalog.kt
│       │   │   ├── i18n/
│       │   │   │   ├── AppLanguage.kt
│       │   │   │   ├── LanguagePreferences.kt
│       │   │   │   └── Strings.kt
│       │   │   └── ui/
│       │   │       ├── TezBazarApp.kt
│       │   │       ├── components/
│       │   │       │   ├── AddressMapPicker.kt
│       │   │       │   ├── LiveTashkentCourierMap.kt
│       │   │       │   ├── ProductDetailSheet.kt
│       │   │       │   ├── ProductVisualArtwork.kt
│       │   │       │   └── RegionPickerDialog.kt
│       │   │       ├── screens/
│       │   │       │   ├── AiAssistantScreen.kt
│       │   │       │   ├── AuthScreen.kt
│       │   │       │   ├── CartCheckoutScreen.kt
│       │   │       │   ├── HomeScreen.kt
│       │   │       │   ├── ProfileScreen.kt
│       │   │       │   └── TrackingScreen.kt
│       │   │       ├── theme/
│       │   │       │   ├── Color.kt
│       │   │       │   ├── Theme.kt
│       │   │       │   └── Type.kt
│       │   │       └── viewmodel/
│       │   │           ├── AppSettingsViewModel.kt
│       │   │           └── MarketplaceViewModel.kt
│       │   └── res/
│       └── test/
├── firebase-blueprint.json
├── firebase.json
├── firestore.rules
├── firestore.test.js
├── gradle/libs.versions.toml
└── settings.gradle.kts
Getting Started
Prerequisites
JDK 11+
Android SDK (Compile/Target SDK 36, Min SDK 24)
Gradle 9.x
Node.js & Firebase CLI (optional, for running local Firestore security rules tests)
Configuration
Firebase Project Config: Ensure app/google-services.json corresponds to your Firebase project and registers your debug/release SHA-1 & SHA-256 signing fingerprints for Google Sign-In.
Named Database & App Check: Verify app/src/main/res/values/firebase_applet_config.xml defines:
firestore_database_id — target Firestore database ID.
recaptcha_site_key — reCAPTCHA Enterprise site key for release App Check attestation.
No Client AI Key Required: Gemini calls execute through Firebase AI Logic (Firebase.ai) backed by App Check rather than a hardcoded client GEMINI_API_KEY.
Build & Run
Bash
# Compile Kotlin debug sources
gradle :app:compileDebugKotlin

# Build full debug APK
gradle :app:assembleDebug

# Run local JVM / Robolectric unit tests
gradle :app:testDebugUnitTest
Firestore Security Rules Verification
The repository includes a Kotlin-mirrored rules test suite (firestore.test.js) validating unauthenticated denial, cross-user isolation, @DocumentId injection rejection, coordinate/numeric bounds, and terminal order state locking.
Bash
# 1. Start the single-database Firestore emulator on port 8085
firebase emulators:start --only firestore --project demo-no-project

# 2. In a separate terminal, execute the security rules test suite
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
Permissions & Privacy
Permission	Purpose	Handling
INTERNET, ACCESS_NETWORK_STATE	Cloud Firestore, Auth, Firebase AI Logic	Normal install-time permission
ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION	Current GPS centering in AddressMapPicker	Runtime prompt with graceful manual fallback
CAMERA	In-app visual product/dish search capture	Runtime prompt; zero-permission PickVisualMedia also supported
RECORD_AUDIO	Voice shopping search via Gemini audio input	Runtime prompt on microphone press
License
This project is intended for demonstration and portfolio engineering reference. See your repository distribution settings for licensing terms.
