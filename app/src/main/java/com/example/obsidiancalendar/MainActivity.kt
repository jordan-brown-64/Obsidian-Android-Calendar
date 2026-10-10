package com.example.obsidiancalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@registerForActivityResult
        EventStore.saveTree(this, uri)
        setContent { CalendarApp() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalendarApp(onChooseFolder = { folderPicker.launch(EventStore.selectedTree(this)) })
        }
        if (EventStore.selectedTree(this) == null) folderPicker.launch(null)
    }
}

class CalendarViewModel : ViewModel() {
    var events by mutableStateOf(emptyList<CalendarEvent>())
        private set
    var selectedMonth by mutableStateOf(YearMonth.now())
        private set

    fun load(context: android.content.Context) {
        events = EventStore.cached(context)
        EventStore.selectedTree(context)?.let { tree ->
            viewModelScope.launch {
                events = EventStore.scan(context.applicationContext, tree)
                UpcomingEventsWidget().updateAll(context)
                scheduleRefresh(context)
            }
        }
    }

    fun month(offset: Long) { selectedMonth = selectedMonth.plusMonths(offset) }
    fun scheduleRefresh(context: android.content.Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "calendar-refresh", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<RefreshWorker>().build()
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "calendar-periodic-refresh", androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES).build()
        )
    }
}

@Composable
fun CalendarApp(onChooseFolder: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val model: CalendarViewModel = viewModel()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) { model.load(context) }
    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Obsidian Calendar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onChooseFolder) { Text("Folder") }
            }
            Spacer(Modifier.height(12.dp))
            MonthHeader(model.selectedMonth, model::month)
            MonthGrid(model.selectedMonth, model.events) { selectedDate = it }
            Spacer(Modifier.height(12.dp))
            DayDetails(selectedDate, model.events)
            Spacer(Modifier.height(8.dp))
            Text("Upcoming", style = MaterialTheme.typography.titleLarge)
            Box(Modifier.weight(1f)) { UpcomingList(model.events) }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onMove: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onMove(-1) }) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
        Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = { onMove(1) }) { Text("›", style = MaterialTheme.typography.headlineMedium) }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, events: List<CalendarEvent>, onSelect: (LocalDate) -> Unit) {
    val first = month.atDay(1)
    val start = first.minusDays((first.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val days = (0 until 42).map { start.plusDays(it.toLong()) }
    val eventDates = events.groupingBy { it.date }.eachCount()
    Column {
        Row(Modifier.fillMaxWidth()) { listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, Modifier.weight(1f).padding(6.dp), fontWeight = FontWeight.Bold) } }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) { week.forEach { day ->
                val active = day.month == month.month
                Column(Modifier.weight(1f).height(48.dp).padding(2.dp)
                    .background(if (day == LocalDate.now()) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onSelect(day) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.dayOfMonth.toString(), color = if (active) Color.Unspecified else Color.LightGray)
                    if ((eventDates[day] ?: 0) > 0) Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.primary))
                }
            } }
        }
    }
}

@Composable
private fun DayDetails(date: LocalDate, events: List<CalendarEvent>) {
    val dayEvents = events.filter { it.date == date }
    Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.titleMedium)
    if (dayEvents.isEmpty()) {
        Text("No events", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    } else {
        dayEvents.forEach { event ->
            Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text(event.timeLabel())
            }
        }
    }
}

@Composable
private fun UpcomingList(events: List<CalendarEvent>) {
    val upcoming = events.filter { !it.date.isBefore(LocalDate.now()) }.take(8)
    if (upcoming.isEmpty()) Text("No upcoming events", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
    else LazyColumn { items(upcoming) { event ->
        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(event.title, fontWeight = FontWeight.SemiBold)
                    Text(event.date.format(DateTimeFormatter.ofPattern("EEE, d MMM")), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(event.timeLabel())
            }
        }
    } }
}
