package com.snj.travelvista.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WikivoyageSectionsResponseDto(
    @SerializedName("parse")
    val parse: WikivoyageSectionsParseDto? = null
)

data class WikivoyageSectionsParseDto(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("pageid")
    val pageId: Long? = null,

    @SerializedName("sections")
    val sections: List<WikivoyageSectionItemDto>? = null
)

data class WikivoyageSectionItemDto(
    @SerializedName("toclevel")
    val toclevel: Int = 1,

    @SerializedName("level")
    val level: String? = null,

    @SerializedName("line")
    val line: String? = null,

    @SerializedName("number")
    val number: String? = null,

    @SerializedName("index")
    val index: String? = null,

    @SerializedName("anchor")
    val anchor: String? = null
)

data class WikivoyageParseTextResponseDto(
    @SerializedName("parse")
    val parse: WikivoyageParseTextDto? = null
)

data class WikivoyageParseTextDto(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("pageid")
    val pageId: Long? = null,

    @SerializedName("text")
    val text: WikivoyageTextContentDto? = null
)

data class WikivoyageTextContentDto(
    @SerializedName("*")
    val html: String? = null
) {
    val text: String? get() = html
}
