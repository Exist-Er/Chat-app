# Chat Application – Architecture, Storage, and Security Design

> **Status:** Draft (living document)
>
> This document captures the design decisions agreed so far. Sections marked **OPEN** indicate areas where further discussion and decisions are required. The document will evolve as those decisions are finalized.

---

## 1. High-Level Goals

- Offline-first chat experience
- Strong privacy guarantees (client-side encryption)
- Minimal server-side data retention
- Clear separation of responsibilities between client, backend, and cloud services
- Modular, evolvable architecture

---

## 2. System Components

### 2.1 Client (Android Application)

**Responsibilities**

- User interface and interaction
- Message creation and display
- Local persistence (SQLite)
- Encryption and decryption of all message content
- Key management (user keys and group keys)
- Google Drive backup and restore

**Local Storage**

- SQLite is the primary source of truth for chat history
- Messages are written locally before any network operation
- Message state tracking (pending, sent, delivered, etc.).

---

### 2.2 Backend Server

**Role**

- Message relay and coordination layer
- Authentication and authorization
- Group membership management
- Temporary storage of undelivered messages

**Explicit Non-Responsibilities**

- No long-term chat history storage
- No access to plaintext messages
- No storage or generation of encryption keys

The backend operates as a **zero-knowledge relay**.

---

### 2.3 Cloud Database

**Purpose**

- Store messages that could not yet be delivered to recipients

**Characteristics**

- Stores ciphertext only
- Messages are deleted after successful delivery acknowledgement
- Optional TTL-based cleanup for messages never acknowledged

---

### 2.4 Google Drive

**Purpose**

- Encrypted backup and restore of user chat history

**Key Properties**

- Used only from the client
- Stores encrypted data only
- Backend never accesses Google Drive
- Backup is not part of live message routing

---

## 3. Message Lifecycle

> All message delivery and coordination is implemented using a **per-user event queue** on the backend. Messages, key updates, and membership changes are represented as events and are removed only after explicit per-event acknowledgements (ACKs).

### 3.1 Sending a Message

1. User composes message
2. Message is stored locally in SQLite with state = `PENDING`
3. Message is encrypted on the client
4. Encrypted message is sent to backend as an event
5. Backend enqueues event for the recipient
6. Backend delivers event when recipient is available

---

### 3.2 Receiving a Message

1. Backend delivers encrypted message to receiver
2. Receiver decrypts message locally
3. Receiver persists message to SQLite
4. Receiver sends delivery acknowledgement to backend
5. Backend deletes message after ACK

---

## 4. Delivery Guarantees and Failure Handling

### 4.1 Receiver Crashes Before ACK

**Approach: At-least-once delivery**

- Backend retains message until ACK is received
- Message may be resent on reconnect
- Receiver must deduplicate messages using `message_id`

---

### 4.2 Sender Goes Offline Mid-Send

**Approach: Client-side send queue**

- Messages are persisted locally before sending
- Backend acknowledges receipt separately from delivery
- Client retries sending until backend ACK is received

---

## 5. Encryption Model

> The system uses **client-side encryption with unencrypted metadata and encrypted payloads**. Metadata required for routing, ordering, and protocol state is visible to the backend, while all sensitive user content remains encrypted.

### 5.1 Encryption Boundary

- All encryption and decryption happens on the client
- Backend only handles ciphertext
- Google Drive stores encrypted backups only

---

### 5.2 Key Ownership

- Keys are owned by users and group members
- Backend never has access to encryption keys

---

### 5.3 Key Types

#### User Key

- One long-term symmetric key per user
- Used for one-to-one messaging
- Stored securely using Android Keystore

#### Group Keys

- One symmetric key per group
- Used for group messaging
- Stored encrypted on the client
- Distributed to members encrypted with their user keys

---

## 6. One-to-One Messaging

- Sender encrypts message using receiver’s user key
- Backend routes encrypted message
- Receiver decrypts using own user key

---

## 7. Group Messaging

- Each group has a shared symmetric group key
- Messages are encrypted once per group
- Backend routes ciphertext to all group members

### Group Key Distribution

- Group key is encrypted with each member’s user key
- Encrypted group key is delivered via backend
- Backend never sees plaintext group keys

### Group Key Rotation

- Group keys are rotated **automatically** on any membership change (member added or removed).
- Message delivery for the group is paused during rotation until new keys are acknowledged.
- New group keys are distributed encrypted with each member’s user key.
- If a member does not acknowledge the new group key within a timeout window, rotation proceeds without that member (consistency over availability).

---

## 8. Backup and Restore (Google Drive)

### Backup

- Backups are **manual-only** in the initial design.
- Backup is explicitly triggered by the user.
- Client pauses message sync
- SQLite database is snapshotted
- Snapshot is encrypted
- Encrypted backup is uploaded to Google Drive

### Restore

- Restore uses **overwrite semantics**.
- Existing local SQLite data is deleted before restore.
- Encrypted backup is downloaded
- Decrypted locally using user-owned keys
- Local database is replaced with backup snapshot
- After restore, client reconnects to backend and replays all unacknowledged events

**Guarantees**

- Undelivered messages are recovered via backend event queues
- Delivered and acknowledged messages after the backup timestamp may be lost locally
- Restore is a destructive, user-confirmed operation



---

## 9. Multi-Device Considerations

**Current Assumption**

- Single active device per user

**OPEN:**

- Support for multiple devices per user
- Message delivery semantics across devices
- Key synchronization strategy

---

## 10. Backend Data Retention

- Backend maintains **per-user event queues** for undelivered or unacknowledged events.
- Events are removed immediately after per-event ACK.
- A **fixed TTL of 14 days** is applied to unacknowledged events.

### TTL Expiry Behavior

- After 14 days without ACK, events are permanently deleted from the backend.
- Backend does not retain any further copy of expired events.

### Client Experience After TTL Expiry

- If a user comes online after TTL expiry:
  - Undelivered messages are no longer recoverable from the backend.
  - The client may request chat re-synchronization from other participants (1-to-1 or group).
  - Chats display a **"last synced" timestamp** to clearly communicate state gaps to the user.

This design enforces minimal server-side retention while preserving user clarity and privacy.

---

## 11. Non-Goals (For Now)

- Forward secrecy with ratcheting
- Large-scale group optimization
- Server-side search
- Message analytics n---

## 11A. Temporary Peer-to-Peer Chat Mode

### Overview

Temporary chat mode provides an **ephemeral, consent-based peer-to-peer session** intended for live, transient conversations. Temporary chats are designed to leave **no persistent history** on either client or the backend.

### Activation and Consent

- Temporary mode is enabled via a **toggle** within an existing one-to-one chat.
- Activation requires **mutual, explicit consent** from both parties.
- Until both users accept, no temporary messages may be sent.

### Session Model

- A temporary chat creates a **temporary session** identified by a `session_id`.
- Messages sent in this mode are associated with the active session.
- Temporary sessions are **online-only** and exist only while both users are connected.

### Message Handling

- Temporary messages are encrypted normally on the client.
- Messages are delivered via the standard per-user event queue.
- Messages are **never written to persistent storage** (SQLite) and exist only in memory or volatile storage.
- Temporary messages are excluded from backups and long-term history.

### Session Termination

A temporary session is terminated when:

- Either user closes the chat
- Either user disconnects
- The application crashes or loses connectivity

On termination:

- A session-closure event is delivered to both clients
- All temporary messages associated with the session are deleted immediately on both sides
- The backend discards all session metadata

Temporary sessions **cannot be resumed** and must be re-established with fresh consent.

---

## 11B. Historical (Time-Travel) Chat View

### Overview

The historical chat view allows users to inspect the state of a chat as it existed at a selected point in time, without modifying stored data.

### Behavior

- The user selects a past date or timestamp.
- The chat enters **HISTORICAL mode**.
- Only messages with timestamps **less than or equal to** the selected cutoff are displayed.
- Messages sent after the cutoff are hidden but not deleted.

### Constraints

- The historical view is **read-only**.
- Sending messages is disabled while in historical mode.
- A clear UI indicator shows the selected historical cutoff.
- Users may return to live mode at any time.

This feature is implemented purely as a **local query and presentation-layer filter** and does not affect encryption, backups, or backend state.

---

## 11C. AI-Assisted Group Chat Summaries (Gemini)

### Overview

AI-assisted summaries provide an optional, user-triggered way to generate concise summaries of recent group chat activity using a third-party AI service (Gemini). This feature is designed to preserve the system’s client-side encryption and zero-knowledge backend guarantees.

### Enablement and Governance

- AI-assisted summaries are **disabled by default** for all groups.
- A group **administrator** may enable or disable this feature at any time.
- Once enabled, **any group member** may trigger a summary.

### Triggering a Summary

- Summaries are generated **only on explicit user action**.
- The user selects the scope of messages to summarize, such as:
  - Unread messages
  - Messages from the last *N* days
  - A custom message range

### Client-Side Summarization Model

- Message selection, decryption, and prompt construction occur **entirely on the client**.
- Selected message plaintext is sent directly from the client to the Gemini API.
- The backend never receives plaintext message content and does not participate in AI processing.
- The Gemini API key is held and managed **on the client**, associated with the user’s Google account.

Optional context, such as the group description, may be included in the prompt if visible to the user.

### Summary Representation

- The generated summary is posted to the group as a **normal group message** with a distinct type (e.g., `AI_SUMMARY`).
- Summary messages are encrypted using the group key and routed like any other group message.
- The message is visually separated from regular chat messages and clearly attributed, indicating:
  - That the content is AI-generated
  - Which user triggered the summary
  - The AI model used

### Privacy and Transparency

- Summaries make explicit that selected chat content was shared with a third-party AI service.
- No automatic, scheduled, or background summaries are performed.
- The scope of content sent to the AI is always user-selected and visible.

This design ensures AI-assisted summaries remain transparent, consensual, and compatible with the system’s security and privacy guarantees.

---

## 12. Protocol Specification (v1)

This section defines the **wire-level protocol and event model** used between clients and the backend. It is normative for version 1 and is designed to be minimal, explicit, and evolvable.

### 12.1 Core Principles

- All client-backend communication is expressed as **events**.
- Events are delivered via **per-user FIFO queues**.
- Each event must be **explicitly acknowledged**.
- Metadata is plaintext; payloads are encrypted.
- Backend does not interpret encrypted payloads.

---

### 12.2 Event Envelope

All events share a common envelope:

```
Event {
  event_id: UUID
  recipient_id: UserID
  sender_id: UserID | null
  event_type: EventType
  sequence: int64
  timestamp: int64
  metadata: Map<String, Any>
  encrypted_payload: bytes
}
```

**Notes**

- `sequence` is monotonically increasing per recipient queue.
- `sender_id` may be null for system events.
- `metadata` MUST NOT contain sensitive content.

---

### 12.3 Acknowledgements (ACK)

ACKs are sent **per event**.

```
ACK {
  event_id: UUID
  recipient_id: UserID
}
```

Rules:

- ACK is sent only **after the client has safely processed the event**.
- Backend deletes the event only after ACK.
- ACKs are idempotent.

---

### 12.4 Event Types

#### MESSAGE

Used for one-to-one and group messages.

Metadata:

```
chat_id
group_id (optional)
message_mode: NORMAL | TEMPORARY
```

Encrypted payload:

```
message_id
ciphertext
```

---

#### GROUP\_KEY\_UPDATE

Used during automatic group key rotation.

Metadata:

```
group_id
key_version
```

Encrypted payload:

```
encrypted_group_key
```

---

#### TEMP\_SESSION\_START / TEMP\_SESSION\_END

Used to manage temporary peer-to-peer chat sessions.

Metadata:

```
session_id
peer_id
```

Encrypted payload: (empty)

---

#### AI\_SUMMARY

Represents an AI-generated group summary.

Metadata:

```
group_id
triggered_by
model
scope
```

Encrypted payload:

```
summary_text
```

---

### 12.5 Ordering and Deduplication

- Clients must process events strictly in `sequence` order.
- Clients must deduplicate events using `event_id`.
- MESSAGE events for a group must not be processed before the latest GROUP\_KEY\_UPDATE.

---

### 12.6 Error Handling

- If an event cannot be processed (e.g., missing key), the client must **not ACK** it.
- Backend will retry delivery until ACK or TTL expiry.

---

### 12.7 Protocol Evolution

- New event types may be added without breaking existing clients.
- Clients must ignore unknown event types gracefully.

---

## 13. Next Decisions to Make

All core architectural decisions for version 1 are finalized.

**OPEN (Future Versions):**

- Multi-device support
- Forward secrecy and ratcheting
- Advanced restore/merge strategies

---

## 13. Threat Model

### 13.1 Assets Protected

**High-value assets**

- Message contents
- User keys and group keys
- Chat history (local database and backups)

**Medium-value assets**

- Message ordering and delivery state
- Group membership state

**Low-value but unavoidable assets**

- Metadata (sender/receiver identifiers, timestamps, event types)

---

### 13.2 Adversary Model

The system considers the following adversaries:

1. **Network attacker**

   - Can observe, replay, or inject network traffic
   - Mitigated by TLS and encrypted payloads

2. **Honest-but-curious backend operator**

   - Has full access to backend storage and logs
   - Cannot read message content or keys due to client-side encryption

3. **Cloud storage provider**

   - Can access stored database records or backup files
   - Only encrypted data is visible

4. **Malicious chat participant**

   - May send malformed or replayed messages
   - Mitigated by event IDs, ordering, and backend-enforced membership

5. **Compromised user device**

   - Full compromise of a device exposes that user’s data
   - This scenario is explicitly out of scope

---

### 13.3 Security Guarantees

- Message content confidentiality against backend and cloud providers
- Integrity and ordering via per-user event queues
- Removal of access for removed group members via automatic key rotation
- Bounded server-side data exposure via 14-day TTL

---

### 13.4 Accepted Trade-offs

- Metadata is visible to the backend for routing and ordering
- No forward secrecy in version 1
- Messages may be lost if users remain offline beyond TTL expiry
- No protection against compromised end-user devices

---

### 13.5 Explicit Non-Goals

- Traffic analysis resistance
- Screenshot or screen-capture prevention
- Protection against social engineering
- Legal coercion of end users

