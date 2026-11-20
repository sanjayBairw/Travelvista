package com.snj.travelvista.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.snj.travelvista.data.local.entity.CachedDestinationEntity
import com.snj.travelvista.data.local.entity.DestinationEntity
import com.snj.travelvista.data.local.entity.ItineraryItemEntity
import com.snj.travelvista.data.local.entity.RecentDestinationEntity
import com.snj.travelvista.data.local.entity.SearchHistoryEntity
import com.snj.travelvista.data.local.entity.TripEntity

@Database(
    entities = [
        DestinationEntity::class,
        CachedDestinationEntity::class,
        SearchHistoryEntity::class,
        RecentDestinationEntity::class,
        TripEntity::class,
        ItineraryItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TravelDatabase : RoomDatabase() {

    abstract fun destinationDao(): DestinationDao
    abstract fun globalTravelDao(): GlobalTravelDao

    companion object {
        @Volatile
        private var INSTANCE: TravelDatabase? = null

        /**
         * Safe production migration from Version 1 (favorites only)
         * to Version 2 (adding offline cache table with image attribution).
         * Preserves all existing user favorites without data loss.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_destinations` (
                        `id` INTEGER NOT NULL,
                        `slug` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `country` TEXT NOT NULL,
                        `city` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `rating` REAL NOT NULL,
                        `reviewCount` INTEGER NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `highlightsJson` TEXT NOT NULL,
                        `travelTipsJson` TEXT NOT NULL,
                        `bestTimeToVisit` TEXT NOT NULL,
                        `pricePerDay` INTEGER NOT NULL,
                        `isFeatured` INTEGER NOT NULL,
                        `isPopular` INTEGER NOT NULL,
                        `isTrending` INTEGER NOT NULL,
                        `latitude` REAL,
                        `longitude` REAL,
                        `imageAuthor` TEXT NOT NULL DEFAULT '',
                        `imageLicense` TEXT NOT NULL DEFAULT '',
                        `imageCreditUrl` TEXT NOT NULL DEFAULT '',
                        `cachedAtTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Safe production migration from Version 2 to Version 3:
         * Adds worldwide search history, recently viewed destinations, trips, and day-wise itinerary items.
         * Preserves all existing favorites and cached destinations without data loss.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `search_history` (
                        `query` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`query`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recent_destinations` (
                        `destinationId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `location` TEXT NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `viewedAtTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`destinationId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `trips` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `destinationTitle` TEXT NOT NULL,
                        `destinationId` INTEGER NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `endDate` TEXT NOT NULL,
                        `travelersCount` INTEGER NOT NULL,
                        `estimatedBudgetUsd` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAtTimestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `itinerary_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tripId` INTEGER NOT NULL,
                        `dayNumber` INTEGER NOT NULL,
                        `timeSlot` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `itemType` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `providerUrl` TEXT NOT NULL,
                        FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_itinerary_items_tripId` ON `itinerary_items` (`tripId`)"
                )
            }
        }

        fun getDatabase(context: Context): TravelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TravelDatabase::class.java,
                    "travel_vista_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
