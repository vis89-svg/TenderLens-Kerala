package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateText(prompt: String, systemPrompt: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is not configured. Please configure it in the Secrets Panel."
        }

        try {
            val root = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject().put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            if (systemPrompt != null) {
                val sysInstruction = JSONObject()
                val sysParts = JSONArray().put(JSONObject().put("text", systemPrompt))
                sysInstruction.put("parts", sysParts)
                root.put("systemInstruction", sysInstruction)
            }

            val requestBody = root.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Failed to communicate with AI model: Code ${response.code}"
                }
                val bodyString = response.body?.string() ?: return@withContext "Empty response from Gemini API"
                
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                
                firstPart?.optString("text") ?: "No parsed response text."
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: "Unknown network error"}"
        }
    }
}
