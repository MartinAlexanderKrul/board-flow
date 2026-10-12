package cz.nicolsburg.boardflow.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PickTimeSlotTest {

    // 2026-10-10 is a Saturday, 2026-10-12 a Monday.
    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 10, day, hour, minute)

    @Test
    fun weekendBeforeFiveIsDeepDive() {
        assertEquals(PickTimeSlot.WEEKEND_DAY, PickTimeSlot.at(at(10, 10)))
        assertEquals(PickTimeSlot.WEEKEND_DAY, PickTimeSlot.at(at(11, 16, 59)))
    }

    @Test
    fun fivePmIsEveningOnAnyDay() {
        assertEquals(PickTimeSlot.EVENING, PickTimeSlot.at(at(10, 17)))
        assertEquals(PickTimeSlot.EVENING, PickTimeSlot.at(at(12, 21, 59)))
    }

    @Test
    fun weekdayDaytimeAndLateNight() {
        assertEquals(PickTimeSlot.WEEKDAY_DAY, PickTimeSlot.at(at(12, 9)))
        assertEquals(PickTimeSlot.LATE_NIGHT, PickTimeSlot.at(at(12, 22)))
        assertEquals(PickTimeSlot.LATE_NIGHT, PickTimeSlot.at(at(10, 2)))
    }

    @Test
    fun matchesNeedsKnownWeightAndTime() {
        assertTrue(PickTimeSlot.WEEKEND_DAY.matches(3.6, 120))
        assertFalse(PickTimeSlot.WEEKEND_DAY.matches(2.4, 120))
        assertFalse(PickTimeSlot.WEEKEND_DAY.matches(3.6, 45))
        assertFalse(PickTimeSlot.WEEKEND_DAY.matches(null, 120))
        assertTrue(PickTimeSlot.EVENING.matches(1.8, 40))
        assertFalse(PickTimeSlot.EVENING.matches(1.8, 90))
        assertFalse(PickTimeSlot.LATE_NIGHT.matches(1.5, null))
    }

    @Test
    fun eveningCapsOtherLanes() {
        assertFalse(PickTimeSlot.EVENING.allows(1.8, 90))
        assertFalse(PickTimeSlot.EVENING.allows(3.2, 45))
        assertTrue(PickTimeSlot.EVENING.allows(2.0, 60))
        assertTrue(PickTimeSlot.EVENING.allows(null, null))
        assertFalse(PickTimeSlot.LATE_NIGHT.allows(1.5, 40))
        assertTrue(PickTimeSlot.WEEKEND_DAY.allows(4.5, 240))
    }
}
