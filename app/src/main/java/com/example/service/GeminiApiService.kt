package com.example.service

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.GeminiTargetModel
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
import java.util.concurrent.TimeUnit

class GeminiApiService(private val context: Context) {

    private val prefs = context.getSharedPreferences("omni_gemini_prefs", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

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

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
    }

    fun clearCustomApiKey() {
        prefs.edit().remove("custom_gemini_api_key").apply()
    }

    suspend fun testConnection(): Result<Long> = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) {
            return@withContext Result.failure(Exception("API key is not configured. Add your key via Secrets or in-app settings."))
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
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key"

            val request = Request.Builder().url(url).post(body).build()
            val response = okHttpClient.newCall(request).execute()

            val latency = System.currentTimeMillis() - startTime
            if (response.isSuccessful) {
                response.close()
                Result.success(latency)
            } else {
                val err = response.body?.string() ?: "HTTP ${response.code}"
                response.close()
                Result.failure(Exception("API returned error: $err"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun streamGenerate(
        model: GeminiTargetModel,
        prompt: String,
        systemInstruction: String? = null,
        inlineAudioUri: Uri? = null,
        audioMimeType: String? = null,
        onThinkingChunk: (String) -> Unit = {},
        onTextChunk: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API key is required. Please set it in AI Studio Secrets or enter it in app settings."))
        }

        try {
            val root = JSONObject()

            // System Instruction
            if (!systemInstruction.isNullOrBlank()) {
                root.put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
            }

            // Contents
            val contentsArray = JSONArray()
            val userContent = JSONObject()
            val partsArray = JSONArray()

            // Inline Audio if provided (for Gemini 3.5 Transcribe or Gemini 2.5 Audio)
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

            // User Prompt Text
            partsArray.put(JSONObject().put("text", prompt))
            userContent.put("parts", partsArray)
            contentsArray.put(userContent)
            root.put("contents", contentsArray)

            // Generation Config & Extended Thinking
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
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.technicalModelId}:streamGenerateContent?alt=sse&key=$key"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "HTTP ${response.code}"
                response.close()
                return@withContext Result.failure(Exception("Gemini Error (${response.code}): $errBody"))
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

                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            // Check for thinking content
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
                        Log.w("GeminiApiService", "Failed to parse SSE chunk: ${e.message}")
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
}
