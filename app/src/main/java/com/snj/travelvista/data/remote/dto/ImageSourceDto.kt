package com.snj.travelvista.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ImageSourceDto(
    @SerializedName("source")
    val source: String,

    @SerializedName("width")
    val width: Int? = null,

    @SerializedName("height")
    val height: Int? = null
)
