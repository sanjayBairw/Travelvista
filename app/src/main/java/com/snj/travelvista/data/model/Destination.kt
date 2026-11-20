package com.snj.travelvista.data.model

import com.google.gson.annotations.SerializedName

data class Destination(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("country")
    val country: String,

    @SerializedName("city")
    val city: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("rating")
    val rating: Double,

    @SerializedName("reviewCount")
    val reviewCount: Int,

    @SerializedName("imageUrl")
    val imageUrl: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("highlights")
    val highlights: List<String>,

    @SerializedName("travelTips")
    val travelTips: List<String>,

    @SerializedName("bestTimeToVisit")
    val bestTimeToVisit: String,

    @SerializedName("pricePerDay")
    val pricePerDay: Int,

    @SerializedName("isFeatured")
    val isFeatured: Boolean = false,

    @SerializedName("isPopular")
    val isPopular: Boolean = false,

    @SerializedName("isTrending")
    val isTrending: Boolean = false,

    @SerializedName("tags")
    val tags: List<String> = emptyList(),

    var isFavorite: Boolean = false,

    val slug: String = "",

    val latitude: Double? = null,

    val longitude: Double? = null,

    val imageAuthor: String = "",

    val imageLicense: String = "",

    val imageCreditUrl: String = ""
) {
    val location: String
        get() = when {
            city.isNotBlank() && country.isNotBlank() && !country.contains(city, ignoreCase = true) -> "$city, $country"
            city.isNotBlank() -> city
            country.isNotBlank() -> country
            else -> "World Destination"
        }

    val formattedPrice: String
        get() = if (pricePerDay > 0) "$$pricePerDay / day" else "Est. N/A"

    val formattedRating: String
        get() = if (rating > 0.0) String.format("%.1f", rating) else "New"

    val formattedReviews: String
        get() = if (reviewCount > 0) "($reviewCount)" else ""

    val attributionSummary: String
        get() = when {
            imageAuthor.isNotBlank() && imageLicense.isNotBlank() ->
                "Photo by $imageAuthor ($imageLicense)"
            imageLicense.isNotBlank() ->
                "Photo license: $imageLicense"
            else ->
                "Photo via Wikimedia Commons"
        }
}
