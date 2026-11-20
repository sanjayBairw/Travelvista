package com.snj.travelvista.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from Open-Meteo real-time global weather API (keyless, open data).
 */
data class OpenMeteoResponseDto(
    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null,

    @SerializedName("timezone")
    val timezone: String? = null,

    @SerializedName("timezone_abbreviation")
    val timezoneAbbreviation: String? = null,

    @SerializedName("current")
    val current: CurrentWeatherDto? = null,

    @SerializedName("daily")
    val daily: DailyWeatherDto? = null
)

data class CurrentWeatherDto(
    @SerializedName("time")
    val time: String? = null,

    @SerializedName("temperature_2m")
    val temperatureCelsius: Double? = null,

    @SerializedName("relative_humidity_2m")
    val humidityPercent: Int? = null,

    @SerializedName("weather_code")
    val weatherCode: Int? = null,

    @SerializedName("wind_speed_10m")
    val windSpeedKmh: Double? = null
) {
    fun conditionDescription(): String = decodeWmoWeatherCode(weatherCode)

    companion object {
        fun decodeWmoWeatherCode(code: Int?): String = when (code) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Light drizzle"
            56, 57 -> "Freezing drizzle"
            61, 63 -> "Rain"
            65 -> "Heavy rain"
            66, 67 -> "Freezing rain"
            71, 73, 75 -> "Snowfall"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Conditions unavailable"
        }
    }
}

data class DailyWeatherDto(
    @SerializedName("temperature_2m_max")
    val maxTempsCelsius: List<Double>? = null,

    @SerializedName("temperature_2m_min")
    val minTempsCelsius: List<Double>? = null
)
