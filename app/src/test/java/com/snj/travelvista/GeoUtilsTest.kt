package com.snj.travelvista

import com.snj.travelvista.utils.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun testHaversineLondonToParis() {
        // London (51.5074, -0.1278) to Paris (48.8566, 2.3522) ≈ 343.5 km
        val km = GeoUtils.haversineDistanceKm(51.5074, -0.1278, 48.8566, 2.3522)
        assertTrue("Expected ~343 km, got $km", km in 335.0..350.0)
    }

    @Test
    fun testTemperatureConversionMetricAndImperial() {
        assertEquals("25°C", GeoUtils.formatTemperature(25.0, useMetric = true))
        assertEquals("77°F", GeoUtils.formatTemperature(25.0, useMetric = false))
        assertEquals(32.0, GeoUtils.celsiusToFahrenheit(0.0), 0.01)
    }

    @Test
    fun testDistanceFormattingMetricAndImperial() {
        assertEquals("5.0 km", GeoUtils.formatDistance(5000.0, useMetric = true))
        assertEquals("3.1 mi", GeoUtils.formatDistance(5000.0, useMetric = false))
        assertEquals("450 m", GeoUtils.formatDistance(450.0, useMetric = true))
    }

    @Test
    fun testCurrencyFormatting() {
        assertEquals("Est. €184 / day", GeoUtils.formatConvertedBudget(200, "EUR", 0.92))
        assertEquals("Est. ₹8400 / day", GeoUtils.formatConvertedBudget(100, "INR", 84.0))
        assertEquals("Est. N/A", GeoUtils.formatConvertedBudget(0, "USD", 1.0))
    }
}
