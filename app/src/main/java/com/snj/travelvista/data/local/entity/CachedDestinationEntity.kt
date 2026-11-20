package com.snj.travelvista.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.snj.travelvista.data.model.Destination

@Entity(tableName = "cached_destinations")
data class CachedDestinationEntity(
    @PrimaryKey
    val id: Long,
    val slug: String,
    val title: String,
    val country: String,
    val city: String,
    val category: String,
    val rating: Double,
    val reviewCount: Int,
    val imageUrl: String,
    val description: String,
    val highlightsJson: String,
    val travelTipsJson: String,
    val bestTimeToVisit: String,
    val pricePerDay: Int,
    val isFeatured: Boolean,
    val isPopular: Boolean,
    val isTrending: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageAuthor: String = "",
    val imageLicense: String = "",
    val imageCreditUrl: String = "",
    val cachedAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toDestination(): Destination {
        val gson = Gson()
        val listType = object : TypeToken<List<String>>() {}.type
        val highlightsList: List<String> = try {
            gson.fromJson(highlightsJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val tipsList: List<String> = try {
            gson.fromJson(travelTipsJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return Destination(
            id = id,
            title = title,
            country = country,
            city = city,
            category = category,
            rating = rating,
            reviewCount = reviewCount,
            imageUrl = imageUrl,
            description = description,
            highlights = highlightsList,
            travelTips = tipsList,
            bestTimeToVisit = bestTimeToVisit,
            pricePerDay = pricePerDay,
            isFeatured = isFeatured,
            isPopular = isPopular,
            isTrending = isTrending,
            slug = slug,
            latitude = latitude,
            longitude = longitude,
            imageAuthor = imageAuthor,
            imageLicense = imageLicense,
            imageCreditUrl = imageCreditUrl,
            isFavorite = false
        )
    }

    companion object {
        fun fromDestination(destination: Destination, slug: String = destination.slug): CachedDestinationEntity {
            val gson = Gson()
            return CachedDestinationEntity(
                id = destination.id,
                slug = slug.ifEmpty { destination.title.replace(" ", "_") },
                title = destination.title,
                country = destination.country,
                city = destination.city,
                category = destination.category,
                rating = destination.rating,
                reviewCount = destination.reviewCount,
                imageUrl = destination.imageUrl,
                description = destination.description,
                highlightsJson = gson.toJson(destination.highlights),
                travelTipsJson = gson.toJson(destination.travelTips),
                bestTimeToVisit = destination.bestTimeToVisit,
                pricePerDay = destination.pricePerDay,
                isFeatured = destination.isFeatured,
                isPopular = destination.isPopular,
                isTrending = destination.isTrending,
                latitude = destination.latitude,
                longitude = destination.longitude,
                imageAuthor = destination.imageAuthor,
                imageLicense = destination.imageLicense,
                imageCreditUrl = destination.imageCreditUrl,
                cachedAtTimestamp = System.currentTimeMillis()
            )
        }
    }
}
