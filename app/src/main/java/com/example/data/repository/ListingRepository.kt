package com.example.data.repository

import com.example.data.local.CheckLogDao
import com.example.data.local.CheckLogEntity
import com.example.data.local.CheckStatus
import com.example.data.local.ListingDao
import com.example.data.local.ListingEntity
import com.example.data.local.MonitorPreferences
import com.example.data.local.MonitorSettings
import com.example.data.network.EsamListingScraper
import com.example.data.network.ScrapeResult
import com.example.notification.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SyncOutcome(
    val success: Boolean,
    val totalFound: Int,
    val newCount: Int,
    val isBaselineSync: Boolean,
    val message: String
)

class ListingRepository(
    private val listingDao: ListingDao,
    private val checkLogDao: CheckLogDao,
    private val preferences: MonitorPreferences,
    private val scraper: EsamListingScraper,
    private val notificationHelper: NotificationHelper
) {

    private val syncMutex = Mutex()

    val listingsFlow: Flow<List<ListingEntity>> = listingDao.observeAllListings()
    val logsFlow: Flow<List<CheckLogEntity>> = checkLogDao.observeRecentLogs()
    val settingsFlow: Flow<MonitorSettings> = preferences.settingsFlow

    suspend fun syncNow(triggerSource: String): SyncOutcome = syncMutex.withLock {
        val now = System.currentTimeMillis()
        val settings = preferences.getCurrentSettings()
        val targetUrl = settings.targetUrl

        val outcome = when (val result = scraper.fetchListings(targetUrl)) {
            is ScrapeResult.Success -> {
                val existingIds = listingDao.getExistingIdsForSource(targetUrl).toHashSet()
                val isBaseline = existingIds.isEmpty()

                val newlyDiscovered = mutableListOf<ListingEntity>()
                for (item in result.listings) {
                    if (item.externalId !in existingIds) {
                        newlyDiscovered += ListingEntity(
                            externalId = item.externalId,
                            title = item.title,
                            priceText = item.priceText,
                            imageUrl = item.imageUrl,
                            listingUrl = item.listingUrl,
                            badgeText = item.badgeText,
                            sellerInfo = item.sellerInfo,
                            firstSeenAt = now,
                            lastSeenAt = now,
                            isNew = !isBaseline,
                            isBookmarked = false,
                            sourceUrl = targetUrl
                        )
                    } else {
                        listingDao.updateExistingListing(
                            externalId = item.externalId,
                            title = item.title,
                            priceText = item.priceText,
                            imageUrl = item.imageUrl,
                            badgeText = item.badgeText,
                            lastSeenAt = now
                        )
                    }
                }

                if (newlyDiscovered.isNotEmpty()) {
                    listingDao.insertNewListings(newlyDiscovered)
                }

                val alertedCount = if (isBaseline) 0 else newlyDiscovered.size
                if (alertedCount > 0) {
                    notificationHelper.notifyNewListings(newlyDiscovered)
                }

                val logMessage = when {
                    isBaseline -> "ثبت اولیه ${result.listings.size} آگهی مودم انجام شد. از این پس هر آگهی جدیدی ثبت شود اعلان دریافت می‌کنید."
                    alertedCount > 0 -> "$alertedCount آگهی جدید شناسایی شد و اعلان ارسال گردید."
                    else -> "${result.listings.size} آگهی بررسی شد؛ مورد جدیدی ثبت نشده بود."
                }

                checkLogDao.insertLog(
                    CheckLogEntity(
                        checkedAt = now,
                        status = CheckStatus.SUCCESS.name,
                        totalFound = result.listings.size,
                        newCount = alertedCount,
                        triggerSource = triggerSource,
                        message = logMessage
                    )
                )
                checkLogDao.trimOldLogs()
                preferences.setLastCheckedAt(now)

                SyncOutcome(
                    success = true,
                    totalFound = result.listings.size,
                    newCount = alertedCount,
                    isBaselineSync = isBaseline,
                    message = logMessage
                )
            }

            is ScrapeResult.EmptyOrProtected -> {
                checkLogDao.insertLog(
                    CheckLogEntity(
                        checkedAt = now,
                        status = CheckStatus.EMPTY.name,
                        totalFound = 0,
                        newCount = 0,
                        triggerSource = triggerSource,
                        message = result.reason
                    )
                )
                checkLogDao.trimOldLogs()
                preferences.setLastCheckedAt(now)

                SyncOutcome(
                    success = false,
                    totalFound = 0,
                    newCount = 0,
                    isBaselineSync = false,
                    message = result.reason
                )
            }

            is ScrapeResult.Failure -> {
                checkLogDao.insertLog(
                    CheckLogEntity(
                        checkedAt = now,
                        status = CheckStatus.ERROR.name,
                        totalFound = 0,
                        newCount = 0,
                        triggerSource = triggerSource,
                        message = result.reason
                    )
                )
                checkLogDao.trimOldLogs()
                preferences.setLastCheckedAt(now)

                SyncOutcome(
                    success = false,
                    totalFound = 0,
                    newCount = 0,
                    isBaselineSync = false,
                    message = result.reason
                )
            }
        }
        outcome
    }

    suspend fun markAsSeen(externalId: String) = listingDao.markAsSeen(externalId)

    suspend fun markAllAsSeen() = listingDao.markAllAsSeen()

    suspend fun toggleBookmark(externalId: String, currentBookmarked: Boolean) =
        listingDao.setBookmarked(externalId, !currentBookmarked)

    suspend fun clearCachedListings() = listingDao.clearAll()

    suspend fun clearLogs() = checkLogDao.clearAll()

    suspend fun updateTargetUrl(url: String) = preferences.setTargetUrl(url)

    suspend fun resetTargetUrl() = preferences.resetTargetUrl()

    suspend fun setMonitoringEnabled(enabled: Boolean) = preferences.setMonitoringEnabled(enabled)

    suspend fun setIntervalMinutes(minutes: Long) = preferences.setIntervalMinutes(minutes)

    suspend fun setForegroundAutoPollEnabled(enabled: Boolean) =
        preferences.setForegroundAutoPollEnabled(enabled)

    fun sendTestNotification(): Boolean = notificationHelper.sendTestNotification()

    fun hasNotificationPermission(): Boolean = notificationHelper.hasNotificationPermission()
}
