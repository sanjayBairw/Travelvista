package com.snj.travelvista.data.remote.service

import com.snj.travelvista.data.remote.dto.OpenMeteoResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo Global Weather API Service.
 * Keyless, open-source weather & forecast provider returning live temperature, wind, humidity, and WMO conditions.
 */
interface WeatherService {

    @GET("forecast")
    suspend fun getWeatherByCoordinates(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 1
    ): OpenMeteoResponseDto
}
