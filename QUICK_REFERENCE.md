# 📚 Quick Reference Card - Multi-Simulator Testing

## 🎯 TLDR - Just Run Tests!

### On iPhone 17 Pro (Default):
```bash
./run-device1.sh
```

### On iPhone 17 Pro Max:
```bash
./run-device2.sh
```

---

## 📋 File Overview

| File | Purpose | Used In CI? |
|------|---------|-------------|
| `run-device1.sh` | Quick script for Device 1 | ❌ No |
| `run-device2.sh` | Quick script for Device 2 | ❌ No |
| `SIMULATOR_TESTING_GUIDE.md` | Full documentation | ❌ No |
| `testng.xml` | Main test configuration | ✅ Yes |
| `config/config.properties` | Default configuration | ✅ Yes |
| `testng-single-device2.xml` | Single device config | ❌ No |
| `testng-local-parallel.xml` | Parallel config (future) | ❌ No |

---

## ✅ What's Working Now

1. **TC_SS_015 Fixed** - Now verifies actual asset count ✅
2. **TC_SS_018 Improved** - Added stability wait ✅
3. **Multi-simulator support** - Can test on either device ✅
4. **System property override** - Working perfectly ✅
5. **GitHub Actions** - Unaffected by local changes ✅

---

## 🔧 Quick Commands

```bash
# Start Appium servers (keep in separate terminals)
appium -p 4723  # Device 1
appium -p 4724  # Device 2

# Boot simulators (if needed)
xcrun simctl boot B745C0EF-01AA-4355-8B08-86812A8CBBAA  # Device 1
xcrun simctl boot E042B830-41DF-4690-AAB2-11FDE47916DD  # Device 2

# Run on specific device
./run-device1.sh  # iPhone 17 Pro
./run-device2.sh  # iPhone 17 Pro Max

# Default run (Device 1)
mvn clean test
```

---

## 💡 Remember

- Always start the Appium server on the correct port before running tests
- System properties (`-D` flags) override config files
- GitHub Actions always uses the default config
- Both simulators must be booted to run tests

---

**For full documentation, see:** `SIMULATOR_TESTING_GUIDE.md`
