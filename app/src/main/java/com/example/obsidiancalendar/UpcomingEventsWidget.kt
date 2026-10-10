package com.example.obsidiancalendar

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UpcomingEventsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingEventsWidget()
}

class UpcomingEventsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val events = EventStore.cached(context)
                .filter { !it.date.isBefore(LocalDate.now()) }
                .take(4)
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(14.dp).clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.Vertical.Top
            ) {
                Text("Obsidian Calendar", style = TextStyle(fontSize = 16.sp))
                Spacer(GlanceModifier.height(6.dp))
                if (events.isEmpty()) Text("No upcoming events", style = TextStyle(fontSize = 13.sp))
                events.forEach { event ->
                    Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Column(GlanceModifier.padding(end = 8.dp)) {
                            Text(event.title, style = TextStyle(fontSize = 13.sp))
                            Text(event.date.format(DateTimeFormatter.ofPattern("EEE d MMM")), style = TextStyle(fontSize = 11.sp))
                        }
                        Text(event.timeLabel(), style = TextStyle(fontSize = 11.sp))
                    }
                }
            }
        }
    }
}
