package com.snj.travelvista.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WikivoyageSummaryDto(
    @SerializedName("type")
    val type: String? = null,

    @SerializedName("title")
    val title: String,

    @SerializedName("displaytitle")
    val displayTitle: String? = null,

    @SerializedName("pageid")
    val pageId: Long? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("extract")
    val extract: String? = null,

    @SerializedName("extract_html")
    val extractHtml: String? = null,

    @SerializedName("thumbnail")
    val thumbnail: ImageSourceDto? = null,

    @SerializedName("originalimage")
    val originalImage: ImageSourceDto? = null,

    @SerializedName("coordinates")
    val coordinates: CoordinatesDto? = null,

    @SerializedName("lang")
    val lang: String? = null
) {
    /**
     * Resolves the best available image URL (prefers high-res original, falls back to thumbnail).
     */
    val bestImageUrl: String?
        get() = originalImage?.source ?: thumbnail?.source
}
