package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.local.ListingEntity
import com.example.ui.theme.HeroOverlayDark
import com.example.ui.theme.HeroSignalGlow
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.util.PersianFormatters

@Composable
fun RadarScreen(
    uiState: ModemRadarUiState,
    onCheckNow: () -> Unit,
    onOpenTargetSite: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSelectFilter: (ListingFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onListingClick: (ListingEntity) -> Unit,
    onToggleBookmark: (ListingEntity) -> Unit,
    onMarkAllSeen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            item(key = "hero_radar") {
                RadarHeroCard(
                    uiState = uiState,
                    onCheckNow = onCheckNow,
                    onOpenTargetSite = onOpenTargetSite
                )
            }

            if (!uiState.hasNotificationPermission) {
                item(key = "notif_permission_banner") {
                    NotificationPermissionCard(onEnableClick = onRequestNotificationPermission)
                }
            }

            if (uiState.lastErrorMessage != null) {
                item(key = "error_banner") {
                    DiagnosticErrorCard(
                        message = uiState.lastErrorMessage,
                        onRetry = onCheckNow,
                        onOpenSite = onOpenTargetSite
                    )
                }
            }

            item(key = "search_and_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_listings_input"),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_hint),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.testTag("clear_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cd_clear_search)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = uiState.selectedFilter == ListingFilter.ALL,
                            onClick = { onSelectFilter(ListingFilter.ALL) },
                            modifier = Modifier.testTag("filter_chip_all"),
                            label = {
                                Text(
                                    PersianFormatters.toPersianDigits(
                                        stringResource(R.string.filter_all, uiState.totalCount)
                                    )
                                )
                            }
                        )

                        FilterChip(
                            selected = uiState.selectedFilter == ListingFilter.NEW_ONLY,
                            onClick = { onSelectFilter(ListingFilter.NEW_ONLY) },
                            modifier = Modifier.testTag("filter_chip_new"),
                            leadingIcon = if (uiState.newCount > 0) {
                                {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StatusWarning)
                                    )
                                }
                            } else {
                                null
                            },
                            label = {
                                Text(
                                    PersianFormatters.toPersianDigits(
                                        stringResource(R.string.filter_new, uiState.newCount)
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )

                        FilterChip(
                            selected = uiState.selectedFilter == ListingFilter.BOOKMARKED,
                            onClick = { onSelectFilter(ListingFilter.BOOKMARKED) },
                            modifier = Modifier.testTag("filter_chip_bookmarked"),
                            label = {
                                Text(
                                    PersianFormatters.toPersianDigits(
                                        stringResource(R.string.filter_bookmarked, uiState.bookmarkedCount)
                                    )
                                )
                            }
                        )

                        AnimatedVisibility(
                            visible = uiState.newCount > 0,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            TextButton(
                                onClick = onMarkAllSeen,
                                modifier = Modifier.testTag("mark_all_seen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_mark_all_seen))
                            }
                        }
                    }
                }
            }

            if (uiState.filteredListings.isEmpty()) {
                item(key = "empty_state") {
                    EmptyListingsCard(
                        hasAnyListings = uiState.allListings.isNotEmpty(),
                        isChecking = uiState.isChecking,
                        onCheckNow = onCheckNow,
                        onOpenSite = onOpenTargetSite
                    )
                }
            } else {
                items(
                    items = uiState.filteredListings,
                    key = { it.externalId }
                ) { listing ->
                    ListingCard(
                        listing = listing,
                        onOpenClick = {
                            onListingClick(listing)
                            PersianFormatters.openSafeUrl(context, listing.listingUrl)
                        },
                        onBookmarkToggle = { onToggleBookmark(listing) }
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RadarHeroCard(
    uiState: ModemRadarUiState,
    onCheckNow: () -> Unit,
    onOpenTargetSite: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val isActive = uiState.settings.isMonitoringEnabled

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_radar_banner_1795209125930),
                contentDescription = stringResource(R.string.cd_radar_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                HeroOverlayDark.copy(alpha = 0.78f),
                                HeroOverlayDark.copy(alpha = 0.90f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.radar_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.radar_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.80f)
                        )
                    }

                    // Live pulse pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(
                            1.dp,
                            if (isActive) HeroSignalGlow.copy(alpha = 0.55f)
                            else Color.White.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(14.dp)
                            ) {
                                if (isActive || uiState.isChecking) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .scale(pulseScale)
                                            .alpha(pulseAlpha)
                                            .clip(CircleShape)
                                            .background(
                                                if (uiState.isChecking) HeroSignalGlow else StatusSuccess
                                            )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                uiState.isChecking -> HeroSignalGlow
                                                isActive -> StatusSuccess
                                                else -> Color.White.copy(alpha = 0.45f)
                                            }
                                        )
                                )
                            }
                            Text(
                                text = when {
                                    uiState.isChecking -> stringResource(R.string.status_checking)
                                    isActive -> stringResource(R.string.status_active)
                                    else -> stringResource(R.string.status_paused)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }

                // 3-stat strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroStatTile(
                        label = stringResource(R.string.stat_total_ads),
                        value = PersianFormatters.toPersianDigits(uiState.totalCount),
                        accent = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatTile(
                        label = stringResource(R.string.stat_new_ads),
                        value = PersianFormatters.toPersianDigits(uiState.newCount),
                        accent = if (uiState.newCount > 0) HeroSignalGlow else Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatTile(
                        label = stringResource(R.string.stat_interval),
                        value = PersianFormatters.formatIntervalLabel(uiState.settings.intervalMinutes),
                        accent = Color(0xFF7CE8DF),
                        modifier = Modifier.weight(1.25f)
                    )
                }

                val lastCheckText = if (uiState.settings.lastCheckedAt > 0L) {
                    stringResource(
                        R.string.last_check_prefix,
                        "${PersianFormatters.formatRelativeTime(uiState.settings.lastCheckedAt)} (${PersianFormatters.formatTimeOfDay(uiState.settings.lastCheckedAt)})"
                    )
                } else {
                    stringResource(R.string.last_check_never)
                }

                Text(
                    text = lastCheckText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f)
                )

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCheckNow,
                        enabled = !uiState.isChecking,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("check_now_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HeroSignalGlow,
                            contentColor = Color(0xFF1F1300),
                            disabledContainerColor = HeroSignalGlow.copy(alpha = 0.55f)
                        )
                    ) {
                        if (uiState.isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF1F1300)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_check_now),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenTargetSite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_site_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.action_open_site),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "سایت")
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NotificationPermissionCard(onEnableClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.permission_notif_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.permission_notif_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onEnableClick,
                    modifier = Modifier.testTag("request_notif_permission_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.permission_notif_action))
                }
            }
        }
    }
}

@Composable
private fun DiagnosticErrorCard(
    message: String,
    onRetry: () -> Unit,
    onOpenSite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = StatusError.copy(alpha = 0.10f)
        ),
        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = StatusError
                )
                Text(
                    text = stringResource(R.string.error_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag("retry_check_button")
                ) {
                    Text(stringResource(R.string.action_retry))
                }
                TextButton(onClick = onOpenSite) {
                    Text(stringResource(R.string.action_open_site))
                }
            }
        }
    }
}

@Composable
private fun ListingCard(
    listing: ListingEntity,
    onOpenClick: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    val borderColor = if (listing.isNew) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenClick)
            .testTag("listing_card_${listing.externalId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(if (listing.isNew) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (listing.isNew) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Listing thumbnail
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (!listing.imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = listing.imageUrl,
                        contentDescription = stringResource(R.string.cd_listing_image),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = {
                            Icon(
                                imageVector = Icons.Filled.Router,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                modifier = Modifier.size(34.dp)
                            )
                        },
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Router,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        modifier = Modifier.size(34.dp)
                    )
                }

                if (listing.isNew) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(5.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = stringResource(R.string.badge_new),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Details column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("bookmark_button_${listing.externalId}")
                    ) {
                        Icon(
                            imageVector = if (listing.isBookmarked) {
                                Icons.Filled.Bookmark
                            } else {
                                Icons.Outlined.BookmarkBorder
                            },
                            contentDescription = if (listing.isBookmarked) {
                                stringResource(R.string.cd_bookmark_remove)
                            } else {
                                stringResource(R.string.cd_bookmark_add)
                            },
                            tint = if (listing.isBookmarked) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = PersianFormatters.toPersianDigits(
                                listing.priceText.ifBlank {
                                    stringResource(R.string.price_unspecified)
                                }
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (!listing.badgeText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = listing.badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "شناسه ${PersianFormatters.toPersianDigits(listing.externalId)} • ${PersianFormatters.formatRelativeTime(listing.firstSeenAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.action_open_listing),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyListingsCard(
    hasAnyListings: Boolean,
    isChecking: Boolean,
    onCheckNow: () -> Unit,
    onOpenSite: () -> Unit
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Radar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            Text(
                text = if (hasAnyListings) {
                    stringResource(R.string.empty_filter_title)
                } else {
                    stringResource(R.string.empty_title)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (hasAnyListings) {
                    stringResource(R.string.empty_filter_body)
                } else {
                    stringResource(R.string.empty_body)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasAnyListings) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onCheckNow,
                        enabled = !isChecking,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.action_check_now))
                    }
                    OutlinedButton(
                        onClick = onOpenSite,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.action_open_site))
                    }
                }
            }
        }
    }
}
