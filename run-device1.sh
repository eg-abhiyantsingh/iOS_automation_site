#!/bin/bash

# Run tests on iPhone 17 Pro (First Simulator)
# Port: 4723

echo "🚀 Running tests on iPhone 17 Pro (Device 1)"
echo "📱 UDID: B745C0EF-01AA-4355-8B08-86812A8CBBAA"
echo "🔌 Appium Port: 4723"
echo ""
echo "⚠️  Make sure Appium is running on port 4723:"
echo "   appium -p 4723"
echo ""

mvn clean test \
  -Dappium.server=http://127.0.0.1:4723 \
  -Ddevice.name="iPhone 17 Pro" \
  -Dudid=B745C0EF-01AA-4355-8B08-86812A8CBBAA
