#!/usr/bin/env fish

# Function to get local IP address
function get_local_ip
    # Try getting IP from dominant interface (usually starts with 192.168...)
    set ip (ip -4 addr show | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | head -n 1)
    if test -z "$ip"
        echo "Could not detect local IP. Defaulting to 0.0.0.0"
        echo "0.0.0.0"
    else
        echo "$ip"
    end
end

set IP_ADDR (get_local_ip)
set PORT 8080

echo "=================================================="
echo "🚀 Starting Chat App Backend"
echo "=================================================="
echo "Detected Local IP: $IP_ADDR"
echo "Server will run on: http://$IP_ADDR:$PORT"
echo "=================================================="

# Navigate to backend directory
cd backend

# Ensure .venv exists
if not test -d .venv
    echo "Creating virtual environment..."
    python3 -m venv .venv
    source .venv/bin/activate.fish
    pip install -r requirements.txt
end

# Activate virtual environment
source .venv/bin/activate.fish

# Update .env file with detected IP (Optional, but good for reference)
# We won't overwrite the file, just export the var for this session
set -x HOST "0.0.0.0" # Listen on all interfaces
set -x PORT $PORT

# Start the server using uvicorn
echo "Starting uvicorn..."
uvicorn main:app --host 0.0.0.0 --port $PORT --reload
