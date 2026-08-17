import { NativeModules, Platform } from 'react-native';

const { FrameCaptureModule } = NativeModules;

export interface FrameCaptureOptions {
  videoUri: string;
  timeMillis: number;
  outputPath: string;
}

export interface FrameCaptureResponse {
  success: boolean;
  path: string;
}

/**
 * Extracts a decoded exact video frame at the target millisecond timestamp.
 * Bypasses keyframe/I-frame snapping on Android.
 */
export async function captureFrameAtTime(
  options: FrameCaptureOptions
): Promise<FrameCaptureResponse> {
  if (Platform.OS !== 'android') {
    throw new Error('Exact frame extraction is currently supported on Android.');
  }

  if (!FrameCaptureModule) {
    throw new Error(
      'FrameCaptureModule is not linked. Ensure the native package is installed and registered.'
    );
  }

  const cleanPath = options.outputPath.replace('file://', '');
  return await FrameCaptureModule.captureFrameAtTime(
    options.videoUri,
    options.timeMillis,
    cleanPath
  );
}

export default {
  captureFrameAtTime,
};