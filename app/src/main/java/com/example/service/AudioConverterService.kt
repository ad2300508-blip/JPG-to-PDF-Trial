package com.example.service

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.AudioBitrate
import com.example.model.AudioConversionSettings
import com.example.model.AudioConvertItem
import com.example.model.AudioOutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioConverterService(private val context: Context) {

    suspend fun transcodeAudio(
        item: AudioConvertItem,
        settings: AudioConversionSettings,
        onProgress: (Float) -> Unit
    ): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(item.uri, "r")
                ?: return@withContext Result.failure(Exception("Cannot open audio descriptor"))

            val extractor = MediaExtractor()
            extractor.setDataSource(pfd.fileDescriptor)

            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    inputFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || inputFormat == null) {
                pfd.close()
                return@withContext Result.failure(Exception("No valid audio track found in file"))
            }

            extractor.selectTrack(audioTrackIndex)
            val inputMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: ""
            val sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            val channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
            val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) inputFormat.getLong(MediaFormat.KEY_DURATION) else 1000000L

            val decoder = MediaCodec.createDecoderByType(inputMime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val pcmStream = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false

            while (!isEos) {
                val inIndex = decoder.dequeueInputBuffer(10000)
                if (inIndex >= 0) {
                    val inBuffer = decoder.getInputBuffer(inIndex)
                    if (inBuffer != null) {
                        val sampleSize = extractor.readSampleData(inBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEos = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                            val progress = (extractor.sampleTime.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
                            onProgress(progress * 0.5f)
                        }
                    }
                }

                var outIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                while (outIndex >= 0) {
                    val outBuffer = decoder.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0) {
                        val chunk = ByteArray(bufferInfo.size)
                        outBuffer.get(chunk)
                        pcmStream.write(chunk)
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEos = true
                        break
                    }
                    outIndex = decoder.dequeueOutputBuffer(bufferInfo, 0)
                }
            }

            decoder.stop()
            decoder.release()
            extractor.release()
            pfd.close()

            val rawPcm = pcmStream.toByteArray()
            val baseName = item.name.substringBeforeLast(".")

            val (outBytes, mimeType, outExtension) = if (settings.outputFormat == AudioOutputFormat.WAV_PCM_16BIT) {
                val wavHeader = createWavHeader(rawPcm.size, sampleRate, channelCount)
                val fullWav = ByteArray(wavHeader.size + rawPcm.size)
                System.arraycopy(wavHeader, 0, fullWav, 0, wavHeader.size)
                System.arraycopy(rawPcm, 0, fullWav, wavHeader.size, rawPcm.size)
                Triple(fullWav, "audio/wav", "wav")
            } else {
                // Lossy AAC / M4A encoding
                val targetChannels = if (settings.forceMono) 1 else channelCount
                val aacBytes = encodePcmToAac(rawPcm, sampleRate, targetChannels, settings.bitrate.kbps * 1000)
                Triple(aacBytes, "audio/mp4", "m4a")
            }

            val outFilename = "${baseName}_converted.$outExtension"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, outFilename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OmniConvert/Audio")
                }
            }

            val outputUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Could not create audio MediaStore entry"))

            resolver.openOutputStream(outputUri)?.use { out ->
                out.write(outBytes)
            }

            onProgress(1.0f)
            Result.success(Pair(outputUri, outBytes.size.toLong()))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun createWavHeader(pcmDataSize: Int, sampleRate: Int, channels: Int): ByteArray {
        val header = ByteArray(44)
        val totalDataLen = pcmDataSize + 36
        val byteRate = sampleRate * channels * 2
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        bb.put("RIFF".toByteArray())
        bb.putInt(totalDataLen)
        bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray())
        bb.putInt(16) // Subchunk1Size
        bb.putShort(1.toShort()) // AudioFormat (1 = PCM)
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(byteRate)
        bb.putShort((channels * 2).toShort()) // BlockAlign
        bb.putShort(16.toShort()) // BitsPerSample
        bb.put("data".toByteArray())
        bb.putInt(pcmDataSize)

        return header
    }

    private fun encodePcmToAac(pcmData: ByteArray, sampleRate: Int, channels: Int, bitrate: Int): ByteArray {
        val tempFile = File(context.cacheDir, "temp_encoded_${System.currentTimeMillis()}.m4a")
        val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var muxerTrackIndex = -1
        var muxerStarted = false
        var pcmOffset = 0
        var isEos = false

        while (!isEos) {
            val inIndex = encoder.dequeueInputBuffer(10000)
            if (inIndex >= 0) {
                val inBuffer = encoder.getInputBuffer(inIndex)
                if (inBuffer != null) {
                    val remaining = pcmData.size - pcmOffset
                    if (remaining <= 0) {
                        encoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEos = true
                    } else {
                        val chunkSize = minOf(inBuffer.capacity(), remaining)
                        inBuffer.clear()
                        inBuffer.put(pcmData, pcmOffset, chunkSize)
                        pcmOffset += chunkSize
                        encoder.queueInputBuffer(inIndex, 0, chunkSize, (pcmOffset * 1000000L) / (sampleRate * channels * 2), 0)
                    }
                }
            }

            var outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
            while (outIndex >= 0) {
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        muxerTrackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                } else if (outIndex >= 0) {
                    val outBuffer = encoder.getOutputBuffer(outIndex)
                    if (outBuffer != null && muxerStarted && bufferInfo.size > 0) {
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(muxerTrackIndex, outBuffer, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEos = true
                        break
                    }
                }
                outIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
            }
        }

        encoder.stop()
        encoder.release()
        if (muxerStarted) {
            muxer.stop()
        }
        muxer.release()

        val resultBytes = tempFile.readBytes()
        tempFile.delete()
        return resultBytes
    }
}
