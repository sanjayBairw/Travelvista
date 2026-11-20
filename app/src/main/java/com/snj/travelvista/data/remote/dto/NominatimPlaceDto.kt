package com.snj.travelvista.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response item from OpenStreetMap Nominatim Geocoding Search API.
 * Enables worldwide discovery of any country, city, region, island, mountain, beach, or attraction.
 */
data class NominatimPlaceDto(
    @SerializedName("place_id")
    val placeId: Long = 0L,

    @SerializedName("osm_type")
    val osmType: String? = null,

    @SerializedName("osm_id")
    val osmId: Long? = null,

    @SerializedName("lat")
    val lat: String? = null,

    @SerializedName("lon")
    val lon: String? = null,

    @SerializedName("category")
    val category: String? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("display_name")
    val displayName: String? = null,

    @SerializedName("importance")
    val importance: Double? = null,

    @SerializedName("address")
    val address: NominatimAddressDto? = null
) {
    val latitudeDouble: Double?
        get() = lat?.toDoubleOrNull()

    val longitudeDouble: Double?
        get() = lon?.toDoubleOrNull()

    val resolvedTitle: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: address?.city
            ?: address?.town
            ?: address?.village
            ?: address?.state
            ?: address?.country
            ?: displayName?.substringBefore(",")?.trim()
            ?: "Unknown Location"

    val resolvedCountry: String
        get() = address?.country?.takeIf { it.isNotBlank() }
            ?: displayName?.substringAfterLast(",")?.trim()
            ?: ""

    val resolvedRegion: String
        get() = listOfNotNull(
            address?.state?.takeIf { it.isNotBlank() && !it.equals(resolvedTitle, ignoreCase = true) },
            resolvedCountry.takeIf { it.isNotBlank() && !it.equals(resolvedTitle, ignoreCase = true) }
        ).joinToString(", ").ifBlank { resolvedCountry.ifBlank { "Global Destination" } }

    fun mapToTravelCategory(): String {
        val t = (type ?: "").lowercase()
        val c = (category ?: "").lowercase()
        return when {
            t in listOf("peak", "mountain_range", "volcano", "ridge", "glacier", "saddle") -> "Mountains"
            t in listOf("beach", "island", "islet", "bay", "coastline", "reef", "archipelago") -> "Beaches"
            c == "historic" || t in listOf("monument", "castle", "ruins", "archaeological_site", "museum", "heritage") -> "Historical Places"
            c == "natural" || t in listOf("national_park", "nature_reserve", "forest", "waterfall", "park", "lake", "valley") -> "Nature"
            else -> "Cities"
        }
    }
}

data class NominatimAddressDto(
    @SerializedName("city")
    val city: String? = null,

    @SerializedName("town")
    val town: String? = null,

    @SerializedName("village")
    val village: String? = null,

    @SerializedName("county")
    val county: String? = null,

    @SerializedName("state")
    val state: String? = null,

    @SerializedName("region")
    val region: String? = null,

    @SerializedName("country")
    val country: String? = null,

    @SerializedName("country_code")
    val countryCode: String? = null
)
