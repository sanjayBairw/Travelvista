package com.snj.travelvista.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.snj.travelvista.data.local.entity.ItineraryItemEntity
import com.snj.travelvista.data.local.entity.RecentDestinationEntity
import com.snj.travelvista.data.local.entity.SearchHistoryEntity
import com.snj.travelvista.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlobalTravelDao {

    // --- SEARCH HISTORY ---

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // --- RECENTLY VIEWED DESTINATIONS ---

    @Query("SELECT * FROM recent_destinations ORDER BY viewedAtTimestamp DESC LIMIT 10")
    fun getRecentDestinations(): Flow<List<RecentDestinationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentDestination(entry: RecentDestinationEntity)

    @Query("DELETE FROM recent_destinations")
    suspend fun clearRecentDestinations()

    // --- OFFLINE TRIP PLANNER ---

    @Query("SELECT * FROM trips ORDER BY createdAtTimestamp DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun getTripById(tripId: Long): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTripById(tripId: Long): Int

    // --- DAY-WISE ITINERARY ITEMS ---

    @Query("SELECT * FROM itinerary_items WHERE tripId = :tripId ORDER BY dayNumber ASC, id ASC")
    fun getItineraryForTrip(tripId: Long): Flow<List<ItineraryItemEntity>>

    @Query("SELECT * FROM itinerary_items WHERE tripId = :tripId ORDER BY dayNumber ASC, id ASC")
    suspend fun getItineraryForTripSync(tripId: Long): List<ItineraryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItineraryItem(item: ItineraryItemEntity): Long

    @Query("DELETE FROM itinerary_items WHERE id = :itemId")
    suspend fun deleteItineraryItemById(itemId: Long): Int
}
