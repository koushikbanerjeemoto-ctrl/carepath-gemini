package com.example.data.remote

import android.util.Log
import com.example.domain.AIResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CarePathBackendClient {
    private const val TAG = "CarePathBackendClient"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(75, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Configured backend candidate endpoints to ensure connectivity across emulator host, bridge, and dev
    private val CANDIDATE_ENDPOINTS = listOf(
        "https://carepath-qw1h.onrender.com/api/chat"
    )

    var lastConnectionError: String? = null
        private set

    suspend fun queryMedicalAI(
        conversationHistory: List<Pair<String, String>>, // role ("user" or "assistant") to text
        language: String
    ): AIResponse? = withContext(Dispatchers.IO) {
        lastConnectionError = null
        val payload = JSONObject().apply {
            put("language", language)
            val msgsArray = JSONArray()
            for ((role, text) in conversationHistory) {
                val msgObj = JSONObject().apply {
                    put("role", if (role == "user") "user" else "assistant")
                    put("content", text)
                }
                msgsArray.put(msgObj)
            }
            put("messages", msgsArray)
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        for (endpoint in CANDIDATE_ENDPOINTS) {
            try {
                Log.i("CarePathAI", "[CarePathAI] Sending request to: $endpoint")
                val request = Request.Builder()
                    .url(endpoint)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                Log.i("CarePathAI", "[CarePathAI] HTTP status: ${response.code}")
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.i("CarePathAI", "[CarePathAI] Response received: ${responseBody?.take(100)}")
                    if (!responseBody.isNullOrBlank()) {
                        val json = JSONObject(responseBody)
                        val reply = json.optString("response", json.optString("reply", ""))
                        if (reply.isNotBlank()) {
                            val actionType = if (json.has("actionType") && !json.isNull("actionType")) json.getString("actionType") else null
                            val actionLabel = if (json.has("actionLabel") && !json.isNull("actionLabel")) json.getString("actionLabel") else null
                            val actionPayload = if (json.has("actionPayload") && !json.isNull("actionPayload")) json.getString("actionPayload") else null

                            return@withContext AIResponse(
                                replyText = reply,
                                actionType = actionType,
                                actionLabel = actionLabel,
                                actionPayload = actionPayload
                            )
                        }
                    }
                } else {
                    lastConnectionError = "HTTP ${response.code} from $endpoint"
                    Log.w("CarePathAI", "[CarePathAI] HTTP error: ${response.code} from $endpoint")
                }
            } catch (e: Exception) {
                lastConnectionError = "${e.javaClass.simpleName}: ${e.message}"
                Log.e("CarePathAI", "[CarePathAI] Connection failed to $endpoint: ${e.javaClass.simpleName} - ${e.message}")
            }
        }

        null
    }
}
