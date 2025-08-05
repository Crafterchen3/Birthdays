package com.deckerpw.birthdays.api

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class Birthday(
    val id: Int? = null,
    val name: String,
    val date: LocalDateTime,
    val video: String? = null,
) {
    fun daysUntilNextBirthday(): Long {
        val today = LocalDate.now()
        var nextBirthday = LocalDate.of(today.year, date.month, date.dayOfMonth)

        if (nextBirthday.isBefore(today) || nextBirthday.isEqual(today).not() && today.isAfter(
                nextBirthday
            )
        ) {
            nextBirthday = nextBirthday.plusYears(1)
        }

        return ChronoUnit.DAYS.between(today, nextBirthday)
    }

    val isBirthday: Boolean
        get() = LocalDate.now().dayOfYear == date.dayOfYear
}
