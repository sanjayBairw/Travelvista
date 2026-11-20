package com.snj.travelvista.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from Wikivoyage MediaWiki API geosearch endpoint for real nearby destinations/attractions.
 */
data class WikivoyageGeosearchResponseDto(
    @SerializedName("query")
    val query: WikivoyageGeosearchQueryDto? = null
)

data class WikivoyageGeosearchQueryDto(
    @SerializedName("geosearch")
    val geosearch: List<WikivoyageGeoPlaceDto> = emptyList()
)

data class WikivoyageGeoPlaceDto(
    @SerializedName("pageid")
    val pageId: Long = 0L,

    @SerializedName("title")
    val title: String = "",

    @SerializedName("lat")
    val lat: Double = 0.0,

    @SerializedName("lon")
    val lon: Double = 0.0,

    @SerializedName("dist")
    val distanceMeters: Double = 0.0
)
