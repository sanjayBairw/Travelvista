package com.snj.travelvista.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snj.travelvista.data.local.entity.TripEntity
import com.snj.travelvista.data.model.CountryTravelInfo
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.data.model.DestinationCulinaryAndSights
import com.snj.travelvista.data.model.DestinationWeather
import com.snj.travelvista.data.model.NearbyAttraction
import com.snj.travelvista.data.repository.GlobalTravelRepository
import com.snj.travelvista.data.repository.TravelRepository
import com.snj.travelvista.ui.common.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: TravelRepository,
    private val globalRepository: GlobalTravelRepository? = null
) : ViewModel() {

    private val _destination = MutableStateFlow<Resource<Destination>>(Resource.Loading)
    val destination: StateFlow<Resource<Destination>> = _destination.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _weather = MutableStateFlow<Resource<DestinationWeather>?>(null)
    val weather: StateFlow<Resource<DestinationWeather>?> = _weather.asStateFlow()

    private val _countryInfo = MutableStateFlow<Resource<CountryTravelInfo>?>(null)
    val countryInfo: StateFlow<Resource<CountryTravelInfo>?> = _countryInfo.asStateFlow()

    private val _nearbyAttractions = MutableStateFlow<List<NearbyAttraction>>(emptyList())
    val nearbyAttractions: StateFlow<List<NearbyAttraction>> = _nearbyAttractions.asStateFlow()

    private val _culinaryAndSights = MutableStateFlow<DestinationCulinaryAndSights?>(null)
    val culinaryAndSights: StateFlow<DestinationCulinaryAndSights?> = _culinaryAndSights.asStateFlow()

    private var currentId: Long = -1L
    private var enrichedDestinationId: Long = -1L

    fun loadDestination(id: Long, fallbackSlug: String? = null, fallbackTitle: String? = null) {
        currentId = id
        viewModelScope.launch {
            repository.getDestinationById(id, fallbackSlug, fallbackTitle)
                .catch { emit(Resource.Error(it.localizedMessage ?: "Failed to load destination details")) }
                .collect { res ->
                    _destination.value = res
                    if (res is Resource.Success) {
                        val dest = res.data
                        _isFavorite.value = dest.isFavorite
                        if (enrichedDestinationId != dest.id) {
                            enrichedDestinationId = dest.id
                            recordViewAndLoadGlobalEnrichment(dest)
                        }
                    }
                }
        }

        viewModelScope.launch {
            repository.isFavorite(id)
                .collect { fav ->
                    _isFavorite.value = fav
                }
        }
    }

    private fun recordViewAndLoadGlobalEnrichment(dest: Destination) {
        val globalRepo = globalRepository ?: return

        viewModelScope.launch {
            globalRepo.recordDestinationView(dest)
        }

        // 1. Live Weather & Nearby Attractions (requires coordinates — geocode via Nominatim if absent)
        viewModelScope.launch {
            _weather.value = Resource.Loading
            var lat = dest.latitude
            var lon = dest.longitude
            var geocodedCountry = dest.country

            if (lat == null || lon == null) {
                val geoHit = globalRepo.geocodeWorldwide("${dest.title} ${dest.country}").firstOrNull()
                if (geoHit?.latitudeDouble != null && geoHit.longitudeDouble != null) {
                    lat = geoHit.latitudeDouble
                    lon = geoHit.longitudeDouble
                    if (geocodedCountry.isBlank() || geocodedCountry == "Travel Destination") {
                        geocodedCountry = geoHit.resolvedCountry
                    }
                }
            }

            if (lat != null && lon != null) {
                _weather.value = globalRepo.getWeather(lat, lon)
                _nearbyAttractions.value = globalRepo.getNearbyAttractions(lat, lon, dest.title)
            } else {
                _weather.value = Resource.Error("Coordinates unavailable for live weather")
            }

            // 2. Live Country Metadata (Currency, Language, Timezone, Dialing Code)
            _countryInfo.value = Resource.Loading
            val countryResult = globalRepo.getCountryInfo(geocodedCountry.ifBlank { dest.title })
            _countryInfo.value = countryResult
        }

        // 3. Live Culinary Specialties (Must-Try Foods) & Most Visited Sights
        viewModelScope.launch {
            val culinaryData = globalRepo.getCulinaryAndSights(
                slug = dest.slug,
                title = dest.title,
                locationDesc = dest.location,
                category = dest.category
            )
            _culinaryAndSights.value = culinaryData
        }
    }

    fun resolveNearbyPlace(attraction: NearbyAttraction, onResolved: (Long) -> Unit) {
        viewModelScope.launch {
            val dest = repository.resolveAndCachePlace(
                title = attraction.title,
                latitude = attraction.latitude,
                longitude = attraction.longitude,
                explicitId = 600000L + attraction.pageId
            )
            onResolved(dest.id)
        }
    }

    fun saveTripToPlanner(
        destination: Destination,
        tripName: String,
        startDate: String,
        endDate: String,
        travelersCount: Int,
        days: Int,
        notes: String,
        onSaved: (Long) -> Unit
    ) {
        val globalRepo = globalRepository ?: return
        viewModelScope.launch {
            val basePrice = if (destination.pricePerDay > 0) destination.pricePerDay else 135
            val estimatedTotal = basePrice * days * travelersCount
            val id = globalRepo.createTrip(
                TripEntity(
                    name = tripName.ifBlank { "${destination.title} Trip" },
                    destinationTitle = destination.title,
                    destinationId = destination.id,
                    startDate = startDate,
                    endDate = endDate,
                    travelersCount = travelersCount,
                    estimatedBudgetUsd = estimatedTotal,
                    notes = notes
                )
            )
            onSaved(id)
        }
    }

    fun toggleFavorite() {
        val dest = (_destination.value as? Resource.Success)?.data ?: return
        viewModelScope.launch {
            val newState = repository.toggleFavorite(dest)
            _isFavorite.value = newState
        }
    }
}
