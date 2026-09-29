package com.example.jamuchat.network

import android.os.Handler
import android.os.Looper
import com.example.jamuchat.getHttpBaseUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AiRepository {

    fun askAi(
        serverUrl: String,
        prompt: String,
        onResult: (String) -> Unit
    ) {
        Thread {
            try {
                val httpBase = getHttpBaseUrl(serverUrl)
                val aiUrl = "$httpBase/api/ai/chat"

                val jsonBody = JSONObject().apply {
                    put("prompt", prompt)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = jsonBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(aiUrl)
                    .post(requestBody)
                    .build()

                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val json = JSONObject(bodyString)
                        val reply = json.optString("reply", "AI javob qaytara olmadi.")
                        Handler(Looper.getMainLooper()).post { onResult(reply) }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            onResult("Server xatosi (${response.code}). Iltimos qayta urinib ko'ring.")
                        }
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    onResult("Savolga javob olishda xatolik yuz berdi: ${e.localizedMessage}")
                }
            }
        }.start()
    }
}
