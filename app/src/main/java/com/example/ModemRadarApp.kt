package com.example

import android.app.Application
import com.example.data.local.ModemRadarDatabase
import com.example.data.local.MonitorPreferences
import com.example.data.network.EsamListingScraper
import com.example.data.repository.ListingRepository
import com.example.notification.NotificationHelper
import com.example.worker.MonitorScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ModemRadarApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: ModemRadarDatabase
        private set

    lateinit var preferences: MonitorPreferences
        private set

    lateinit var notificationHelper: NotificationHelper
        private set

    lateinit var repository: ListingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = ModemRadarDatabase.getInstance(this)
        preferences = MonitorPreferences(this)
        notificationHelper = NotificationHelper(this).apply { ensureChannelCreated() }
        repository = ListingRepository(
            listingDao = database.listingDao(),
            checkLogDao = database.checkLogDao(),
            preferences = preferences,
            scraper = EsamListingScraper(),
            notificationHelper = notificationHelper
        )

        appScope.launch {
            val settings = preferences.getCurrentSettings()
            if (settings.isMonitoringEnabled) {
                MonitorScheduler.schedule(this@ModemRadarApp, settings.intervalMinutes)
            }
        }
    }
}
