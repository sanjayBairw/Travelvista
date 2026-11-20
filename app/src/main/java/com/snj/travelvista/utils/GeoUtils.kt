package com.snj.travelvista.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Computes great-circle distance in kilometers between two WGS-84 coordinates using the Haversine formula.
     */
    fun haversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    fun formatDistance(distanceMeters: Double, useMetric: Boolean): String {
        val km = distanceMeters / 1000.0
        return if (useMetric) {
            if (km < 1.0) "${distanceMeters.roundToInt()} m"
            else String.format(Locale.US, "%.1f km", km)
        } else {
            val miles = km * 0.621371
            if (miles < 0.2) "${(distanceMeters * 3.28084).roundToInt()} ft"
            else String.format(Locale.US, "%.1f mi", miles)
        }
    }

    fun celsiusToFahrenheit(celsius: Double): Double = (celsius * 9.0 / 5.0) + 32.0

    fun formatTemperature(celsius: Double, useMetric: Boolean): String {
        return if (useMetric) {
            "${celsius.roundToInt()}°C"
        } else {
            "${celsiusToFahrenheit(celsius).roundToInt()}°F"
        }
    }

    fun formatWindSpeed(kmh: Double, useMetric: Boolean): String {
        return if (useMetric) {
            "${kmh.roundToInt()} km/h"
        } else {
            "${(kmh * 0.621371).roundToInt()} mph"
        }
    }

    fun currencySymbol(code: String): String = when (code.uppercase(Locale.US)) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "INR" -> "₹"
        "JPY" -> "¥"
        "AUD" -> "A$"
        "CAD" -> "C$"
        "CHF" -> "CHF "
        "SGD" -> "S$"
        "AED" -> "AED "
        else -> "$code "
    }

    fun formatConvertedBudget(usdAmount: Int, targetCurrency: String, rateFromUsd: Double): String {
        if (usdAmount <= 0) return "Est. N/A"
        val converted = (usdAmount * rateFromUsd).roundToInt()
        val symbol = currencySymbol(targetCurrency)
        return "Est. $symbol$converted / day"
    }

    fun formatFreshness(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        return "Updated ${formatter.format(Date(timestampMillis))}"
    }
}
