from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorDatabase
from config import settings
import logging

logger = logging.getLogger(__name__)


class Database:
    """MongoDB database connection manager"""
    
    client: AsyncIOMotorClient = None
    db: AsyncIOMotorDatabase = None
    
    @classmethod
    async def connect(cls):
        """Connect to MongoDB"""
        try:
            cls.client = AsyncIOMotorClient(settings.mongodb_uri)
            cls.db = cls.client[settings.mongodb_db_name]
            
            # Test connection
            await cls.client.admin.command('ping')
            logger.info(f"Connected to MongoDB: {settings.mongodb_db_name}")
            
            # Create indexes
            await cls._create_indexes()
            
        except Exception as e:
            logger.error(f"Failed to connect to MongoDB: {e}")
            raise
    
    @classmethod
    async def disconnect(cls):
        """Disconnect from MongoDB"""
        if cls.client:
            cls.client.close()
            logger.info("Disconnected from MongoDB")
    
    @classmethod
    async def _create_indexes(cls):
        """Create necessary indexes for optimal performance"""
        
        # Users collection
        await cls.db.users.create_index("user_id", unique=True)
        await cls.db.users.create_index("email", unique=True)
        
        # Events collection (per-user event queues)
        await cls.db.events.create_index([("recipient_id", 1), ("sequence", 1)])
        await cls.db.events.create_index("event_id", unique=True)
        await cls.db.events.create_index("timestamp")  # For TTL cleanup
        
        # Groups collection
        await cls.db.groups.create_index("group_id", unique=True)
        await cls.db.groups.create_index("member_ids")
        
        # Public keys collection (for quick lookup)
        await cls.db.public_keys.create_index("user_id", unique=True)
        
        logger.info("Database indexes created")
    
    @classmethod
    def get_db(cls) -> AsyncIOMotorDatabase:
        """Get database instance"""
        if cls.db is None:
            raise RuntimeError("Database not connected. Call connect() first.")
        return cls.db


# Convenience function
def get_database() -> AsyncIOMotorDatabase:
    """Get database instance"""
    return Database.get_db()
