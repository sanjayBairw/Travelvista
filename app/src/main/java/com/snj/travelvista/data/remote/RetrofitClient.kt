package com.snj.travelvista.data.remote

import com.snj.travelvista.BuildConfig
import com.snj.travelvista.data.remote.service.CountryInfoService
import com.snj.travelvista.data.remote.service.CurrencyService
import com.snj.travelvista.data.remote.service.GeocodingService
import com.snj.travelvista.data.remote.service.WeatherService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val MOCK_BASE_URL = "https://api.travelvista.internal/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    /**
     * Real HTTP client for live public API calls.
     * Uses UserAgentInterceptor per Wikimedia & OpenStreetMap Nominatim guidelines.
     * Never logs secrets in release builds.
     */
    val realOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Real Retrofit service connecting to Wikivoyage / Wikimedia REST API.
     */
    val wikivoyageApiService: WikivoyageApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.DESTINATION_API_BASE_URL)
            .client(realOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WikivoyageApiService::class.java)
    }

    /**
     * OpenStreetMap Nominatim worldwide geocoding & search service.
     */
    val geocodingService: GeocodingService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.NOMINATIM_API_BASE_URL)
            .client(realOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingService::class.java)
    }

    /**
     * Open-Meteo real-time global weather service.
     */
    val weatherService: WeatherService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.WEATHER_API_BASE_URL)
            .client(realOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherService::class.java)
    }

    /**
     * REST Countries v3.1 metadata service (currency, language, timezone, dialing code).
     */
    val countryInfoService: CountryInfoService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.COUNTRIES_API_BASE_URL)
            .client(realOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CountryInfoService::class.java)
    }

    /**
     * Frankfurter European Central Bank verified live currency conversion service.
     */
    val currencyService: CurrencyService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.CURRENCY_API_BASE_URL)
            .client(realOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyService::class.java)
    }

    /**
     * Client with MockTravelInterceptor for explicit demo/mock fallback.
     */
    val mockOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(MockTravelInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Preserved TravelApiService for offline/mock fallback capability.
     */
    val apiService: TravelApiService by lazy {
        Retrofit.Builder()
            .baseUrl(MOCK_BASE_URL)
            .client(mockOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TravelApiService::class.java)
    }
}
