# Shopizzo — Your Trusted Plug for Electronic Products

An Android e-commerce marketplace app built with **Kotlin**, **Jetpack Compose**,
**Firebase (Authentication + Firestore)**, and advanced UI/UX enhancements.





Developer Details

| Name | Cherby Chirwa |
| Email address |cherbychirwa@gmail.com  |


1. How to Open This Project in Android Studio

1. **Android Studio version** — Use **Android Studio Koala (2024.1)** or newer.
   Older versions cannot parse Gradle 8.7 / AGP 8.5.

2. **Unzip / clone** the project so the folder structure looks like this:
   ```
   Shopizzo/
   ├── app/
   ├── gradle/
   ├── gradlew
   ├── gradlew.bat
   ├── build.gradle.kts
   ├── settings.gradle.kts
   └── gradle.properties
   ```

3. Open Android Studio → **File → Open** → select the `Shopizzo` root folder
   (the one containing `settings.gradle.kts`), **not** the `app` subfolder.

4. Let Gradle sync. The first sync downloads Gradle 8.7 and all dependencies.

5. **IMPORTANT — Firebase setup is required before the app will run.**
   See section 2 below. `app/google-services.json` is required for the app to initialize Firebase correctly.

---

## 2. Firebase Setup (Required)

1. Go to [https://console.firebase.google.com](https://console.firebase.google.com)
   and create a new project.
2. Add an Android app to the project with package name **`com.shopizzo`**.
3. Download the generated **`google-services.json`** file and place it in the `app/` folder.
4. In the Firebase Console, enable:
   - **Authentication** (Email/Password, Google, Phone)
   - **Firestore Database** (start in test mode)
5. The app seeds Firestore automatically with **20+ premium electronic products** on the first run.

### Suggested Firestore Security Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /products/{productId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

---

## 3. Features Implemented

| Category | Feature | Status | Notes |
|---|---|---|---|
| **Authentication** | Multi-method Auth | ✅ | Email, Google, and Phone/OTP supported |
| | Biometric Login | ✅ | Fingerprint / Face Unlock toggle in Profile |
| | Password Reset | ✅ | Self-service forgot password flow |
| **Home Experience** | Search & Filtering | ✅ | Real-time search by Product Name or Brand |
| | Featured Brands | ✅ | Horizontal slider featuring top manufacturers |
| | Auto-Slider | ✅ | Dynamic featured products hero section |
| | Personalized UI | ✅ | Persistent user greeting ("Hi, Name 👋") |
| **Browsing** | Product Grid | ✅ | Category-based grouping with visual headers |
| | Product Badges | ✅ | "NEW" badges for recent items & Discount tags |
| | Real-time Sync | ✅ | Live Firestore updates for price/stock |
| **Details & Cart** | Product Rating | ✅ | Interactive 1–5 star picker with live average |
| | Favourites | ✅ | Persistent per-user liked items list |
| | Cart Management | ✅ | Quantity steppers and live total calculation |
| **Design & Tech** | Image Optimization | ✅ | Coil caching (Disk/Memory), placeholders & crossfade |
| | Dark Mode | ✅ | Advanced Appearance settings (Light/Dark/System) |
| | Architecture | ✅ | Clean MVVM with StateFlow and Repository pattern |
| | Seed Data | ✅ | 20+ products seeded automatically |

---

## 4. UI/UX Highlights

- **Branding**: Consistent use of **Shopizzo Blue**, Black, and White.
- **Image Stability**: Coil implementation ensures no blank spaces, using gallery placeholders during loads and alert icons for broken links.
- **Categorization**: The Products screen automatically groups items by category (Smartphones, Laptops, etc.) for easier navigation.
- **Discovery**: Real-time filtering allows users to find products or specific brands (Samsung, Apple, Sony) instantly.

---

## 5. Architecture

```
com.shopizzo
├── ShopizzoApplication.kt     – Global Coil & Caching configuration
├── MainActivity.kt            – NavHost + Dynamic Bottom Bar
├── data/
│   ├── model/                 – Product, UserProfile, Order, CartItem
│   └── repository/            – AuthRepository, ProductRepository, UserPreferences
├── viewmodel/                 – AuthViewModel, ProductViewModel (StateFlow-based)
└── ui/
    ├── screens/                – All screens (Home, Products, Details, Profile, etc.)
    ├── components/             – Custom UI (Search Bar, Brand Slider, Product Card)
    └── theme/                  – Color, Typography, and Dynamic Theme support
```

---

## 6. Known Limitations

- The PayChangu integration currently operates in a simulated sandbox mode for grading purposes.
- The web administration portal (bonus feature) was not implemented in this submission.
- Demo images require an active internet connection to download from Unsplash.
