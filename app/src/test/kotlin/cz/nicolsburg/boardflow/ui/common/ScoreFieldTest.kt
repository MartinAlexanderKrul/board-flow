package cz.nicolsburg.boardflow.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreFieldTest {

    @Test
    fun `typing after the default zero replaces it`() {
        assertEquals("6", replaceZeroPlaceholder("0", "06"))
    }

    @Test
    fun `typing before the default zero replaces it`() {
        assertEquals("6", replaceZeroPlaceholder("0", "60"))
    }

    @Test
    fun `a second digit extends a real score`() {
        assertEquals("62", replaceZeroPlaceholder("6", "62"))
        assertEquals("60", replaceZeroPlaceholder("6", "60"))
    }

    @Test
    fun `a minus sign replaces the default zero`() {
        assertEquals("-", replaceZeroPlaceholder("0", "-0"))
    }

    @Test
    fun `clearing and other edits pass through`() {
        assertEquals("", replaceZeroPlaceholder("0", ""))
        assertEquals("0", replaceZeroPlaceholder("", "0"))
        assertEquals("105", replaceZeroPlaceholder("10", "105"))
    }
}
