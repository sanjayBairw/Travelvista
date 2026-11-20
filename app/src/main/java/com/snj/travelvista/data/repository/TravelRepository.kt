package com.snj.travelvista.data.repository

import com.snj.travelvista.data.local.DestinationDao
import com.snj.travelvista.data.local.entity.CachedDestinationEntity
import com.snj.travelvista.data.local.entity.DestinationEntity
import com.snj.travelvista.data.model.Category
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.data.model.SortOption
import com.snj.travelvista.data.remote.RetrofitClient
import com.snj.travelvista.data.remote.TravelApiService
import com.snj.travelvista.data.remote.TravelMockData
import com.snj.travelvista.data.remote.WikivoyageApiService
import com.snj.travelvista.data.remote.service.GeocodingService
import com.snj.travelvista.ui.common.Resource
import com.snj.travelvista.utils.ImageLoader
import com.snj.travelvista.utils.NetworkConnectivityMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

class TravelRepository(
    private val wikivoyageApiService: WikivoyageApiService,
    private val destinationDao: DestinationDao,
    private val fallbackApiService: TravelApiService? = null,
    private val connectivityMonitor: NetworkConnectivityMonitor? = null,
    private val geocodingService: GeocodingService? = null
) {

    /**
     * Secondary constructor for backward compatibility.
     */
    constructor(
        apiService: TravelApiService,
        destinationDao: DestinationDao
    ) : this(
        wikivoyageApiService = RetrofitClient.wikivoyageApiService,
        destinationDao = destinationDao,
        fallbackApiService = apiService
    )

    data class ImageCredit(
        val author: String,
        val license: String,
        val creditUrl: String
    )

    /**
     * Map of destination ID to its verified Wikivoyage article slug.
     */
    private val slugMap: Map<Long, String> = mapOf(
        1L to "Santorini",
        2L to "Banff_National_Park",
        3L to "Kyoto",
        4L to "Amalfi_Coast",
        5L to "Swiss_Alps",
        6L to "New_York_City",
        7L to "Paris",
        8L to "Serengeti_National_Park",
        9L to "Bali",
        10L to "Machu_Picchu",
        11L to "Rome",
        12L to "Great_Barrier_Reef",
        13L to "Mount_Fuji",
        14L to "Petra"
    )

    /**
     * Verified individual image attribution, photographers, and licenses
     * directly queried from Wikimedia Commons metadata.
     */
    val imageCreditsMap: Map<Long, ImageCredit> = mapOf(
        1L to ImageCredit("(WT-en) Chmouel", "CC BY-SA 1.0", "https://commons.wikimedia.org/wiki/File:Greece-Santorini.jpg"),
        2L to ImageCredit("Traveler100", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:MoraineLakeP02.jpg"),
        3L to ImageCredit("Tmv", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Kiyomizu_Temple_in_Kyoto.jpg"),
        4L to ImageCredit("Jensens", "Public domain", "https://commons.wikimedia.org/wiki/File:Pastena.jpg"),
        5L to ImageCredit("Andrew Bossi", "CC BY-SA 2.5", "https://commons.wikimedia.org/wiki/File:4940_-_M%C3%BCrren-Gimmelwald.JPG"),
        6L to ImageCredit("Dllu", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:View_of_Empire_State_Building_from_Rockefeller_Center_New_York_City_dllu_(cropped).jpg"),
        7L to ImageCredit("Skyguy414", "Public domain", "https://commons.wikimedia.org/wiki/File:View_of_the_Seine_in_Paris_in_september_2006.jpg"),
        8L to ImageCredit("Cliff Kemper", "CC BY-SA 2.0", "https://commons.wikimedia.org/wiki/File:Serengeti_gazelles.jpg"),
        9L to ImageCredit("Riza Nugraha", "CC BY 2.0", "https://commons.wikimedia.org/wiki/File:Odalan_preparation_inside_a_pura.jpg"),
        10L to ImageCredit("Martin St-Amant (S23678)", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:80_-_Machu_Picchu_-_Juin_2009_-_edit.2.jpg"),
        11L to ImageCredit("FeaturedPics", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Colosseo_2020.jpg"),
        12L to ImageCredit("Toby Hudson", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:Coral_Sphere_Flynn_Reef.jpg"),
        13L to ImageCredit("Alpsdake", "Public domain", "https://commons.wikimedia.org/wiki/File:01_Fujisan_from_Yamanakako_2004-2-7.jpg"),
        14L to ImageCredit("Berthold Werner", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:Petra_Jordan_BW_21.JPG")
    )

    /**
     * Returns destinations with offline-first architecture:
     * 1. Emits cached data from Room immediately if available.
     * 2. Fetches live data from Wikivoyage REST API when online.
     * 3. Updates Room cache with real photography and descriptions.
     * 4. Reactively combines with user's persistent favorites.
     */
    fun getDestinations(
        category: String? = null,
        searchQuery: String? = null,
        sortOption: SortOption = SortOption.RATING,
        regionFilter: String? = null
    ): Flow<Resource<List<Destination>>> = flow {
        // Step 1: Check Room cache
        val cachedEntities = destinationDao.getAllCachedDestinationsSync()
        if (cachedEntities.isNotEmpty()) {
            val cachedList = cachedEntities.map { it.toDestination() }
            emit(Resource.Success(filterAndSort(cachedList, category, searchQuery, sortOption, regionFilter)))
        } else {
            emit(Resource.Loading)
        }

        // Step 2: Attempt network fetch from Real Wikivoyage + Nominatim APIs
        try {
            val queryTrimmed = searchQuery?.trim().orEmpty()
            val liveDestinations = if (cachedEntities.isNotEmpty() && queryTrimmed.length >= 2) {
                cachedEntities.map { it.toDestination() }
            } else {
                val fetched = fetchLiveEnrichedDestinations()
                destinationDao.insertCachedDestinations(
                    fetched.map { dest ->
                        CachedDestinationEntity.fromDestination(dest, dest.slug)
                    }
                )
                fetched
            }

            var filteredList = filterAndSort(liveDestinations, category, searchQuery, sortOption, regionFilter)

            // Step 2b: Worldwide search via Wikivoyage + OpenStreetMap Nominatim when user searches globally
            if (queryTrimmed.length >= 2 && filteredList.size < 3) {
                val remoteResults = performWorldwideSearch(queryTrimmed)
                if (remoteResults.isNotEmpty()) {
                    val existingTitles = filteredList.map { it.title.lowercase() }.toMutableSet()
                    val merged = filteredList.toMutableList()
                    remoteResults.forEach { remote ->
                        if (existingTitles.add(remote.title.lowercase())) {
                            merged.add(remote)
                        }
                    }
                    filteredList = filterAndSort(merged, category, null, sortOption, regionFilter)
                }
            }

            emit(Resource.Success(filteredList))
        } catch (e: Exception) {
            // Step 3: Handle network failure gracefully
            if (cachedEntities.isEmpty()) {
                val fallbackList = TravelMockData.destinations.map { seed ->
                    val credit = imageCreditsMap[seed.id]
                    seed.copy(
                        slug = slugMap[seed.id] ?: "",
                        imageAuthor = credit?.author ?: "",
                        imageLicense = credit?.license ?: "",
                        imageCreditUrl = credit?.creditUrl ?: ""
                    )
                }
                val filtered = filterAndSort(fallbackList, category, searchQuery, sortOption, regionFilter)
                emit(Resource.Success(filtered))
            }
        }
    }.combine(destinationDao.getAllFavorites()) { resource, favorites ->
        mergeFavoritesWithResource(resource, favorites)
    }.flowOn(Dispatchers.IO)

    suspend fun getAllCachedDestinationsSnapshot(): List<Destination> = withContext(Dispatchers.IO) {
        val cached = destinationDao.getAllCachedDestinationsSync().map { it.toDestination() }
        if (cached.isNotEmpty()) cached else TravelMockData.destinations
    }

    /**
     * Resolves and caches a destination discovered via Map or Nearby Geosearch so DetailFragment can display it.
     */
    suspend fun resolveAndCachePlace(
        title: String,
        country: String = "",
        category: String = "Cities",
        latitude: Double? = null,
        longitude: Double? = null,
        explicitId: Long? = null
    ): Destination = withContext(Dispatchers.IO) {
        val slug = title.trim().replace(" ", "_")
        val existingBySlug = destinationDao.getCachedDestinationBySlug(slug)?.toDestination()
        if (existingBySlug != null) return@withContext existingBySlug

        val summary = try {
            retryIO(retries = 1, delayMs = 200) { wikivoyageApiService.getPageSummary(slug) }
        } catch (e: Exception) {
            null
        }

        val rawImg = summary?.bestImageUrl ?: summary?.thumbnail?.source ?: ""
        val cleanImg = ImageLoader.resolveValidImageUrl(rawImg, title, category)
        val id = explicitId ?: (700000L + (slug.hashCode().toLong() and 0x7FFFFL))
        val resolvedCountry = country.ifBlank {
            summary?.description?.takeIf { it.isNotBlank() } ?: "Global Destination"
        }

        val dest = Destination(
            id = id,
            title = summary?.title?.ifBlank { title } ?: title,
            country = resolvedCountry,
            city = title,
            category = category,
            rating = 0.0,
            reviewCount = 0,
            imageUrl = cleanImg,
            description = summary?.extract?.ifBlank { null }
                ?: "Explore $title. Real-time coordinates, weather, local currency, and nearby points of interest are provided via live open travel APIs.",
            highlights = listOf(
                "Discover landmarks and cultural spots around $title",
                "Explore nearby attractions on the interactive map",
                "Check live local weather and travel conditions"
            ),
            travelTips = listOf(
                "Check local currency, time zone, and seasonal weather before departure.",
                "Verify hotel and flight availability through authorized booking providers."
            ),
            bestTimeToVisit = "Check live weather and seasonal travel advisories for $title.",
            pricePerDay = 0,
            slug = slug,
            latitude = summary?.coordinates?.latitude ?: latitude,
            longitude = summary?.coordinates?.longitude ?: longitude,
            imageAuthor = if (cleanImg.isNotBlank()) "Wikimedia Commons Contributor" else "",
            imageLicense = if (cleanImg.isNotBlank()) "Check Wikimedia Commons source" else "",
            imageCreditUrl = "https://en.wikivoyage.org/wiki/$slug"
        )
        destinationDao.insertCachedDestination(CachedDestinationEntity.fromDestination(dest, slug))
        dest
    }

    fun getFeaturedDestinations(): Flow<Resource<List<Destination>>> = flow {
        val cached = destinationDao.getAllCachedDestinationsSync().map { it.toDestination() }
        if (cached.isNotEmpty()) {
            emit(Resource.Success(cached.filter { it.isFeatured }))
        } else {
            emit(Resource.Loading)
        }

        try {
            val live = fetchLiveEnrichedDestinations()
            destinationDao.insertCachedDestinations(live.map { CachedDestinationEntity.fromDestination(it, it.slug) })
            emit(Resource.Success(live.filter { it.isFeatured }))
        } catch (e: Exception) {
            if (cached.isEmpty()) {
                val fallback = TravelMockData.destinations.filter { it.isFeatured }
                emit(Resource.Success(fallback))
            }
        }
    }.combine(destinationDao.getAllFavorites()) { resource, favorites ->
        mergeFavoritesWithResource(resource, favorites)
    }.flowOn(Dispatchers.IO)

    fun getPopularDestinations(): Flow<Resource<List<Destination>>> = flow {
        val cached = destinationDao.getAllCachedDestinationsSync().map { it.toDestination() }
        if (cached.isNotEmpty()) {
            emit(Resource.Success(cached.filter { it.isPopular }))
        } else {
            emit(Resource.Loading)
        }

        try {
            val live = fetchLiveEnrichedDestinations()
            destinationDao.insertCachedDestinations(live.map { CachedDestinationEntity.fromDestination(it, it.slug) })
            emit(Resource.Success(live.filter { it.isPopular }))
        } catch (e: Exception) {
            if (cached.isEmpty()) {
                val fallback = TravelMockData.destinations.filter { it.isPopular }
                emit(Resource.Success(fallback))
            }
        }
    }.combine(destinationDao.getAllFavorites()) { resource, favorites ->
        mergeFavoritesWithResource(resource, favorites)
    }.flowOn(Dispatchers.IO)

    fun getTrendingDestinations(): Flow<Resource<List<Destination>>> = flow {
        val cached = destinationDao.getAllCachedDestinationsSync().map { it.toDestination() }
        if (cached.isNotEmpty()) {
            emit(Resource.Success(cached.filter { it.isTrending }))
        } else {
            emit(Resource.Loading)
        }

        try {
            val live = fetchLiveEnrichedDestinations()
            destinationDao.insertCachedDestinations(live.map { CachedDestinationEntity.fromDestination(it, it.slug) })
            emit(Resource.Success(live.filter { it.isTrending }))
        } catch (e: Exception) {
            if (cached.isEmpty()) {
                val fallback = TravelMockData.destinations.filter { it.isTrending }
                emit(Resource.Success(fallback))
            }
        }
    }.combine(destinationDao.getAllFavorites()) { resource, favorites ->
        mergeFavoritesWithResource(resource, favorites)
    }.flowOn(Dispatchers.IO)

    fun getDestinationById(
        id: Long,
        fallbackSlug: String? = null,
        fallbackTitle: String? = null
    ): Flow<Resource<Destination>> = flow {
        // 1. Check Room cache, favorites, or lookup by slug
        var localCached = destinationDao.getCachedDestinationById(id)?.toDestination()
            ?: destinationDao.getFavoriteById(id)?.toDestination()
            ?: (if (!fallbackSlug.isNullOrBlank()) destinationDao.getCachedDestinationBySlug(fallbackSlug)?.toDestination() else null)

        // If not in Room yet, check in-memory seed or construct instant recovery destination
        if (localCached == null) {
            val fallbackSeed = TravelMockData.destinations.find { it.id == id }
            if (fallbackSeed != null) {
                val credit = imageCreditsMap[id]
                localCached = fallbackSeed.copy(
                    slug = slugMap[id] ?: fallbackSeed.slug,
                    imageAuthor = credit?.author ?: fallbackSeed.imageAuthor,
                    imageLicense = credit?.license ?: fallbackSeed.imageLicense,
                    imageCreditUrl = credit?.creditUrl ?: fallbackSeed.imageCreditUrl
                )
            } else if (!fallbackTitle.isNullOrBlank() || !fallbackSlug.isNullOrBlank()) {
                val name = fallbackTitle?.ifBlank { null } ?: fallbackSlug?.replace("_", " ") ?: "Destination"
                val cat = "Cities"
                val photo = ImageLoader.resolveValidImageUrl("", name, cat)
                localCached = Destination(
                    id = if (id > 0) id else (700000L + (name.hashCode().toLong() and 0x7FFFFL)),
                    title = name,
                    country = "Travel Destination",
                    city = name,
                    category = cat,
                    rating = 4.8,
                    reviewCount = 120,
                    imageUrl = photo,
                    description = "Explore $name. Live weather, local culinary specialties, and top attractions are loaded dynamically.",
                    highlights = listOf(
                        "Discover iconic landmarks in $name",
                        "Experience authentic regional culture and cuisine",
                        "Explore panoramic viewpoints and nearby sights"
                    ),
                    travelTips = listOf(
                        "Check seasonal weather and travel advisories before arriving.",
                        "Compare booking options for accommodations and transit."
                    ),
                    bestTimeToVisit = "Spring and autumn offer optimal travel conditions.",
                    pricePerDay = 120,
                    slug = fallbackSlug?.ifBlank { null } ?: name.replace(" ", "_")
                )
            }
        }

        if (localCached != null) {
            val isFav = destinationDao.isFavoriteSync(localCached.id)
            emit(Resource.Success(localCached.copy(isFavorite = isFav)))
        } else {
            emit(Resource.Loading)
        }

        // 2. Determine slug for live enrichment from Wikivoyage
        val slug = slugMap[id] ?: localCached?.slug?.ifBlank { null } ?: fallbackSlug?.ifBlank { null } ?: fallbackTitle?.replace(" ", "_") ?: ""
        if (slug.isNotEmpty()) {
            try {
                val summary = retryIO(retries = 1, delayMs = 250) { wikivoyageApiService.getPageSummary(slug) }
                val base = localCached ?: TravelMockData.destinations.find { it.id == id }
                val credit = imageCreditsMap[id]
                val rawImage = summary.bestImageUrl ?: summary.thumbnail?.source ?: base?.imageUrl ?: ""
                val resolvedTitle = summary.title?.ifBlank { null } ?: base?.title ?: fallbackTitle ?: slug.replace("_", " ")
                val realImage = ImageLoader.resolveValidImageUrl(rawImage, resolvedTitle, base?.category ?: "Cities")
                val realDesc = summary.extract?.ifBlank { null }
                    ?: base?.description
                    ?: "Explore $resolvedTitle. Discover iconic landmarks, local food specialties, live weather, and travel tips."

                val enriched = if (base != null) {
                    base.copy(
                        imageUrl = if (realImage.isNotBlank()) realImage else base.imageUrl,
                        description = realDesc,
                        slug = slug,
                        latitude = summary.coordinates?.latitude ?: base.latitude,
                        longitude = summary.coordinates?.longitude ?: base.longitude,
                        imageAuthor = credit?.author ?: base.imageAuthor,
                        imageLicense = credit?.license ?: base.imageLicense,
                        imageCreditUrl = credit?.creditUrl ?: base.imageCreditUrl
                    )
                } else {
                    Destination(
                        id = if (id > 0) id else (700000L + (slug.hashCode().toLong() and 0x7FFFFL)),
                        title = resolvedTitle,
                        country = summary.description?.takeIf { it.isNotBlank() } ?: "Global Destination",
                        city = resolvedTitle,
                        category = base?.category ?: "Cities",
                        rating = 4.8,
                        reviewCount = 150,
                        imageUrl = realImage,
                        description = realDesc,
                        highlights = listOf(
                            "Discover iconic landmarks in $resolvedTitle",
                            "Experience authentic regional culture and cuisine",
                            "Explore panoramic viewpoints and nearby sights"
                        ),
                        travelTips = listOf(
                            "Check seasonal weather and travel advisories before arriving.",
                            "Compare booking options for accommodations and transit."
                        ),
                        bestTimeToVisit = "Spring and autumn typically offer pleasant travel conditions.",
                        pricePerDay = 120,
                        slug = slug,
                        latitude = summary.coordinates?.latitude,
                        longitude = summary.coordinates?.longitude,
                        imageAuthor = "Wikimedia Commons Contributor",
                        imageLicense = "Wikimedia Commons",
                        imageCreditUrl = "https://en.wikivoyage.org/wiki/$slug"
                    )
                }
                destinationDao.insertCachedDestination(CachedDestinationEntity.fromDestination(enriched, slug))
                val isFav = destinationDao.isFavoriteSync(enriched.id)
                emit(Resource.Success(enriched.copy(isFavorite = isFav)))
            } catch (e: Exception) {
                if (localCached == null) {
                    val fallback = TravelMockData.destinations.find { it.id == id }
                    if (fallback != null) {
                        val credit = imageCreditsMap[id]
                        val isFav = destinationDao.isFavoriteSync(fallback.id)
                        emit(Resource.Success(fallback.copy(
                            isFavorite = isFav,
                            imageAuthor = credit?.author ?: "",
                            imageLicense = credit?.license ?: "",
                            imageCreditUrl = credit?.creditUrl ?: ""
                        )))
                    } else if (!fallbackTitle.isNullOrBlank() || slug.isNotBlank()) {
                        val name = fallbackTitle ?: slug.replace("_", " ")
                        val photo = ImageLoader.resolveValidImageUrl("", name, "Cities")
                        val recoveryDest = Destination(
                            id = if (id > 0) id else (700000L + (name.hashCode().toLong() and 0x7FFFFL)),
                            title = name,
                            country = "Travel Destination",
                            city = name,
                            category = "Cities",
                            rating = 4.8,
                            reviewCount = 150,
                            imageUrl = photo,
                            description = "Explore $name. Live coordinates, weather, currency, and local travel details are dynamically loaded.",
                            highlights = listOf(
                                "Discover scenic spots around $name",
                                "Explore local food specialties and top sights",
                                "Check live weather and travel conditions"
                            ),
                            travelTips = listOf(
                                "Verify local currency and seasonal weather before departure.",
                                "Plan transit and lodging with official providers."
                            ),
                            bestTimeToVisit = "Consult live weather data for $name.",
                            pricePerDay = 120,
                            slug = slug
                        )
                        destinationDao.insertCachedDestination(CachedDestinationEntity.fromDestination(recoveryDest, slug))
                        val isFav = destinationDao.isFavoriteSync(recoveryDest.id)
                        emit(Resource.Success(recoveryDest.copy(isFavorite = isFav)))
                    } else {
                        emit(Resource.Error("Destination not found: ${e.localizedMessage}", e))
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getCategories(): Flow<Resource<List<Category>>> = flow {
        emit(Resource.Loading)
        try {
            val categories = fallbackApiService?.getCategories() ?: TravelMockData.categories
            emit(Resource.Success(categories))
        } catch (e: Exception) {
            emit(Resource.Success(TravelMockData.categories))
        }
    }.flowOn(Dispatchers.IO)

    // --- FAVORITES (Persistent in Room) ---

    fun getAllFavorites(): Flow<List<Destination>> =
        destinationDao.getAllFavorites().map { entities ->
            entities.map { it.toDestination() }
        }.flowOn(Dispatchers.IO)

    fun isFavorite(id: Long): Flow<Boolean> =
        destinationDao.isFavorite(id).flowOn(Dispatchers.IO)

    suspend fun toggleFavorite(destination: Destination): Boolean = withContext(Dispatchers.IO) {
        val currentlyFavorite = destinationDao.isFavoriteSync(destination.id)
        if (currentlyFavorite) {
            destinationDao.deleteFavoriteById(destination.id)
            false
        } else {
            val entity = DestinationEntity.fromDestination(destination)
            destinationDao.insertFavorite(entity)
            true
        }
    }

    suspend fun removeFavorite(destinationId: Long): Boolean = withContext(Dispatchers.IO) {
        destinationDao.deleteFavoriteById(destinationId) > 0
    }

    suspend fun addFavorite(destination: Destination): Long = withContext(Dispatchers.IO) {
        val entity = DestinationEntity.fromDestination(destination)
        destinationDao.insertFavorite(entity)
    }

    // --- PRIVATE HELPERS ---

    /**
     * Fetches real destination data concurrently from Wikivoyage REST API
     * in polite batches of 4 to prevent network bursts and 429 rate-limiting.
     */
    suspend fun fetchLiveEnrichedDestinations(): List<Destination> = withContext(Dispatchers.IO) {
        val baseDestinations = TravelMockData.destinations
        val enrichedList = mutableListOf<Destination>()

        baseDestinations.chunked(4).forEach { chunk ->
            val chunkResults = coroutineScope {
                chunk.map { dest ->
                    async {
                        val slug = slugMap[dest.id] ?: dest.slug
                        val credit = imageCreditsMap[dest.id]
                        if (slug.isBlank()) {
                            return@async dest.copy(
                                imageAuthor = credit?.author ?: dest.imageAuthor,
                                imageLicense = credit?.license ?: dest.imageLicense,
                                imageCreditUrl = credit?.creditUrl ?: dest.imageCreditUrl
                            )
                        }

                        try {
                            val summary = retryIO { wikivoyageApiService.getPageSummary(slug) }
                            val realImageUrl = summary.bestImageUrl ?: dest.imageUrl
                            val realDescription = summary.extract?.ifBlank { dest.description } ?: dest.description

                            dest.copy(
                                imageUrl = realImageUrl,
                                description = realDescription,
                                slug = slug,
                                latitude = summary.coordinates?.latitude ?: dest.latitude,
                                longitude = summary.coordinates?.longitude ?: dest.longitude,
                                imageAuthor = credit?.author ?: dest.imageAuthor,
                                imageLicense = credit?.license ?: dest.imageLicense,
                                imageCreditUrl = credit?.creditUrl ?: dest.imageCreditUrl
                            )
                        } catch (e: Exception) {
                            dest.copy(
                                slug = slug,
                                imageAuthor = credit?.author ?: dest.imageAuthor,
                                imageLicense = credit?.license ?: dest.imageLicense,
                                imageCreditUrl = credit?.creditUrl ?: dest.imageCreditUrl
                            )
                        }
                    }
                }.awaitAll()
            }
            enrichedList.addAll(chunkResults)
        }

        enrichedList
    }

    /**
     * Retries transient network failures with exponential backoff.
     */
    private suspend fun <T> retryIO(
        retries: Int = 2,
        delayMs: Long = 300,
        block: suspend () -> T
    ): T {
        var currentDelay = delayMs
        repeat(retries) {
            try {
                return block()
            } catch (e: IOException) {
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return block()
    }

    /**
     * Performs worldwide destination search combining live Wikivoyage Search + OpenStreetMap Nominatim Geocoding.
     * Enables finding any country, city, region, island, beach, mountain, or attraction on Earth.
     */
    private suspend fun performWorldwideSearch(query: String): List<Destination> = coroutineScope {
        val wikivoyageDeferred = async { performRemoteWikivoyageSearch(query) }
        val nominatimDeferred = async { performNominatimSearch(query) }

        val wikiResults = wikivoyageDeferred.await()
        val geoResults = nominatimDeferred.await()

        val seenTitles = mutableSetOf<String>()
        val combined = mutableListOf<Destination>()

        // Prefer Wikivoyage hits first (rich travel guide extracts & photography)
        wikiResults.forEach { dest ->
            if (seenTitles.add(dest.title.lowercase())) {
                // Enrich missing coordinates from matching Nominatim hit if available
                val geoMatch = geoResults.find {
                    it.title.equals(dest.title, ignoreCase = true)
                }
                val enriched = if (dest.latitude == null && geoMatch?.latitude != null) {
                    dest.copy(
                        latitude = geoMatch.latitude,
                        longitude = geoMatch.longitude,
                        country = if (dest.country == "Travel Destination" && geoMatch.country.isNotBlank()) geoMatch.country else dest.country
                    )
                } else dest
                combined.add(enriched)
            }
        }

        // Add global Nominatim places not already covered by Wikivoyage
        geoResults.forEach { geo ->
            if (seenTitles.add(geo.title.lowercase())) {
                combined.add(geo)
            }
        }

        if (combined.isNotEmpty()) {
            destinationDao.insertCachedDestinations(
                combined.map { CachedDestinationEntity.fromDestination(it, it.slug) }
            )
        }
        combined
    }

    /**
     * Queries OpenStreetMap Nominatim for any worldwide location and enriches with Wikivoyage imagery when available.
     */
    private suspend fun performNominatimSearch(query: String): List<Destination> {
        val service = geocodingService ?: return emptyList()
        return try {
            val places = retryIO(retries = 1, delayMs = 250) { service.searchWorldwide(query, limit = 6) }
            places.mapNotNull { place ->
                val title = place.resolvedTitle
                val lat = place.latitudeDouble
                val lon = place.longitudeDouble
                if (title.isBlank() || lat == null || lon == null) return@mapNotNull null

                val slug = title.replace(" ", "_")
                val summary = try {
                    wikivoyageApiService.getPageSummary(slug)
                } catch (e: Exception) {
                    null
                }

                val rawImg = summary?.bestImageUrl ?: summary?.thumbnail?.source ?: ""
                val cat = place.mapToTravelCategory()
                val imgUrl = ImageLoader.resolveValidImageUrl(rawImg, title, cat)

                Destination(
                    id = 900000L + (place.placeId % 900000L),
                    title = title,
                    country = place.resolvedRegion,
                    city = title,
                    category = cat,
                    rating = 4.8,
                    reviewCount = 120,
                    imageUrl = imgUrl,
                    description = summary?.extract?.ifBlank { null }
                        ?: (place.displayName ?: "Worldwide location discovered via OpenStreetMap Nominatim."),
                    highlights = listOf(
                        "Explore $title (${place.resolvedRegion})",
                        "View exact coordinates and nearby attractions on the map",
                        "Check live local weather and country travel essentials"
                    ),
                    travelTips = listOf(
                        "Verify local weather, visa rules, and transport links before traveling.",
                        "Compare hotel and flight options via official booking providers."
                    ),
                    bestTimeToVisit = "Consult live seasonal weather data in the weather card above.",
                    pricePerDay = estimateDailyPrice(cat),
                    slug = slug,
                    latitude = lat,
                    longitude = lon,
                    imageAuthor = if (imgUrl.isNotBlank()) "Wikimedia Commons Contributor" else "OpenStreetMap Contributors",
                    imageLicense = if (imgUrl.isNotBlank()) "Wikimedia Commons" else "ODbL (OpenStreetMap)",
                    imageCreditUrl = "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=12/$lat/$lon"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Searches live Wikivoyage API dynamically when a keyword is searched globally.
     */
    private suspend fun performRemoteWikivoyageSearch(query: String): List<Destination> {
        return try {
            val searchResponse = wikivoyageApiService.searchDestinations(query, limit = 6)
            val pages = searchResponse.pages
            if (pages.isEmpty()) return emptyList()

            val dynamicDestinations = pages.mapNotNull { page ->
                try {
                    val summary = try {
                        retryIO(retries = 1, delayMs = 200) { wikivoyageApiService.getPageSummary(page.key) }
                    } catch (e: Exception) {
                        null
                    }

                    val pageThumb = page.thumbnail?.url?.let {
                        if (it.startsWith("//")) "https:$it" else it
                    }
                    val rawImg = summary?.bestImageUrl
                        ?: summary?.thumbnail?.source
                        ?: pageThumb
                        ?: ""
                    val locationDesc = summary?.description
                        ?.takeIf { it.isNotBlank() }
                        ?: page.description
                        ?.takeIf { it.isNotBlank() }
                        ?: "Travel Destination"

                    val cat = inferCategoryFromText("${page.title} $locationDesc ${summary?.extract.orEmpty()} ${page.excerpt.orEmpty()}")
                    val imgUrl = ImageLoader.resolveValidImageUrl(rawImg, page.title, cat)

                    val desc = summary?.extract?.ifBlank { null }
                        ?: page.excerpt?.replace(Regex("<[^>]*>"), "")?.ifBlank { null }
                        ?: "Explore ${page.title} on Wikivoyage."

                    Destination(
                        id = 50000L + page.id,
                        title = page.title,
                        country = locationDesc,
                        city = page.title,
                        category = cat,
                        rating = 4.8,
                        reviewCount = 150,
                        imageUrl = imgUrl,
                        description = desc,
                        highlights = listOf(
                            "Discover iconic landmarks in ${page.title}",
                            "Immerse in local culture and traditions",
                            "Explore nearby attractions and viewpoints"
                        ),
                        travelTips = listOf(
                            "Research local transportation and transit cards in advance.",
                            "Check local weather, currency, and attraction opening hours."
                        ),
                        bestTimeToVisit = "Check live weather and seasonal travel advisories.",
                        pricePerDay = estimateDailyPrice(cat),
                        slug = page.key,
                        latitude = summary?.coordinates?.latitude,
                        longitude = summary?.coordinates?.longitude,
                        imageAuthor = "Wikimedia Commons Contributor",
                        imageLicense = "Wikimedia Commons (see source link)",
                        imageCreditUrl = "https://en.wikivoyage.org/wiki/${page.key}"
                    )
                } catch (e: Exception) {
                    null
                }
            }

            if (dynamicDestinations.isNotEmpty()) {
                destinationDao.insertCachedDestinations(
                    dynamicDestinations.map { CachedDestinationEntity.fromDestination(it, it.slug) }
                )
            }

            dynamicDestinations
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun inferCategoryFromText(text: String): String {
        val lower = text.lowercase()
        return when {
            listOf("mountain", "alps", "peak", "himalaya", "andes", "volcano", "ski").any { it in lower } -> "Mountains"
            listOf("beach", "island", "coast", "atoll", "bay", "archipelago", "sea").any { it in lower } -> "Beaches"
            listOf("national park", "forest", "nature", "wildlife", "safari", "lake", "waterfall", "fjord").any { it in lower } -> "Nature"
            listOf("ancient", "historic", "temple", "ruins", "castle", "unesco", "heritage", "citadel").any { it in lower } -> "Historical Places"
            else -> "Cities"
        }
    }

    private fun filterAndSort(
        destinations: List<Destination>,
        category: String?,
        searchQuery: String?,
        sortOption: SortOption,
        regionFilter: String? = null
    ): List<Destination> {
        var result = destinations

        // Filter by category
        if (!category.isNullOrBlank() && !category.equals("all", ignoreCase = true)) {
            result = result.filter { it.category.equals(category, ignoreCase = true) }
        }

        // Filter by global region if selected
        if (!regionFilter.isNullOrBlank() && !regionFilter.equals("All Regions", ignoreCase = true)) {
            result = result.filter { matchesRegion(it, regionFilter) }
        }

        // Filter by search query
        if (!searchQuery.isNullOrBlank()) {
            val query = searchQuery.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(query) ||
                        it.country.lowercase().contains(query) ||
                        it.city.lowercase().contains(query) ||
                        it.description.lowercase().contains(query) ||
                        it.tags.any { tag -> tag.lowercase().contains(query) }
            }
        }

        // Apply sort option
        return when (sortOption) {
            SortOption.RATING -> result.sortedByDescending { it.rating }
            SortOption.POPULARITY -> result.sortedByDescending { it.reviewCount }
            SortOption.PRICE_LOW_TO_HIGH -> result.sortedBy { if (it.pricePerDay > 0) it.pricePerDay else Int.MAX_VALUE }
            SortOption.ALPHABETICAL -> result.sortedBy { it.title }
        }
    }

    private fun matchesRegion(dest: Destination, region: String): Boolean {
        val text = "${dest.country} ${dest.city} ${dest.description}".lowercase()
        val keywords = when (region.lowercase()) {
            "europe" -> listOf("greece", "switzerland", "italy", "iceland", "france", "spain", "uk", "england", "germany", "portugal", "norway", "austria", "netherlands", "europe")
            "asia" -> listOf("japan", "indonesia", "bali", "india", "goa", "maldives", "thailand", "vietnam", "china", "korea", "singapore", "dubai", "uae", "nepal", "asia")
            "americas" -> listOf("peru", "canada", "usa", "united states", "mexico", "brazil", "argentina", "chile", "colombia", "america", "new york", "banff")
            "africa" -> listOf("morocco", "egypt", "kenya", "south africa", "tanzania", "africa", "marrakech", "cairo")
            "oceania" -> listOf("australia", "new zealand", "fiji", "bora bora", "tahiti", "oceania", "sydney", "queenstown")
            else -> return true
        }
        return keywords.any { it in text }
    }

    private fun mergeFavoritesWithResource(
        resource: Resource<List<Destination>>,
        favorites: List<DestinationEntity>
    ): Resource<List<Destination>> {
        return if (resource is Resource.Success) {
            val favoriteIds = favorites.map { it.id }.toSet()
            val updated = resource.data.map { destination ->
                destination.copy(isFavorite = favoriteIds.contains(destination.id))
            }
            Resource.Success(updated)
        } else {
            resource
        }
    }

    private fun estimateDailyPrice(category: String): Int = when (category.lowercase()) {
        "beaches" -> 145
        "mountains" -> 115
        "cities" -> 160
        "historical", "historical places" -> 125
        "nature" -> 110
        else -> 135
    }
}
