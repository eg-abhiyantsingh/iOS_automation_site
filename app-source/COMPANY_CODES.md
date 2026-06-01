# Company Codes for iOS Mobile App

## Overview
The iOS app now supports company-specific authentication using company codes (subdomains). This allows users to authenticate against their specific company's Cognito user pool.

## How It Works
1. **Company Code Field**: A new optional field has been added to the login screen
2. **Subdomain Mapping**: The company code is sent as a subdomain to the backend
3. **Automatic Routing**: The backend routes authentication to the correct Cognito user pool based on the subdomain
4. **Fallback Support**: If no company code is provided, the app falls back to default authentication behavior

## Available Company Codes

### Production Companies
| Company | Code | User Pool |
|---------|------|-----------|
| Alkegen | alkegen | us-east-2_wJgTUGPZp |
| Christenson Electric | christenson | us-east-2_UXINrT2CB |
| Continental (CECCO) | cecco | us-east-2_YDMUcq1OC |
| Decker Electric | decker | us-east-2_SMp8rxYxZ |
| EC Electric | ecpowerslife | us-east-2_9ysZuhesB |
| Egan Company | eganco | us-east-2_NnA5bbKpI |
| Electrical Energy Experts | eee | us-east-2_6HruJYMHg |
| G&B Electric | gbelectric | us-east-2_cMUcKMgg1 |
| Hi-Tech Electric | hi-techelectric | us-east-2_A1vrePjix |
| Inglett & Stubbs | inglett-stubbs | us-east-2_IN2Yz8JYY |
| J.W. Didado | jwdidado | us-east-2_GQTZJmqxM |
| Miller Electric | meco | us-east-2_2rUVD3lwV |
| Newkirk Electric | newkirkelectric | us-east-2_eg8ugxO10 |
| Precision Electric Group | precisionelectricgroup | us-east-2_kUMmoG2pk |

### Test/Development
| Company | Code | User Pool |
|---------|------|-----------|
| ACME | acme | us-east-2_IZSq7j5lN |

## Features

### Persistent Company Code
- The app remembers the last used company code using UserDefaults
- Users don't need to re-enter their company code on subsequent logins
- The saved company code is automatically populated on the login screen

### Biometric Authentication
- Company code is preserved when using Face ID/Touch ID login
- The saved company code is used automatically with biometric authentication

### Backend Integration
- Company code is sent in two ways for compatibility:
  1. In the JSON body as `subdomain` field
  2. As an HTTP header `X-Subdomain`
- Backend validates and routes to appropriate Cognito pool

## Testing

### To Test Company-Specific Login:
1. Enter the company code in the "Company Code" field
2. Enter email and password for a user in that company's pool
3. Tap "Sign In"

### Example Test Accounts:
- CECCO: Use code `cecco` with any CECCO user credentials
- Miller Electric: Use code `meco` with Miller Electric credentials
- ACME: Use code `acme` for testing

### To Test Fallback:
- Leave the Company Code field empty
- The app will use default authentication behavior

## Implementation Details

### Files Modified:
- `AuthService.swift`: Added subdomain parameter to login method
- `ContentView.swift`: Added company code UI field and persistence
- `LoginRequest`: Added optional subdomain field

### API Changes:
- Login endpoint accepts optional `subdomain` in request body
- `X-Subdomain` header is sent when company code is provided
- Backend routes authentication based on subdomain

## Security Notes
- Company codes are not sensitive information
- They simply route to the correct authentication pool
- All authentication still requires valid email/password
- Company codes are stored in UserDefaults (not encrypted)
- Actual credentials are stored securely in Keychain (for biometric auth)