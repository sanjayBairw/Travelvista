package com.snj.travelvista.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.snj.travelvista.data.local.entity.CachedDestinationEntity
import com.snj.travelvista.data.local.entity.DestinationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DestinationDao {

    // --- FAVORITES TABLE ---

    @Query("SELECT * FROM favorite_destinations ORDER BY savedAtTimestamp DESC")
    fun getAllFavorites(): Flow<List<DestinationEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_destinations WHERE id = :id)")
    fun isFavorite(id: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_destinations WHERE id = :id)")
    suspend fun isFavoriteSync(id: Long): Boolean

    @Query("SELECT * FROM favorite_destinations WHERE id = :id LIMIT 1")
    suspend fun getFavoriteById(id: Long): DestinationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(destination: DestinationEntity): Long

    @Query("DELETE FROM favorite_destinations WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long): Int

    // --- OFFLINE CACHE TABLE ---

    @Query("SELECT * FROM cached_destinations ORDER BY id ASC")
    fun getAllCachedDestinations(): Flow<List<CachedDestinationEntity>>

    @Query("SELECT * FROM cached_destinations ORDER BY id ASC")
    suspend fun getAllCachedDestinationsSync(): List<CachedDestinationEntity>

    @Query("SELECT * FROM cached_destinations WHERE id = :id LIMIT 1")
    suspend fun getCachedDestinationById(id: Long): CachedDestinationEntity?

    @Query("SELECT * FROM cached_destinations WHERE slug = :slug LIMIT 1")
    suspend fun getCachedDestinationBySlug(slug: String): CachedDestinationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedDestinations(destinations: List<CachedDestinationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedDestination(destination: CachedDestinationEntity): Long

    @Query("DELETE FROM cached_destinations")
    suspend fun clearCachedDestinations()
}
