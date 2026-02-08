from fastapi import WebSocket, WebSocketDisconnect
from typing import Dict, Set
from models import Event, ACK
from event_queue import EventQueueService
from user_service import UserService
import json
import logging
import asyncio

logger = logging.getLogger(__name__)


class ConnectionManager:
    """
    WebSocket connection manager.
    Maintains active connections and handles real-time event delivery.
    """
    
    def __init__(self):
        # Map of user_id -> set of WebSocket connections
        self.active_connections: Dict[str, Set[WebSocket]] = {}
        # Lock for thread-safe operations
        self._lock = asyncio.Lock()
    
    async def connect(self, websocket: WebSocket, user_id: str):
        """
        Accept a new WebSocket connection.
        
        Args:
            websocket: WebSocket connection
            user_id: User ID
        """
        await websocket.accept()
        
        async with self._lock:
            if user_id not in self.active_connections:
                self.active_connections[user_id] = set()
            self.active_connections[user_id].add(websocket)
        
        # Update last seen
        await UserService.update_last_seen(user_id)
        
        logger.info(f"User {user_id} connected (total connections: {len(self.active_connections[user_id])})")
        
        # Send pending events
        await self._send_pending_events(user_id, websocket)
    
    async def disconnect(self, websocket: WebSocket, user_id: str):
        """
        Remove a WebSocket connection.
        
        Args:
            websocket: WebSocket connection
            user_id: User ID
        """
        async with self._lock:
            if user_id in self.active_connections:
                self.active_connections[user_id].discard(websocket)
                
                # Remove user entry if no more connections
                if not self.active_connections[user_id]:
                    del self.active_connections[user_id]
        
        logger.info(f"User {user_id} disconnected")
    
    async def send_event(self, event: Event) -> bool:
        """
        Send an event to a user's active connections.
        
        Args:
            event: Event to send
            
        Returns:
            True if sent to at least one connection, False if user offline
        """
        user_id = event.recipient_id
        
        async with self._lock:
            connections = self.active_connections.get(user_id, set()).copy()
        
        if not connections:
            logger.debug(f"User {user_id} is offline, event queued")
            return False
        
        # Send to all active connections
        event_json = event.model_dump_json()
        disconnected = []
        
        for websocket in connections:
            try:
                await websocket.send_text(event_json)
                logger.debug(f"Sent event {event.event_id} to user {user_id}")
            except Exception as e:
                logger.warning(f"Failed to send to connection: {e}")
                disconnected.append(websocket)
        
        # Clean up disconnected sockets
        if disconnected:
            async with self._lock:
                if user_id in self.active_connections:
                    for ws in disconnected:
                        self.active_connections[user_id].discard(ws)
        
        return len(connections) > len(disconnected)
    
    async def _send_pending_events(self, user_id: str, websocket: WebSocket):
        """
        Send all pending events to a newly connected user.
        
        Args:
            user_id: User ID
            websocket: WebSocket connection
        """
        try:
            pending_events = await EventQueueService.get_pending_events(user_id)
            
            if pending_events:
                logger.info(f"Sending {len(pending_events)} pending events to user {user_id}")
                
                for event in pending_events:
                    try:
                        await websocket.send_text(event.model_dump_json())
                    except Exception as e:
                        logger.error(f"Failed to send pending event: {e}")
                        break
        
        except Exception as e:
            logger.error(f"Failed to send pending events: {e}")
    
    async def handle_ack(self, ack: ACK):
        """
        Handle event acknowledgement.
        
        Args:
            ack: Acknowledgement
        """
        success = await EventQueueService.acknowledge_event(ack)
        if success:
            logger.debug(f"Event {ack.event_id} acknowledged by {ack.recipient_id}")
        else:
            logger.warning(f"Failed to acknowledge event {ack.event_id}")
    
    def is_online(self, user_id: str) -> bool:
        """Check if user has any active connections"""
        return user_id in self.active_connections and len(self.active_connections[user_id]) > 0
    
    async def get_online_users(self) -> Set[str]:
        """Get set of all online user IDs"""
        async with self._lock:
            return set(self.active_connections.keys())


# Global connection manager instance
connection_manager = ConnectionManager()
