# Obsidian Calendar

An Android calendar and home-screen widget for events stored as Markdown notes by Obsidian Full Calendar.

## Current Scope

- Select the vault or `Events` folder when the app launches.
- Read-only parsing of Markdown files with Full Calendar YAML frontmatter.
- Month calendar with selectable day details.
- Upcoming-events home-screen widget.
- Persisted folder access and periodic background refresh.

The calendar date comes from the frontmatter `date` field. Supported fields are `title`, `date`, `allDay`, `startTime`, and `endTime`.

## Build

Open this directory in Android Studio with a current Android SDK and JDK 17. The project targets Android API 35 and supports Android 8.0 (API 26) and newer.
