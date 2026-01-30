# Android Chat App - Project Documentation

This project follows a **Package by Feature** architecture, which organizes the codebase into functional modules. This approach improves maintainability, scalability, and makes it easier to locate related code.

## Architecture Overview

The app is divided into `features` (business logic and UI) and `common` (shared infrastructure).

### Features

| Feature | Responsibility | Key Files |
| :--- | :--- | :--- |
| **Auth** | Handles user authentication, including login, registration, and session management. | `AuthViewModel.kt` |
| **Chat** | Manages real-time messaging, message lists, and conversation history. | `ChatViewModel.kt` |
| **Profile** | Manages user profile information, settings, and avatars. | `ProfileViewModel.kt` |

### Common & Infrastructure

| Module | Responsibility | Key Files |
| :--- | :--- | :--- |
| **Network** | Handles API requests and socket connections for real-time updates. | `NetworkClient.kt` |
| **Database** | Manages local data persistence (e.g., caching messages offline). | (TBD) |
| **DI** | Configuration for Dependency Injection (e.g., Hilt or Koin). | (TBD) |

## File Responsibilities

### [AuthViewModel.kt](file:///home/exister/Desktop/Personal-files/projects/Project%205/app/src/main/java/com/example/chatapp/features/auth/AuthViewModel.kt)
- Manages the state of the Authentication screen.
- Communicates with the authentication repository to log users in or sign them up.
- Handles validation of user input (email, password).

### [ChatViewModel.kt](file:///home/exister/Desktop/Personal-files/projects/Project%205/app/src/main/java/com/example/chatapp/features/chat/ChatViewModel.kt)
- Orchestrates the fetching and sending of messages.
- Maintains the list of messages to be displayed in the UI.
- Observes real-time data streams from the database or network.

### [ProfileViewModel.kt](file:///home/exister/Desktop/Personal-files/projects/Project%205/app/src/main/java/com/example/chatapp/features/profile/ProfileViewModel.kt)
- Handles user-specific settings and profile updates.
- Manages loading and caching of user profile data.

### [NetworkClient.kt](file:///home/exister/Desktop/Personal-files/projects/Project%205/app/src/main/java/com/example/chatapp/common/network/NetworkClient.kt)
- Centralized configuration for network requests (e.g., using Retrofit, Ktor, or OkHttp).
- Defines base URLs, headers, and interceptors for authentication.

---

## How to Run the Project

Since this is a standard Android project setup, follow these steps to run it:

1.  **Open Android Studio**: Select **Open** and navigate to the `Project 5` directory.
2.  **Sync Gradle**: Allow Android Studio to sync the Gradle files (this may take a few minutes as it downloads dependencies).
3.  **Run on Emulator/Device**:
    - Click the **Run** button (green play icon) in the toolbar.
    - Select a virtual device (Emulator) or a connected physical Android device.
4.  **Verification**: You should see a screen displaying **"Hello Chat App!"**.

---

## Next Steps
- **Backend Setup**: I will provide a `backend/` directory with a FastAPI template.
- **Login UI**: We will implement the first screen in Kotlin using Material Design.
- **Real-Time messaging**: Setting up WebSockets for live chat functionality.
