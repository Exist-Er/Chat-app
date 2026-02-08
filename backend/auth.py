from google.oauth2 import id_token
from google.auth.transport import requests
from config import settings
from typing import Optional, Dict
import logging

logger = logging.getLogger(__name__)


class AuthService:
    """Google Sign-In authentication service"""
    
    @staticmethod
    async def verify_google_token(token: str) -> Optional[Dict[str, str]]:
        """
        Verify Google ID token and extract user information.
        
        Args:
            token: Google ID token from client
            
        Returns:
            Dict with user info (sub, email, name) or None if invalid
        """
        try:
            # Verify the token
            idinfo = id_token.verify_oauth2_token(
                token, 
                requests.Request(), 
                settings.google_client_id
            )
            
            # Token is valid, extract user info
            user_info = {
                "google_id": idinfo['sub'],
                "email": idinfo['email'],
                "name": idinfo.get('name', ''),
                "picture": idinfo.get('picture', '')
            }
            
            logger.info(f"Successfully verified token for user: {user_info['email']}")
            return user_info
            
        except ValueError as e:
            # Invalid token
            logger.warning(f"Invalid Google token: {e}")
            return None
        except Exception as e:
            logger.error(f"Error verifying Google token: {e}")
            return None
    
    @staticmethod
    def generate_user_id(google_id: str) -> str:
        """
        Generate a consistent user_id from Google ID.
        
        Args:
            google_id: Google user ID (sub claim)
            
        Returns:
            User ID string
        """
        # For now, use google_id directly as user_id
        # In production, you might want to hash or transform this
        return f"user_{google_id}"
