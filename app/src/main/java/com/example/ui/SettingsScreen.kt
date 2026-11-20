package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.util.PersianFormatters

private val INTERVAL_OPTIONS = listOf(15L, 30L, 60L, 180L)

@Composable
fun SettingsScreen(
    uiState: ModemRadarUiState,
    onToggleBackgroundMonitoring: (Boolean) -> Unit,
    onSelectInterval: (Long) -> Unit,
    onToggleForegroundAutoPoll: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSendTestNotification: () -> Unit,
    onSaveTargetUrl: (String) -> Unit,
    onResetDefaultUrl: () -> Unit,
    onOpenTargetSite: () -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlDraft by remember { mutableStateOf(uiState.settings.targetUrl) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.settings.targetUrl) {
        urlDraft = uiState.settings.targetUrl
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.settings_clear_cache)) },
            text = { Text(stringResource(R.string.settings_clear_cache_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheDialog = false
                        onClearCache()
                    },
                    modifier = Modifier.testTag("confirm_clear_cache_button")
                ) {
                    Text("پاک‌سازی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "settings_title") {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Monitoring Card
            item(key = "monitoring_card") {
                SettingsSectionCard(
                    title = stringResource(R.string.settings_monitoring_section),
                    icon = Icons.Filled.Schedule
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_enable_bg),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_enable_bg_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = uiState.settings.isMonitoringEnabled,
                            onCheckedChange = onToggleBackgroundMonitoring,
                            modifier = Modifier.testTag("toggle_bg_monitoring")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = stringResource(R.string.settings_interval_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        INTERVAL_OPTIONS.forEach { minutes ->
                            FilterChip(
                                selected = uiState.settings.intervalMinutes == minutes,
                                onClick = { onSelectInterval(minutes) },
                                enabled = uiState.settings.isMonitoringEnabled,
                                modifier = Modifier.testTag("interval_chip_$minutes"),
                                label = {
                                    Text(PersianFormatters.formatIntervalLabel(minutes))
                                }
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.settings_interval_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_foreground_poll),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_foreground_poll_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = uiState.settings.foregroundAutoPollEnabled,
                            onCheckedChange = onToggleForegroundAutoPoll,
                            modifier = Modifier.testTag("toggle_foreground_poll")
                        )
                    }
                }
            }

            // Notifications Card
            item(key = "notifications_card") {
                SettingsSectionCard(
                    title = stringResource(R.string.settings_notification_section),
                    icon = Icons.Filled.NotificationsActive
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.hasNotificationPermission) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Filled.NotificationsOff
                            },
                            contentDescription = null,
                            tint = if (uiState.hasNotificationPermission) StatusSuccess else StatusError
                        )
                        Text(
                            text = if (uiState.hasNotificationPermission) {
                                stringResource(R.string.settings_notif_granted)
                            } else {
                                stringResource(R.string.settings_notif_denied)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!uiState.hasNotificationPermission) {
                            Button(
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("settings_enable_notif_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.permission_notif_action))
                            }
                        }

                        OutlinedButton(
                            onClick = onSendTestNotification,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("test_notification_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_test_notif))
                        }
                    }
                }
            }

            // Connected URL Card
            item(key = "url_card") {
                SettingsSectionCard(
                    title = stringResource(R.string.settings_url_section),
                    icon = Icons.Filled.Link
                ) {
                    Text(
                        text = stringResource(R.string.settings_url_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = urlDraft,
                            onValueChange = { urlDraft = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_url_input"),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSaveTargetUrl(urlDraft) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("save_url_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_url_save))
                        }

                        OutlinedButton(
                            onClick = onOpenTargetSite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_open_url_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = stringResource(R.string.action_open_site),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    TextButton(
                        onClick = onResetDefaultUrl,
                        modifier = Modifier.testTag("reset_url_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_url_reset))
                    }
                }
            }

            // Cache Management Card
            item(key = "cache_card") {
                SettingsSectionCard(
                    title = stringResource(R.string.settings_cache_section),
                    icon = Icons.Filled.CleaningServices
                ) {
                    Text(
                        text = stringResource(R.string.settings_clear_cache_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showClearCacheDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("clear_cache_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_clear_cache))
                    }
                }
            }

            item(key = "settings_bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}
