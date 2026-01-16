#!/bin/bash
# Quick UI dump helper - auto-creates session if needed

SESSION_FILE="/tmp/appium_session_id.txt"
APPIUM_URL="http://127.0.0.1:4723"

# Create new session with long timeout
create_session() {
    echo "🔌 Creating new Appium session (1 hour timeout)..."
    
    RESPONSE=$(curl -s -X POST "$APPIUM_URL/session" \
      -H "Content-Type: application/json" \
      -d '{
        "capabilities": {
          "alwaysMatch": {
            "platformName": "iOS",
            "appium:deviceName": "iPhone 17 Pro",
            "appium:udid": "B745C0EF-01AA-4355-8B08-86812A8CBBAA",
            "appium:app": "/Users/abhiyantsingh/Downloads/Z Platform-QA.app",
            "appium:automationName": "XCUITest",
            "appium:noReset": true,
            "appium:newCommandTimeout": 3600
          }
        }
      }' 2>/dev/null)
    
    SESSION_ID=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('value',{}).get('sessionId',''))" 2>/dev/null)
    
    if [ -n "$SESSION_ID" ] && [ "$SESSION_ID" != "" ]; then
        echo "$SESSION_ID" > "$SESSION_FILE"
        echo "✅ Session: $SESSION_ID"
        return 0
    else
        echo "❌ Failed to create session. Is Appium running?"
        return 1
    fi
}

# Check/create session
SESSION_ID=""
if [ -f "$SESSION_FILE" ]; then
    SESSION_ID=$(cat "$SESSION_FILE")
    # Test if session is valid
    TEST=$(curl -s "$APPIUM_URL/session/$SESSION_ID" 2>/dev/null)
    if echo "$TEST" | grep -q "invalid session"; then
        create_session || exit 1
        SESSION_ID=$(cat "$SESSION_FILE")
    fi
else
    create_session || exit 1
    SESSION_ID=$(cat "$SESSION_FILE")
fi

# Dump UI
echo ""
echo "📱 CURRENT SCREEN UI:"
echo "=========================="

curl -s "$APPIUM_URL/session/$SESSION_ID/source" 2>/dev/null | \
python3 << 'PYEOF'
import sys, json, xml.etree.ElementTree as ET

try:
    data = json.load(sys.stdin)
    xml_str = data.get('value', '')
    
    if not xml_str or isinstance(xml_str, dict):
        print("❌ Could not get UI source")
        sys.exit(1)
    
    root = ET.fromstring(xml_str)
    
    def print_elem(elem, indent=0):
        t = elem.tag.replace('XCUIElementType', '')
        name = elem.get('name', '')
        label = elem.get('label', '')
        value = elem.get('value', '')
        visible = elem.get('visible', '')
        
        if visible == 'true' and (name or label or value):
            line = '  ' * indent + t
            if name:
                line += f' name="{name}"'
            if label and label != name:
                line += f' label="{label}"'
            if value and value != name and value != label:
                line += f' value="{value}"'
            print(line)
        
        for child in elem:
            print_elem(child, indent + 1)
    
    print_elem(root)

except Exception as e:
    print(f"❌ Error: {e}")
PYEOF
