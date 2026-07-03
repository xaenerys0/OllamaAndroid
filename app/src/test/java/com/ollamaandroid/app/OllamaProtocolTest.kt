package com.ollamaandroid.app

import com.ollamaandroid.app.data.network.ChatRequest
import com.ollamaandroid.app.data.network.ApiChatMessage
import com.ollamaandroid.app.data.network.ChatResponseChunk
import com.ollamaandroid.app.data.network.ModelsResponse
import com.ollamaandroid.app.data.network.ReasoningLevel
import com.ollamaandroid.app.data.network.ShowResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertFalse("think must be omitted when null", encoded.contains("think"))
    }

    @Test
    fun `serializes think as boolean and as effort level`() {
        val base = listOf(ApiChatMessage(role = "user", content = "Hi"))

        val on = json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(model = "deepseek-v3.1:671b", messages = base, think = ReasoningLevel.ON.toThinkValue()),
        )
        assertTrue(on.contains(""""think":true"""))

        val off = json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(model = "deepseek-v3.1:671b", messages = base, think = ReasoningLevel.OFF.toThinkValue()),
        )
        assertTrue(off.contains(""""think":false"""))

        val high = json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(model = "gpt-oss:120b", messages = base, think = ReasoningLevel.HIGH.toThinkValue()),
        )
        assertTrue(high.contains(""""think":"high""""))

        assertNull(ReasoningLevel.DEFAULT.toThinkValue())
    }

    @Test
    fun `parses thinking delta from streaming chunk`() {
        val line = """{"model":"deepseek-v3.1:671b","message":{"role":"assistant","content":"","thinking":"Let me consider"},"done":false}"""
        val chunk = json.decodeFromString(ChatResponseChunk.serializer(), line)
        assertEquals("Let me consider", chunk.message?.thinking)
        assertEquals("", chunk.message?.content)
    }

    @Test
    fun `detects thinking capability from show response`() {
        val thinking = json.decodeFromString(
            ShowResponse.serializer(),
            """{"license":"x","capabilities":["completion","tools","thinking"]}""",
        )
        assertTrue(thinking.supportsThinking)

        val plain = json.decodeFromString(
            ShowResponse.serializer(),
            """{"capabilities":["completion"]}""",
        )
        assertFalse(plain.supportsThinking)

        val missing = json.decodeFromString(ShowResponse.serializer(), """{}""")
        assertFalse(missing.supportsThinking)
    }

    @Test
    fun `reasoning level round-trips through preference keys`() {
        ReasoningLevel.entries.forEach { level ->
            assertEquals(level, ReasoningLevel.fromKey(level.key))
        }
        assertEquals(ReasoningLevel.DEFAULT, ReasoningLevel.fromKey(null))
        assertEquals(ReasoningLevel.DEFAULT, ReasoningLevel.fromKey("bogus"))
    }
}
