package com.snj.travelvista.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WikivoyageSearchResponseDto(
    @SerializedName("pages")
    val pages: List<WikivoyageSearchPageDto> = emptyList()
)

data class WikivoyageSearchPageDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("key")
    val key: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("excerpt")
    val excerpt: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("thumbnail")
    val thumbnail: SearchThumbnailDto? = null
)

data class SearchThumbnailDto(
    @SerializedName("url")
    val url: String? = null,

    @SerializedName("width")
    val width: Int? = null,

    @SerializedName("height")
    val height: Int? = null
)
