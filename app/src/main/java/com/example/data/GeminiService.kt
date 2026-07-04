package com.example.data

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Sends a text prompt to Gemini 3.5 Flash and returns the text response.
     */
    suspend fun generateText(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder!")
            return@withContext "API Key not configured in AI Studio Secrets."
        }

        try {
            // Build request JSON manually using standard Android org.json
            val contentsJson = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsJson)
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Empty error response"
                    Log.e(TAG, "API Error: ${response.code} - $errorBody")
                    return@withContext "Failed to fetch response from Gemini. Code: ${response.code}"
                }

                val responseBodyStr = response.body?.string() ?: return@withContext "Empty response"
                val jsonResponse = JSONObject(responseBodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No text part found")
                        }
                    }
                }
                return@withContext "Could not parse Gemini response."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateText: ${e.message}", e)
            return@withContext "Error: ${e.localizedMessage}"
        }
    }

    /**
     * Sends a text prompt and an image (base64) to Gemini 3.5 Flash.
     */
    suspend fun analyzeImage(prompt: String, base64Image: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder!")
            return@withContext "API Key not configured. Please add it in your AI Studio Secrets."
        }

        try {
            val partsArray = JSONArray().apply {
                // Add text part
                put(JSONObject().apply {
                    put("text", prompt)
                })
                // Add image inlineData part
                put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    })
                })
            }

            val contentsJson = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", partsArray)
                })
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsJson)
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Empty error response"
                    Log.e(TAG, "API Error: ${response.code} - $errorBody")
                    return@withContext "Failed to analyze image. Code: ${response.code}"
                }

                val responseBodyStr = response.body?.string() ?: return@withContext "Empty response"
                val jsonResponse = JSONObject(responseBodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No text part found")
                        }
                    }
                }
                return@withContext "Could not parse Gemini image analysis."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in analyzeImage: ${e.message}", e)
            return@withContext "Error: ${e.localizedMessage}"
        }
    }
}
