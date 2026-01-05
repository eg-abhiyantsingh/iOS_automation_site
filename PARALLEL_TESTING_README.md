# Parallel Testing Setup - Known Issue & Workaround

## ❌ Current Issue
TestNG parameters defined in XML are not being passed to `@BeforeMethod` in BaseTest.
This causes the tests to always use the default config (port 4723, iPhone 17 Pro) instead of the parameters.

## ✅ Workaround Options

### Option 1: Temporarily Change Default Config (EASIEST)
Edit `config/config.properties` to point to your second simulator:

```properties
# Change these lines:
appium.server=http://127.0.0.1:4724
device.name=iPhone 17 Pro Max
udid=E042B830-41DF-4690-AAB2-11FDE47916DD
```

Then run:
```bash
mvn test
```

**Remember to change it back when done!**

### Option 2: Use System Properties
Run with system properties to override config:

```bash
mvn test \
  -Dappium.server=http://127.0.0.1:4724 \
  -Ddevice.name="iPhone 17 Pro Max" \
  -Dudid=E042B830-41DF-4690-AAB2-11FDE47916DD
```

### Option 3: Fix BaseTest Architecture (PROPER FIX - Requires Code Change)
Move the driver initialization from `@BeforeMethod` to `@BeforeTest` or create a custom test listener.

## 🔧 Recommended Solution
For now, use **Option 1** (temporarily edit config.properties) when you want to test on the second simulator.

For true parallel testing, we need to refactor BaseTest to use `@BeforeTest` instead of `@BeforeMethod`.

## 📝 Your Current Setup
- **Device 1 (Default):** iPhone 17 Pro - UDID: B745C0EF-01AA-4355-8B08-86812A8CBBAA - Port: 4723
- **Device 2:** iPhone 17 Pro Max - UDID: E042B830-41DF-4690-AAB2-11FDE47916DD - Port: 4724
