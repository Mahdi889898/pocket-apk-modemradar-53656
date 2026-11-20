package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ModemRadarApp

class ListingCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ModemRadarApp ?: return Result.failure()
        return try {
            app.repository.syncNow(triggerSource = "پایش خودکار پس‌زمینه")
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
