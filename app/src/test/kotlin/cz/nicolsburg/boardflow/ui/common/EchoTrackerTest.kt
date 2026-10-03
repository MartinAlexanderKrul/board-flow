package cz.nicolsburg.boardflow.ui.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EchoTrackerTest {

    @Test
    fun `stale value while typing is ignored`() {
        val echo = EchoTracker("0")
        echo.reported("6")
        // Recomposed before state caught up: still the old value.
        assertFalse(echo.isExternalChange("0", "6"))
        echo.reported("64")
        // State skipped "6" and delivered "64".
        assertFalse(echo.isExternalChange("64", "64"))
    }

    @Test
    fun `late echo does not undo newer typing`() {
        val echo = EchoTracker("0")
        echo.reported("6")
        echo.reported("64")
        assertFalse(echo.isExternalChange("6", "64"))
        assertFalse(echo.isExternalChange("64", "64"))
    }

    @Test
    fun `value set elsewhere is adopted`() {
        val echo = EchoTracker("12")
        assertTrue(echo.isExternalChange("0", "12"))
        assertFalse(echo.isExternalChange("0", "0"))
    }
}
