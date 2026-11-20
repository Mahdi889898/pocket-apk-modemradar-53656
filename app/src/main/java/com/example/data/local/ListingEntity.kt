package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey val externalId: String,
    val title: String,
    val priceText: String,
    val imageUrl: String?,
    val listingUrl: String,
    val badgeText: String? = null,
    val sellerInfo: String? = null,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val isNew: Boolean = false,
    val isBookmarked: Boolean = false,
    val sourceUrl: String
)
