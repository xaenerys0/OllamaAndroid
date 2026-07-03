package com.ollamaandroid.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OllamaException(message: String, val statusCode: Int? = null) : IOException(message)

/**
 * Thin client for the Ollama Cloud HTTP API (also works against a self-hosted
 * Ollama server). Chat responses stream as newline-delimited JSON.
 */
class OllamaClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Streams a chat completion. Emits one [ChatResponseChunk] per NDJSON line
     * until the server reports `done: true`.
     */
    fun streamChat(
        baseUrl: String,
        apiKey: String,
        request: ChatRequest,
    ): Flow<ChatResponseChunk> = flow {
        val httpRequest = Request.Builder()
            .url(apiUrl(baseUrl, "api/chat"))
            .applyAuth(apiKey)
            .post(json.encodeToString(ChatRequest.serializer(), request).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw OllamaException(readError(response.code, response.body?.string()), response.code)
            }
            val source = response.body?.source() ?: throw OllamaException("Empty response body")
            while (true) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue
                val chunk = json.decodeFromString(ChatResponseChunk.serializer(), line)
                emit(chunk)
                if (chunk.done) break
            }
        }
    }.flowOn(Dispatchers.IO)

    /** Lists the models available to this API key. */
    suspend fun listModels(baseUrl: String, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val httpRequest = Request.Builder()
            .url(apiUrl(baseUrl, "api/tags"))
            .applyAuth(apiKey)
            .get()
            .build()

        httpClient.newCall(httpRequest).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw OllamaException(readError(response.code, body), response.code)
            }
            json.decodeFromString(ModelsResponse.serializer(), body)
                .models
                .map { it.id }
                .filter { it.isNotBlank() }
                .sorted()
        }
    }

    /** Fetches model metadata; used to detect the `thinking` capability. */
    suspend fun showModel(baseUrl: String, apiKey: String, model: String): ShowResponse =
        withContext(Dispatchers.IO) {
            val httpRequest = Request.Builder()
                .url(apiUrl(baseUrl, "api/show"))
                .applyAuth(apiKey)
                .post(json.encodeToString(ShowRequest.serializer(), ShowRequest(model)).toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw OllamaException(readError(response.code, body), response.code)
                }
                json.decodeFromString(ShowResponse.serializer(), body)
            }
        }

    private fun apiUrl(baseUrl: String, path: String): String =
        baseUrl.trim().trimEnd('/') + "/" + path

    private fun Request.Builder.applyAuth(apiKey: String): Request.Builder = apply {
        if (apiKey.isNotBlank()) {
            header("Authorization", "Bearer ${apiKey.trim()}")
        }
    }

    private fun readError(code: Int, body: String?): String {
        val apiMessage = body?.takeIf { it.isNotBlank() }?.let {
            runCatching { json.decodeFromString(ApiError.serializer(), it).error }.getOrNull()
        }
        return when {
            !apiMessage.isNullOrBlank() -> apiMessage
            code == 401 || code == 403 -> "Invalid or missing API key (HTTP $code)"
            code == 404 -> "Endpoint not found (HTTP 404) — check the server URL"
            else -> "Request failed (HTTP $code)"
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
