package com.ollamaandroid.app

import com.ollamaandroid.app.data.network.ChatRequest
import com.ollamaandroid.app.data.network.ApiChatMessage
import com.ollamaandroid.app.data.network.ChatResponseChunk
import com.ollamaandroid.app.data.network.ModelsResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OllamaProtocolTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

    @Test
    fun `parses streaming chat chunk`() {
        val line = """{"model":"gpt-oss:120b","created_at":"2025-01-01T00:00:00Z","message":{"role":"assistant","content":"Hello"},"done":false}"""
        val chunk = json.decodeFromString(ChatResponseChunk.serializer(), line)
        assertEquals("Hello", chunk.message?.content)
        assertFalse(chunk.done)
    }

    @Test
    fun `parses final chunk with stats and unknown fields`() {
        val line = """{"model":"gpt-oss:120b","created_at":"2025-01-01T00:00:00Z","message":{"role":"assistant","content":""},"done":true,"done_reason":"stop","total_duration":123456,"load_duration":1,"prompt_eval_count":5,"eval_count":42}"""
        val chunk = json.decodeFromString(ChatResponseChunk.serializer(), line)
        assertTrue(chunk.done)
        assertEquals("stop", chunk.doneReason)
        assertEquals(42L, chunk.evalCount)
    }

    @Test
    fun `parses models response`() {
        val body = """{"models":[{"name":"gpt-oss:120b","model":"gpt-oss:120b","size":0},{"name":"qwen3-coder:480b"}]}"""
        val response = json.decodeFromString(ModelsResponse.serializer(), body)
        assertEquals(listOf("gpt-oss:120b", "qwen3-coder:480b"), response.models.map { it.id })
    }

    @Test
    fun `serializes chat request with stream flag`() {
        val request = ChatRequest(
            model = "gpt-oss:120b",
            messages = listOf(ApiChatMessage(role = "user", content = "Hi")),
        )
        val encoded = json.encodeToString(ChatRequest.serializer(), request)
        assertTrue(encoded.contains(""""stream":true"""))
        assertTrue(encoded.contains(""""role":"user""""))
    }
}
