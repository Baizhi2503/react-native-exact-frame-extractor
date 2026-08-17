package com.siinvestigation

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

@ReactModule(name = "FrameCaptureModule")
class FrameCaptureModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val TAG = "FrameCaptureModule"
        private const val NAME = "FrameCaptureModule"
    }

    override fun getName(): String = NAME

    @ReactMethod
    fun captureFrameAtTime(
        videoUri: String,
        timeMillis: Double,
        outputPath: String,
        promise: Promise
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val targetUs = (timeMillis * 1000).toLong()
                val success = extractExactFrameToJpeg(videoUri, targetUs, outputPath)
                if (success) {
                    val map = Arguments.createMap()
                    map.putBoolean("success", true)
                    map.putString("path", "file://$outputPath")
                    promise.resolve(map)
                } else {
                    promise.reject("FRAME_NOT_FOUND", "Could not decode frame at ${timeMillis}ms")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Frame capture error", e)
                promise.reject("CAPTURE_ERROR", e.message ?: "Unknown decode error")
            }
        }
    }

    private fun extractExactFrameToJpeg(videoUri: String, targetTimeUs: Long, outputPath: String): Boolean {
        val mediaExtractor = MediaExtractor()
        var mediaCodec: MediaCodec? = null

        return try {
            val uri = Uri.parse(videoUri)
            mediaExtractor.setDataSource(reactContext, uri, null)

            var videoTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until mediaExtractor.trackCount) {
                val fmt = mediaExtractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    format = fmt
                    break
                }
            }

            if (videoTrackIndex < 0 || format == null) {
                Log.e(TAG, "No video track found")
                return false
            }

            mediaExtractor.selectTrack(videoTrackIndex)
            val mimeType = format.getString(MediaFormat.KEY_MIME)!!
            mediaCodec = MediaCodec.createDecoderByType(mimeType)
            mediaCodec.configure(format, null, null, 0)
            mediaCodec.start()

            // Seek to previous keyframe, then decode forward until reaching exact target timestamp
            mediaExtractor.seekTo(targetTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false
            var decodedTarget = false
            var iterations = 0
            val maxIterations = 500

            while (!decodedTarget && iterations < maxIterations) {
                iterations++

                if (!isEOS) {
                    val inputIndex = mediaCodec.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = mediaCodec.getInputBuffer(inputIndex)!!
                        val sampleSize = mediaExtractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            mediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            mediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, mediaExtractor.sampleTime, 0)
                            mediaExtractor.advance()
                        }
                    }
                }

                val outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10_000L)
                if (outputIndex >= 0) {
                    val presentationTimeUs = bufferInfo.presentationTimeUs

                    if (presentationTimeUs >= targetTimeUs || (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        val image = mediaCodec.getOutputImage(outputIndex)
                        if (image != null) {
                            saveYUVImageToJpeg(image, outputPath)
                            image.close()
                            decodedTarget = true
                        }
                        mediaCodec.releaseOutputBuffer(outputIndex, false)
                        break
                    }

                    mediaCodec.releaseOutputBuffer(outputIndex, false)
                }
            }

            decodedTarget
        } catch (e: Exception) {
            Log.e(TAG, "Extraction loop failed", e)
            false
        } finally {
            try {
                mediaCodec?.stop()
                mediaCodec?.release()
            } catch (ignored: Exception) {}
            try {
                mediaExtractor.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun saveYUVImageToJpeg(image: Image, outputPath: String) {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)

        val vPixelStride = image.planes[2].pixelStride
        val vRowStride = image.planes[2].rowStride

        var offset = ySize
        val width = image.width
        val height = image.height

        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vIndex = row * vRowStride + col * vPixelStride
                val uIndex = row * image.planes[1].rowStride + col * image.planes[1].pixelStride
                nv21[offset++] = vBuffer.get(vIndex)
                nv21[offset++] = uBuffer.get(uIndex)
            }
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val outFile = File(outputPath)
        outFile.parentFile?.mkdirs()

        FileOutputStream(outFile).use { fos ->
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 95, fos)
            fos.flush()
        }
    }
}