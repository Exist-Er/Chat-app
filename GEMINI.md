# Gemini Context: Manga Encrypted Chat App

This file provides instructional context for Gemini when interacting with this project. It outlines the architecture, technology stack, and development workflows.

## Project Overview

The **Manga Encrypted Chat App** is an offline-first, end-to-end encrypted messaging platform with a distinctive black-and-white Manga/Anime aesthetic. It consists of an Android client and a zero-knowledge Python backend.

### Key Features
- **Zero-Knowledge Backend**: The server only relays encrypted payloads and never has access to plaintext messages or encryption keys.
- **End-to-End Encryption (E2EE)**: Powered by **Google Tink**. Uses Hybrid Encryption (ECIES) for 1:1 chats and Symmetric Encryption (AES-GCM) for group chats.
- **Offline-First**: Android client uses **Room SQLite** for local persistence, ensuring immediate UI responsiveness.
- **Real-Time Messaging**: Implemented via **WebSockets** for both client-to-server and server-to-client communication.
- **Manga Aesthetic**: High-contrast B&W UI built with **Jetpack Compose**.

### Tech Stack
- **Android**: Kotlin, Jetpack Compose, Room, Retrofit, OkHttp, Google Tink.
- **Backend**: Python, FastAPI, MongoDB (Motor), WebSockets, Pydantic.
- **Architecture**: MVVM + Repository Pattern (Android), Service-based Architecture (Backend).

---

## Building and Running

### 1. Backend Server
The backend requires Python 3.10+ and a MongoDB instance.

**Setup & Start:**
```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate  # or activate.fish/activate.ps1
pip install -r requirements.txt
cp .env.example .env       # Configure MONGODB_URI and GOOGLE_CLIENT_ID
python main.py             # Starts on http://0.0.0.0:8080
```
Alternatively, use the helper script: `./start_server.fish`

**Testing:**
```bash
cd backend
pytest
```

### 2. Android Application
Requires **Android Studio Hedgehog** (2023.1.1) or newer.

**Setup & Start:**
1. Open the project root in Android Studio.
2. Sync Gradle.
3. If running on an emulator, the app is pre-configured to connect to `10.0.2.2:8080`.
4. Build and run the `app` module.

**Testing:**
- **Local Unit Tests**: `./gradlew :app:test`
- **Instrumented Tests**: `./gradlew :app:connectedAndroidTest`

---

## Development Conventions

### General
- **Zero-Knowledge**: Never add logic to the backend that requires decrypting message payloads.
- **Security**: Private keys must never leave the Android device (stored in Android Keystore).
- **Architecture**: Follow MVVM strictly on Android. Use `ChatRepository` as the single source of truth for data operations.

### Android (UI & Data)
- **Theming**: All UI components should use `MangaTheme` and adhere to the B&W comic style.
- **Async**: Use Kotlin Coroutines and Flow for all asynchronous operations.
- **Persistence**: Messages must be saved to Room *before* attempting to send via WebSocket.

### Backend (API & Events)
- **Event-Driven**: All communication is via the `Event` model. The backend acts as a FIFO relay.
- **Acknowledgements**: Messages are only deleted from the backend queue after an explicit `ACK` from the recipient.
- **TTL**: Unacknowledged events expire after 14 days.

---

## Key Files and Directories

| Path | Description |
| :--- | :--- |
| `app/src/main/java/com/chatapp/crypto/` | **CryptoManager**: Core E2EE logic using Tink. |
| `app/src/main/java/com/chatapp/network/` | WebSocket and Retrofit API clients. |
| `app/src/main/java/com/chatapp/ui/` | Jetpack Compose screens and Manga theme definitions. |
| `backend/main.py` | FastAPI entry point and WebSocket route. |
| `backend/event_queue.py` | Logic for per-user event delivery and persistence. |
| `backend/models.py` | Pydantic models for the protocol (Events, ACKs, Users). |
| `chat_app_architecture_security_design.md` | Detailed architectural design document. |

---

## Protocol Overview (v1)

Messages and system updates are wrapped in an **Event Envelope**:
- `event_id`: Unique UUID.
- `recipient_id`: Target user.
- `event_type`: `MESSAGE`, `GROUP_KEY_UPDATE`, etc.
- `encrypted_payload`: The Base64 encoded ciphertext.

Clients must acknowledge every event by sending an `ACK` message back through the WebSocket to ensure reliable delivery.
