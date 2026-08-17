# Contributing to react-native-exact-frame-extractor

Thanks for your interest in contributing! We're building a forensic-grade frame extraction tool, and your help makes it better.

---

## Code of Conduct

Be respectful. No harassment, discrimination, or bad faith. We're all here to make investigation tools more reliable.

---

## How to Contribute

### 1. Report Bugs

**Before opening an issue, check if it already exists.**

When reporting, include:

```
**Device:** Pixel 5 (Android 13, API 33)
**Video Details:** 1080p H.264 MP4, 30fps, 5min
**Timestamp Requested:** 2:34:500 (2 mins 34.5 secs)
**Actual Result:** Black frame returned
**Expected Result:** Valid frame from that timestamp

**Logcat:**
[paste error logs here]
```

### 2. Suggest Features

Open an issue with the `enhancement` label. Describe:
- The use case (forensics? compliance? content analysis?)
- Why existing solutions don't work
- Proposed API (if you have one in mind)

**High-priority features we're tracking:**
- iOS support
- Batch frame extraction
- WebM/VP9 optimization

### 3. Submit Code

#### Fork & Branch

```bash
git clone https://github.com/yourusername/react-native-exact-frame-extractor.git
cd react-native-exact-frame-extractor
git checkout -b feature/your-feature-name
```

#### Code Style

- **Kotlin:** Follow Google's [Kotlin style guide](https://developer.android.com/kotlin/style-guide)
- **TypeScript:** Use `prettier` + `eslint` (run `npm run lint` before committing)
- **Commit messages:** Be clear and concise
  - ✅ `fix: handle corrupted video frames gracefully`
  - ❌ `fix stuff`

#### Testing

Before opening a PR:

```bash
# Build the Android module
cd android && ./gradlew build

# Run type check
npm run type-check

# Test on a real device or emulator
npx expo run:android
```

#### Pull Request Checklist

- [ ] Tests pass locally
- [ ] No console errors or warnings
- [ ] Logcat shows no native errors
- [ ] Code follows style guide
- [ ] Commit messages are clear
- [ ] Documentation is updated (if applicable)

---

## Development Setup

### Prerequisites

- Node.js 16+
- JDK 17 (OpenJDK or Eclipse Temurin)
- Android SDK (API 24+)
- Android NDK (optional, for JNI work)
- Kotlin 2.1.20+

### Local Build

```bash
# Install JS dependencies
npm install

# Build native Android module
cd android
./gradlew build
cd ..

# Link to local Expo/React Native project
npm link
```

### Testing Your Changes

1. Create a test Expo app:
```bash
npx create-expo-app test-frame-extractor
cd test-frame-extractor
npm link react-native-exact-frame-extractor
npx expo prebuild --clean
npx expo run:android
```

2. Write a quick test in your app:
```typescript
import { captureFrameAtTime } from 'react-native-exact-frame-extractor';

async function testCapture() {
  try {
    const result = await captureFrameAtTime({
      videoUri: 'file:///path/to/test.mp4',
      timeMillis: 5000,
      outputPath: FileSystem.cacheDirectory + 'test_frame.jpg',
    });
    console.log('Success:', result.path);
  } catch (err) {
    console.error('Failed:', err);
  }
}
```

3. Check logcat for errors:
```bash
adb logcat | grep FrameCaptureModule
```

---

## Architecture Notes

### Module Structure

```
android/src/
├── main/kotlin/com/framecapture/
│   ├── FrameCaptureModule.kt       # TurboModule JSI bridge
│   ├── FrameCapturePackage.kt      # Native package registration
│   └── FrameExtractor.kt           # Core decoding logic
└── main/jniLibs/                   # Pre-compiled native libraries (if needed)
```

### Key Classes

**`FrameCaptureModule`**
- Exposes `captureFrameAtTime()` to React Native
- Handles coroutine dispatch and promise resolution
- Validates input parameters

**`FrameExtractor` (planned refactor)**
- Encapsulates `MediaExtractor` + `MediaCodec` logic
- Manages buffer lifecycle
- YUV-to-JPEG conversion

### Performance Considerations

- **Seekable files only:** Non-seekable streams (HLS) not supported
- **Keyframe alignment:** Module seeks to prior sync point, then forward-decodes
- **Memory:** Decoder buffers held in native memory; released after frame capture
- **Threading:** All I/O runs on `Dispatchers.IO`, never blocks main thread

---

## Common Development Tasks

### Adding a New Parameter to `captureFrameAtTime()`

1. Update the Kotlin function signature:
```kotlin
fun captureFrameAtTime(
    videoUri: String,
    timeMillis: Long,
    outputPath: String,
    quality: Int = 95,  // ← New param
    promise: Promise
) { ... }
```

2. Update TypeScript types (`index.d.ts`):
```typescript
export interface FrameCaptureOptions {
  videoUri: string;
  timeMillis: number;
  outputPath: string;
  quality?: number;  // ← Add here
}
```

3. Update README with the new parameter in the API table.

4. Test locally and submit PR.

### Improving YUV-to-RGB Conversion

The current implementation uses Android's built-in `YuvImage` conversion. For better performance:

1. Integrate [libyuv](https://chromium.googlesource.com/libyuv/libyuv):
   - Add CMakeLists.txt for native build
   - Link libyuv in `build.gradle`
   - Replace `YuvImage.compressToJpeg()` with `libyuv::I420ToARGB()`

2. Benchmark before/after:
```bash
adb shell am start-profiling com.example.app
# ... capture frames ...
adb shell am stop-profiling
```

3. Submit PR with performance benchmarks in commit message.

---

## Review Process

1. **CI checks:** Your PR must pass GitHub Actions (linting, type checks)
2. **Code review:** Maintainers will review within 5 business days
3. **Testing:** If changes affect Kotlin, we'll test on multiple devices
4. **Merge:** Upon approval, a maintainer will merge and tag a new release

---

## Release Process

Maintainers only. When releasing:

```bash
# Bump version in package.json
npm version patch|minor|major

# Push tag
git push origin v<version>

# Publish to npm
npm publish
```

---

## Questions?

- Open an issue with the `question` label
- Check existing issues/discussions first
- Be specific and include reproduction steps

---

**Thank you for contributing to forensic-grade frame extraction.** Your work helps investigators, security teams, and compliance teams worldwide. 🔍