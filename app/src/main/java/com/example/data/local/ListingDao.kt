package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {

    @Query("SELECT * FROM listings ORDER BY isNew DESC, firstSeenAt DESC")
    fun observeAllListings(): Flow<List<ListingEntity>>

    @Query("SELECT externalId FROM listings WHERE sourceUrl = :sourceUrl")
    suspend fun getExistingIdsForSource(sourceUrl: String): List<String>

    @Query("SELECT COUNT(*) FROM listings WHERE sourceUrl = :sourceUrl")
    suspend fun countForSource(sourceUrl: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNewListings(listings: List<ListingEntity>)

    @Query(
        """
        UPDATE listings
        SET title = :title,
            priceText = :priceText,
            imageUrl = COALESCE(:imageUrl, imageUrl),
            badgeText = COALESCE(:badgeText, badgeText),
            lastSeenAt = :lastSeenAt
        WHERE externalId = :externalId
        """
    )
    suspend fun updateExistingListing(
        externalId: String,
        title: String,
        priceText: String,
        imageUrl: String?,
        badgeText: String?,
        lastSeenAt: Long
    )

    @Query("UPDATE listings SET isNew = 0 WHERE externalId = :externalId")
    suspend fun markAsSeen(externalId: String)

    @Query("UPDATE listings SET isNew = 0 WHERE isNew = 1")
    suspend fun markAllAsSeen()

    @Query("UPDATE listings SET isBookmarked = :bookmarked WHERE externalId = :externalId")
    suspend fun setBookmarked(externalId: String, bookmarked: Boolean)

    @Query("DELETE FROM listings")
    suspend fun clearAll()
}
