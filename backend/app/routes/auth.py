from fastapi import APIRouter, HTTPException, Depends, status
from app.models.user import UserCreate, UserLogin
from app.database import get_database
from app.core.security import get_password_hash, verify_password
from bson import ObjectId

router = APIRouter()

@router.post("/signup", status_code=status.HTTP_201_CREATED)
async def signup(user: UserCreate, db = Depends(get_database)):
    # Check if user already exists
    existing_user = await db.users.find_one({"email": user.email})
    if existing_user:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    # Hash password and save
    user_dict = user.model_dump()
    user_dict["hashed_password"] = get_password_hash(user_dict.pop("password"))
    
    result = await db.users.insert_one(user_dict)
    return {"id": str(result.inserted_id), "message": "User created successfully"}

@router.post("/login")
async def login(user_credentials: UserLogin, db = Depends(get_database)):
    user = await db.users.find_one({"email": user_credentials.email})
    if not user or not verify_password(user_credentials.password, user["hashed_password"]):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    
    return {"message": "Login successful", "username": user["username"]}
