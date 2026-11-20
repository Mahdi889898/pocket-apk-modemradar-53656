package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ModemRadarApp
import com.example.data.local.CheckLogEntity
import com.example.data.local.ListingEntity
import com.example.data.local.MonitorSettings
import com.example.worker.MonitorScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ListingFilter {
    ALL,
    NEW_ONLY,
    BOOKMARKED
}

enum class AppDestination {
    RADAR,
    LOGS,
    SETTINGS
}

data class ModemRadarUiState(
    val allListings: List<ListingEntity> = emptyList(),
    val filteredListings: List<ListingEntity> = emptyList(),
    val logs: List<CheckLogEntity> = emptyList(),
    val settings: MonitorSettings = MonitorSettings(),
    val selectedFilter: ListingFilter = ListingFilter.ALL,
    val searchQuery: String = "",
    val isChecking: Boolean = false,
    val lastErrorMessage: String? = null,
    val hasNotificationPermission: Boolean = true
) {
    val totalCount: Int get() = allListings.size
    val newCount: Int get() = allListings.count { it.isNew }
    val bookmarkedCount: Int get() = allListings.count { it.isBookmarked }
}

class ModemRadarViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ModemRadarApp
    private val repository = app.repository

    private val selectedFilter = MutableStateFlow(ListingFilter.ALL)
    private val searchQuery = MutableStateFlow("")
    private val isChecking = MutableStateFlow(false)
    private val lastErrorMessage = MutableStateFlow<String?>(null)
    private val hasNotificationPermission = MutableStateFlow(repository.hasNotificationPermission())

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val coreDataFlow = combine(
        repository.listingsFlow,
        repository.logsFlow,
        repository.settingsFlow
    ) { listings, logs, settings ->
        Triple(listings, logs, settings)
    }

    private val uiControlFlow = combine(
        selectedFilter,
        searchQuery,
        isChecking,
        lastErrorMessage,
        hasNotificationPermission
    ) { filter, query, checking, errorMsg, notifGranted ->
        UiControls(filter, query, checking, errorMsg, notifGranted)
    }

    val uiState: StateFlow<ModemRadarUiState> = combine(
        coreDataFlow,
        uiControlFlow
    ) { (listings, logs, settings), controls ->
        val trimmedQuery = controls.searchQuery.trim()
        val filtered = listings.filter { item ->
            val matchesFilter = when (controls.filter) {
                ListingFilter.ALL -> true
                ListingFilter.NEW_ONLY -> item.isNew
                ListingFilter.BOOKMARKED -> item.isBookmarked
            }
            val matchesQuery = trimmedQuery.isBlank() ||
                item.title.contains(trimmedQuery, ignoreCase = true) ||
                item.priceText.contains(trimmedQuery, ignoreCase = true) ||
                (item.badgeText?.contains(trimmedQuery, ignoreCase = true) == true)
            matchesFilter && matchesQuery
        }

        ModemRadarUiState(
            allListings = listings,
            filteredListings = filtered,
            logs = logs,
            settings = settings,
            selectedFilter = controls.filter,
            searchQuery = controls.searchQuery,
            isChecking = controls.isChecking,
            lastErrorMessage = controls.errorMessage,
            hasNotificationPermission = controls.hasNotificationPermission
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ModemRadarUiState(
            hasNotificationPermission = repository.hasNotificationPermission()
        )
    )

    init {
        // Trigger an immediate check on launch so the user sees live status right away
        checkNow(triggerSource = "شروع برنامه", showSuccessToast = false)

        // Foreground live polling loop (every 60s while app is active if enabled)
        viewModelScope.launch {
            repository.settingsFlow
                .map { it.foregroundAutoPollEnabled }
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    if (!enabled) return@collectLatest
                    while (isActive) {
                        delay(60_000L)
                        if (!isChecking.value) {
                            checkNow(triggerSource = "پایش زنده داخل برنامه", showSuccessToast = false)
                        }
                    }
                }
        }
    }

    fun refreshNotificationPermission() {
        hasNotificationPermission.value = repository.hasNotificationPermission()
    }

    fun checkNow(triggerSource: String = "بررسی دستی", showSuccessToast: Boolean = true) {
        if (isChecking.value) return
        viewModelScope.launch {
            isChecking.value = true
            try {
                val outcome = repository.syncNow(triggerSource)
                if (outcome.success) {
                    lastErrorMessage.value = null
                    if (showSuccessToast || outcome.newCount > 0) {
                        _snackbarMessages.tryEmit(outcome.message)
                    }
                } else {
                    lastErrorMessage.value = outcome.message
                    if (showSuccessToast) {
                        _snackbarMessages.tryEmit(outcome.message)
                    }
                }
            } finally {
                isChecking.value = false
            }
        }
    }

    fun selectFilter(filter: ListingFilter) {
        selectedFilter.value = filter
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun markListingSeen(externalId: String) {
        viewModelScope.launch {
            repository.markAsSeen(externalId)
        }
    }

    fun markAllSeen() {
        viewModelScope.launch {
            repository.markAllAsSeen()
            _snackbarMessages.tryEmit("همه آگهی‌های جدید به‌عنوان دیده‌شده علامت خوردند.")
        }
    }

    fun toggleBookmark(item: ListingEntity) {
        viewModelScope.launch {
            repository.toggleBookmark(item.externalId, item.isBookmarked)
        }
    }

    fun setBackgroundMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMonitoringEnabled(enabled)
            val interval = uiState.value.settings.intervalMinutes
            if (enabled) {
                MonitorScheduler.schedule(app, interval)
                _snackbarMessages.tryEmit("پایش خودکار پس‌زمینه فعال شد.")
            } else {
                MonitorScheduler.cancel(app)
                _snackbarMessages.tryEmit("پایش خودکار پس‌زمینه متوقف شد.")
            }
        }
    }

    fun setIntervalMinutes(minutes: Long) {
        viewModelScope.launch {
            repository.setIntervalMinutes(minutes)
            if (uiState.value.settings.isMonitoringEnabled) {
                MonitorScheduler.schedule(app, minutes)
            }
            _snackbarMessages.tryEmit("فاصله بررسی پس‌زمینه به‌روزرسانی شد.")
        }
    }

    fun setForegroundAutoPoll(enabled: Boolean) {
        viewModelScope.launch {
            repository.setForegroundAutoPollEnabled(enabled)
        }
    }

    fun saveTargetUrl(url: String) {
        val cleaned = url.trim()
        if (!cleaned.startsWith("https://") && !cleaned.startsWith("http://")) {
            _snackbarMessages.tryEmit("لطفاً یک آدرس معتبر با شروع https:// وارد کنید.")
            return
        }
        viewModelScope.launch {
            repository.updateTargetUrl(cleaned)
            _snackbarMessages.tryEmit("لینک هدف ذخیره شد. در حال بررسی آدرس جدید…")
            checkNow(triggerSource = "تغییر لینک هدف", showSuccessToast = true)
        }
    }

    fun resetDefaultUrl() {
        viewModelScope.launch {
            repository.resetTargetUrl()
            _snackbarMessages.tryEmit("لینک پیش‌فرض دسته مودم بازگردانی شد.")
            checkNow(triggerSource = "بازگردانی لینک پیش‌فرض", showSuccessToast = true)
        }
    }

    fun clearCachedListings() {
        viewModelScope.launch {
            repository.clearCachedListings()
            _snackbarMessages.tryEmit("حافظه آگهی‌ها پاک شد. بررسی بعدی لیست جدید را ثبت می‌کند.")
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _snackbarMessages.tryEmit("گزارش‌های پایش پاک شدند.")
        }
    }

    fun sendTestNotification() {
        refreshNotificationPermission()
        val sent = repository.sendTestNotification()
        if (sent) {
            _snackbarMessages.tryEmit("اعلان آزمایشی ارسال شد.")
        } else {
            _snackbarMessages.tryEmit("ابتدا مجوز ارسال اعلان را فعال کنید.")
        }
    }

    private data class UiControls(
        val filter: ListingFilter,
        val searchQuery: String,
        val isChecking: Boolean,
        val errorMessage: String?,
        val hasNotificationPermission: Boolean
    )
}
