from datetime import datetime
from pydantic import BaseModel, Field
from typing import Optional

class MessageBase(BaseModel):
    content: str
    sender_id: str
    receiver_id: Optional[str] = None
    group_id: Optional[str] = None
    is_temporary: bool = False
    expires_at: Optional[datetime] = None

class MessageCreate(MessageBase):
    pass

class MessageResponse(MessageBase):
    id: str = Field(alias="_id")
    timestamp: datetime = Field(default_factory=datetime.utcnow)

    class Config:
        populate_by_name = True
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }
