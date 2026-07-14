<img width="1080" height="2340" alt="Screenshot_20260614_163921" src="https://github.com/user-attachments/assets/59d22d40-bce7-4ffb-9b00-19c589f80286" />


# Sodor

Sodor is a native Persian invoice and proforma invoice application for Android. It is designed for small businesses that need a fast, RTL-first workflow for managing customers, products, payments, and printable documents.

The application stores its core business data locally, so everyday invoice management works offline.

## Features

- Create and manage invoices and proforma invoices
- Maintain customer and product catalogs
- Track payments, balances, and invoice status
- Scan product barcodes with the device camera
- Generate, print, and share Persian PDF invoices
- Use Jalali dates, Persian digits, and right-to-left layouts
- Attach supporting files to invoices
- View dashboard metrics and sales reports
- Back up and restore the local database
- Optionally store backups in the app's private Google Drive data folder
- Customize business information, logo, stamp, signature, theme, and currency

## Tech Stack

- Kotlin
- Jetpack Compose and Material 3
- Room database
- Kotlin Coroutines and Flow
- Navigation Compose
- CameraX, Google Code Scanner, and ML Kit
- Android `PdfDocument` and printing APIs
- Gradle Kotlin DSL with a version catalog

## Requirements

- Android Studio with JDK 17 or newer
- Android SDK 36
- An Android device or emulator running Android 7.0 (API 24) or newer

## Getting Started

1. Clone the repository:

   ```bash
   git clone https://github.com/your-username/sodor.git
   cd sodor
   ```

2. Open the project directory in Android Studio.
3. Let Gradle synchronize and install any missing Android SDK components.
4. Select the `app` run configuration and an emulator or connected device.
5. Run the application.

You can also build a debug APK from the command line:

```bash
./gradlew assembleDebug
```

On Windows, use:

```powershell
.\gradlew.bat assembleDebug
```

The generated APK is placed under `app/build/outputs/apk/debug/`.

## Release Signing

Release builds read signing credentials from environment variables. Keep the keystore and all credentials outside version control.

```text
KEYSTORE_PATH=/absolute/path/to/my-upload-key.jks
STORE_PASSWORD=your-store-password
KEY_PASSWORD=your-key-password
```

The configured key alias is `upload`. Change it in `app/build.gradle.kts` if your keystore uses a different alias.

## Project Structure

```text
app/src/main/java/com/example/
├── data/          # Room entities, DAOs, database, and repositories
├── ui/            # Compose screens, reusable components, and theme
├── utils/         # PDF, backup, validation, date, file, and Drive helpers
└── MainActivity.kt
```

Additional design and architecture notes are available in:

- [`ARCHITECTURE.md`](ARCHITECTURE.md)
- [`DATABASE_DESIGN.md`](DATABASE_DESIGN.md)
- [`UI_UX_SPECIFICATION.md`](UI_UX_SPECIFICATION.md)

## Privacy

Invoice, customer, product, and payment data is stored in the application's local database. Network access is used only by features that require it, such as optional Google Drive backup and downloading bundled fonts when they are absent during a build.

## Contributing

Issues and pull requests are welcome. Before submitting a change, run the unit tests and make sure the debug build completes successfully:

```bash
./gradlew test assembleDebug
```

## License

No open-source license has been added yet. Until a license is provided, all rights are reserved by the project owner.
