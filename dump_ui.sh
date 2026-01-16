#!/bin/bash
SESSION_ID=$(cat /tmp/appium_session_id.txt 2>/dev/null)

if [ -z "$SESSION_ID" ]; then
    echo "❌ No active Appium session. Run tests first or create a session."
    exit 1
fi

echo "📱 CURRENT SCREEN UI:"
echo "===================="

curl -s "http://127.0.0.1:4723/session/$SESSION_ID/source" | \
python3 -c "
import sys, json, xml.etree.ElementTree as ET

data = json.load(sys.stdin)
xml_str = data.get('value', '')
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
            line += f' name=\"{name}\"'
        if label and label != name:
            line += f' label=\"{label}\"'
        if value and value != name and value != label:
            line += f' value=\"{value}\"'
        print(line)
    
    for child in elem:
        print_elem(child, indent + 1)

print_elem(root)
"
