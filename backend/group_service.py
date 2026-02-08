from database import get_database
from models import Group
from datetime import datetime
from typing import Optional, List
import logging

logger = logging.getLogger(__name__)


class GroupService:
    """Group management service"""
    
    @staticmethod
    async def create_group(
        group_id: str,
        name: str,
        creator_id: str,
        member_ids: List[str]
    ) -> Optional[Group]:
        """
        Create a new group.
        
        Args:
            group_id: Unique group identifier
            name: Group name
            creator_id: User ID of creator
            member_ids: List of member user IDs (including creator)
            
        Returns:
            Created Group object or None if failed
        """
        try:
            # Ensure creator is in member list
            if creator_id not in member_ids:
                member_ids.append(creator_id)
            
            group = Group(
                group_id=group_id,
                name=name,
                creator_id=creator_id,
                member_ids=member_ids,
                key_version=1,
                created_at=datetime.utcnow(),
                updated_at=datetime.utcnow()
            )
            
            db = get_database()
            await db.groups.insert_one(group.model_dump())
            
            logger.info(f"Created group {group_id} with {len(member_ids)} members")
            return group
            
        except Exception as e:
            logger.error(f"Failed to create group: {e}")
            return None
    
    @staticmethod
    async def get_group(group_id: str) -> Optional[Group]:
        """Get group by ID"""
        try:
            db = get_database()
            doc = await db.groups.find_one({"group_id": group_id})
            
            if doc:
                return Group(**doc)
            return None
            
        except Exception as e:
            logger.error(f"Failed to get group: {e}")
            return None
    
    @staticmethod
    async def get_user_groups(user_id: str) -> List[Group]:
        """Get all groups a user is a member of"""
        try:
            db = get_database()
            cursor = db.groups.find({"member_ids": user_id})
            
            groups = []
            async for doc in cursor:
                groups.append(Group(**doc))
            
            return groups
            
        except Exception as e:
            logger.error(f"Failed to get user groups: {e}")
            return []
    
    @staticmethod
    async def add_member(group_id: str, user_id: str) -> bool:
        """
        Add a member to a group.
        Triggers key rotation (handled by caller).
        
        Args:
            group_id: Group ID
            user_id: User ID to add
            
        Returns:
            True if successful
        """
        try:
            db = get_database()
            
            result = await db.groups.update_one(
                {"group_id": group_id},
                {
                    "$addToSet": {"member_ids": user_id},
                    "$set": {"updated_at": datetime.utcnow()}
                }
            )
            
            if result.modified_count > 0:
                logger.info(f"Added user {user_id} to group {group_id}")
                return True
            
            return False
            
        except Exception as e:
            logger.error(f"Failed to add member: {e}")
            return False
    
    @staticmethod
    async def remove_member(group_id: str, user_id: str) -> bool:
        """
        Remove a member from a group.
        Triggers key rotation (handled by caller).
        
        Args:
            group_id: Group ID
            user_id: User ID to remove
            
        Returns:
            True if successful
        """
        try:
            db = get_database()
            
            result = await db.groups.update_one(
                {"group_id": group_id},
                {
                    "$pull": {"member_ids": user_id},
                    "$set": {"updated_at": datetime.utcnow()}
                }
            )
            
            if result.modified_count > 0:
                logger.info(f"Removed user {user_id} from group {group_id}")
                return True
            
            return False
            
        except Exception as e:
            logger.error(f"Failed to remove member: {e}")
            return False
    
    @staticmethod
    async def increment_key_version(group_id: str) -> int:
        """
        Increment group key version (for key rotation).
        
        Args:
            group_id: Group ID
            
        Returns:
            New key version number
        """
        try:
            db = get_database()
            
            result = await db.groups.find_one_and_update(
                {"group_id": group_id},
                {
                    "$inc": {"key_version": 1},
                    "$set": {"updated_at": datetime.utcnow()}
                },
                return_document=True
            )
            
            if result:
                new_version = result["key_version"]
                logger.info(f"Incremented key version for group {group_id} to {new_version}")
                return new_version
            
            return 0
            
        except Exception as e:
            logger.error(f"Failed to increment key version: {e}")
            return 0
    
    @staticmethod
    async def is_member(group_id: str, user_id: str) -> bool:
        """Check if user is a member of group"""
        try:
            db = get_database()
            
            group = await db.groups.find_one({
                "group_id": group_id,
                "member_ids": user_id
            })
            
            return group is not None
            
        except Exception as e:
            logger.error(f"Failed to check membership: {e}")
            return False
