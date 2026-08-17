# react-native-exact-frame-extractor

A high-performance, hardware-accelerated Android video frame extractor for React Native and Expo. Extract exact, frame-accurate images from video files at any given timestamp without keyframe rounding or snapping.

---

## The Problem

Standard video thumbnail solutions on Android (such as `expo-video-thumbnails` or standard `MediaMetadataRetriever` wrappers) rely on `OPTION_CLOSEST_SYNC` under the hood. In web and screen-recorded videos where I-frames (keyframes) may only appear every 5–10 seconds, requesting a frame at `00:07` often snaps back to the nearest keyframe at `00:02`, returning the wrong image.

`react-native-exact-frame-extractor` uses low-level Android `MediaExtractor` and `MediaCodec` pipelines to seek to the prior sync point and forward-decode intermediate B/P-frames directly up to the requested millisecond, guaranteeing pixel-accurate frame capture.

---

## Features

* **Frame-Accurate Extraction:** Eliminates keyframe snapping; decodes the exact millisecond frame requested.
* **Hardware-Accelerated:** Leverages native Android `MediaCodec` and direct YUV-to-JPEG conversion via `YuvImage`.
* **Non-Blocking Execution:** Runs extraction on background coroutines (`Dispatchers.IO`) without freezing UI or video playback.
* **TypeScript Support:** Out-of-the-box typings for clean integration.
* **Expo & React Native Compatible:** Works with bare React Native and Expo projects using custom native builds / Dev Clients.

---

## Installation

```bash
npm install react-native-exact-frame-extractor

```

*or using Yarn:*

```bash
yarn add react-native-exact-frame-extractor

```

---

## Setup & Linking

### Expo Projects (Config Plugins / Dev Client)

1. Prebuild your native project if not already using a custom development client:
```bash
npx expo prebuild

```


2. Recompile your Android application:
```bash
npx expo run:android

```



### Bare React Native Projects

If autolinking is enabled (React Native 0.60+), the package links automatically.

If manual registration is required, add the package to `MainApplication.kt`:

```kotlin
import com.framecapture.FrameCapturePackage

override fun getPackages(): List<ReactPackage> =
    PackageList(this).packages.apply {
        add(FrameCapturePackage())
    }

```

---

## Usage

```typescript
import { captureFrameAtTime } from 'react-native-exact-frame-extractor';
import * as FileSystem from 'expo-file-system/legacy';

async function extractTimestampFrame(videoUri: string, targetTimeInSeconds: number) {
  try {
    const timeMillis = Math.floor(targetTimeInSeconds * 1000);
    const outputPath = `${FileSystem.cacheDirectory}frame_${Date.now()}.jpg`;

    const result = await captureFrameAtTime({
      videoUri,
      timeMillis,
      outputPath,
    });

    console.log('Saved frame URI:', result.path);
    return result.path;
  } catch (error) {
    console.error('Frame capture failed:', error);
  }
}

```

---

## API Reference

### `captureFrameAtTime(options: FrameCaptureOptions): Promise<FrameCaptureResponse>`

#### Parameters (`FrameCaptureOptions`)

| Property | Type | Description |
| --- | --- | --- |
| `videoUri` | `string` | The `file://` or `content://` URI of the video file. |
| `timeMillis` | `number` | The target position in milliseconds ($t_{\text{ms}} = t_{\text{sec}} \times 1000$). |
| `outputPath` | `string` | The target absolute file path to store the extracted JPEG image. |

#### Return Value (`FrameCaptureResponse`)

| Property | Type | Description |
| --- | --- | --- |
| `success` | `boolean` | `true` if extraction and saving succeeded. |
| `path` | `string` | The absolute `file://` URI to the saved JPEG image. |

---

## Platform Support

| Platform | Supported | Notes |
| --- | --- | --- |
| **Android** | Yes | Hardware-accelerated decoding via `MediaCodec` + `MediaExtractor` |
| **iOS** | Planned | Native `AVAssetImageGenerator` implementation in progress |

---

## License

This project is licensed under the **MIT License** — see the [LICENSE]([LICENSE](https://opensource.org/license/mit)) file for full details. 

You are free to use, modify, distribute, and integrate this software in both personal and commercial projects, provided the original copyright and permission notice are retained.
