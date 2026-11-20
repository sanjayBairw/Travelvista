package com.snj.travelvista.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from REST Countries v3.1 API for real country metadata:
 * local currency, languages, time zones, dialing codes, region, and capital.
 */
data class RestCountryDto(
    @SerializedName("name")
    val name: CountryNameDto? = null,

    @SerializedName("cca2")
    val countryCode: String? = null,

    @SerializedName("region")
    val region: String? = null,

    @SerializedName("subregion")
    val subregion: String? = null,

    @SerializedName("capital")
    val capital: List<String>? = null,

    @SerializedName("languages")
    val languages: Map<String, String>? = null,

    @SerializedName("currencies")
    val currencies: Map<String, CurrencyDetailDto>? = null,

    @SerializedName("timezones")
    val timezones: List<String>? = null,

    @SerializedName("idd")
    val idd: IddDto? = null,

    @SerializedName("flags")
    val flags: CountryFlagDto? = null
) {
    val primaryCurrencyCode: String?
        get() = currencies?.keys?.firstOrNull()

    val formattedCurrency: String
        get() {
            val entry = currencies?.entries?.firstOrNull() ?: return "Unavailable"
            val symbol = entry.value.symbol?.let { " ($it)" } ?: ""
            val currencyName = entry.value.name ?: entry.key
            return "$currencyName — ${entry.key}$symbol"
        }

    val formattedLanguages: String
        get() = languages?.values?.take(3)?.joinToString(", ")?.ifBlank { "Unavailable" } ?: "Unavailable"

    val formattedTimezone: String
        get() = timezones?.firstOrNull()?.ifBlank { "Unavailable" } ?: "Unavailable"

    val formattedDialingCode: String
        get() {
            val root = idd?.root ?: return "Unavailable"
            val suffix = idd.suffixes?.firstOrNull().orEmpty()
            return "$root$suffix"
        }

    val formattedRegion: String
        get() = listOfNotNull(
            subregion?.takeIf { it.isNotBlank() },
            region?.takeIf { it.isNotBlank() }
        ).joinToString(", ").ifBlank { "Global" }
}

data class CountryNameDto(
    @SerializedName("common")
    val common: String? = null,

    @SerializedName("official")
    val official: String? = null
)

data class CurrencyDetailDto(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("symbol")
    val symbol: String? = null
)

data class IddDto(
    @SerializedName("root")
    val root: String? = null,

    @SerializedName("suffixes")
    val suffixes: List<String>? = null
)

data class CountryFlagDto(
    @SerializedName("png")
    val png: String? = null,

    @SerializedName("alt")
    val alt: String? = null
)
