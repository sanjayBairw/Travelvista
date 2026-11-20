package com.snj.travelvista

import com.snj.travelvista.data.local.DestinationDao
import com.snj.travelvista.data.local.entity.CachedDestinationEntity
import com.snj.travelvista.data.local.entity.DestinationEntity
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.data.model.SortOption
import com.snj.travelvista.data.remote.WikivoyageApiService
import com.snj.travelvista.data.remote.dto.CoordinatesDto
import com.snj.travelvista.data.remote.dto.ImageSourceDto
import com.snj.travelvista.data.remote.dto.WikivoyageSearchResponseDto
import com.snj.travelvista.data.remote.dto.WikivoyageSummaryDto
import com.snj.travelvista.data.repository.TravelRepository
import com.snj.travelvista.ui.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class TravelRepositoryTest {

    private lateinit var fakeDao: FakeDestinationDao
    private lateinit var fakeApi: FakeWikivoyageApiService
    private lateinit var repository: TravelRepository

    @Before
    fun setUp() {
        fakeDao = FakeDestinationDao()
        fakeApi = FakeWikivoyageApiService()
        repository = TravelRepository(
            wikivoyageApiService = fakeApi,
            destinationDao = fakeDao
        )
    }

    @Test
    fun testLiveApiEnrichmentAndCacheUpdate() = runBlocking {
        // Given: API provides real Wikimedia image and extract for Santorini
        fakeApi.customSummaryMap["Santorini"] = WikivoyageSummaryDto(
            title = "Santorini",
            extract = "Real live extract for Santorini from Wikivoyage.",
            thumbnail = ImageSourceDto("https://thumb.wikimedia.org/santorini_thumb.jpg", 330, 220),
            originalImage = ImageSourceDto("https://thumb.wikimedia.org/santorini_full.jpg", 3840, 2160),
            coordinates = CoordinatesDto(36.4067, 25.4333)
        )

        // When: getDestinations is requested
        val result = repository.getDestinations().first { it is Resource.Success }
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data

        // Then: Santorini has live photo and live extract
        val santorini = data.find { it.slug == "Santorini" || it.id == 1L }
        assertNotNull(santorini)
        assertEquals("https://thumb.wikimedia.org/santorini_full.jpg", santorini?.imageUrl)
        assertEquals("Real live extract for Santorini from Wikivoyage.", santorini?.description)
        assertEquals(36.4067, santorini?.latitude ?: 0.0, 0.001)
        assertEquals("(WT-en) Chmouel", santorini?.imageAuthor)
        assertEquals("CC BY-SA 1.0", santorini?.imageLicense)
        assertTrue(santorini?.attributionSummary?.contains("CC BY-SA 1.0") == true)

        // And: Data has been saved to Room cache
        val cached = fakeDao.getAllCachedDestinationsSync()
        assertTrue(cached.isNotEmpty())
        val cachedSantorini = cached.find { it.slug == "Santorini" }
        assertEquals("https://thumb.wikimedia.org/santorini_full.jpg", cachedSantorini?.imageUrl)
    }

    @Test
    fun testDuplicatePreventionInCache() = runBlocking {
        val sample1 = CachedDestinationEntity(
            id = 100L,
            slug = "Kyoto",
            title = "Kyoto Initial",
            country = "Japan",
            city = "Kyoto",
            category = "Historical Places",
            rating = 4.9,
            reviewCount = 1000,
            imageUrl = "https://thumb.wikimedia.org/1.jpg",
            description = "Initial description",
            highlightsJson = "[]",
            travelTipsJson = "[]",
            bestTimeToVisit = "Spring",
            pricePerDay = 150,
            isFeatured = true,
            isPopular = true,
            isTrending = false
        )

        // Insert first
        fakeDao.insertCachedDestination(sample1)
        assertEquals(1, fakeDao.getAllCachedDestinationsSync().size)

        // Insert updated version with the SAME id
        val sample1Updated = sample1.copy(title = "Kyoto Updated", pricePerDay = 175)
        fakeDao.insertCachedDestination(sample1Updated)

        // Verify: No duplicate records created (OnConflictStrategy.REPLACE)
        val allCached = fakeDao.getAllCachedDestinationsSync()
        assertEquals(1, allCached.size)
        assertEquals("Kyoto Updated", allCached.first().title)
        assertEquals(175, allCached.first().pricePerDay)
    }

    @Test
    fun testOfflineCacheFallbackWhenNetworkFails() = runBlocking {
        // Given: Cache already has cached destinations
        val cachedEntity = CachedDestinationEntity(
            id = 55L,
            slug = "Alps",
            title = "Cached Swiss Alps",
            country = "Switzerland",
            city = "Zermatt",
            category = "Mountains",
            rating = 4.9,
            reviewCount = 3000,
            imageUrl = "https://thumb.wikimedia.org/alps.jpg",
            description = "Cached alpine extract",
            highlightsJson = "[]",
            travelTipsJson = "[]",
            bestTimeToVisit = "Winter",
            pricePerDay = 240,
            isFeatured = true,
            isPopular = false,
            isTrending = true
        )
        fakeDao.insertCachedDestination(cachedEntity)

        // And: Network is down
        fakeApi.shouldThrowNetworkError = true

        // When: getDestinations is called offline
        val result = repository.getDestinations().first { it is Resource.Success }

        // Then: Emits cached destinations without crashing or throwing
        assertTrue(result is Resource.Success)
        val destinations = (result as Resource.Success).data
        val alps = destinations.find { it.id == 55L }
        assertNotNull(alps)
        assertEquals("Cached Swiss Alps", alps?.title)
    }

    @Test
    fun testSearchAndFilterOptions() = runBlocking {
        // Search by query
        val searchResult = repository.getDestinations(searchQuery = "Santorini")
            .first { it is Resource.Success } as Resource.Success
        assertTrue(searchResult.data.isNotEmpty())
        assertTrue(searchResult.data.all {
            it.title.contains("Santorini", ignoreCase = true) ||
                    it.city.contains("Santorini", ignoreCase = true)
        })

        // Filter by category
        val mountainResult = repository.getDestinations(category = "Mountains")
            .first { it is Resource.Success } as Resource.Success
        assertTrue(mountainResult.data.isNotEmpty())
        assertTrue(mountainResult.data.all { it.category.equals("Mountains", ignoreCase = true) })

        // Sort by price ascending
        val priceSortResult = repository.getDestinations(sortOption = SortOption.PRICE_LOW_TO_HIGH)
            .first { it is Resource.Success } as Resource.Success
        val prices = priceSortResult.data.map { it.pricePerDay }
        for (i in 0 until prices.size - 1) {
            assertTrue(prices[i] <= prices[i + 1])
        }

        // Sort by alphabetical title
        val alphaResult = repository.getDestinations(sortOption = SortOption.ALPHABETICAL)
            .first { it is Resource.Success } as Resource.Success
        val titles = alphaResult.data.map { it.title }
        for (i in 0 until titles.size - 1) {
            assertTrue(titles[i] <= titles[i + 1])
        }
    }

    @Test
    fun testFavoritesPersistenceAndToggle() = runBlocking {
        val dest = Destination(
            id = 999L,
            title = "Test Paradise",
            country = "Nowhere",
            city = "City",
            category = "Beaches",
            rating = 5.0,
            reviewCount = 500,
            imageUrl = "https://example.com/img.jpg",
            description = "A wonderful test paradise",
            highlights = emptyList(),
            travelTips = emptyList(),
            bestTimeToVisit = "All year",
            pricePerDay = 100
        )

        assertFalse(fakeDao.isFavoriteSync(999L))

        // Toggle favorite -> now saved
        val newState = repository.toggleFavorite(dest)
        assertTrue(newState)
        assertTrue(fakeDao.isFavoriteSync(999L))

        val favorites = repository.getAllFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals(999L, favorites.first().id)

        // Toggle again -> removed
        val removedState = repository.toggleFavorite(dest)
        assertFalse(removedState)
        assertFalse(fakeDao.isFavoriteSync(999L))
    }

    // --- FAKE IMPLEMENTATIONS ---

    private class FakeWikivoyageApiService : WikivoyageApiService {
        var shouldThrowNetworkError = false
        val customSummaryMap = mutableMapOf<String, WikivoyageSummaryDto>()

        override suspend fun getPageSummary(title: String): WikivoyageSummaryDto {
            if (shouldThrowNetworkError) {
                throw IOException("Simulated network connection failure")
            }
            return customSummaryMap[title] ?: WikivoyageSummaryDto(
                title = title.replace("_", " "),
                extract = "Real traveler guide extract for $title",
                thumbnail = ImageSourceDto("https://thumb.wikimedia.org/${title}_thumb.jpg", 330, 200),
                originalImage = ImageSourceDto("https://thumb.wikimedia.org/${title}_full.jpg", 1920, 1080)
            )
        }

        override suspend fun searchDestinations(query: String, limit: Int): WikivoyageSearchResponseDto {
            if (shouldThrowNetworkError) throw IOException("Simulated search error")
            return WikivoyageSearchResponseDto()
        }

        override suspend fun getNearbyPlaces(
            action: String,
            list: String,
            coordinates: String,
            radiusMeters: Int,
            limit: Int,
            format: String
        ): com.snj.travelvista.data.remote.dto.WikivoyageGeosearchResponseDto {
            if (shouldThrowNetworkError) throw IOException("Simulated geosearch error")
            return com.snj.travelvista.data.remote.dto.WikivoyageGeosearchResponseDto()
        }

        override suspend fun getPageSections(
            page: String,
            action: String,
            prop: String,
            format: String
        ): com.snj.travelvista.data.remote.dto.WikivoyageSectionsResponseDto {
            if (shouldThrowNetworkError) throw IOException("Simulated sections error")
            return com.snj.travelvista.data.remote.dto.WikivoyageSectionsResponseDto()
        }

        override suspend fun getSectionHtml(
            page: String,
            sectionIndex: String,
            action: String,
            prop: String,
            format: String
        ): com.snj.travelvista.data.remote.dto.WikivoyageParseTextResponseDto {
            if (shouldThrowNetworkError) throw IOException("Simulated parse error")
            return com.snj.travelvista.data.remote.dto.WikivoyageParseTextResponseDto()
        }
    }

    private class FakeDestinationDao : DestinationDao {
        private val cachedMap = LinkedHashMap<Long, CachedDestinationEntity>()
        private val cachedFlow = MutableStateFlow<List<CachedDestinationEntity>>(emptyList())

        private val favoritesMap = LinkedHashMap<Long, DestinationEntity>()
        private val favoritesFlow = MutableStateFlow<List<DestinationEntity>>(emptyList())

        override fun getAllFavorites(): Flow<List<DestinationEntity>> = favoritesFlow.asStateFlow()

        override fun isFavorite(id: Long): Flow<Boolean> =
            favoritesFlow.map { list -> list.any { it.id == id } }

        override suspend fun isFavoriteSync(id: Long): Boolean =
            favoritesMap.containsKey(id)

        override suspend fun getFavoriteById(id: Long): DestinationEntity? =
            favoritesMap[id]

        override suspend fun insertFavorite(destination: DestinationEntity): Long {
            favoritesMap[destination.id] = destination
            favoritesFlow.value = favoritesMap.values.toList()
            return destination.id
        }

        override suspend fun deleteFavoriteById(id: Long): Int {
            val removed = favoritesMap.remove(id)
            favoritesFlow.value = favoritesMap.values.toList()
            return if (removed != null) 1 else 0
        }

        override fun getAllCachedDestinations(): Flow<List<CachedDestinationEntity>> =
            cachedFlow.asStateFlow()

        override suspend fun getAllCachedDestinationsSync(): List<CachedDestinationEntity> =
            cachedMap.values.toList()

        override suspend fun getCachedDestinationById(id: Long): CachedDestinationEntity? =
            cachedMap[id]

        override suspend fun getCachedDestinationBySlug(slug: String): CachedDestinationEntity? =
            cachedMap.values.find { it.slug == slug }

        override suspend fun insertCachedDestinations(destinations: List<CachedDestinationEntity>) {
            for (dest in destinations) {
                cachedMap[dest.id] = dest
            }
            cachedFlow.value = cachedMap.values.toList()
        }

        override suspend fun insertCachedDestination(destination: CachedDestinationEntity): Long {
            cachedMap[destination.id] = destination
            cachedFlow.value = cachedMap.values.toList()
            return destination.id
        }

        override suspend fun clearCachedDestinations() {
            cachedMap.clear()
            cachedFlow.value = emptyList()
        }
    }
}
