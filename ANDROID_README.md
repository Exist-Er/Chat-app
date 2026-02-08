# Encrypted Chat App (Android) - Manga Edition

Offline-first, end-to-end encrypted chat application with a distinctive black & white Manga aesthetic.

## Features

- **Manga/Anime Theme**: High contrast, black & white UI with comic-style bubbles.
- **End-to-End Encryption**: Powered by Google Tink (Hybrid Encryption).
- **Offline-First**: Local SQLite database (Room) ensures instant UI updates.
- **Real-Time**: WebSocket connection for instant message delivery.
- **Secure**: 
  - Private keys never leave the device (Android Keystore).
  - Backend is Zero-Knowledge (only relays ciphertext).

## Architecture

- **UI**: Jetpack Compose (Material3)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Local Data**: Room Database + Flow
- **Network**: Retrofit + OkHttp (WebSocket)
- **Encryption**: Tink (HybridEncrypt/Decrypt)

## Setup

1. Open project in **Android Studio Hedgehog** (or newer).
2. Sync Gradle.
3. Ensure backend server is running on `localhost:8080`.
   - If running on Emulator, the app connects to `10.0.2.2:8080`.
   - If running on real device, update `MainActivity.kt` with your PC's local IP (e.g., `192.168.1.X`).
4. Run the app.

## Development Status

- **Theme**: Implemented (Manga Light/Dark modes).
- **Crypto**: Stubbed implementation using Tink.
- **Data Layer**: Room implemented.
- **Network**: WebSocket client implemented.
- **Mock Mode**: Currently hardcoded to simulate "Alice" talking to "Bob".

## Future Work

- User Registration Screen
- QR Code Scanning (Zxing) for Contact Exchange
- Group Chat support (Symmetric Keys)
