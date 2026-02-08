from pydantic_settings import BaseSettings
from pydantic import field_validator
from typing import List


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""
    
    # MongoDB
    mongodb_uri: str = "mongodb://localhost:27017"
    mongodb_db_name: str = "chatapp"
    
    # Server
    host: str = "0.0.0.0"
    port: int = 8080
    debug: bool = True
    
    # Google OAuth
    google_client_id: str = ""
    
    # Security
    secret_key: str = "dev-secret-key-change-in-production"
    event_ttl_days: int = 14
    
    # CORS
    allowed_origins: List[str] = ["http://localhost:3000", "http://localhost:8080"]

    @field_validator("allowed_origins", mode="before")
    @classmethod
    def parse_allowed_origins(cls, v):
        if isinstance(v, str) and not v.strip().startswith("["):
            return [origin.strip() for origin in v.split(",")]
        return v
    
    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
