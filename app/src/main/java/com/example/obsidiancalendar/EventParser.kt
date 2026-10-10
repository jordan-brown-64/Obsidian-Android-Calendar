package com.example.obsidiancalendar

import java.time.LocalDate
import java.time.LocalTime

object EventParser {
    fun parse(markdown: String, notePath: String): CalendarEvent? {
        val lines = markdown.lineSequence().toList()
        if (lines.firstOrNull()?.trim()?.removePrefix("\uFEFF") != "---") return null
        val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (end < 0) return null

        val values = lines.drop(1).take(end).mapNotNull { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) return@mapNotNull null
            val key = line.substring(0, separator).trim().lowercase()
            val value = line.substring(separator + 1).trim().trim('"', '\'')
            key to value
        }.toMap()

        val date = values["date"]?.let(::parseDate)
            ?: values["startdate"]?.let(::parseDate)
            ?: DATE_IN_PATH.find(notePath)?.groupValues?.get(1)?.let(::parseDate)
            ?: return null
        val title = values["title"]?.trim()
            ?.takeIf { it.isNotEmpty() } ?: notePath.substringAfterLast('/').removeSuffix(".md")
        val allDay = values["allday"]?.equals("true", ignoreCase = true) == true
        val start = values["starttime"]?.let(::parseTime)
        val endTime = values["endtime"]?.let(::parseTime)

        return CalendarEvent(title, date, start, endTime, allDay, notePath)
    }

    private fun parseDate(value: String): LocalDate? = runCatching {
        LocalDate.parse(value.trim().trim('"', '\''))
    }.getOrNull()

    private fun parseTime(value: String): LocalTime? = runCatching {
        LocalTime.parse(value.trim().trim('"', '\''))
    }.getOrNull()

    private val DATE_IN_PATH = Regex("(?:^|/)(\\d{4}-\\d{2}-\\d{2})(?:\\s|$)")
}
