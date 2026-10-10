package com.example.obsidiancalendar

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

object EventStore {
    private const val PREFS = "calendar"
    private const val TREE_URI = "tree_uri"
    private const val EVENTS = "events"

    fun selectedTree(context: Context): Uri? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(TREE_URI, null)?.let(Uri::parse)

    fun saveTree(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(TREE_URI, uri.toString()).apply()
    }

    suspend fun scan(context: Context, treeUri: Uri): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val events = buildList { collect(context, root, "", this) }
            .sortedWith(compareBy<CalendarEvent> { it.date }.thenBy { it.start ?: LocalTime.MIN }.thenBy { it.title })
        saveCached(context, events)
        events
    }

    private fun collect(context: Context, directory: DocumentFile, prefix: String, output: MutableList<CalendarEvent>) {
        directory.listFiles().forEach { file ->
            val path = if (prefix.isEmpty()) file.name.orEmpty() else "$prefix/${file.name.orEmpty()}"
            if (file.isDirectory) collect(context, file, path, output)
            else if (file.name?.endsWith(".md", ignoreCase = true) == true) {
                runCatching {
                    context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { reader ->
                        EventParser.parse(reader.readText(), path)?.let(output::add)
                    }
                }
            }
        }
    }

    fun cached(context: Context): List<CalendarEvent> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(EVENTS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { item ->
                val value = array.getJSONObject(item)
                CalendarEvent(
                    value.getString("title"),
                    LocalDate.parse(value.getString("date")),
                    value.optString("start").takeIf(String::isNotEmpty)?.let(LocalTime::parse),
                    value.optString("end").takeIf(String::isNotEmpty)?.let(LocalTime::parse),
                    value.getBoolean("allDay"),
                    value.getString("path")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun saveCached(context: Context, events: List<CalendarEvent>) {
        val array = JSONArray().apply {
            events.forEach { event -> put(JSONObject().apply {
                put("title", event.title)
                put("date", event.date.toString())
                put("start", event.start?.toString().orEmpty())
                put("end", event.end?.toString().orEmpty())
                put("allDay", event.allDay)
                put("path", event.notePath)
            }) }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(EVENTS, array.toString()).apply()
    }
}
