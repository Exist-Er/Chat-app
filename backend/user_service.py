from database import get_database
from models import User, UserRegistration
from auth import AuthService
from datetime import datetime
from typing import Optional, List
import logging

logger = logging.getLogger(__name__)


class UserService:
    """User management service"""
    
    @staticmethod
    async def register_user(registration: UserRegistration) -> Optional[User]:
        """
        Register a new user with Google Sign-In.
        
        Args:
            registration: User registration data with Google token and public key
            
        Returns:
            Created User object or None if registration failed
        """
        try:
            # Verify Google token
            google_info = await AuthService.verify_google_token(registration.google_id_token)
            if not google_info:
                logger.warning("Invalid Google token during registration")
                return None
            
            # Generate user_id
            user_id = AuthService.generate_user_id(google_info["google_id"])
            
            # Check if user already exists
            existing_user = await UserService.get_user_by_id(user_id)
            if existing_user:
                logger.info(f"User {user_id} already exists, updating public key")
                # Update public key if changed
                await UserService.update_public_key(user_id, registration.public_key)
                return existing_user
            
            # Create new user
            user = User(
                user_id=user_id,
                email=google_info["email"],
                display_name=registration.display_name or google_info["name"],
                public_key=registration.public_key,
                created_at=datetime.utcnow(),
                last_seen=datetime.utcnow()
            )
            
            db = get_database()
            await db.users.insert_one(user.model_dump())
            
            # Also store in public_keys collection for quick lookup
            await db.public_keys.insert_one({
                "user_id": user_id,
                "public_key": registration.public_key,
                "updated_at": datetime.utcnow()
            })
            
            logger.info(f"Registered new user: {user_id} ({user.email})")
            return user
            
        except Exception as e:
            logger.error(f"Failed to register user: {e}")
            return None
    
    @staticmethod
    async def get_user_by_id(user_id: str) -> Optional[User]:
        """Get user by ID"""
        try:
            db = get_database()
            doc = await db.users.find_one({"user_id": user_id})
            
            if doc:
                return User(**doc)
            return None
            
        except Exception as e:
            logger.error(f"Failed to get user by ID: {e}")
            return None
    
    @staticmethod
    async def get_user_by_email(email: str) -> Optional[User]:
        """Get user by email"""
        try:
            db = get_database()
            doc = await db.users.find_one({"email": email})
            
            if doc:
                return User(**doc)
            return None
            
        except Exception as e:
            logger.error(f"Failed to get user by email: {e}")
            return None
    
    @staticmethod
    async def get_public_key(user_id: str) -> Optional[str]:
        """
        Get user's public key.
        
        Args:
            user_id: User ID
            
        Returns:
            Base64 encoded public key or None
        """
        try:
            db = get_database()
            doc = await db.public_keys.find_one({"user_id": user_id})
            
            if doc:
                return doc["public_key"]
            return None
            
        except Exception as e:
            logger.error(f"Failed to get public key: {e}")
            return None
    
    @staticmethod
    async def get_public_keys(user_ids: List[str]) -> dict[str, str]:
        """
        Get public keys for multiple users.
        
        Args:
            user_ids: List of user IDs
            
        Returns:
            Dict mapping user_id to public_key
        """
        try:
            db = get_database()
            cursor = db.public_keys.find({"user_id": {"$in": user_ids}})
            
            keys = {}
            async for doc in cursor:
                keys[doc["user_id"]] = doc["public_key"]
            
            return keys
            
        except Exception as e:
            logger.error(f"Failed to get public keys: {e}")
            return {}
    
    @staticmethod
    async def update_public_key(user_id: str, public_key: str) -> bool:
        """Update user's public key"""
        try:
            db = get_database()
            
            # Update in users collection
            await db.users.update_one(
                {"user_id": user_id},
                {"$set": {"public_key": public_key}}
            )
            
            # Update in public_keys collection
            await db.public_keys.update_one(
                {"user_id": user_id},
                {
                    "$set": {
                        "public_key": public_key,
                        "updated_at": datetime.utcnow()
                    }
                },
                upsert=True
            )
            
            logger.info(f"Updated public key for user {user_id}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to update public key: {e}")
            return False
    
    @staticmethod
    async def update_last_seen(user_id: str) -> bool:
        """Update user's last seen timestamp"""
        try:
            db = get_database()
            
            await db.users.update_one(
                {"user_id": user_id},
                {"$set": {"last_seen": datetime.utcnow()}}
            )
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to update last seen: {e}")
            return False
    
    @staticmethod
    async def search_users(query: str, limit: int = 20) -> List[User]:
        """
        Search users by email or display name.
        
        Args:
            query: Search query
            limit: Maximum results
            
        Returns:
            List of matching users
        """
        try:
            db = get_database()
            
            # Case-insensitive regex search
            cursor = db.users.find({
                "$or": [
                    {"email": {"$regex": query, "$options": "i"}},
                    {"display_name": {"$regex": query, "$options": "i"}}
                ]
            }).limit(limit)
            
            users = []
            async for doc in cursor:
                users.append(User(**doc))
            
            return users
            
        except Exception as e:
            logger.error(f"Failed to search users: {e}")
            return []
