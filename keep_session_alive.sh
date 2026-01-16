#!/bin/bash
# Keep Appium session alive by sending periodic requests

SESSION_FILE="/tmp/appium_session_id.txt"
INTERVAL=30  # Send keep-alive every 30 seconds

echo "🔄 Starting Appium session keep-alive..."
echo "   Interval: ${INTERVAL}s"
echo "   Press Ctrl+C to stop"

while true; do
    if [ -f "$SESSION_FILE" ]; then
        SESSION_ID=$(cat "$SESSION_FILE")
        
        # Send a lightweight request to keep session alive
        RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null \
            "http://127.0.0.1:4723/session/$SESSION_ID/timeouts")
        
        if [ "$RESPONSE" == "200" ]; then
            echo "$(date '+%H:%M:%S') ✅ Session alive: ${SESSION_ID:0:8}..."
        else
            echo "$(date '+%H:%M:%S') ❌ Session expired or invalid"
        fi
    else
        echo "$(date '+%H:%M:%S') ⚠️ No session file found"
    fi
    
    sleep $INTERVAL
done
