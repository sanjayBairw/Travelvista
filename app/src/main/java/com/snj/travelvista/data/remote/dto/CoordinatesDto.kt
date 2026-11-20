package com.snj.travelvista.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CoordinatesDto(
    @SerializedName("lat")
    val latitude: Double? = null,

    @SerializedName("lon")
    val longitude: Double? = null
)
