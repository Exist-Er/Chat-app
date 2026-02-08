from pydantic import BaseModel, Field
from typing import Optional, Dict, Any, Literal
from datetime import datetime
from uuid import UUID, uuid4


class EventType:
    """Event type constants"""
    MESSAGE = "MESSAGE"
    GROUP_KEY_UPDATE = "GROUP_KEY_UPDATE"
    TEMP_SESSION_START = "TEMP_SESSION_START"
    TEMP_SESSION_END = "TEMP_SESSION_END"
    AI_SUMMARY = "AI_SUMMARY"


class MessageMode:
    """Message mode constants"""
    NORMAL = "NORMAL"
    TEMPORARY = "TEMPORARY"


class Event(BaseModel):
    """
    Core event envelope for all client-backend communication.
    Follows Protocol Specification v1 (Section 12.2)
    """
    event_id: UUID = Field(default_factory=uuid4)
    recipient_id: str
    sender_id: Optional[str] = None
    event_type: str
    sequence: int
    timestamp: int  # Unix timestamp in milliseconds
    metadata: Dict[str, Any] = Field(default_factory=dict)
    encrypted_payload: str  # Base64 encoded encrypted bytes
    
    class Config:
        json_schema_extra = {
            "example": {
                "event_id": "550e8400-e29b-41d4-a716-446655440000",
                "recipient_id": "user123",
                "sender_id": "user456",
                "event_type": "MESSAGE",
                "sequence": 42,
                "timestamp": 1707401640000,
                "metadata": {"chat_id": "chat789", "message_mode": "NORMAL"},
                "encrypted_payload": "base64encodedciphertext=="
            }
        }


class ACK(BaseModel):
    """
    Acknowledgement for event delivery.
    Follows Protocol Specification v1 (Section 12.3)
    """
    event_id: UUID
    recipient_id: str
    
    class Config:
        json_schema_extra = {
            "example": {
                "event_id": "550e8400-e29b-41d4-a716-446655440000",
                "recipient_id": "user123"
            }
        }


class User(BaseModel):
    """User model with public key storage"""
    user_id: str
    email: str
    display_name: str
    public_key: str  # Base64 encoded public key
    created_at: datetime = Field(default_factory=datetime.utcnow)
    last_seen: datetime = Field(default_factory=datetime.utcnow)
    
    class Config:
        json_schema_extra = {
            "example": {
                "user_id": "user123",
                "email": "alice@example.com",
                "display_name": "Alice",
                "public_key": "base64encodedpublickey==",
                "created_at": "2024-02-08T12:00:00Z",
                "last_seen": "2024-02-08T12:00:00Z"
            }
        }


class Group(BaseModel):
    """Group metadata"""
    group_id: str
    name: str
    creator_id: str
    member_ids: list[str]
    key_version: int = 1
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    
    class Config:
        json_schema_extra = {
            "example": {
                "group_id": "group123",
                "name": "Team Chat",
                "creator_id": "user123",
                "member_ids": ["user123", "user456", "user789"],
                "key_version": 1,
                "created_at": "2024-02-08T12:00:00Z",
                "updated_at": "2024-02-08T12:00:00Z"
            }
        }


class MessageEvent(BaseModel):
    """Message event metadata (for MESSAGE type)"""
    chat_id: str
    group_id: Optional[str] = None
    message_mode: Literal["NORMAL", "TEMPORARY"] = "NORMAL"


class GroupKeyUpdateEvent(BaseModel):
    """Group key update event metadata"""
    group_id: str
    key_version: int


class TempSessionEvent(BaseModel):
    """Temporary session event metadata"""
    session_id: str
    peer_id: str


class AISummaryEvent(BaseModel):
    """AI summary event metadata"""
    group_id: str
    triggered_by: str
    model: str
    scope: str


class UserRegistration(BaseModel):
    """User registration request"""
    google_id_token: str
    public_key: str  # Base64 encoded public key
    display_name: Optional[str] = None


class ContactExchange(BaseModel):
    """Contact exchange via QR code or invite link"""
    user_id: str
    public_key: str
    display_name: str
    exchange_token: str  # Temporary token for verification
