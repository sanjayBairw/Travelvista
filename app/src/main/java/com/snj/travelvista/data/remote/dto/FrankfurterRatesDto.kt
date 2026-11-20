package com.snj.travelvista.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from Frankfurter API (European Central Bank verified live exchange rates).
 */
data class FrankfurterRatesDto(
    @SerializedName("amount")
    val amount: Double = 1.0,

    @SerializedName("base")
    val base: String = "USD",

    @SerializedName("date")
    val date: String = "",

    @SerializedName("rates")
    val rates: Map<String, Double> = emptyMap()
)
