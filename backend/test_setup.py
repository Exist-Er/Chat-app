#!/usr/bin/env python3
"""
Test script to verify backend dependencies and imports
"""

print("Testing Python backend setup...")
print("=" * 50)

# Test 1: Check Python version
import sys
print(f"✓ Python version: {sys.version}")

# Test 2: Try importing required packages
packages = [
    "fastapi",
    "uvicorn",
    "websockets",
    "pymongo",
    "motor",
    "pydantic",
    "jose",
    "google.auth",
    "pytest"
]

print("\nChecking required packages:")
for package in packages:
    try:
        __import__(package)
        print(f"✓ {package}")
    except ImportError:
        print(f"✗ {package} - NOT INSTALLED")

print("\n" + "=" * 50)
print("If any packages show 'NOT INSTALLED', run:")
print("./.venv/bin/pip install -r requirements.txt")
