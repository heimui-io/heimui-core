# HeimUI Core: Server-Driven UI Framework for Kotlin Multiplatform

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-purple.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20iOS-orange.svg)]()

> **The extensible Server-Driven UI framework for Kotlin Multiplatform & Compose.**

**HeimUI Core** is an enterprise-grade Server-Driven UI (SDUI) SDK designed for Android and iOS using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. It renders remote UI layouts natively at 60/120 FPS without WebViews or JavaScript bridges.

---

## 🌟 Key Features

* **Zero-Bridge Native Performance:** Direct rendering through Jetpack Compose (Android) and Skiko (iOS).
* **Strict Clean Architecture:** Strict separation between pure Domain (zero UI dependencies), Data/Storage, and Presentation layers.
* **100% Type-Safe & Zero Reflection:** Compile-time polymorphic serialization via `kotlinx.serialization` (ProGuard / R8 safe).
* **15 Built-in Primitives:** `Container`, `Box`, `LazyColumn`, `LazyRow`, `Text`, `Image`, `Card`, `Badge`, `Button`, `TextField`, `Switch`, `Icon`, `Spacer`, `Divider`, `Custom/Unknown`.
* **Dynamic Condition Engine:** Client-side rule evaluation (`visibleIf`) supporting logical and comparison operators.
* **Form Validation Engine:** Built-in validator for required fields, regex, email, min/max length, and numeric inputs.
* **Reactive State & Crash Recovery:** Reactive state management (`HeimStateManager`) with persistent recovery across app restarts.
* **Native Accessibility (A11y):** Declarative TalkBack (Android) and VoiceOver (iOS) semantic bindings.
* **Signed Screens (opt-in):** ES256 verification through each platform's own cryptography, in the JWS profile — a detached `X-Heim-Signature` from a server, or a sealed object from a bucket that cannot send one. The app carries public keys only, so extracting them from an APK gains nothing. Off until `trustedSigningKeys` names a key.

---

## 🏛️ Clean Architecture Structure

```
io.heimui.core/
│
├── domain/                               # Pure Kotlin (Zero Compose, Zero Platform APIs)
│   ├── model/                            # Polymorphic @Serializable contracts
│   │   ├── component/HeimComponent.kt    # 15 Core UI Primitives
│   │   ├── action/HeimAction.kt          # Polymorphic actions (Navigate, Submit, etc.)
│   │   ├── accessibility/HeimA11y.kt     # Screen reader semantic roles
│   │   ├── validation/ValidationRule.kt  # Input validation definitions
│   │   └── HeimScreenResponse.kt         # Root screen payload contract
│   ├── evaluator/
│   │   ├── HeimConditionEvaluator.kt     # Evaluates visibleIf expressions
│   │   └── HeimValidationEngine.kt       # Validates form inputs against rules
│   └── repository/
│       └── HeimScreenRepository.kt       # Abstract repository interface
│
├── data/                                 # Network, Cache, & Security Layer
│
└── presentation/                         # UI & State Management Layer
    └── state/HeimStateManager.kt         # StateFlow state machine & interpolation
```

---

## 🚀 Running the Apps & Tests

### Running Unit Tests (Android & iOS)
```bash
./gradlew allTests
```

### Running the Android Sample App
```bash
./gradlew :androidApp:assembleDebug
```

### Running the iOS App
Open the `iosApp` directory in Xcode and run the target.

---

## 📄 License

HeimUI Core is licensed under the [Apache License 2.0](LICENSE).
