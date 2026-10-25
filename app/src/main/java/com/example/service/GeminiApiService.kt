package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.GeminiModelTier
import com.example.model.GeminiTargetModel
import com.example.model.ModelQuotaUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiApiService(private val context: Context) {

    private val prefs = context.getSharedPreferences("omni_gemini_prefs", Context.MODE_PRIVATE)
    private val quotaPrefs = context.getSharedPreferences("omni_gemini_quota_prefs", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // --- API Key Vault Management ---

    fun getApiKey(): String {
        val userKey = prefs.getString("custom_gemini_api_key", "") ?: ""
        if (userKey.isNotBlank()) return userKey.trim()

        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }
        return buildKey.trim()
    }

    fun getMaskedApiKey(): String {
        val key = getApiKey()
        if (key.isBlank()) return "Not Configured"
        if (key.length <= 10) return "••••••••"
        return "${key.take(7)}...${key.takeLast(4)}"
    }

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
    }

    fun clearCustomApiKey() {
        prefs.edit().remove("custom_gemini_api_key").apply()
    }

    // --- Quota & Rate Limit Tracking ---

    private fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    }

    private fun checkAndResetDailyQuota() {
        val savedDate = quotaPrefs.getString("quota_date", "")
        val today = getTodayDateKey()
        if (savedDate != today) {
            quotaPrefs.edit().clear().putString("quota_date", today).apply()
        }
    }

    fun getUsedRpd(model: GeminiTargetModel): Int {
        checkAndResetDailyQuota()
        return quotaPrefs.getInt("rpd_${model.technicalModelId}", 0)
    }

    fun recordRequest(model: GeminiTargetModel) {
        checkAndResetDailyQuota()
        val current = getUsedRpd(model)
        quotaPrefs.edit().putInt("rpd_${model.technicalModelId}", current + 1).apply()
    }

    fun markModelQuotaExhausted(model: GeminiTargetModel) {
        checkAndResetDailyQuota()
        quotaPrefs.edit().putBoolean("exhausted_${model.technicalModelId}", true).apply()
    }

    fun isModelAvailable(model: GeminiTargetModel): Boolean {
        checkAndResetDailyQuota()
        val isExhausted = quotaPrefs.getBoolean("exhausted_${model.technicalModelId}", false)
        if (isExhausted) return false
        val used = getUsedRpd(model)
        return used < model.maxRpd
    }

    fun getQuotaUsages(): List<ModelQuotaUsage> {
        checkAndResetDailyQuota()
        return GeminiTargetModel.values().map { model ->
            val used = getUsedRpd(model)
            val isExhausted = quotaPrefs.getBoolean("exhausted_${model.technicalModelId}", false)
            val remaining = (model.maxRpd - used).coerceAtLeast(0)
            ModelQuotaUsage(
                model = model,
                usedRpd = used,
                remainingRpd = if (isExhausted) 0 else remaining,
                isThrottled = isExhausted || used >= model.maxRpd
            )
        }
    }

    fun resetQuotasManually() {
        quotaPrefs.edit().clear().putString("quota_date", getTodayDateKey()).apply()
    }

    // --- Smart Model Fallback Chain ---

    fun getFallbackChain(
        targetModel: GeminiTargetModel,
        hasAudio: Boolean = false,
        hasImage: Boolean = false
    ): List<GeminiTargetModel> {
        val candidates = when {
            hasAudio -> listOf(
                GeminiTargetModel.GEMINI_3_8_TRANSCRIBE,
                GeminiTargetModel.GEMINI_3_8_FLASH,
                GeminiTargetModel.GEMINI_2_5_FLASH,
                GeminiTargetModel.GEMINI_3_8_FLASH_LITE
            )
            hasImage -> listOf(
                GeminiTargetModel.GEMINI_3_8_FLASH_LITE,
                GeminiTargetModel.GEMINI_3_6_FLASH_LITE,
                GeminiTargetModel.GEMINI_3_1_FLASH_LITE,
                GeminiTargetModel.GEMINI_3_8_FLASH,
                GeminiTargetModel.GEMINI_3_6_FLASH,
                GeminiTargetModel.GEMINI_2_5_FLASH
            )
            targetModel.tier == GeminiModelTier.DEEP_REASONING -> listOf(
                GeminiTargetModel.GEMINI_3_8_FLASH,
                GeminiTargetModel.GEMINI_3_6_FLASH,
                GeminiTargetModel.GEMINI_2_5_FLASH,
                GeminiTargetModel.GEMINI_3_8_FLASH_LITE,
                GeminiTargetModel.GEMINI_3_6_FLASH_LITE,
                GeminiTargetModel.GEMINI_3_1_FLASH_LITE,
                GeminiTargetModel.GEMMA_4_31B_IT,
                GeminiTargetModel.GEMMA_4_26B_IT
            )
            targetModel.tier == GeminiModelTier.ULTRA_QUOTA -> listOf(
                GeminiTargetModel.GEMMA_4_31B_IT,
                GeminiTargetModel.GEMMA_4_26B_IT,
                GeminiTargetModel.GEMINI_3_8_FLASH_LITE,
                GeminiTargetModel.GEMINI_3_6_FLASH_LITE
            )
            else -> listOf(
                GeminiTargetModel.GEMINI_3_8_FLASH_LITE,
                GeminiTargetModel.GEMINI_3_6_FLASH_LITE,
                GeminiTargetModel.GEMINI_3_1_FLASH_LITE,
                GeminiTargetModel.GEMINI_3_8_FLASH,
                GeminiTargetModel.GEMINI_3_6_FLASH,
                GeminiTargetModel.GEMINI_2_5_FLASH,
                GeminiTargetModel.GEMMA_4_31B_IT,
                GeminiTargetModel.GEMMA_4_26B_IT
            )
        }

        // Put the requested model first if present, then available backups
        val ordered = mutableListOf<GeminiTargetModel>()
        if (targetModel in candidates) {
            ordered.add(targetModel)
        }
        candidates.forEach { if (it !in ordered) ordered.add(it) }
        return ordered
    }

    // --- Test Connection ---

    suspend fun testConnection(): Result<Long> = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) {
            return@withContext Result.failure(Exception("API Key not configured. Enter key in Settings or AI Studio Secrets."))
        }

        val startTime = System.currentTimeMillis()
        try {
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "ping"))
                        })
                    })
                })
            }

            val mediaType = "application/json".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash-lite:generateContent?key=$key"

            val request = Request.Builder().url(url).post(body).build()
            val response = okHttpClient.newCall(request).execute()

            val latency = System.currentTimeMillis() - startTime
            if (response.isSuccessful) {
                response.close()
                Result.success(latency)
            } else {
                val err = response.body?.string() ?: "HTTP ${response.code}"
                response.close()
                Result.failure(Exception("HTTP ${response.code}: $err"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    // --- Robust Multi-Model Generation with Auto-Fallback ---

    data class GenerationExecutionResult(
        val finalModelUsed: GeminiTargetModel,
        val fallbackNotice: String? = null
    )

    suspend fun streamGenerateWithFallback(
        requestedModel: GeminiTargetModel,
        prompt: String,
        systemInstruction: String? = null,
        inlineAudioUri: Uri? = null,
        audioMimeType: String? = null,
        inlineImageUri: Uri? = null,
        smartRouting: Boolean = true,
        onFallbackEvent: (fromModel: GeminiTargetModel, toModel: GeminiTargetModel, reason: String) -> Unit = { _, _, _ -> },
        onThinkingChunk: (String) -> Unit = {},
        onTextChunk: (String) -> Unit
    ): Result<GenerationExecutionResult> = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API key is required. Set it in the top header Key Vault or AI Studio Secrets."))
        }

        val modelChain = if (smartRouting) {
            getFallbackChain(
                targetModel = requestedModel,
                hasAudio = inlineAudioUri != null,
                hasImage = inlineImageUri != null
            )
        } else {
            listOf(requestedModel)
        }

        var lastError: Exception? = null
        var fallbackNotice: String? = null

        for (i in modelChain.indices) {
            val candidateModel = modelChain[i]

            // If smart routing is active, check if locally marked exhausted
            if (smartRouting && !isModelAvailable(candidateModel)) {
                Log.d("GeminiApiService", "Skipping ${candidateModel.technicalModelId}: local quota limit reached")
                continue
            }

            if (i > 0) {
                val reason = lastError?.message ?: "Rate limit / service fallback"
                val notice = "Switched to ${candidateModel.displayName} (Auto-fallback from ${modelChain[i - 1].displayName})"
                fallbackNotice = notice
                onFallbackEvent(modelChain[i - 1], candidateModel, reason)
                Log.i("GeminiApiService", notice)
            }

            val callResult = executeStream(
                model = candidateModel,
                prompt = prompt,
                systemInstruction = systemInstruction,
                inlineAudioUri = inlineAudioUri,
                audioMimeType = audioMimeType,
                inlineImageUri = inlineImageUri,
                apiKey = key,
                onThinkingChunk = onThinkingChunk,
                onTextChunk = onTextChunk
            )

            if (callResult.isSuccess) {
                recordRequest(candidateModel)
                return@withContext Result.success(
                    GenerationExecutionResult(
                        finalModelUsed = candidateModel,
                        fallbackNotice = fallbackNotice
                    )
                )
            } else {
                val error = callResult.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                lastError = error
                val errMsg = error.message ?: ""

                // Detect Rate Limit 429 or 503 or 404
                val is429 = errMsg.contains("429") || errMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true)
                val isRetryable = is429 || errMsg.contains("503") || errMsg.contains("404") || errMsg.contains("Quota", ignoreCase = true)

                if (is429) {
                    markModelQuotaExhausted(candidateModel)
                }

                if (!smartRouting || !isRetryable) {
                    // Do not fallback on non-retryable user errors (like 400 Bad Request)
                    return@withContext Result.failure(error)
                }
            }
        }

        Result.failure(lastError ?: Exception("All available Free-Tier models exhausted. Please try again in a few minutes."))
    }

    private suspend fun executeStream(
        model: GeminiTargetModel,
        prompt: String,
        systemInstruction: String?,
        inlineAudioUri: Uri?,
        audioMimeType: String?,
        inlineImageUri: Uri?,
        apiKey: String,
        onThinkingChunk: (String) -> Unit,
        onTextChunk: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()

            // System Instruction (if supported by model family)
            if (!systemInstruction.isNullOrBlank() && !model.technicalModelId.startsWith("gemma")) {
                root.put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
            }

            val contentsArray = JSONArray()
            val userContent = JSONObject()
            val partsArray = JSONArray()

            // 1. Inline Audio attachment
            if (inlineAudioUri != null) {
                val base64Audio = readUriToBase64(inlineAudioUri)
                if (base64Audio != null) {
                    val mime = audioMimeType ?: "audio/mp4"
                    partsArray.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", mime)
                            put("data", base64Audio)
                        })
                    })
                }
            }

            // 2. Inline Image attachment (Vision Inspector)
            if (inlineImageUri != null && model.supportsVision) {
                val base64Image = readImageUriToBase64(inlineImageUri)
                if (base64Image != null) {
                    partsArray.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        })
                    })
                }
            }

            // 3. User Prompt Text
            partsArray.put(JSONObject().put("text", prompt))
            userContent.put("parts", partsArray)
            contentsArray.put(userContent)
            root.put("contents", contentsArray)

            // 4. Generation Config & Extended Thinking
            val genConfig = JSONObject()
            genConfig.put("temperature", model.defaultTemperature)

            if (model.hasExtendedThinking) {
                genConfig.put("thinkingConfig", JSONObject().apply {
                    put("thinkingLevel", "high")
                })
            }
            root.put("generationConfig", genConfig)

            val mediaType = "application/json".toMediaType()
            val requestBody = root.toString().toRequestBody(mediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.technicalModelId}:streamGenerateContent?alt=sse&key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "HTTP ${response.code}"
                response.close()
                return@withContext Result.failure(Exception("HTTP ${response.code}: $errBody"))
            }

            val source = response.body?.byteStream()
                ?: return@withContext Result.failure(Exception("Empty response body from Gemini"))

            source.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val raw = line?.trim() ?: continue
                    if (!raw.startsWith("data:")) continue
                    val payload = raw.removePrefix("data:").trim()
                    if (payload.isEmpty() || payload == "[DONE]") continue

                    try {
                        val chunkObj = JSONObject(payload)
                        val candidates = chunkObj.optJSONArray("candidates") ?: continue
                        if (candidates.length() == 0) continue

                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content") ?: continue
                        val parts = content.optJSONArray("parts") ?: continue

                        for (p in 0 until parts.length()) {
                            val part = parts.getJSONObject(p)
                            if (part.has("thought") && part.getBoolean("thought")) {
                                val thoughtText = part.optString("text", "")
                                if (thoughtText.isNotEmpty()) {
                                    onThinkingChunk(thoughtText)
                                }
                            } else {
                                val text = part.optString("text", "")
                                if (text.isNotEmpty()) {
                                    onTextChunk(text)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("GeminiApiService", "Failed to parse SSE payload: ${e.message}")
                    }
                }
            }
            response.close()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun readUriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val buffer = ByteArray(8192)
            val byteStream = ByteArrayOutputStream()
            var bytesRead: Int
            inputStream?.use { stream ->
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    byteStream.write(buffer, 0, bytesRead)
                }
            }
            Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)
        } catch (t: Throwable) {
            Log.e("GeminiApiService", "Failed to encode audio Uri: ${t.message}")
            null
        }
    }

    private fun readImageUriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            // Compress to maximum 1600px width/height to save bandwidth
            val maxDim = Math.max(bitmap.width, bitmap.height)
            val scaledBitmap = if (maxDim > 1600) {
                val scale = 1600f / maxDim
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else {
                bitmap
            }
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()
            base64
        } catch (t: Throwable) {
            Log.e("GeminiApiService", "Failed to encode image Uri: ${t.message}")
            null
        }
    }
}
