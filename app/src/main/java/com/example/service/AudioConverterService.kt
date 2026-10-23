package com.example.service

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.AudioBitrate
import com.example.model.AudioLabItem
import com.example.model.AudioLabSettings
import com.example.model.AudioOutputMode
import com.example.util.DownloadUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

object AudioConverterService {

  private const val TIMEOUT_US = 10_000L

  suspend fun extractMetadata(context: Context, uri: Uri): AudioLabItem = withContext(Dispatchers.IO) {
    var fileName = "audio_${System.currentTimeMillis()}"
    var fileSize = 0L

    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (nameIndex != -1) {
            val name = cursor.getString(nameIndex)
            if (!name.isNullOrBlank()) fileName = name
          }
          if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
            fileSize = cursor.getLong(sizeIndex)
          }
        }
      }
    } catch (_: Exception) { }

    var durationMs = 0L
    var sampleRate = 44100
    var channels = 2
    var mime = context.contentResolver.getType(uri) ?: "audio/*"

    try {
      val retriever = MediaMetadataRetriever()
      val pfd = context.contentResolver.openFileDescriptor(uri, "r")
      pfd?.use {
        retriever.setDataSource(it.fileDescriptor)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
          durationMs = it
        }
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let {
          mime = it
        }
      }
      retriever.release()
    } catch (_: Exception) { }

    // Inspect track format for exact sample rate & channels
    try {
      val extractor = MediaExtractor()
      val pfd = context.contentResolver.openFileDescriptor(uri, "r")
      pfd?.use {
        extractor.setDataSource(it.fileDescriptor)
        for (i in 0 until extractor.trackCount) {
          val format = extractor.getTrackFormat(i)
          val trackMime = format.getString(MediaFormat.KEY_MIME) ?: ""
          if (trackMime.startsWith("audio/")) {
            if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
              sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            }
            if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
              channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            }
            break
          }
        }
      }
      extractor.release()
    } catch (_: Exception) { }

    AudioLabItem(
      id = UUID.randomUUID().toString(),
      uri = uri,
      fileName = fileName,
      durationMs = durationMs,
      sizeBytes = fileSize,
      sampleRate = sampleRate,
      channels = channels,
      mimeType = mime
    )
  }

  suspend fun transcodeAudio(
    context: Context,
    item: AudioLabItem,
    settings: AudioLabSettings
  ): Result<AudioLabItem> = withContext(Dispatchers.IO) {
    try {
      val pfd = context.contentResolver.openFileDescriptor(item.uri, "r")
        ?: throw IllegalStateException("Could not open file descriptor for ${item.fileName}")

      val baseName = item.fileName.substringBeforeLast(".")
      val outputFile = File(
        context.cacheDir,
        "omni_${baseName}_${System.currentTimeMillis()}.${settings.mode.extension}"
      )

      pfd.use { descriptor ->
        // Step 1: Decode input audio to raw PCM temporary file
        val tempPcmFile = File(context.cacheDir, "raw_pcm_${UUID.randomUUID()}.pcm")
        var decodedSampleRate = if (item.sampleRate > 0) item.sampleRate else 44100
        var decodedChannels = if (item.channels > 0) item.channels else 2

        try {
          decodeAudioToPcm(
            fileDescriptor = descriptor.fileDescriptor,
            outputPcmFile = tempPcmFile,
            forceMono = settings.forceMono,
            onFormatResolved = { sr, ch ->
              decodedSampleRate = sr
              decodedChannels = ch
            }
          )

          // Step 2: Convert PCM file to target format (WAV or M4A/AAC)
          val outChannels = if (settings.forceMono) 1 else decodedChannels

          if (settings.mode == AudioOutputMode.LOSSLESS_WAV_PCM) {
            writeWavFile(tempPcmFile, outputFile, decodedSampleRate, outChannels)
          } else {
            encodePcmToAac(
              inputPcmFile = tempPcmFile,
              outputM4aFile = outputFile,
              sampleRate = decodedSampleRate,
              channels = outChannels,
              bitrate = settings.bitrate.bps
            )
          }
        } finally {
          if (tempPcmFile.exists()) tempPcmFile.delete()
        }
      }

      Result.success(
        item.copy(
          convertedFile = outputFile,
          convertedSizeBytes = outputFile.length(),
          isProcessing = false,
          error = null
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun decodeAudioToPcm(
    fileDescriptor: java.io.FileDescriptor,
    outputPcmFile: File,
    forceMono: Boolean,
    onFormatResolved: (sampleRate: Int, channels: Int) -> Unit
  ) {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    var outStream: FileOutputStream? = null

    try {
      extractor.setDataSource(fileDescriptor)
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
        throw IllegalStateException("No audio track found in media file")
      }

      extractor.selectTrack(audioTrackIndex)
      val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
      decoder = MediaCodec.createDecoderByType(mime)
      decoder.configure(inputFormat, null, null, 0)
      decoder.start()

      outStream = FileOutputStream(outputPcmFile)

      val info = MediaCodec.BufferInfo()
      var isInputEos = false
      var isOutputEos = false

      var actualSampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
        inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      } else 44100

      var actualChannels = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
        inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      } else 2

      onFormatResolved(actualSampleRate, actualChannels)

      while (!isOutputEos) {
        if (!isInputEos) {
          val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
          if (inIndex >= 0) {
            val inputBuffer = decoder.getInputBuffer(inIndex)
            if (inputBuffer != null) {
              val sampleSize = extractor.readSampleData(inputBuffer, 0)
              if (sampleSize < 0) {
                decoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                isInputEos = true
              } else {
                decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                extractor.advance()
              }
            }
          }
        }

        val outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
        if (outIndex >= 0) {
          val outputBuffer = decoder.getOutputBuffer(outIndex)
          if (outputBuffer != null && info.size > 0) {
            outputBuffer.position(info.offset)
            outputBuffer.limit(info.offset + info.size)

            if (forceMono && actualChannels == 2) {
              // Downmix 16-bit stereo PCM to mono
              outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
              val monoBytes = ByteArray(info.size / 2)
              var writeIdx = 0
              while (outputBuffer.remaining() >= 4) {
                val left = outputBuffer.short
                val right = outputBuffer.short
                val mono = ((left.toInt() + right.toInt()) / 2).toShort()
                monoBytes[writeIdx++] = (mono.toInt() and 0xFF).toByte()
                monoBytes[writeIdx++] = ((mono.toInt() shr 8) and 0xFF).toByte()
              }
              outStream.write(monoBytes, 0, writeIdx)
            } else {
              val chunk = ByteArray(info.size)
              outputBuffer.get(chunk)
              outStream.write(chunk)
            }
          }

          decoder.releaseOutputBuffer(outIndex, false)

          if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            isOutputEos = true
          }
        } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          val newFormat = decoder.outputFormat
          if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            actualSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
          }
          if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            actualChannels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
          }
          onFormatResolved(actualSampleRate, actualChannels)
        }
      }
    } finally {
      try { outStream?.flush(); outStream?.close() } catch (_: Exception) { }
      try { decoder?.stop(); decoder?.release() } catch (_: Exception) { }
      try { extractor.release() } catch (_: Exception) { }
    }
  }

  private fun writeWavFile(pcmFile: File, wavFile: File, sampleRate: Int, channels: Int) {
    val pcmLength = pcmFile.length()
    val totalDataLen = pcmLength + 36
    val byteRate = sampleRate * channels * 2
    val blockAlign = channels * 2

    FileOutputStream(wavFile).use { out ->
      val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
      header.put("RIFF".toByteArray())
      header.putInt(totalDataLen.toInt())
      header.put("WAVE".toByteArray())
      header.put("fmt ".toByteArray())
      header.putInt(16) // Subchunk1Size for PCM
      header.putShort(1) // AudioFormat 1 = PCM
      header.putShort(channels.toShort())
      header.putInt(sampleRate)
      header.putInt(byteRate)
      header.putShort(blockAlign.toShort())
      header.putShort(16) // BitsPerSample 16
      header.put("data".toByteArray())
      header.putInt(pcmLength.toInt())

      out.write(header.array())

      // Stream raw PCM payload
      pcmFile.inputStream().use { input ->
        input.copyTo(out)
      }
      out.flush()
    }
  }

  private fun encodePcmToAac(
    inputPcmFile: File,
    outputM4aFile: File,
    sampleRate: Int,
    channels: Int,
    bitrate: Int
  ) {
    val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
    val muxer = MediaMuxer(outputM4aFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    var trackIndex = -1
    var muxerStarted = false

    try {
      val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
        setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
      }

      encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      encoder.start()

      val pcmStream = inputPcmFile.inputStream()
      val buffer = ByteArray(4096)
      var inputEos = false
      var outputEos = false
      val info = MediaCodec.BufferInfo()
      var presentationTimeUs = 0L

      while (!outputEos) {
        if (!inputEos) {
          val inIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
          if (inIndex >= 0) {
            val inputBuffer = encoder.getInputBuffer(inIndex)
            if (inputBuffer != null) {
              val bytesRead = pcmStream.read(buffer)
              if (bytesRead <= 0) {
                encoder.queueInputBuffer(inIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                inputEos = true
              } else {
                inputBuffer.clear()
                inputBuffer.put(buffer, 0, bytesRead)
                encoder.queueInputBuffer(inIndex, 0, bytesRead, presentationTimeUs, 0)

                // Advance presentation time by samples processed
                val samplesPerFrame = bytesRead / (channels * 2)
                presentationTimeUs += (samplesPerFrame * 1_000_000L) / sampleRate
              }
            }
          }
        }

        val outIndex = encoder.dequeueOutputBuffer(info, TIMEOUT_US)
        if (outIndex >= 0) {
          val outputBuffer = encoder.getOutputBuffer(outIndex)
          if (outputBuffer != null && (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && info.size > 0) {
            if (muxerStarted) {
              outputBuffer.position(info.offset)
              outputBuffer.limit(info.offset + info.size)
              muxer.writeSampleData(trackIndex, outputBuffer, info)
            }
          }

          encoder.releaseOutputBuffer(outIndex, false)

          if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            outputEos = true
          }
        } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          if (!muxerStarted) {
            val newFormat = encoder.outputFormat
            trackIndex = muxer.addTrack(newFormat)
            muxer.start()
            muxerStarted = true
          }
        }
      }

      pcmStream.close()
    } finally {
      try {
        if (muxerStarted) {
          muxer.stop()
        }
        muxer.release()
      } catch (_: Exception) { }
      try {
        encoder.stop()
        encoder.release()
      } catch (_: Exception) { }
    }
  }

  suspend fun exportBatchToDownloads(
    context: Context,
    items: List<AudioLabItem>
  ): List<DownloadUtils.DownloadResult> = withContext(Dispatchers.IO) {
    val results = mutableListOf<DownloadUtils.DownloadResult>()
    for (item in items) {
      val file = item.convertedFile ?: continue
      val mime = if (file.extension.equals("wav", ignoreCase = true)) "audio/wav" else "audio/mp4"
      val desiredName = file.name
      val saveResult = DownloadUtils.saveFileToDownloads(
        context = context,
        sourceFile = file,
        desiredFileName = desiredName,
        mimeType = mime,
        subfolder = "OmniConvert/Audio"
      )
      saveResult.getOrNull()?.let { results.add(it) }
    }
    results
  }
}
