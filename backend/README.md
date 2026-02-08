# Chat Application Backend

Zero-knowledge encrypted chat backend with WebSocket support.

## Features

- **Google Sign-In Authentication**
- **Per-user event queues** with at-least-once delivery
- **WebSocket real-time messaging**
- **Public key distribution** (zero-knowledge backend)
- **Group management** with automatic key rotation
- **14-day TTL** for unacknowledged events
- **MongoDB Atlas** for scalable storage

## Setup

### 1. Create Virtual Environment

```bash
python3 -m venv .venv
source .venv/bin/activate  # On Linux/Mac
# or
.venv\Scripts\activate  # On Windows
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 3. Configure Environment

Copy `.env.example` to `.env` and configure:

```bash
cp .env.example .env
```

Edit `.env`:

```env
# MongoDB Atlas connection string
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/?retryWrites=true&w=majority
MONGODB_DB_NAME=chatapp

# Google OAuth Client ID (from Google Cloud Console)
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com

# Server configuration
HOST=0.0.0.0
PORT=8080
DEBUG=true
```

### 4. Get Google OAuth Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable **Google Sign-In API**
4. Create OAuth 2.0 credentials
5. Add authorized origins and redirect URIs
6. Copy the **Client ID** to `.env`

### 5. Setup MongoDB Atlas

1. Create account at [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
2. Create a free cluster
3. Create database user
4. Whitelist your IP (or use 0.0.0.0/0 for development)
5. Get connection string and add to `.env`

## Running the Server

```bash
# Activate virtual environment
source .venv/bin/activate

# Run with uvicorn
python main.py

# Or use uvicorn directly with auto-reload
uvicorn main:app --host 0.0.0.0 --port 8080 --reload
```

Server will start on `http://localhost:8080`

## API Documentation

Once running, visit:
- **Swagger UI**: http://localhost:8080/docs
- **ReDoc**: http://localhost:8080/redoc

## API Endpoints

### Authentication
- `POST /api/register` - Register new user with Google Sign-In
- `GET /api/users/me` - Get current user info

### Users
- `GET /api/users/{user_id}/public-key` - Get user's public key
- `POST /api/users/public-keys` - Get multiple public keys
- `GET /api/users/search?q=query` - Search users

### Groups
- `POST /api/groups` - Create group
- `GET /api/groups` - Get user's groups
- `GET /api/groups/{group_id}` - Get group details
- `POST /api/groups/{group_id}/members` - Add member
- `DELETE /api/groups/{group_id}/members/{user_id}` - Remove member

### Events
- `POST /api/events` - Send event (message, key update, etc.)

### WebSocket
- `WS /ws/{user_id}` - Real-time event delivery

## WebSocket Protocol

### Connection
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/user_123');
```

### Receiving Events
Server sends events as JSON:
```json
{
  "event_id": "uuid",
  "recipient_id": "user_123",
  "sender_id": "user_456",
  "event_type": "MESSAGE",
  "sequence": 42,
  "timestamp": 1707401640000,
  "metadata": {"chat_id": "chat_789"},
  "encrypted_payload": "base64..."
}
```

### Sending ACKs
Client acknowledges events:
```json
{
  "type": "ACK",
  "event_id": "uuid",
  "recipient_id": "user_123"
}
```

### Keepalive
```json
// Client -> Server
{"type": "ping"}

// Server -> Client
{"type": "pong"}
```

## Testing

```bash
# Install test dependencies (already in requirements.txt)
pip install pytest pytest-asyncio httpx

# Run tests
pytest
```

## Architecture

### Event Queue System
- Each user has a FIFO event queue
- Events are delivered in sequence order
- Events persist until explicitly acknowledged
- 14-day TTL for unacknowledged events

### Zero-Knowledge Design
- Backend only stores public keys (not secret)
- All message content is encrypted client-side
- Backend only relays ciphertext
- No access to encryption keys

### Delivery Guarantees
- **At-least-once delivery**: Events may be redelivered
- Clients must deduplicate using `event_id`
- Events are deleted only after ACK

## Maintenance

### Cleanup Expired Events
```bash
curl -X POST http://localhost:8080/api/admin/cleanup-expired-events
```

Run this daily via cron job.

## Security Notes

1. **Always use HTTPS in production**
2. **Change SECRET_KEY in production**
3. **Restrict CORS origins in production**
4. **Use environment variables, never commit .env**
5. **Rotate Google OAuth credentials periodically**

## License

MIT
