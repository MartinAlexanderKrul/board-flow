package cz.nicolsburg.boardflow.model

import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * When the user opens Log Play, which shapes the time-of-day lane in "Good picks right now".
 * Complexity is BGG's average weight (1 light - 5 heavy); playing time is BGG's minutes.
 */
enum class PickTimeSlot(
    val laneTitle: String,
    val laneSubtitle: String,
    val minWeight: Double?,
    val maxWeight: Double?,
    val minMinutes: Int?,
    val maxMinutes: Int?
) {
    /** Saturday or Sunday, 6:00 - 16:59: time for something big. */
    WEEKEND_DAY("Weekend deep dive", "Heavier games for a free afternoon", 3.0, null, 60, null),

    /** Monday - Friday, 6:00 - 16:59: planning a weeknight game. */
    WEEKDAY_DAY("Weeknight picks", "Medium games that fit an evening", 2.0, 3.0, null, 90),

    /** Any day, 17:00 - 21:59: casual and quick. */
    EVENING("Easy evening", "Light games that wrap up tonight", null, 2.3, null, 60),

    /** 22:00 - 5:59: a filler before bed. */
    LATE_NIGHT("One more before bed", "Fillers of half an hour or less", null, 2.0, null, 30);

    fun matches(weight: Double?, minutes: Int?): Boolean {
        if (minWeight != null || maxWeight != null) {
            if (weight == null || weight <= 0.0) return false
            if (minWeight != null && weight < minWeight) return false
            if (maxWeight != null && weight > maxWeight) return false
        }
        if (minMinutes != null || maxMinutes != null) {
            if (minutes == null || minutes <= 0) return false
            if (minMinutes != null && minutes < minMinutes) return false
            if (maxMinutes != null && minutes > maxMinutes) return false
        }
        return true
    }

    companion object {
        fun at(time: LocalDateTime): PickTimeSlot {
            val hour = time.hour
            val weekend = time.dayOfWeek == DayOfWeek.SATURDAY || time.dayOfWeek == DayOfWeek.SUNDAY
            return when {
                hour < 6 || hour >= 22 -> LATE_NIGHT
                hour >= 17 -> EVENING
                weekend -> WEEKEND_DAY
                else -> WEEKDAY_DAY
            }
        }
    }
}
