from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException, Header, Depends
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from typing import Optional, List
import logging
import json
from uuid import UUID

from config import settings
from database import Database
from models import (
    Event, ACK, User, Group, UserRegistration, ContactExchange,
    EventType, MessageEvent, GroupKeyUpdateEvent
)
from auth import AuthService
from user_service import UserService
from group_service import GroupService
from event_queue import EventQueueService
from websocket_manager import connection_manager

# Configure logging
logging.basicConfig(
    level=logging.INFO if settings.debug else logging.WARNING,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager"""
    # Startup
    logger.info("Starting Chat Application Backend...")
    await Database.connect()
    logger.info(f"Server running on {settings.host}:{settings.port}")
    
    yield
    
    # Shutdown
    logger.info("Shutting down...")
    await Database.disconnect()


app = FastAPI(
    title="Encrypted Chat Application API",
    description="Zero-knowledge encrypted chat backend with WebSocket support",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ============================================================================
# Authentication Dependency
# ============================================================================

async def verify_token(authorization: Optional[str] = Header(None)) -> str:
    """
    Verify Google ID token from Authorization header.
    
    Returns:
        user_id of authenticated user
    """
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid authorization header")
    
    token = authorization.replace("Bearer ", "")
    user_info = await AuthService.verify_google_token(token)
    
    if not user_info:
        raise HTTPException(status_code=401, detail="Invalid token")
    
    user_id = AuthService.generate_user_id(user_info["google_id"])
    return user_id


# ============================================================================
# HTTP Routes
# ============================================================================

@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "status": "online",
        "service": "Encrypted Chat Backend",
        "version": "1.0.0"
    }


@app.post("/api/register", response_model=User)
async def register(registration: UserRegistration):
    """Register a new user with Google Sign-In"""
    user = await UserService.register_user(registration)
    
    if not user:
        raise HTTPException(status_code=400, detail="Registration failed")
    
    return user


@app.get("/api/users/me", response_model=User)
async def get_current_user(user_id: str = Depends(verify_token)):
    """Get current authenticated user"""
    user = await UserService.get_user_by_id(user_id)
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    return user


@app.get("/api/users/{user_id}/public-key")
async def get_public_key(user_id: str, current_user: str = Depends(verify_token)):
    """Get a user's public key"""
    public_key = await UserService.get_public_key(user_id)
    
    if not public_key:
        raise HTTPException(status_code=404, detail="Public key not found")
    
    return {"user_id": user_id, "public_key": public_key}


@app.post("/api/users/public-keys")
async def get_public_keys(user_ids: List[str], current_user: str = Depends(verify_token)):
    """Get public keys for multiple users"""
    keys = await UserService.get_public_keys(user_ids)
    return {"public_keys": keys}


@app.get("/api/users/search")
async def search_users(q: str, current_user: str = Depends(verify_token)):
    """Search users by email or display name"""
    users = await UserService.search_users(q)
    return {"users": users}


@app.post("/api/groups", response_model=Group)
async def create_group(
    group_id: str,
    name: str,
    member_ids: List[str],
    current_user: str = Depends(verify_token)
):
    """Create a new group"""
    group = await GroupService.create_group(group_id, name, current_user, member_ids)
    
    if not group:
        raise HTTPException(status_code=400, detail="Failed to create group")
    
    return group


@app.get("/api/groups/{group_id}", response_model=Group)
async def get_group(group_id: str, current_user: str = Depends(verify_token)):
    """Get group details"""
    # Verify membership
    is_member = await GroupService.is_member(group_id, current_user)
    if not is_member:
        raise HTTPException(status_code=403, detail="Not a member of this group")
    
    group = await GroupService.get_group(group_id)
    if not group:
        raise HTTPException(status_code=404, detail="Group not found")
    
    return group


@app.get("/api/groups", response_model=List[Group])
async def get_user_groups(current_user: str = Depends(verify_token)):
    """Get all groups for current user"""
    groups = await GroupService.get_user_groups(current_user)
    return groups


@app.post("/api/groups/{group_id}/members")
async def add_group_member(
    group_id: str,
    user_id: str,
    current_user: str = Depends(verify_token)
):
    """Add a member to a group"""
    # Verify current user is a member
    is_member = await GroupService.is_member(group_id, current_user)
    if not is_member:
        raise HTTPException(status_code=403, detail="Not a member of this group")
    
    success = await GroupService.add_member(group_id, user_id)
    if not success:
        raise HTTPException(status_code=400, detail="Failed to add member")
    
    # Increment key version (triggers key rotation on client side)
    new_version = await GroupService.increment_key_version(group_id)
    
    return {"success": True, "new_key_version": new_version}


@app.delete("/api/groups/{group_id}/members/{user_id}")
async def remove_group_member(
    group_id: str,
    user_id: str,
    current_user: str = Depends(verify_token)
):
    """Remove a member from a group"""
    # Verify current user is a member
    is_member = await GroupService.is_member(group_id, current_user)
    if not is_member:
        raise HTTPException(status_code=403, detail="Not a member of this group")
    
    success = await GroupService.remove_member(group_id, user_id)
    if not success:
        raise HTTPException(status_code=400, detail="Failed to remove member")
    
    # Increment key version (triggers key rotation)
    new_version = await GroupService.increment_key_version(group_id)
    
    return {"success": True, "new_key_version": new_version}


@app.post("/api/events")
async def send_event(event: Event, current_user: str = Depends(verify_token)):
    """
    Send an event (message, key update, etc.) to a recipient.
    The event is enqueued and delivered via WebSocket if recipient is online.
    """
    # Verify sender
    if event.sender_id != current_user:
        raise HTTPException(status_code=403, detail="Sender ID mismatch")
    
    # Enqueue event
    success = await EventQueueService.enqueue_event(event)
    if not success:
        raise HTTPException(status_code=500, detail="Failed to enqueue event")
    
    # Try to deliver immediately if recipient is online
    delivered = await connection_manager.send_event(event)
    
    return {
        "success": True,
        "event_id": str(event.event_id),
        "delivered": delivered,
        "queued": not delivered
    }


# ============================================================================
# WebSocket Endpoint
# ============================================================================

@app.websocket("/ws/{user_id}")
async def websocket_endpoint(websocket: WebSocket, user_id: str):
    """
    WebSocket endpoint for real-time event delivery.
    
    Protocol:
    - Client connects with user_id
    - Server sends all pending events
    - Client sends ACKs for received events
    - Server delivers new events in real-time
    """
    await connection_manager.connect(websocket, user_id)
    
    try:
        while True:
            # Receive messages from client (ACKs)
            data = await websocket.receive_text()
            
            try:
                message = json.loads(data)
                
                # Handle ACK
                if message.get("type") == "ACK":
                    ack = ACK(
                        event_id=UUID(message["event_id"]),
                        recipient_id=user_id
                    )
                    await connection_manager.handle_ack(ack)
                
                # Handle ping/pong for keepalive
                elif message.get("type") == "ping":
                    await websocket.send_text(json.dumps({"type": "pong"}))
                
            except json.JSONDecodeError:
                logger.warning(f"Invalid JSON from user {user_id}")
            except Exception as e:
                logger.error(f"Error processing message: {e}")
    
    except WebSocketDisconnect:
        await connection_manager.disconnect(websocket, user_id)
    except Exception as e:
        logger.error(f"WebSocket error for user {user_id}: {e}")
        await connection_manager.disconnect(websocket, user_id)


# ============================================================================
# Admin/Maintenance Endpoints
# ============================================================================

@app.post("/api/admin/cleanup-expired-events")
async def cleanup_expired_events():
    """
    Cleanup events that have exceeded TTL.
    Should be called periodically (e.g., daily cron job).
    """
    count = await EventQueueService.cleanup_expired_events()
    return {"deleted_count": count}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.debug
    )
