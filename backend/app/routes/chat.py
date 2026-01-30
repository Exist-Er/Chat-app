from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Depends, Query, HTTPException
from app.core.websocket import manager
from app.database import get_database
from datetime import datetime, time
import json
from bson import ObjectId
from typing import List, Optional

router = APIRouter()

@router.websocket("/ws/{user_id}")
async def websocket_endpoint(websocket: WebSocket, user_id: str, db = Depends(get_database)):
    await manager.connect(websocket, user_id)
    try:
        while True:
            data = await websocket.receive_text()
            message_data = json.loads(data)
            
            # Common message fields
            message_doc = {
                "sender_id": user_id,
                "content": message_data["content"],
                "timestamp": datetime.utcnow(),
                "is_temporary": message_data.get("is_temporary", False)
            }
            
            if message_data.get("is_temporary"):
                # Default 24h for temp messages or custom
                expires_in = message_data.get("expires_in_hours", 24)
                message_doc["expires_at"] = datetime.utcnow() # Fixed by TTL index later
            
            # Route: Personal or Group
            if "group_id" in message_data:
                message_doc["group_id"] = message_data["group_id"]
                await db.messages.insert_one(message_doc)
                
                # Fetch group members
                group = await db.groups.find_one({"_id": ObjectId(message_data["group_id"])})
                if group:
                    await manager.broadcast_to_group(
                        {
                            "sender_id": user_id,
                            "group_id": message_data["group_id"],
                            "content": message_data["content"],
                            "timestamp": message_doc["timestamp"].isoformat(),
                            "is_temporary": message_doc["is_temporary"]
                        },
                        group["members"]
                    )
            else:
                message_doc["receiver_id"] = message_data["receiver_id"]
                await db.messages.insert_one(message_doc)
                
                await manager.send_personal_message(
                    {
                        "sender_id": user_id,
                        "content": message_data["content"],
                        "timestamp": message_doc["timestamp"].isoformat(),
                        "is_temporary": message_doc["is_temporary"]
                    },
                    message_data["receiver_id"]
                )
            
    except WebSocketDisconnect:
        manager.disconnect(websocket, user_id)
    except Exception as e:
        print(f"WebSocket error: {e}")
        manager.disconnect(websocket, user_id)

@router.get("/history")
async def get_chat_history(
    user_id: str, 
    other_id: Optional[str] = None, 
    group_id: Optional[str] = None,
    search_date: Optional[str] = Query(None, description="Format: YYYY-MM-DD"),
    db = Depends(get_database)
):
    query = {}
    if group_id:
        query["group_id"] = group_id
    elif other_id:
        query["$or"] = [
            {"sender_id": user_id, "receiver_id": other_id},
            {"sender_id": other_id, "receiver_id": user_id}
        ]
    else:
        raise HTTPException(status_code=400, detail="Must provide other_id or group_id")

    if search_date:
        try:
            start_dt = datetime.combine(datetime.strptime(search_date, "%Y-%m-%d"), time.min)
            end_dt = datetime.combine(start_dt.date(), time.max)
            query["timestamp"] = {"$gte": start_dt, "$lte": end_dt}
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid date format. Use YYYY-MM-DD")

    cursor = db.messages.find(query).sort("timestamp", 1)
    messages = []
    async for doc in cursor:
        doc["_id"] = str(doc["_id"])
        doc["timestamp"] = doc["timestamp"].isoformat()
        if "expires_at" in doc:
            doc["expires_at"] = doc["expires_at"].isoformat()
        messages.append(doc)
    
    return messages

@router.post("/groups")
async def create_group(name: str, admin_id: str, members: List[str], db = Depends(get_database)):
    if admin_id not in members:
        members.append(admin_id)
    
    group_doc = {
        "name": name,
        "admin_id": admin_id,
        "members": members,
        "created_at": datetime.utcnow()
    }
    result = await db.groups.insert_one(group_doc)
    return {"id": str(result.inserted_id), "message": "Group created"}
