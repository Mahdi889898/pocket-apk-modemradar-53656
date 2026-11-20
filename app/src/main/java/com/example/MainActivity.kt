package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppDestination
import com.example.ui.LogsScreen
import com.example.ui.ModemRadarViewModel
import com.example.ui.RadarScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.ModemRadarTheme
import com.example.ui.util.PersianFormatters
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ModemRadarTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    ModemRadarRoot()
                }
            }
        }
    }
}

private data class NavItemSpec(
    val destination: AppDestination,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
private fun ModemRadarRoot(
    viewModel: ModemRadarViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.RADAR) }
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshNotificationPermission()
        if (granted) {
            viewModel.sendTestNotification()
        }
    }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.refreshNotificationPermission()
        }
    }

    // Request notification permission once on Android 13+ if not yet granted
    var promptedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!promptedOnce && !uiState.hasNotificationPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            promptedOnce = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = currentDestination != AppDestination.RADAR) {
        currentDestination = AppDestination.RADAR
    }

    val navItems = remember {
        listOf(
            NavItemSpec(
                destination = AppDestination.RADAR,
                labelRes = R.string.nav_radar,
                selectedIcon = Icons.Filled.Radar,
                unselectedIcon = Icons.Outlined.Radar,
                testTag = "nav_tab_radar"
            ),
            NavItemSpec(
                destination = AppDestination.LOGS,
                labelRes = R.string.nav_logs,
                selectedIcon = Icons.Filled.History,
                unselectedIcon = Icons.Outlined.History,
                testTag = "nav_tab_logs"
            ),
            NavItemSpec(
                destination = AppDestination.SETTINGS,
                labelRes = R.string.nav_settings,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_tab_settings"
            )
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination == item.destination
                            val label = stringResource(item.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = item.destination },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    NavIconWithBadge(
                                        icon = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label,
                                        badgeCount = if (item.destination == AppDestination.RADAR) {
                                            uiState.newCount
                                        } else {
                                            0
                                        }
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination == item.destination
                            val label = stringResource(item.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentDestination = item.destination },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    NavIconWithBadge(
                                        icon = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label,
                                        badgeCount = if (item.destination == AppDestination.RADAR) {
                                            uiState.newCount
                                        } else {
                                            0
                                        }
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentDestination) {
                        AppDestination.RADAR -> RadarScreen(
                            uiState = uiState,
                            onCheckNow = { viewModel.checkNow() },
                            onOpenTargetSite = {
                                PersianFormatters.openSafeUrl(context, uiState.settings.targetUrl)
                            },
                            onRequestNotificationPermission = requestNotificationPermission,
                            onSelectFilter = viewModel::selectFilter,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onListingClick = { listing ->
                                viewModel.markListingSeen(listing.externalId)
                            },
                            onToggleBookmark = viewModel::toggleBookmark,
                            onMarkAllSeen = viewModel::markAllSeen
                        )

                        AppDestination.LOGS -> LogsScreen(
                            logs = uiState.logs,
                            onClearLogs = viewModel::clearLogs
                        )

                        AppDestination.SETTINGS -> SettingsScreen(
                            uiState = uiState,
                            onToggleBackgroundMonitoring = viewModel::setBackgroundMonitoring,
                            onSelectInterval = viewModel::setIntervalMinutes,
                            onToggleForegroundAutoPoll = viewModel::setForegroundAutoPoll,
                            onRequestNotificationPermission = requestNotificationPermission,
                            onSendTestNotification = viewModel::sendTestNotification,
                            onSaveTargetUrl = viewModel::saveTargetUrl,
                            onResetDefaultUrl = viewModel::resetDefaultUrl,
                            onOpenTargetSite = {
                                PersianFormatters.openSafeUrl(context, uiState.settings.targetUrl)
                            },
                            onClearCache = viewModel::clearCachedListings
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavIconWithBadge(
    icon: ImageVector,
    contentDescription: String,
    badgeCount: Int
) {
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Text(PersianFormatters.toPersianDigits(badgeCount.coerceAtMost(99)))
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    } else {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}
