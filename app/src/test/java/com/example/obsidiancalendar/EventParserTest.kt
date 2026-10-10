package com.example.obsidiancalendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class EventParserTest {
    @Test
    fun parsesFullCalendarFrontmatter() {
        val event = EventParser.parse(
            """---
                title: Doctor
                allDay: false
                startTime: 11:30
                endTime: 12:00
                date: 2024-07-30
                ---
                Notes
            """.trimIndent(),
            "Events/2024-07-30 Doctor.md"
        )

        assertNotNull(event)
        assertEquals("Doctor", event?.title)
        assertEquals("2024-07-30", event?.date.toString())
        assertEquals("11:30", event?.start.toString())
        assertEquals("12:00", event?.end.toString())
        assertFalse(event?.allDay ?: true)
    }

    @Test
    fun rejectsNotesWithoutDate() {
        assertEquals(null, EventParser.parse("---\ntitle: Missing date\n---", "missing.md"))
    }

    @Test
    fun acceptsBomCrLfAndDifferentTimeKeyCasing() {
        val event = EventParser.parse(
            "\uFEFF---\r\nTitle: Playtesting\r\nAllDay: false\r\nStartTime: 19:00\r\nEndTime: 21:00\r\nDate: 2024-08-16\r\n---\r\n",
            "Events/2024-08-16 Playtesting.md"
        )

        assertEquals("2024-08-16", event?.date.toString())
        assertEquals("19:00", event?.start.toString())
        assertEquals("21:00", event?.end.toString())
    }
}
