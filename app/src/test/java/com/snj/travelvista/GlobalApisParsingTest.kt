package com.snj.travelvista

import com.google.gson.Gson
import com.snj.travelvista.data.remote.dto.CurrentWeatherDto
import com.snj.travelvista.data.remote.dto.FrankfurterRatesDto
import com.snj.travelvista.data.remote.dto.NominatimPlaceDto
import com.snj.travelvista.data.remote.dto.OpenMeteoResponseDto
import com.snj.travelvista.data.remote.dto.RestCountryDto
import com.snj.travelvista.data.remote.dto.WikivoyageGeosearchResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalApisParsingTest {

    private val gson = Gson()

    @Test
    fun testParseNominatimWorldwidePlace() {
        val json = """
            {
              "place_id": 123456,
              "osm_type": "node",
              "osm_id": 987654,
              "lat": "27.9881206",
              "lon": "86.9249751",
              "category": "natural",
              "type": "peak",
              "name": "Mount Everest",
              "display_name": "Mount Everest, Solukhumbu, Koshi Province, Nepal",
              "address": {
                "state": "Koshi Province",
                "country": "Nepal",
                "country_code": "np"
              }
            }
        """.trimIndent()

        val dto = gson.fromJson(json, NominatimPlaceDto::class.java)
        assertEquals("Mount Everest", dto.resolvedTitle)
        assertEquals("Nepal", dto.resolvedCountry)
        assertEquals("Koshi Province, Nepal", dto.resolvedRegion)
        assertEquals("Mountains", dto.mapToTravelCategory())
        assertEquals(27.9881, dto.latitudeDouble ?: 0.0, 0.001)
        assertEquals(86.9249, dto.longitudeDouble ?: 0.0, 0.001)
    }

    @Test
    fun testParseOpenMeteoWeatherResponse() {
        val json = """
            {
              "latitude": 35.0,
              "longitude": 135.75,
              "timezone": "Asia/Tokyo",
              "current": {
                "time": "2026-11-18T15:00",
                "temperature_2m": 16.4,
                "relative_humidity_2m": 58,
                "weather_code": 2,
                "wind_speed_10m": 11.2
              },
              "daily": {
                "temperature_2m_max": [18.5],
                "temperature_2m_min": [9.2]
              }
            }
        """.trimIndent()

        val dto = gson.fromJson(json, OpenMeteoResponseDto::class.java)
        assertEquals("Asia/Tokyo", dto.timezone)
        assertNotNull(dto.current)
        assertEquals(16.4, dto.current?.temperatureCelsius ?: 0.0, 0.01)
        assertEquals("Partly cloudy", dto.current?.conditionDescription())
        assertEquals("Clear sky", CurrentWeatherDto.decodeWmoWeatherCode(0))
        assertEquals("Thunderstorm", CurrentWeatherDto.decodeWmoWeatherCode(95))
        assertEquals(18.5, dto.daily?.maxTempsCelsius?.firstOrNull() ?: 0.0, 0.01)
    }

    @Test
    fun testParseRestCountryResponse() {
        val json = """
            {
              "name": { "common": "Japan", "official": "Japan" },
              "cca2": "JP",
              "region": "Asia",
              "subregion": "Eastern Asia",
              "capital": ["Tokyo"],
              "languages": { "jpn": "Japanese" },
              "currencies": { "JPY": { "name": "Japanese yen", "symbol": "¥" } },
              "timezones": ["UTC+09:00"],
              "idd": { "root": "+8", "suffixes": ["1"] }
            }
        """.trimIndent()

        val dto = gson.fromJson(json, RestCountryDto::class.java)
        assertEquals("JP", dto.countryCode)
        assertEquals("JPY", dto.primaryCurrencyCode)
        assertEquals("Japanese yen — JPY (¥)", dto.formattedCurrency)
        assertEquals("Japanese", dto.formattedLanguages)
        assertEquals("UTC+09:00", dto.formattedTimezone)
        assertEquals("+81", dto.formattedDialingCode)
        assertEquals("Eastern Asia, Asia", dto.formattedRegion)
    }

    @Test
    fun testParseFrankfurterExchangeRates() {
        val json = """
            {
              "amount": 1.0,
              "base": "USD",
              "date": "2026-11-18",
              "rates": { "EUR": 0.924, "INR": 84.35, "JPY": 154.2 }
            }
        """.trimIndent()

        val dto = gson.fromJson(json, FrankfurterRatesDto::class.java)
        assertEquals("USD", dto.base)
        assertEquals(84.35, dto.rates["INR"] ?: 0.0, 0.001)
    }

    @Test
    fun testParseWikivoyageGeosearchResponse() {
        val json = """
            {
              "query": {
                "geosearch": [
                  { "pageid": 101, "title": "Oia", "lat": 36.4618, "lon": 25.3753, "dist": 7920.5 },
                  { "pageid": 102, "title": "Fira", "lat": 36.4166, "lon": 25.4324, "dist": 1120.0 }
                ]
              }
            }
        """.trimIndent()

        val dto = gson.fromJson(json, WikivoyageGeosearchResponseDto::class.java)
        val places = dto.query?.geosearch.orEmpty()
        assertEquals(2, places.size)
        assertEquals("Oia", places[0].title)
        assertTrue(places[0].distanceMeters > 7000.0)
    }
}
