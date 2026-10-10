package com.example.obsidiancalendar

import java.time.LocalDate
import java.time.LocalTime

data class CalendarEvent(
    val title: String,
    val date: LocalDate,
    val start: LocalTime?,
    val end: LocalTime?,
    val allDay: Boolean,
    val notePath: String
) {
    fun timeLabel(): String = when {
        allDay -> "All day"
        start == null -> "Time not set"
        end == null -> start.toString()
        else -> "${start} - ${end}"
    }
}
