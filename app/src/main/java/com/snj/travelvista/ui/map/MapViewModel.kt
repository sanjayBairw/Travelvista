package com.snj.travelvista.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.data.remote.dto.NominatimPlaceDto
import com.snj.travelvista.data.repository.GlobalTravelRepository
import com.snj.travelvista.data.repository.TravelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val travelRepository: TravelRepository,
    private val globalRepository: GlobalTravelRepository
) : ViewModel() {

    private val _destinations = MutableStateFlow<List<Destination>>(emptyList())
    val destinations: StateFlow<List<Destination>> = _destinations.asStateFlow()

    private val _searchResults = MutableStateFlow<List<NominatimPlaceDto>>(emptyList())
    val searchResults: StateFlow<List<NominatimPlaceDto>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        loadMapDestinations()
    }

    fun loadMapDestinations() {
        viewModelScope.launch {
            val cached = travelRepository.getAllCachedDestinationsSnapshot()
            _destinations.value = cached.filter { it.latitude != null && it.longitude != null }
        }
    }

    fun searchMapLocation(query: String) {
        val clean = query.trim()
        if (clean.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = globalRepository.geocodeWorldwide(clean)
            _isSearching.value = false
        }
    }

    fun openGeocodedPlace(place: NominatimPlaceDto, onResolved: (Destination) -> Unit) {
        viewModelScope.launch {
            val dest = travelRepository.resolveAndCachePlace(
                title = place.resolvedTitle,
                country = place.resolvedRegion,
                category = place.mapToTravelCategory(),
                latitude = place.latitudeDouble,
                longitude = place.longitudeDouble,
                explicitId = 900000L + (place.placeId % 900000L)
            )
            loadMapDestinations()
            onResolved(dest)
        }
    }
}
