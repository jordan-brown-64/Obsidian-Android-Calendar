package com.example.obsidiancalendar

import java.time.LocalDate
import java.time.LocalTime

object EventParser {
    fun parse(markdown: String, notePath: String): CalendarEvent? {
        val lines = markdown.lineSequence().toList()
        if (lines.firstOrNull()?.trim() != "---") return null
        val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (end < 0) return null

        val values = lines.drop(1).take(end).mapNotNull { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) return@mapNotNull null
            line.substring(0, separator).trim() to line.substring(separator + 1).trim()
        }.toMap()

        val date = values["date"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val title = values["title"]?.trim()?.trim('"', '\'')
            ?.takeIf { it.isNotEmpty() } ?: notePath.substringAfterLast('/').removeSuffix(".md")
        val allDay = values["allDay"]?.equals("true", ignoreCase = true) == true
        val start = values["startTime"]?.let(::parseTime)
        val endTime = values["endTime"]?.let(::parseTime)

        return CalendarEvent(title, date, start, endTime, allDay, notePath)
    }

    private fun parseTime(value: String): LocalTime? = runCatching {
        LocalTime.parse(value.trim().trim('"', '\''))
    }.getOrNull()
}
