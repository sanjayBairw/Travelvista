package com.snj.travelvista.data.model

data class DestinationWeather(
    val temperatureCelsius: Double,
    val conditionText: String,
    val windSpeedKmh: Double,
    val humidityPercent: Int?,
    val dailyMaxCelsius: Double?,
    val dailyMinCelsius: Double?,
    val timezone: String,
    val fetchedAtMillis: Long = System.currentTimeMillis(),
    val sourceAttribution: String = "Weather by Open-Meteo"
)

data class CountryTravelInfo(
    val countryName: String,
    val countryCode: String,
    val region: String,
    val capital: String,
    val currencyDisplay: String,
    val currencyCode: String?,
    val languages: String,
    val timezone: String,
    val dialingCode: String,
    val fetchedAtMillis: Long = System.currentTimeMillis(),
    val sourceAttribution: String = "Country info by REST Countries"
)

data class NearbyAttraction(
    val pageId: Long,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double
)

data class CurrencyRateInfo(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val date: String,
    val sourceAttribution: String = "Exchange rates by Frankfurter (ECB)"
)

data class CurrencyConversionResult(
    val fromAmount: Double,
    val fromCurrency: String,
    val toAmount: Double,
    val toCurrency: String,
    val rate: Double,
    val date: String,
    val sourceAttribution: String = "Exchange rates by European Central Bank (Frankfurter)"
)
