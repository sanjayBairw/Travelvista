package com.snj.travelvista.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.data.repository.TravelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: TravelRepository
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Destination>>(emptyList())
    val favorites: StateFlow<List<Destination>> = _favorites.asStateFlow()

    init {
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getAllFavorites().collect { list ->
                _favorites.value = list
            }
        }
    }

    fun removeFavorite(destination: Destination) {
        viewModelScope.launch {
            repository.removeFavorite(destination.id)
        }
    }

    fun restoreFavorite(destination: Destination) {
        viewModelScope.launch {
            repository.addFavorite(destination)
        }
    }
}
