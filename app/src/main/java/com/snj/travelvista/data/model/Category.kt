package com.snj.travelvista.data.model

import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("iconName")
    val iconName: String = "",

    val iconResId: Int = 0,

    @SerializedName("count")
    val count: Int = 0,

    var isSelected: Boolean = false
)
