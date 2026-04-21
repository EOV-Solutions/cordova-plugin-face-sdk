# cordova-plugin-face-sdk

Cordova plugin for Face Recognition SDK with enrollment, recognition, and license management.

Wrapper for **FaceSDK.xcframework** (iOS) and **FaceKit AAR** (Android).

## Installation

```bash
cordova plugin add cordova-plugin-face-sdk
```

Or from local path:

```bash
cordova plugin add /path/to/cordova-plugin-face-sdk
```

### Setup Native Libraries

Before using the plugin, you need to copy the native SDK libraries:

**iOS:** Copy `FaceSDK.xcframework` to `src/ios/Frameworks/`

```bash
cp -R /path/to/FaceSDK.xcframework cordova-plugin-face-sdk/src/ios/Frameworks/
```

**Android:** Copy `facekit-release.aar` to `libs/android/`

```bash
cp /path/to/facekit-release.aar cordova-plugin-face-sdk/libs/android/
```

## Supported Platforms

- iOS (12.0+)
- Android (API 23+)

## API Reference

The plugin exposes a global `FaceSDK` object. All methods use callback-style `(successCallback, errorCallback)`.

A **Promise-based** wrapper is also available via `FaceSDK.promise.*`.

### Initialization

#### `FaceSDK.initialize(options, success, error)`

Initialize the SDK with a license key.

```javascript
FaceSDK.initialize(
  {
    licenseKey: 'YOUR_LICENSE_KEY',
    faceId: 'user-face-id',        // optional, used as userId in registration
    userName: 'John Doe'             // optional, used in registration
  },
  function(result) {
    console.log('SDK initialized:', result.success);
  },
  function(err) {
    console.error('Init failed:', err.message);
  }
);
```

#### `FaceSDK.isLicenseValid(success, error)`

Check if license is valid.

```javascript
FaceSDK.isLicenseValid(
  function(result) {
    console.log('License valid:', result.valid);
    console.log('Status:', result.status, result.message);
  },
  function(err) { console.error(err); }
);
```

#### `FaceSDK.getLicenseInfo(success, error)`

Get detailed license information.

#### `FaceSDK.getLicenseStatus(success, error)`

Get license status code.

#### `FaceSDK.isInitialized(success, error)`

Check if SDK is initialized.

```javascript
FaceSDK.isInitialized(
  function(initialized) {
    console.log('SDK initialized:', initialized);
  },
  function(err) { console.error(err); }
);
```

#### `FaceSDK.terminate(success, error)`

Terminate the SDK and release all resources. Call `initialize()` again before using other SDK functions.

### Registration

#### `FaceSDK.startRegistration([options], success, error)`

Start the face registration flow (5 poses: front, left, right, up, down).

```javascript
FaceSDK.startRegistration(
  {
    userId: 'user-123',          // optional, defaults to faceId from initialize
    userName: 'John Doe',        // optional, defaults to userName from initialize
    skipNameDialog: true,         // optional, skip name input dialog
    mode: 'upsert'                // optional: 'create_only' | 'upsert' (default) | 'overwrite'
  },
  function(result) {
    if (result.success) {
      console.log('Registered:', result.userId, result.userName);
      console.log('Features:', result.featureCount);
    } else {
      console.log('Cancelled:', result.error);
    }
  },
  function(err) { console.error(err); }
);
```

**Registration Modes:**
| Mode | User EXISTS | User NOT EXISTS |
|------|-------------|------------------|
| `overwrite` | Delete all data + insert new | Insert |
| `upsert` | (Default) Update embeddings (keep user record) | Insert |
| `create_only` | Reject with error | Insert |

#### `FaceSDK.isUserEnrolled(userId, success, error)`

Check if a user is enrolled.

#### `FaceSDK.deleteUser(userId, success, error)`

Delete a user from the local database.

### Recognition

#### `FaceSDK.startRecognition([options], success, error)`

Start face recognition with liveness detection (3 random poses).

By default runs in **1:1 verify mode** — compares face only against the user enrolled with `faceId` from `initialize()`. Pass `mode: 'identify'` to search all users in the org (1:N).

```javascript
// 1:1 Verify (default) — userId auto-filled from initialize()
FaceSDK.startRecognition(
  { timeoutSeconds: 30 },
  function(result) {
    if (result.success) {
      console.log('Live:', result.isLive);
      console.log('Recognized:', result.isRecognized);
      console.log('User:', result.userId, result.userName);
      console.log('Confidence:', result.confidence);
      console.log('Image:', result.imagePath);
      console.log('Base64:', result.imageBase64); // data:image/jpeg;base64,...
    } else {
      console.log('Cancelled:', result.error);
    }
  },
  function(err) { console.error(err); }
);

// 1:N Identify — search all enrolled users
FaceSDK.startRecognition(
  { mode: 'identify', timeoutSeconds: 30 },
  function(result) { /* ... */ },
  function(err) { console.error(err); }
);

// Verify a specific user (different from the initialized faceId)
FaceSDK.startRecognition(
  { mode: 'verify', userId: 'other-user-id' },
  function(result) { /* ... */ },
  function(err) { console.error(err); }
);
```

**Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `mode` | `string` | `'verify'` | `'verify'` (1:1) or `'identify'` (1:N) |
| `userId` | `string` | faceId from `initialize()` | User to verify against (only used when `mode = 'verify'`) |
| `timeoutSeconds` | `number` | `30` | Recognition timeout in seconds |

**Response:**

| Field | Type | Description |
|-------|------|-------------|
| `success` | `boolean` | `true` if recognition completed |
| `isLive` | `boolean` | Liveness check passed (not a photo/video) |
| `isRecognized` | `boolean` | A user was matched |
| `userId` | `string` | Matched user ID (empty string if not recognized) |
| `userName` | `string` | Matched user name |
| `confidence` | `number` | Similarity score (0–1) |
| `imagePath` | `string` | Path to captured face image |
| `imageBase64` | `string` | Base64-encoded JPEG image (`data:image/jpeg;base64,...`) |
| `error` | `string` | Error message (only when cancelled/timeout) |

### Sync

#### `FaceSDK.refreshEmbeddings([faceId], success, error)`

Delete all local embeddings and re-download from server.

```javascript
FaceSDK.refreshEmbeddings(
  'user-face-id',
  function(result) {
    console.log('Refreshed:', result.deletedCount, 'deleted');
    console.log('Re-imported:', result.userId, result.userName);
  },
  function(err) { console.error(err); }
);
```

### Permissions

#### `FaceSDK.checkPermission(success, error)`

Check camera permission status. Alias: `checkCameraPermission()`.

```javascript
FaceSDK.checkPermission(
  function(result) {
    console.log('Camera granted:', result.granted);
    console.log('Status:', result.status); // 'granted' | 'denied' | 'restricted' | 'undetermined'
  },
  function(err) { console.error(err); }
);
```

#### `FaceSDK.requestPermission(success, error)`

Request camera permission. Alias: `requestCameraPermission()`.

### Device Compatibility

#### `FaceSDK.checkDeviceCompatibility(success, error)`

Check if the current device supports the Face SDK. Should be called **before** `initialize()` to give users early feedback if their device cannot run face recognition.

Checks: front camera, OS version (Android 7+ / iOS 12+), CPU architecture (arm64), RAM (>= 2GB), storage (>= 100MB).

```javascript
FaceSDK.checkDeviceCompatibility(
  function(result) {
    console.log('Compatible:', result.compatible);    // true/false
    console.log('Platform:', result.platform);         // 'android' | 'ios'
    console.log('OS:', result.osVersion);              // e.g. '33' or '17.0'
    console.log('Model:', result.deviceModel);         // e.g. 'Samsung SM-G991B'
    console.log('RAM:', result.totalRAM, 'MB');
    console.log('Storage:', result.availableStorage, 'MB');

    // Individual checks
    var c = result.checks;
    console.log('Front camera:', c.hasFrontCamera);
    console.log('OS supported:', c.osVersionSupported);
    console.log('CPU arm64:', c.cpuArchSupported);
    console.log('RAM enough:', c.hasEnoughRAM);
    console.log('Storage enough:', c.hasEnoughStorage);

    // If not compatible, show reasons
    if (!result.compatible) {
      console.warn('Reasons:', result.unsupportedReasons);
    }
  },
  function(err) { console.error(err); }
);
```

**Response:**

| Field | Type | Description |
|-------|------|-------------|
| `compatible` | `boolean` | Overall result — `true` if all checks pass |
| `message` | `string` | Human-readable summary |
| `platform` | `string` | `'android'` or `'ios'` |
| `osVersion` | `string` | OS version (API level on Android, version string on iOS) |
| `deviceModel` | `string` | Device model identifier |
| `checks.hasFrontCamera` | `boolean` | Front-facing camera available |
| `checks.osVersionSupported` | `boolean` | OS meets minimum requirement |
| `checks.cpuArchSupported` | `boolean` | CPU supports arm64 |
| `checks.hasEnoughRAM` | `boolean` | RAM >= 2GB |
| `checks.hasEnoughStorage` | `boolean` | Free storage >= 100MB |
| `totalRAM` | `number` | Total RAM in MB |
| `availableStorage` | `number` | Available storage in MB |
| `unsupportedReasons` | `string[]` | List of failure reasons (empty if compatible) |

## Promise API

All methods are also available as Promises via `FaceSDK.promise`:

```javascript
// Using async/await
async function initSDK() {
  try {
    // 1. Check device compatibility first
    const compat = await FaceSDK.promise.checkDeviceCompatibility();
    console.log('Device compatible:', compat.compatible);
    if (!compat.compatible) {
      alert('Device not supported: ' + compat.unsupportedReasons.join(', '));
      return;
    }

    // 2. Initialize SDK
    const result = await FaceSDK.promise.initialize({
      licenseKey: 'YOUR_KEY',
      faceId: 'user-123',
      userName: 'John Doe'
    });
    console.log('Initialized:', result.success);

    const permission = await FaceSDK.promise.requestPermission();
    console.log('Camera:', permission.granted);

    const regResult = await FaceSDK.promise.startRegistration({
      skipNameDialog: true,
      mode: 'upsert' // 'create_only' | 'upsert' | 'overwrite'
    });
    console.log('Registration:', regResult.success);

    const recResult = await FaceSDK.promise.startRecognition(
      { timeoutSeconds: 30 } // mode defaults to 'verify', userId from initialize()
    );
    if (recResult.success && recResult.isRecognized) {
      console.log('Recognized:', recResult.userId, recResult.confidence);
    }
  } catch (err) {
    console.error('Error:', err);
  }
}
```

## TypeScript Support

TypeScript type definitions are provided in `www/FaceSDK.d.ts`.

```typescript
declare var FaceSDK: FaceSDKPlugin;
```

## Ionic / Capacitor

This plugin works with Ionic Cordova apps. For Ionic usage:

```typescript
declare var FaceSDK: any;

// In your component
async initFaceSDK() {
  const result = await FaceSDK.promise.initialize({
    licenseKey: 'YOUR_KEY'
  });
}
```

## License

MIT © EOV Solutions
