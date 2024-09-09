package com.explorit.dodamdodam

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object CalendarUtils {
    private val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")

    fun daysInMonthArray(selectedDate: LocalDate): List<String> {
        val yearMonth = YearMonth.from(selectedDate)
        val daysInMonth = yearMonth.lengthOfMonth()

        val firstOfMonth = selectedDate.withDayOfMonth(1)
        val dayOfWeek = (firstOfMonth.dayOfWeek.value % 7) + 1

        val days = mutableListOf<String>()

        days.addAll(daysOfWeek)

        for (i in 1 until dayOfWeek) {
            days.add("")
        }

        for (i in 1..daysInMonth) {
            days.add(i.toString())
        }

        return days
    }

    fun monthYearFromDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        return date.format(formatter)
    }
}