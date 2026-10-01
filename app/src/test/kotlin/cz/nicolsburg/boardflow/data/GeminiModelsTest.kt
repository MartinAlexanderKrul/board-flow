package cz.nicolsburg.boardflow.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiModelsTest {

    private val listed = listOf(
        "gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.5-flash-preview-tts", "gemma-4-31b-it",
        "gemini-flash-latest", "gemini-flash-lite-latest", "gemini-pro-latest", "gemini-2.5-flash-lite",
        "gemini-3-flash-preview", "gemini-3.1-flash-image", "gemini-3.5-flash", "gemini-3.5-flash-lite",
        "gemini-3.8-flash", "gemini-2.5-computer-use-preview-10-2025", "lyria-3.5"
    )

    @Test
    fun `automatic order prefers aliases and newest stable flash`() {
        assertEquals(
            listOf(
                "gemini-flash-latest", "gemini-3.8-flash", "gemini-3.5-flash", "gemini-2.5-flash",
                "gemini-flash-lite-latest", "gemini-3.5-flash-lite", "gemini-2.5-flash-lite",
                "gemini-pro-latest", "gemini-2.5-pro",
                "gemini-3-flash-preview"
            ),
            GeminiModels.candidates(GeminiModels.AUTO, listed)
        )
    }

    @Test
    fun `works before the model list was ever fetched`() {
        assertEquals(
            listOf("gemini-flash-latest", "gemini-flash-lite-latest"),
            GeminiModels.candidates(GeminiModels.AUTO, emptyList())
        )
    }

    @Test
    fun `a retired pinned model still has the automatic chain behind it`() {
        val candidates = GeminiModels.candidates("gemini-2.0-flash-lite", listed)
        assertEquals("gemini-2.0-flash-lite", candidates.first())
        assertEquals("gemini-flash-latest", GeminiModels.next("gemini-2.0-flash-lite", candidates))
    }

    @Test
    fun `specialised models are not usable`() {
        assertFalse(GeminiModels.isUsable("gemini-3.8-flash-tts"))
        assertFalse(GeminiModels.isUsable("gemini-3.1-flash-image"))
        assertFalse(GeminiModels.isUsable("lyria-3.5"))
        assertTrue(GeminiModels.isUsable("gemini-flash-lite-latest"))
    }

    @Test
    fun `next skips excluded models and stops at the end`() {
        val candidates = listOf("a", "b", "c")
        assertEquals("c", GeminiModels.next("a", candidates, setOf("b")))
        assertNull(GeminiModels.next("c", candidates))
        assertEquals("a", GeminiModels.next("unknown", candidates))
    }
}
