# 🚀 Running Tests on Different Simulators

## 📱 Your Simulators

| Device | UDID | Appium Port |
|--------|------|-------------|
| **iPhone 17 Pro** (Device 1) | `B745C0EF-01AA-4355-8B08-86812A8CBBAA` | 4723 |
| **iPhone 17 Pro Max** (Device 2) | `E042B830-41DF-4690-AAB2-11FDE47916DD` | 4724 |

---

## ✅ Quick Start (Easiest Method)

### Run on Device 1 (iPhone 17 Pro)
```bash
./run-device1.sh
```

### Run on Device 2 (iPhone 17 Pro Max)
```bash
./run-device2.sh
```

**Note:** Make sure the corresponding Appium server is running first!

---

## 🔧 Manual Commands

### Device 1 (iPhone 17 Pro) - Port 4723
```bash
# Start Appium
appium -p 4723

# Run tests
mvn clean test \
  -Dappium.server=http://127.0.0.1:4723 \
  -Ddevice.name="iPhone 17 Pro" \
  -Dudid=B745C0EF-01AA-4355-8B08-86812A8CBBAA
```

### Device 2 (iPhone 17 Pro Max) - Port 4724
```bash
# Start Appium
appium -p 4724

# Run tests
mvn clean test \
  -Dappium.server=http://127.0.0.1:4724 \
  -Ddevice.name="iPhone 17 Pro Max" \
  -Dudid=E042B830-41DF-4690-AAB2-11FDE47916DD
```

---

## 🔄 Boot Simulators

If simulators are not booted:
```bash
# Boot iPhone 17 Pro
xcrun simctl boot B745C0EF-01AA-4355-8B08-86812A8CBBAA

# Boot iPhone 17 Pro Max
xcrun simctl boot E042B830-41DF-4690-AAB2-11FDE47916DD
```

---

## 📊 Default Behavior (No System Properties)

When running without system properties, tests use `config/config.properties` defaults:
```bash
mvn clean test  # Uses Device 1 (iPhone 17 Pro, port 4723)
```

---

## ⚙️ System Properties Override

System properties (`-D` flags) override the config.properties settings:
- `-Dappium.server` → Overrides `appium.server`
- `-Ddevice.name` → Overrides `device.name`
- `-Dudid` → Overrides `udid`

---

## 🎯 Run Specific Test Suite

### Run only Authentication tests
```bash
mvn test -Dtest=AuthenticationTest \
  -Dappium.server=http://127.0.0.1:4724 \
  -Ddevice.name="iPhone 17 Pro Max" \
  -Dudid=E042B830-41DF-4690-AAB2-11FDE47916DD
```

### Run only Site Selection tests
```bash
mvn test -Dtest=SiteSelectionTest \
  -Dappium.server=http://127.0.0.1:4724 \
  -Ddevice.name="iPhone 17 Pro Max" \
  -Dudid=E042B830-41DF-4690-AAB2-11FDE47916DD
```

---

## 🔍 Troubleshooting

### Issue: "Address already in use"
**Solution:** Another Appium instance is running on that port. Kill it first:
```bash
lsof -ti:4723 | xargs kill -9  # For port 4723
lsof -ti:4724 | xargs kill -9  # For port 4724
```

### Issue: "Could not connect to WebDriverAgent"
**Solution:** Make sure the simulator is booted:
```bash
xcrun simctl list | grep Booted
```

### Issue: Tests run on wrong device
**Solution:** Verify system properties are passed correctly. Check console output for:
```
📱 Device: iPhone 17 Pro Max
📱 UDID: E042B830-41DF-4690-AAB2-11FDE47916DD
📱 Appium Server: http://127.0.0.1:4724
```

---

## 🚫 What Does NOT Affect GitHub Actions

✅ These local testing scripts and configurations do NOT affect CI/CD:
- `run-device1.sh` - Local only
- `run-device2.sh` - Local only
- `testng-local-parallel.xml` - Not used in CI
- `testng-single-device2.xml` - Not used in CI
- `config-local-parallel.properties` - Not used in CI

GitHub Actions continues to use:
- `testng.xml` (default)
- `config/config.properties` (default)

---

## 📝 Notes

1. **GitHub Actions is unaffected** - All local configurations are ignored in CI
2. **System properties have highest priority** - They override config files
3. **Port conflicts** - Make sure each simulator uses a different Appium port and WDA port
4. **Clean state** - Use `mvn clean test` to ensure fresh builds

---

## 💡 Pro Tips

**Tip 1:** Keep two terminals open for easy switching:
```bash
# Terminal 1
appium -p 4723  # Always running for Device 1

# Terminal 2
appium -p 4724  # Always running for Device 2
```

**Tip 2:** Use aliases in your `~/.zshrc` or `~/.bashrc`:
```bash
alias test-device1='cd /Users/abhiyantsingh/Downloads/iOS_automation_site && ./run-device1.sh'
alias test-device2='cd /Users/abhiyantsingh/Downloads/iOS_automation_site && ./run-device2.sh'
```

**Tip 3:** Quick device check:
```bash
xcrun simctl list devices | grep -E "(iPhone|iPad)" | grep Booted
```
