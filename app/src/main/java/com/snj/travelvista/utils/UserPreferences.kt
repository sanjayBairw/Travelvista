package com.snj.travelvista.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * Local device preferences for TravelVista global settings:
 * - Preferred currency & cached ECB exchange rate
 * - Metric (km, °C) vs Imperial (mi, °F) units
 * - Theme mode (System / Light / Dark)
 * - Local traveler profile (name, home city, home airport)
 *
 * Security: Never stores payment cards, passwords, or private API keys.
 */
class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var preferredCurrency: String
        get() = prefs.getString(KEY_CURRENCY, "USD") ?: "USD"
        set(value) = prefs.edit().putString(KEY_CURRENCY, value.uppercase()).apply()

    var cachedUsdToPreferredRate: Double
        get() = prefs.getFloat(KEY_CACHED_RATE, 1.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_CACHED_RATE, value.toFloat()).apply()

    var cachedRateDate: String
        get() = prefs.getString(KEY_CACHED_RATE_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CACHED_RATE_DATE, value).apply()

    var useMetricUnits: Boolean
        get() = prefs.getBoolean(KEY_METRIC_UNITS, true)
        set(value) = prefs.edit().putBoolean(KEY_METRIC_UNITS, value).apply()

    /**
     * 0 = Follow System, 1 = Light, 2 = Dark
     */
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM)
        set(value) {
            prefs.edit().putInt(KEY_THEME_MODE, value).apply()
            applyTheme(value)
        }

    var travelerName: String
        get() = prefs.getString(KEY_TRAVELER_NAME, "Global Explorer") ?: "Global Explorer"
        set(value) = prefs.edit().putString(KEY_TRAVELER_NAME, value.trim().ifBlank { "Global Explorer" }).apply()

    var homeCity: String
        get() = prefs.getString(KEY_HOME_CITY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HOME_CITY, value.trim()).apply()

    var homeAirport: String
        get() = prefs.getString(KEY_HOME_AIRPORT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HOME_AIRPORT, value.trim().uppercase()).apply()

    fun applySavedTheme() {
        applyTheme(themeMode)
    }

    companion object {
        private const val PREFS_NAME = "travelvista_global_prefs"
        private const val KEY_CURRENCY = "preferred_currency"
        private const val KEY_CACHED_RATE = "cached_usd_rate"
        private const val KEY_CACHED_RATE_DATE = "cached_rate_date"
        private const val KEY_METRIC_UNITS = "use_metric_units"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_TRAVELER_NAME = "traveler_name"
        private const val KEY_HOME_CITY = "home_city"
        private const val KEY_HOME_AIRPORT = "home_airport"

        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        val SUPPORTED_CURRENCIES = listOf(
            "USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD", "CHF", "SGD"
        )

        fun applyTheme(mode: Int) {
            val nightMode = when (mode) {
                THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }
}
