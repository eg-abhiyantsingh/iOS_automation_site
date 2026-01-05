#!/bin/bash

# Run tests on iPhone 17 Pro Max (Second Simulator)
# Port: 4724

echo "🚀 Running tests on iPhone 17 Pro Max (Device 2)"
echo "📱 UDID: E042B830-41DF-4690-AAB2-11FDE47916DD"
echo "🔌 Appium Port: 4724"
echo ""
echo "⚠️  Make sure Appium is running on port 4724:"
echo "   appium -p 4724"
echo ""

mvn clean test \
  -Dappium.server=http://127.0.0.1:4724 \
  -Ddevice.name="iPhone 17 Pro Max" \
  -Dudid=E042B830-41DF-4690-AAB2-11FDE47916DD
