package com.snj.travelvista.data.repository

import com.snj.travelvista.data.local.GlobalTravelDao
import com.snj.travelvista.data.local.entity.ItineraryItemEntity
import com.snj.travelvista.data.local.entity.RecentDestinationEntity
import com.snj.travelvista.data.local.entity.SearchHistoryEntity
import com.snj.travelvista.data.local.entity.TripEntity
import com.snj.travelvista.data.model.CountryTravelInfo
import com.snj.travelvista.data.model.CurrencyConversionResult
import com.snj.travelvista.data.model.CurrencyRateInfo
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.data.model.DestinationCulinaryAndSights
import com.snj.travelvista.data.model.DestinationWeather
import com.snj.travelvista.data.model.FoodSpecialty
import com.snj.travelvista.data.model.MostVisitedPlace
import com.snj.travelvista.data.model.NearbyAttraction
import com.snj.travelvista.data.remote.GlobalCulinaryAndSightsCatalog
import com.snj.travelvista.data.remote.WikivoyageApiService
import com.snj.travelvista.data.remote.dto.NominatimPlaceDto
import com.snj.travelvista.data.remote.service.CountryInfoService
import com.snj.travelvista.data.remote.service.CurrencyService
import com.snj.travelvista.data.remote.service.GeocodingService
import com.snj.travelvista.data.remote.service.WeatherService
import com.snj.travelvista.ui.common.Resource
import com.snj.travelvista.utils.UserPreferences
import com.snj.travelvista.utils.WikivoyageSectionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Coordinates global enrichment APIs (Open-Meteo weather, REST Countries metadata,
 * Wikivoyage geosearch nearby attractions, Nominatim worldwide geocoding, Frankfurter ECB currency rates)
 * and offline Room persistence (search history, recently viewed, trips & day-wise itineraries).
 */
class GlobalTravelRepository(
    private val globalTravelDao: GlobalTravelDao,
    private val geocodingService: GeocodingService,
    private val weatherService: WeatherService,
    private val countryInfoService: CountryInfoService,
    private val currencyService: CurrencyService,
    private val wikivoyageApiService: WikivoyageApiService,
    private val userPreferences: UserPreferences? = null
) {

    // --- LIVE WEATHER (Open-Meteo) ---

    suspend fun getWeather(latitude: Double, longitude: Double): Resource<DestinationWeather> =
        withContext(Dispatchers.IO) {
            try {
                val dto = weatherService.getWeatherByCoordinates(latitude, longitude)
                val current = dto.current
                if (current?.temperatureCelsius == null) {
                    return@withContext Resource.Error("Live weather temporarily unavailable")
                }
                Resource.Success(
                    DestinationWeather(
                        temperatureCelsius = current.temperatureCelsius,
                        conditionText = current.conditionDescription(),
                        windSpeedKmh = current.windSpeedKmh ?: 0.0,
                        humidityPercent = current.humidityPercent,
                        dailyMaxCelsius = dto.daily?.maxTempsCelsius?.firstOrNull(),
                        dailyMinCelsius = dto.daily?.minTempsCelsius?.firstOrNull(),
                        timezone = dto.timezone ?: "UTC",
                        fetchedAtMillis = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Resource.Error("Weather unavailable: ${e.localizedMessage ?: "Network error"}", e)
            }
        }

    // --- LIVE COUNTRY METADATA (REST Countries v3.1) ---

    suspend fun getCountryInfo(rawCountry: String): Resource<CountryTravelInfo> =
        withContext(Dispatchers.IO) {
            val cleaned = extractCountryQuery(rawCountry)
            if (cleaned.isBlank()) {
                return@withContext Resource.Error("Country information unavailable")
            }
            try {
                val results = countryInfoService.getCountryByName(cleaned)
                val best = results.find {
                    it.name?.common.equals(cleaned, ignoreCase = true)
                } ?: results.firstOrNull()

                if (best == null) {
                    return@withContext Resource.Error("Country not found")
                }

                Resource.Success(
                    CountryTravelInfo(
                        countryName = best.name?.common ?: cleaned,
                        countryCode = best.countryCode ?: "",
                        region = best.formattedRegion,
                        capital = best.capital?.firstOrNull() ?: "N/A",
                        currencyDisplay = best.formattedCurrency,
                        currencyCode = best.primaryCurrencyCode,
                        languages = best.formattedLanguages,
                        timezone = best.formattedTimezone,
                        dialingCode = best.formattedDialingCode,
                        fetchedAtMillis = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Resource.Error("Country details unavailable", e)
            }
        }

    private fun extractCountryQuery(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || trimmed.equals("Travel Destination", ignoreCase = true) || trimmed.equals("Global Destination", ignoreCase = true)) {
            return ""
        }
        // Take last comma-separated token (e.g. "Goa, India" -> "India")
        val candidate = trimmed.substringAfterLast(",").trim()
        // Handle descriptive Wikivoyage phrases like "state in western India" -> "India"
        val inMatch = Regex("\\bin\\s+(?:western|eastern|northern|southern|central\\s+)?([A-Z][a-zA-Z\\s]+)$").find(candidate)
        val extracted = inMatch?.groupValues?.getOrNull(1)?.trim() ?: candidate
        // Avoid sending overly long sentences to REST Countries
        return if (extracted.split(" ").size <= 4) extracted else ""
    }

    // --- LIVE NEARBY ATTRACTIONS (Wikivoyage Geosearch) ---

    suspend fun getNearbyAttractions(
        latitude: Double,
        longitude: Double,
        excludeTitle: String = ""
    ): List<NearbyAttraction> = withContext(Dispatchers.IO) {
        try {
            val coord = "$latitude|$longitude"
            val response = wikivoyageApiService.getNearbyPlaces(
                coordinates = coord,
                radiusMeters = 10000,
                limit = 8
            )
            response.query?.geosearch
                ?.filter { !it.title.equals(excludeTitle, ignoreCase = true) }
                ?.map { place ->
                    NearbyAttraction(
                        pageId = place.pageId,
                        title = place.title,
                        latitude = place.lat,
                        longitude = place.lon,
                        distanceMeters = place.distanceMeters
                    )
                }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- WORLDWIDE MAP GEOCODING (OpenStreetMap Nominatim) ---

    suspend fun geocodeWorldwide(query: String): List<NominatimPlaceDto> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.length < 2) return@withContext emptyList()
        try {
            geocodingService.searchWorldwide(clean, limit = 8)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- LIVE CURRENCY CONVERSION (Frankfurter ECB) ---

    suspend fun fetchUsdExchangeRate(targetCurrency: String): Resource<CurrencyRateInfo> =
        withContext(Dispatchers.IO) {
            val code = targetCurrency.uppercase()
            if (code == "USD") {
                userPreferences?.cachedUsdToPreferredRate = 1.0
                return@withContext Resource.Success(
                    CurrencyRateInfo("USD", "USD", 1.0, "Base currency")
                )
            }
            try {
                val dto = currencyService.getLatestRates(baseCurrency = "USD")
                val rate = dto.rates[code]
                    ?: return@withContext Resource.Error("Rate unavailable for $code")
                userPreferences?.let { prefs ->
                    if (prefs.preferredCurrency.equals(code, ignoreCase = true)) {
                        prefs.cachedUsdToPreferredRate = rate
                        prefs.cachedRateDate = dto.date
                    }
                }
                Resource.Success(
                    CurrencyRateInfo(
                        baseCurrency = dto.base,
                        targetCurrency = code,
                        rate = rate,
                        date = dto.date
                    )
                )
            } catch (e: Exception) {
                Resource.Error("Exchange rate unavailable: ${e.localizedMessage ?: "Offline"}", e)
            }
        }

    suspend fun convertCurrency(amount: Double, from: String, to: String): Resource<CurrencyConversionResult> =
        withContext(Dispatchers.IO) {
            val fromCode = from.uppercase()
            val toCode = to.uppercase()

            if (fromCode == toCode) {
                return@withContext Resource.Success(
                    CurrencyConversionResult(
                        fromAmount = amount,
                        fromCurrency = fromCode,
                        toAmount = amount,
                        toCurrency = toCode,
                        rate = 1.0,
                        date = "Real-time"
                    )
                )
            }

            try {
                val dto = currencyService.getLatestRates(baseCurrency = fromCode)
                val rate = dto.rates[toCode]
                    ?: return@withContext Resource.Error("Exchange rate unavailable for $toCode")

                Resource.Success(
                    CurrencyConversionResult(
                        fromAmount = amount,
                        fromCurrency = fromCode,
                        toAmount = amount * rate,
                        toCurrency = toCode,
                        rate = rate,
                        date = dto.date
                    )
                )
            } catch (e: Exception) {
                val usdRate = userPreferences?.cachedUsdToPreferredRate ?: 1.0
                if (fromCode == "USD" && toCode == userPreferences?.preferredCurrency) {
                    return@withContext Resource.Success(
                        CurrencyConversionResult(
                            fromAmount = amount,
                            fromCurrency = fromCode,
                            toAmount = amount * usdRate,
                            toCurrency = toCode,
                            rate = usdRate,
                            date = userPreferences?.cachedRateDate?.ifBlank { "Offline" } ?: "Offline"
                        )
                    )
                }
                Resource.Error("Conversion unavailable: ${e.localizedMessage ?: "Network error"}", e)
            }
        }

    // --- SEARCH HISTORY ---

    fun getRecentSearches(): Flow<List<SearchHistoryEntity>> =
        globalTravelDao.getRecentSearches().flowOn(Dispatchers.IO)

    suspend fun saveSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.length >= 2) {
            globalTravelDao.insertSearchQuery(SearchHistoryEntity(clean, System.currentTimeMillis()))
        }
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        globalTravelDao.clearSearchHistory()
    }

    // --- RECENTLY VIEWED DESTINATIONS ---

    fun getRecentDestinations(): Flow<List<RecentDestinationEntity>> =
        globalTravelDao.getRecentDestinations().flowOn(Dispatchers.IO)

    suspend fun recordDestinationView(destination: Destination) = withContext(Dispatchers.IO) {
        globalTravelDao.insertRecentDestination(
            RecentDestinationEntity(
                destinationId = destination.id,
                title = destination.title,
                location = destination.location,
                imageUrl = destination.imageUrl,
                category = destination.category,
                viewedAtTimestamp = System.currentTimeMillis()
            )
        )
    }

    // --- OFFLINE TRIP PLANNER ---

    fun getAllTrips(): Flow<List<TripEntity>> =
        globalTravelDao.getAllTrips().flowOn(Dispatchers.IO)

    suspend fun getTripById(tripId: Long): TripEntity? = withContext(Dispatchers.IO) {
        globalTravelDao.getTripById(tripId)
    }

    suspend fun createTrip(trip: TripEntity): Long = withContext(Dispatchers.IO) {
        globalTravelDao.insertTrip(trip)
    }

    suspend fun deleteTrip(tripId: Long): Boolean = withContext(Dispatchers.IO) {
        globalTravelDao.deleteTripById(tripId) > 0
    }

    fun getItineraryForTrip(tripId: Long): Flow<List<ItineraryItemEntity>> =
        globalTravelDao.getItineraryForTrip(tripId).flowOn(Dispatchers.IO)

    suspend fun getItineraryForTripSync(tripId: Long): List<ItineraryItemEntity> =
        withContext(Dispatchers.IO) {
            globalTravelDao.getItineraryForTripSync(tripId)
        }

    suspend fun addItineraryItem(item: ItineraryItemEntity): Long = withContext(Dispatchers.IO) {
        globalTravelDao.insertItineraryItem(item)
    }

    suspend fun deleteItineraryItem(itemId: Long): Boolean = withContext(Dispatchers.IO) {
        globalTravelDao.deleteItineraryItemById(itemId) > 0
    }

    // --- LIVE CULINARY SPECIALTIES & MOST VISITED SIGHTS ---

    suspend fun getCulinaryAndSights(
        slug: String,
        title: String,
        locationDesc: String,
        category: String
    ): DestinationCulinaryAndSights = withContext(Dispatchers.IO) {
        val regionalFallback = GlobalCulinaryAndSightsCatalog.getRegionalFallback(
            title = title,
            locationDesc = locationDesc,
            category = category
        )

        val pageKey = when {
            slug.isNotBlank() -> slug
            title.isNotBlank() -> title.replace(" ", "_")
            else -> ""
        }

        if (pageKey.isBlank()) {
            return@withContext regionalFallback
        }

        try {
            val sectionsResponse = wikivoyageApiService.getPageSections(pageKey)
            val sectionList = sectionsResponse.parse?.sections

            val eatIndex = WikivoyageSectionParser.findSectionIndex(
                sectionList,
                "Eat", "Eat and drink", "Food", "Cuisine", "Local food", "Restaurants"
            )
            val seeIndex = WikivoyageSectionParser.findSectionIndex(
                sectionList,
                "See", "See and do", "Sights", "Attractions", "Places to see", "Landmarks"
            )

            var parsedOverview = ""
            var parsedFoods = emptyList<FoodSpecialty>()
            var parsedSights = emptyList<MostVisitedPlace>()

            if (eatIndex != null) {
                try {
                    val eatHtml = wikivoyageApiService.getSectionHtml(pageKey, eatIndex)
                    val rawHtml = eatHtml.parse?.text?.html.orEmpty()
                    if (rawHtml.isNotBlank()) {
                        val (overview, foods) = WikivoyageSectionParser.parseFoodFromHtml(rawHtml, title)
                        parsedOverview = overview
                        parsedFoods = foods
                    }
                } catch (e: Exception) {
                    // Ignore live section error, use fallback
                }
            }

            if (seeIndex != null) {
                try {
                    val seeHtml = wikivoyageApiService.getSectionHtml(pageKey, seeIndex)
                    val rawHtml = seeHtml.parse?.text?.html.orEmpty()
                    if (rawHtml.isNotBlank()) {
                        parsedSights = WikivoyageSectionParser.parseSightsFromHtml(rawHtml, title)
                    }
                } catch (e: Exception) {
                    // Ignore live section error, use fallback
                }
            }

            val finalOverview = if (parsedOverview.isNotBlank() && parsedOverview.length > 30) {
                parsedOverview
            } else {
                regionalFallback.cuisineOverview
            }

            val finalFoods = mutableListOf<FoodSpecialty>()
            finalFoods.addAll(parsedFoods)
            val foodNames = finalFoods.map { it.name.lowercase() }.toMutableSet()
            for (fallbackFood in regionalFallback.famousFoods) {
                if (finalFoods.size >= 6) break
                if (foodNames.add(fallbackFood.name.lowercase())) {
                    finalFoods.add(fallbackFood)
                }
            }

            val finalSights = mutableListOf<MostVisitedPlace>()
            finalSights.addAll(parsedSights)
            val sightNames = finalSights.map { it.name.lowercase() }.toMutableSet()
            for (fallbackSight in regionalFallback.mostVisitedPlaces) {
                if (finalSights.size >= 8) break
                if (sightNames.add(fallbackSight.name.lowercase())) {
                    finalSights.add(fallbackSight)
                }
            }

            DestinationCulinaryAndSights(
                destinationTitle = title,
                cuisineOverview = finalOverview,
                famousFoods = if (finalFoods.isNotEmpty()) finalFoods else regionalFallback.famousFoods,
                mostVisitedPlaces = if (finalSights.isNotEmpty()) finalSights else regionalFallback.mostVisitedPlaces,
                diningTips = regionalFallback.diningTips
            )
        } catch (e: Exception) {
            regionalFallback
        }
    }
}
