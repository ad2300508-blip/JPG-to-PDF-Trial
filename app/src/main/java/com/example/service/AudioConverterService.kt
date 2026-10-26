package com.example.service

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.example.model.AudioConversionSettings
import com.example.model.AudioConvertItem
import com.example.model.AudioOutputFormat
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Audio pipeline that never keeps the whole track in RAM:
 * decode → 16-bit PCM temp file (trim + channel mix applied) → optional normalisation → WAV or AAC/M4A.
 * Decoder and encoder are fully drained, so the end of the recording is never cut off.
 */
class AudioConverterService(private val context: Context) {

    private data class PcmInfo(val sampleRate: Int, val channels: Int, val peak: Int)

    suspend fun transcodeAudio(
        item: AudioConvertItem,
        settings: AudioConversionSettings,
        onProgress: (Float) -> Unit
    ): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        resultOf {
            val pcmFile = File(context.cacheDir, "pcm_${System.nanoTime()}.raw")
            val encodedFile = File(context.cacheDir, "enc_${System.nanoTime()}.m4a")
            try {
                val info = decodeToPcm(item, settings.forceMono, pcmFile) { onProgress(it * 0.6f) }
                if (pcmFile.length() == 0L) throw IllegalStateException("Nessun audio decodificato: il file potrebbe essere danneggiato")

                val gain = if (settings.normalizeVolume && info.peak > 0) {
                    (TARGET_PEAK / info.peak.toFloat()).coerceIn(1f, MAX_GAIN)
                } else 1f

                val baseName = FileNames.sanitize(FileNames.baseName(item.name), "Audio")
                when (settings.outputFormat) {
                    AudioOutputFormat.WAV_PCM_16BIT -> {
                        val dataSize = pcmFile.length()
                        val uri = OutputStorage.save(context, "${baseName}_convertito.wav", "audio/wav", OutputFolder.AUDIO) { out ->
                            out.write(wavHeader(dataSize, info.sampleRate, info.channels))
                            copyPcm(pcmFile, out, gain) { onProgress(0.6f + it * 0.4f) }
                        }
                        onProgress(1f)
                        uri to (dataSize + 44)
                    }
                    AudioOutputFormat.M4A_AAC -> {
                        encodeAac(pcmFile, encodedFile, info, settings.bitrate.kbps * 1000, gain) { onProgress(0.6f + it * 0.38f) }
                        val size = encodedFile.length()
                        val uri = OutputStorage.save(context, "${baseName}_convertito.m4a", "audio/mp4", OutputFolder.AUDIO) { out ->
                            encodedFile.inputStream().use { it.copyTo(out) }
                        }
                        onProgress(1f)
                        uri to size
                    }
                }
            } finally {
                pcmFile.delete()
                encodedFile.delete()
            }
        }
    }

    private suspend fun decodeToPcm(
        item: AudioConvertItem,
        forceMono: Boolean,
        pcmFile: File,
        onProgress: (Float) -> Unit
    ): PcmInfo {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(context, item.uri, null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IllegalStateException("Nessuna traccia audio nel file")

            val inputFormat = extractor.getTrackFormat(trackIndex)
            extractor.selectTrack(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)!!
            var sampleRate = inputFormat.intOr(MediaFormat.KEY_SAMPLE_RATE, 44100)
            var channels = inputFormat.intOr(MediaFormat.KEY_CHANNEL_COUNT, 2)
            var floatPcm = false
            val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) inputFormat.getLong(MediaFormat.KEY_DURATION) else item.durationMs * 1000

            val startUs = (item.trimStartMs ?: 0L) * 1000
            val endUs = item.trimEndMs?.let { it * 1000 } ?: Long.MAX_VALUE
            if (startUs > 0) extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val windowUs = ((minOf(endUs, durationUs.takeIf { it > 0 } ?: endUs)) - startUs).coerceAtLeast(1)

            val codec = MediaCodec.createDecoderByType(mime)
            decoder = codec
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            var peak = 0
            var outChannels = if (forceMono) 1 else min(channels, 2)
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            BufferedOutputStream(FileOutputStream(pcmFile), 256 * 1024).use { out ->
                var chunk = ByteArray(0)
                while (!outputDone) {
                    coroutineContext.ensureActive()
                    if (!inputDone) {
                        val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                        if (inIndex >= 0) {
                            val inBuffer = codec.getInputBuffer(inIndex)!!
                            val size = extractor.readSampleData(inBuffer, 0)
                            if (size < 0 || extractor.sampleTime > endUs) {
                                codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val outIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val format = codec.outputFormat
                            sampleRate = format.intOr(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                            channels = format.intOr(MediaFormat.KEY_CHANNEL_COUNT, channels)
                            floatPcm = format.intOr(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT) == AudioFormat.ENCODING_PCM_FLOAT
                            outChannels = if (forceMono) 1 else min(channels, 2)
                        }
                        outIndex >= 0 -> {
                            val pts = bufferInfo.presentationTimeUs
                            if (bufferInfo.size > 0 && pts >= startUs && pts <= endUs) {
                                val outBuffer = codec.getOutputBuffer(outIndex)!!
                                outBuffer.position(bufferInfo.offset)
                                outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                if (chunk.size < bufferInfo.size) chunk = ByteArray(bufferInfo.size)
                                outBuffer.get(chunk, 0, bufferInfo.size)
                                val converted = mixToPcm16(chunk, bufferInfo.size, channels, outChannels, floatPcm)
                                peak = max(peak, peakOf(converted))
                                out.write(converted)
                                onProgress(((pts - startUs).toFloat() / windowUs).coerceIn(0f, 1f))
                            }
                            codec.releaseOutputBuffer(outIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 || pts > endUs) {
                                outputDone = true
                            }
                        }
                    }
                }
            }
            return PcmInfo(sampleRate, outChannels, peak)
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            extractor.release()
        }
    }

    /** Converts one decoded buffer (16-bit or float, N channels) to 16-bit little endian with [dstChannels]. */
    private fun mixToPcm16(src: ByteArray, length: Int, srcChannels: Int, dstChannels: Int, isFloat: Boolean): ByteArray {
        val bytesPerSample = if (isFloat) 4 else 2
        val channels = srcChannels.coerceAtLeast(1)
        val frames = length / (bytesPerSample * channels)
        val input = ByteBuffer.wrap(src, 0, length).order(ByteOrder.LITTLE_ENDIAN)
        val output = ByteBuffer.allocate(frames * dstChannels * 2).order(ByteOrder.LITTLE_ENDIAN)
        val frame = IntArray(channels)
        repeat(frames) {
            for (c in 0 until channels) {
                frame[c] = if (isFloat) {
                    (input.float.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt()
                } else {
                    input.short.toInt()
                }
            }
            if (dstChannels == 1) {
                output.putShort((frame.sum() / channels).toShort())
            } else {
                val left = frame[0]
                val right = if (channels > 1) frame[1] else frame[0]
                output.putShort(left.toShort())
                output.putShort(right.toShort())
            }
        }
        return output.array()
    }

    private fun peakOf(pcm: ByteArray): Int {
        var peak = 0
        var i = 0
        while (i + 1 < pcm.size) {
            val sample = (pcm[i].toInt() and 0xFF) or (pcm[i + 1].toInt() shl 8)
            peak = max(peak, abs(sample.toShort().toInt()))
            i += 2
        }
        return peak
    }

    private suspend fun copyPcm(pcmFile: File, out: OutputStream, gain: Float, onProgress: (Float) -> Unit) {
        val total = pcmFile.length().coerceAtLeast(1)
        var done = 0L
        val buffer = ByteArray(64 * 1024)
        BufferedInputStream(FileInputStream(pcmFile)).use { input ->
            while (true) {
                coroutineContext.ensureActive()
                val read = readFully(input, buffer)
                if (read <= 0) break
                applyGain(buffer, read, gain)
                out.write(buffer, 0, read)
                done += read
                onProgress(done.toFloat() / total)
            }
        }
    }

    private fun applyGain(buffer: ByteArray, length: Int, gain: Float) {
        if (gain == 1f) return
        var i = 0
        while (i + 1 < length) {
            val sample = ((buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)).toShort()
            val boosted = (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = (boosted and 0xFF).toByte()
            buffer[i + 1] = ((boosted shr 8) and 0xFF).toByte()
            i += 2
        }
    }

    /** Reads until the buffer is full or the stream ends, keeping 16-bit samples aligned. */
    private fun readFully(input: InputStream, buffer: ByteArray, limit: Int = buffer.size): Int {
        var total = 0
        while (total < limit) {
            val read = input.read(buffer, total, limit - total)
            if (read < 0) break
            total += read
        }
        return total
    }

    private suspend fun encodeAac(
        pcmFile: File,
        outFile: File,
        info: PcmInfo,
        bitrate: Int,
        gain: Float,
        onProgress: (Float) -> Unit
    ) {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, info.sampleRate, info.channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val muxer = try {
            MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            encoder.release()
            throw e
        }
        var muxerStarted = false
        try {
            try {
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            } catch (e: Exception) {
                throw IllegalStateException("Frequenza ${info.sampleRate} Hz non supportata dall'encoder AAC: prova il formato WAV")
            }
            encoder.start()

            val frameBytes = info.channels * 2
            val total = pcmFile.length().coerceAtLeast(1)
            val bufferInfo = MediaCodec.BufferInfo()
            val chunk = ByteArray(32 * 1024 - (32 * 1024) % frameBytes)
            var trackIndex = -1
            var queuedBytes = 0L
            var inputDone = false
            var outputDone = false

            BufferedInputStream(FileInputStream(pcmFile), 256 * 1024).use { input ->
                while (!outputDone) {
                    coroutineContext.ensureActive()
                    if (!inputDone) {
                        val inIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                        if (inIndex >= 0) {
                            val inBuffer = encoder.getInputBuffer(inIndex)!!
                            inBuffer.clear()
                            val capacity = min(inBuffer.remaining(), chunk.size)
                            val read = readFully(input, chunk, capacity - capacity % frameBytes)
                            val presentationUs = queuedBytes * 1_000_000L / (info.sampleRate.toLong() * frameBytes)
                            if (read <= 0) {
                                encoder.queueInputBuffer(inIndex, 0, 0, presentationUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                applyGain(chunk, read, gain)
                                inBuffer.put(chunk, 0, read)
                                encoder.queueInputBuffer(inIndex, 0, read, presentationUs, 0)
                                queuedBytes += read
                                onProgress(queuedBytes.toFloat() / total)
                            }
                        }
                    }

                    val outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outIndex >= 0 -> {
                            val outBuffer = encoder.getOutputBuffer(outIndex)!!
                            val isConfig = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                            if (!isConfig && bufferInfo.size > 0 && muxerStarted) {
                                outBuffer.position(bufferInfo.offset)
                                outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, outBuffer, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(outIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                        }
                    }
                }
            }
            if (!muxerStarted) throw IllegalStateException("La codifica AAC non ha prodotto audio")
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            runCatching { if (muxerStarted) muxer.stop() }
            runCatching { muxer.release() }
        }
    }

    private fun wavHeader(dataSize: Long, sampleRate: Int, channels: Int): ByteArray {
        val size = dataSize.coerceAtMost(Int.MAX_VALUE.toLong() - 36).toInt()
        val byteRate = sampleRate * channels * 2
        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(size + 36)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1.toShort())
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((channels * 2).toShort())
            putShort(16.toShort())
            put("data".toByteArray())
            putInt(size)
        }.array()
    }

    private fun MediaFormat.intOr(key: String, default: Int): Int =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrDefault(default) else default

    private companion object {
        const val TIMEOUT_US = 10_000L
        const val TARGET_PEAK = 32_000f // ≈ -0.2 dBFS
        const val MAX_GAIN = 8f
    }
}
