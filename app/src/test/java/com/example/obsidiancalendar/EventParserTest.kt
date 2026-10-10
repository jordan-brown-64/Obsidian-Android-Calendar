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
}
