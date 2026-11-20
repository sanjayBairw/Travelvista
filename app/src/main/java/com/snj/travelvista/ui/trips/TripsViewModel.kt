package com.snj.travelvista.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snj.travelvista.data.local.entity.ItineraryItemEntity
import com.snj.travelvista.data.local.entity.TripEntity
import com.snj.travelvista.data.repository.GlobalTravelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TripsViewModel(
    private val globalRepository: GlobalTravelRepository
) : ViewModel() {

    private val _trips = MutableStateFlow<List<TripEntity>>(emptyList())
    val trips: StateFlow<List<TripEntity>> = _trips.asStateFlow()

    init {
        viewModelScope.launch {
            globalRepository.getAllTrips()
                .catch { emit(emptyList()) }
                .collect { _trips.value = it }
        }
    }

    fun createTrip(
        name: String,
        destinationTitle: String,
        startDate: String,
        endDate: String,
        travelersCount: Int,
        estimatedBudgetUsd: Int,
        notes: String
    ) {
        viewModelScope.launch {
            globalRepository.createTrip(
                TripEntity(
                    name = name.trim(),
                    destinationTitle = destinationTitle.trim(),
                    startDate = startDate.trim(),
                    endDate = endDate.trim(),
                    travelersCount = travelersCount.coerceAtLeast(1),
                    estimatedBudgetUsd = estimatedBudgetUsd.coerceAtLeast(0),
                    notes = notes.trim()
                )
            )
        }
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            globalRepository.deleteTrip(tripId)
        }
    }

    fun getItineraryFlow(tripId: Long): Flow<List<ItineraryItemEntity>> =
        globalRepository.getItineraryForTrip(tripId)

    suspend fun getItinerarySync(tripId: Long): List<ItineraryItemEntity> =
        globalRepository.getItineraryForTripSync(tripId)

    fun addItineraryItem(
        tripId: Long,
        dayNumber: Int,
        timeSlot: String,
        title: String,
        itemType: String,
        notes: String
    ) {
        viewModelScope.launch {
            globalRepository.addItineraryItem(
                ItineraryItemEntity(
                    tripId = tripId,
                    dayNumber = dayNumber.coerceAtLeast(1),
                    timeSlot = timeSlot.trim(),
                    title = title.trim(),
                    itemType = itemType,
                    notes = notes.trim()
                )
            )
        }
    }

    fun deleteItineraryItem(itemId: Long) {
        viewModelScope.launch {
            globalRepository.deleteItineraryItem(itemId)
        }
    }
}
