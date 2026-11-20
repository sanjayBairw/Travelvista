package com.snj.travelvista

import com.snj.travelvista.data.local.GlobalTravelDao
import com.snj.travelvista.data.local.entity.ItineraryItemEntity
import com.snj.travelvista.data.local.entity.RecentDestinationEntity
import com.snj.travelvista.data.local.entity.SearchHistoryEntity
import com.snj.travelvista.data.local.entity.TripEntity
import com.snj.travelvista.data.remote.WikivoyageApiService
import com.snj.travelvista.data.remote.dto.CountryNameDto
import com.snj.travelvista.data.remote.dto.CurrencyDetailDto
import com.snj.travelvista.data.remote.dto.CurrentWeatherDto
import com.snj.travelvista.data.remote.dto.DailyWeatherDto
import com.snj.travelvista.data.remote.dto.FrankfurterRatesDto
import com.snj.travelvista.data.remote.dto.IddDto
import com.snj.travelvista.data.remote.dto.NominatimPlaceDto
import com.snj.travelvista.data.remote.dto.OpenMeteoResponseDto
import com.snj.travelvista.data.remote.dto.RestCountryDto
import com.snj.travelvista.data.remote.dto.WikivoyageGeoPlaceDto
import com.snj.travelvista.data.remote.dto.WikivoyageGeosearchQueryDto
import com.snj.travelvista.data.remote.dto.WikivoyageGeosearchResponseDto
import com.snj.travelvista.data.remote.dto.WikivoyageSearchResponseDto
import com.snj.travelvista.data.remote.dto.WikivoyageSummaryDto
import com.snj.travelvista.data.remote.service.CountryInfoService
import com.snj.travelvista.data.remote.service.CurrencyService
import com.snj.travelvista.data.remote.service.GeocodingService
import com.snj.travelvista.data.remote.service.WeatherService
import com.snj.travelvista.data.repository.GlobalTravelRepository
import com.snj.travelvista.ui.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GlobalTravelRepositoryTest {

    private lateinit var fakeDao: FakeGlobalTravelDao
    private lateinit var repository: GlobalTravelRepository

    @Before
    fun setUp() {
        fakeDao = FakeGlobalTravelDao()
        repository = GlobalTravelRepository(
            globalTravelDao = fakeDao,
            geocodingService = FakeGeocodingService(),
            weatherService = FakeWeatherService(),
            countryInfoService = FakeCountryInfoService(),
            currencyService = FakeCurrencyService(),
            wikivoyageApiService = FakeWikivoyageGeoService()
        )
    }

    @Test
    fun testLiveWeatherMapping() = runBlocking {
        val result = repository.getWeather(36.4067, 25.4333)
        assertTrue(result is Resource.Success)
        val weather = (result as Resource.Success).data
        assertEquals(24.5, weather.temperatureCelsius, 0.01)
        assertEquals("Mainly clear", weather.conditionText)
        assertEquals("Europe/Athens", weather.timezone)
    }

    @Test
    fun testCountryMetadataExtractionFromDescriptiveLocation() = runBlocking {
        val result = repository.getCountryInfo("state in western India")
        assertTrue(result is Resource.Success)
        val info = (result as Resource.Success).data
        assertEquals("India", info.countryName)
        assertEquals("INR", info.currencyCode)
        assertEquals("+91", info.dialingCode)
    }

    @Test
    fun testNearbyAttractionsExcludesCurrentDestination() = runBlocking {
        val nearby = repository.getNearbyAttractions(36.4067, 25.4333, excludeTitle = "Santorini")
        assertEquals(1, nearby.size)
        assertEquals("Oia", nearby.first().title)
    }

    @Test
    fun testExchangeRateFetch() = runBlocking {
        val res = repository.fetchUsdExchangeRate("EUR")
        assertTrue(res is Resource.Success)
        val rateInfo = (res as Resource.Success).data
        assertEquals(0.92, rateInfo.rate, 0.001)
    }

    @Test
    fun testSearchHistoryAndOfflineTripPlannerCrud() = runBlocking {
        repository.saveSearchQuery("Goa")
        repository.saveSearchQuery("Reykjavik")
        assertEquals(2, repository.getRecentSearches().first().size)

        repository.clearSearchHistory()
        assertEquals(0, repository.getRecentSearches().first().size)

        val tripId = repository.createTrip(
            TripEntity(
                name = "Nordic Escape",
                destinationTitle = "Reykjavik",
                startDate = "2026-12-01",
                endDate = "2026-12-06",
                travelersCount = 2,
                estimatedBudgetUsd = 2000
            )
        )
        assertTrue(tripId > 0)

        repository.addItineraryItem(
            ItineraryItemEntity(
                tripId = tripId,
                dayNumber = 1,
                timeSlot = "10:00",
                title = "Hallgrimskirkja Visit",
                itemType = "SIGHTSEEING"
            )
        )
        val items = repository.getItineraryForTripSync(tripId)
        assertEquals(1, items.size)
        assertEquals("Hallgrimskirkja Visit", items.first().title)

        assertTrue(repository.deleteTrip(tripId))
        assertEquals(0, repository.getAllTrips().first().size)
    }

    @Test
    fun testCulinaryAndSightsResolution() = runBlocking {
        val culinary = repository.getCulinaryAndSights(
            slug = "Goa",
            title = "Goa",
            locationDesc = "state in western India",
            category = "Beaches"
        )
        assertNotNull(culinary)
        assertEquals("Goa", culinary.destinationTitle)
        assertTrue(culinary.cuisineOverview.isNotBlank())
        assertTrue(culinary.famousFoods.isNotEmpty())
        assertTrue(culinary.mostVisitedPlaces.isNotEmpty())
        assertTrue(culinary.famousFoods.any { it.name.contains("Fish Curry", ignoreCase = true) })
        assertTrue(culinary.mostVisitedPlaces.any { it.name.contains("Basilica", ignoreCase = true) })
    }

    // --- FAKES ---

    private class FakeGeocodingService : GeocodingService {
        override suspend fun searchWorldwide(
            query: String,
            format: String,
            addressDetails: Int,
            limit: Int,
            language: String
        ): List<NominatimPlaceDto> = emptyList()
    }

    private class FakeWeatherService : WeatherService {
        override suspend fun getWeatherByCoordinates(
            latitude: Double,
            longitude: Double,
            current: String,
            daily: String,
            timezone: String,
            forecastDays: Int
        ): OpenMeteoResponseDto = OpenMeteoResponseDto(
            latitude = latitude,
            longitude = longitude,
            timezone = "Europe/Athens",
            current = CurrentWeatherDto(
                temperatureCelsius = 24.5,
                humidityPercent = 60,
                weatherCode = 1,
                windSpeedKmh = 12.0
            ),
            daily = DailyWeatherDto(listOf(27.0), listOf(19.0))
        )
    }

    private class FakeCountryInfoService : CountryInfoService {
        override suspend fun getCountryByName(countryName: String, fields: String): List<RestCountryDto> =
            listOf(
                RestCountryDto(
                    name = CountryNameDto("India", "Republic of India"),
                    countryCode = "IN",
                    region = "Asia",
                    subregion = "Southern Asia",
                    capital = listOf("New Delhi"),
                    languages = mapOf("hin" to "Hindi", "eng" to "English"),
                    currencies = mapOf("INR" to CurrencyDetailDto("Indian rupee", "₹")),
                    timezones = listOf("UTC+05:30"),
                    idd = IddDto("+9", listOf("1"))
                )
            )

        override suspend fun getCountryByCode(isoCode: String, fields: String): RestCountryDto =
            getCountryByName("India", fields).first()
    }

    private class FakeCurrencyService : CurrencyService {
        override suspend fun getLatestRates(baseCurrency: String): FrankfurterRatesDto =
            FrankfurterRatesDto(
                amount = 1.0,
                base = baseCurrency,
                date = "2026-11-18",
                rates = mapOf("EUR" to 0.92, "INR" to 84.0)
            )
    }

    private class FakeWikivoyageGeoService : WikivoyageApiService {
        override suspend fun getPageSummary(title: String) = WikivoyageSummaryDto(title = title)
        override suspend fun searchDestinations(query: String, limit: Int) = WikivoyageSearchResponseDto()
        override suspend fun getNearbyPlaces(
            action: String,
            list: String,
            coordinates: String,
            radiusMeters: Int,
            limit: Int,
            format: String
        ) = WikivoyageGeosearchResponseDto(
            query = WikivoyageGeosearchQueryDto(
                geosearch = listOf(
                    WikivoyageGeoPlaceDto(1L, "Santorini", 36.4067, 25.4333, 0.0),
                    WikivoyageGeoPlaceDto(2L, "Oia", 36.4618, 25.3753, 7500.0)
                )
            )
        )

        override suspend fun getPageSections(
            page: String,
            action: String,
            prop: String,
            format: String
        ) = com.snj.travelvista.data.remote.dto.WikivoyageSectionsResponseDto(
            parse = com.snj.travelvista.data.remote.dto.WikivoyageSectionsParseDto(
                title = page,
                pageId = 12345L,
                sections = listOf(
                    com.snj.travelvista.data.remote.dto.WikivoyageSectionItemDto(
                        toclevel = 1,
                        level = "2",
                        line = "Eat",
                        number = "3",
                        index = "3",
                        anchor = "Eat"
                    ),
                    com.snj.travelvista.data.remote.dto.WikivoyageSectionItemDto(
                        toclevel = 1,
                        level = "2",
                        line = "See",
                        number = "4",
                        index = "4",
                        anchor = "See"
                    )
                )
            )
        )

        override suspend fun getSectionHtml(
            page: String,
            sectionIndex: String,
            action: String,
            prop: String,
            format: String
        ) = com.snj.travelvista.data.remote.dto.WikivoyageParseTextResponseDto(
            parse = com.snj.travelvista.data.remote.dto.WikivoyageParseTextDto(
                title = page,
                pageId = 12345L,
                text = com.snj.travelvista.data.remote.dto.WikivoyageTextContentDto(
                    html = if (sectionIndex == "3") {
                        "<p>Local cuisine is rich and diverse.</p><ul><li><b>Goan Fish Curry</b> — Fresh catch in spiced coconut sauce.</li></ul>"
                    } else {
                        "<ul><li><b>Basilica of Bom Jesus</b> — Famous historical UNESCO site.</li></ul>"
                    }
                )
            )
        )
    }

    private class FakeGlobalTravelDao : GlobalTravelDao {
        private val history = LinkedHashMap<String, SearchHistoryEntity>()
        private val historyFlow = MutableStateFlow<List<SearchHistoryEntity>>(emptyList())

        private val recents = LinkedHashMap<Long, RecentDestinationEntity>()
        private val recentsFlow = MutableStateFlow<List<RecentDestinationEntity>>(emptyList())

        private var nextTripId = 1L
        private val trips = LinkedHashMap<Long, TripEntity>()
        private val tripsFlow = MutableStateFlow<List<TripEntity>>(emptyList())

        private var nextItemId = 1L
        private val items = LinkedHashMap<Long, ItineraryItemEntity>()
        private val itemsFlow = MutableStateFlow<List<ItineraryItemEntity>>(emptyList())

        override fun getRecentSearches(): Flow<List<SearchHistoryEntity>> = historyFlow.asStateFlow()
        override suspend fun insertSearchQuery(entry: SearchHistoryEntity) {
            history[entry.query] = entry
            historyFlow.value = history.values.toList()
        }
        override suspend fun deleteSearchQuery(query: String) {
            history.remove(query)
            historyFlow.value = history.values.toList()
        }
        override suspend fun clearSearchHistory() {
            history.clear()
            historyFlow.value = emptyList()
        }

        override fun getRecentDestinations(): Flow<List<RecentDestinationEntity>> = recentsFlow.asStateFlow()
        override suspend fun insertRecentDestination(entry: RecentDestinationEntity) {
            recents[entry.destinationId] = entry
            recentsFlow.value = recents.values.toList()
        }
        override suspend fun clearRecentDestinations() {
            recents.clear()
            recentsFlow.value = emptyList()
        }

        override fun getAllTrips(): Flow<List<TripEntity>> = tripsFlow.asStateFlow()
        override suspend fun getTripById(tripId: Long): TripEntity? = trips[tripId]
        override suspend fun insertTrip(trip: TripEntity): Long {
            val id = if (trip.id == 0L) nextTripId++ else trip.id
            trips[id] = trip.copy(id = id)
            tripsFlow.value = trips.values.toList()
            return id
        }
        override suspend fun deleteTripById(tripId: Long): Int {
            val removed = trips.remove(tripId)
            items.values.removeAll { it.tripId == tripId }
            tripsFlow.value = trips.values.toList()
            itemsFlow.value = items.values.toList()
            return if (removed != null) 1 else 0
        }

        override fun getItineraryForTrip(tripId: Long): Flow<List<ItineraryItemEntity>> =
            itemsFlow.map { list -> list.filter { it.tripId == tripId } }
        override suspend fun getItineraryForTripSync(tripId: Long): List<ItineraryItemEntity> =
            items.values.filter { it.tripId == tripId }
        override suspend fun insertItineraryItem(item: ItineraryItemEntity): Long {
            val id = if (item.id == 0L) nextItemId++ else item.id
            items[id] = item.copy(id = id)
            itemsFlow.value = items.values.toList()
            return id
        }
        override suspend fun deleteItineraryItemById(itemId: Long): Int {
            val removed = items.remove(itemId)
            itemsFlow.value = items.values.toList()
            return if (removed != null) 1 else 0
        }
    }
}
