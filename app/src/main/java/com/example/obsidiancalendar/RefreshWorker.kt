package com.example.obsidiancalendar

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        EventStore.selectedTree(applicationContext)?.let { EventStore.scan(applicationContext, it) }
        UpcomingEventsWidget().updateAll(applicationContext)
        return Result.success()
    }
}
