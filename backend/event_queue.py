from database import get_database
from models import Event, ACK, EventType
from datetime import datetime, timedelta
from config import settings
from typing import List, Optional
from uuid import UUID
import logging

logger = logging.getLogger(__name__)


class EventQueueService:
    """
    Per-user event queue management.
    Implements at-least-once delivery with explicit ACKs.
    """
    
    @staticmethod
    async def enqueue_event(event: Event) -> bool:
        """
        Add an event to the recipient's queue.
        
        Args:
            event: Event to enqueue
            
        Returns:
            True if successful, False otherwise
        """
        try:
            db = get_database()
            
            # Get next sequence number for this recipient
            last_event = await db.events.find_one(
                {"recipient_id": event.recipient_id},
                sort=[("sequence", -1)]
            )
            
            next_sequence = (last_event["sequence"] + 1) if last_event else 1
            event.sequence = next_sequence
            
            # Set timestamp if not already set
            if event.timestamp == 0:
                event.timestamp = int(datetime.utcnow().timestamp() * 1000)
            
            # Calculate TTL expiry
            ttl_expiry = datetime.utcnow() + timedelta(days=settings.event_ttl_days)
            
            # Insert event
            event_doc = event.model_dump()
            event_doc["event_id"] = str(event.event_id)  # Convert UUID to string
            event_doc["ttl_expiry"] = ttl_expiry
            event_doc["acknowledged"] = False
            
            await db.events.insert_one(event_doc)
            
            logger.info(
                f"Enqueued event {event.event_id} for user {event.recipient_id} "
                f"(type: {event.event_type}, seq: {next_sequence})"
            )
            return True
            
        except Exception as e:
            logger.error(f"Failed to enqueue event: {e}")
            return False
    
    @staticmethod
    async def get_pending_events(user_id: str, limit: int = 100) -> List[Event]:
        """
        Get all pending (unacknowledged) events for a user.
        
        Args:
            user_id: User ID
            limit: Maximum number of events to return
            
        Returns:
            List of pending events in sequence order
        """
        try:
            db = get_database()
            
            cursor = db.events.find(
                {
                    "recipient_id": user_id,
                    "acknowledged": False
                }
            ).sort("sequence", 1).limit(limit)
            
            events = []
            async for doc in cursor:
                # Convert string UUID back to UUID object
                doc["event_id"] = UUID(doc["event_id"])
                events.append(Event(**doc))
            
            logger.info(f"Retrieved {len(events)} pending events for user {user_id}")
            return events
            
        except Exception as e:
            logger.error(f"Failed to get pending events: {e}")
            return []
    
    @staticmethod
    async def acknowledge_event(ack: ACK) -> bool:
        """
        Acknowledge and delete an event.
        
        Args:
            ack: Acknowledgement with event_id and recipient_id
            
        Returns:
            True if event was found and deleted, False otherwise
        """
        try:
            db = get_database()
            
            result = await db.events.delete_one({
                "event_id": str(ack.event_id),
                "recipient_id": ack.recipient_id
            })
            
            if result.deleted_count > 0:
                logger.info(f"Acknowledged and deleted event {ack.event_id}")
                return True
            else:
                logger.warning(f"Event {ack.event_id} not found for acknowledgement")
                return False
                
        except Exception as e:
            logger.error(f"Failed to acknowledge event: {e}")
            return False
    
    @staticmethod
    async def cleanup_expired_events() -> int:
        """
        Delete events that have exceeded TTL.
        Should be called periodically (e.g., daily cron job).
        
        Returns:
            Number of events deleted
        """
        try:
            db = get_database()
            
            result = await db.events.delete_many({
                "ttl_expiry": {"$lt": datetime.utcnow()}
            })
            
            count = result.deleted_count
            if count > 0:
                logger.info(f"Cleaned up {count} expired events")
            
            return count
            
        except Exception as e:
            logger.error(f"Failed to cleanup expired events: {e}")
            return 0
    
    @staticmethod
    async def get_event_by_id(event_id: UUID, recipient_id: str) -> Optional[Event]:
        """
        Get a specific event by ID.
        
        Args:
            event_id: Event UUID
            recipient_id: Recipient user ID (for security)
            
        Returns:
            Event if found, None otherwise
        """
        try:
            db = get_database()
            
            doc = await db.events.find_one({
                "event_id": str(event_id),
                "recipient_id": recipient_id
            })
            
            if doc:
                doc["event_id"] = UUID(doc["event_id"])
                return Event(**doc)
            
            return None
            
        except Exception as e:
            logger.error(f"Failed to get event by ID: {e}")
            return None
