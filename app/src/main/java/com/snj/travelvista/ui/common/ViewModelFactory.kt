package com.snj.travelvista.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.snj.travelvista.data.repository.GlobalTravelRepository
import com.snj.travelvista.data.repository.TravelRepository
import com.snj.travelvista.ui.destinations.DestinationsViewModel
import com.snj.travelvista.ui.details.DetailViewModel
import com.snj.travelvista.ui.explore.ExploreViewModel
import com.snj.travelvista.ui.favorites.FavoritesViewModel
import com.snj.travelvista.ui.map.MapViewModel
import com.snj.travelvista.ui.profile.ProfileViewModel
import com.snj.travelvista.ui.trips.TripsViewModel

class ViewModelFactory(
    private val repository: TravelRepository,
    private val globalRepository: GlobalTravelRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> {
                ExploreViewModel(repository, globalRepository) as T
            }
            modelClass.isAssignableFrom(DestinationsViewModel::class.java) -> {
                DestinationsViewModel(repository, globalRepository) as T
            }
            modelClass.isAssignableFrom(DetailViewModel::class.java) -> {
                DetailViewModel(repository, globalRepository) as T
            }
            modelClass.isAssignableFrom(FavoritesViewModel::class.java) -> {
                FavoritesViewModel(repository) as T
            }
            modelClass.isAssignableFrom(TripsViewModel::class.java) -> {
                requireNotNull(globalRepository) { "GlobalTravelRepository is required for TripsViewModel" }
                TripsViewModel(globalRepository) as T
            }
            modelClass.isAssignableFrom(MapViewModel::class.java) -> {
                requireNotNull(globalRepository) { "GlobalTravelRepository is required for MapViewModel" }
                MapViewModel(repository, globalRepository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                requireNotNull(globalRepository) { "GlobalTravelRepository is required for ProfileViewModel" }
                ProfileViewModel(globalRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
