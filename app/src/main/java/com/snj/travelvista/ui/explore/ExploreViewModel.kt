package com.snj.travelvista.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snj.travelvista.data.local.entity.RecentDestinationEntity
import com.snj.travelvista.data.model.Category
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.data.repository.GlobalTravelRepository
import com.snj.travelvista.data.repository.TravelRepository
import com.snj.travelvista.ui.common.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val repository: TravelRepository,
    private val globalRepository: GlobalTravelRepository? = null
) : ViewModel() {

    private val _featured = MutableStateFlow<Resource<List<Destination>>>(Resource.Loading)
    val featured: StateFlow<Resource<List<Destination>>> = _featured.asStateFlow()

    private val _popular = MutableStateFlow<Resource<List<Destination>>>(Resource.Loading)
    val popular: StateFlow<Resource<List<Destination>>> = _popular.asStateFlow()

    private val _trending = MutableStateFlow<Resource<List<Destination>>>(Resource.Loading)
    val trending: StateFlow<Resource<List<Destination>>> = _trending.asStateFlow()

    private val _recentDestinations = MutableStateFlow<List<RecentDestinationEntity>>(emptyList())
    val recentDestinations: StateFlow<List<RecentDestinationEntity>> = _recentDestinations.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String>("all")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadData()
        observeRecentDestinations()
    }

    private fun observeRecentDestinations() {
        val globalRepo = globalRepository ?: return
        viewModelScope.launch {
            globalRepo.getRecentDestinations()
                .catch { emit(emptyList()) }
                .collect { _recentDestinations.value = it }
        }
    }

    fun loadData() {
        loadCategories()
        loadFeatured()
        loadPopular()
        loadTrending()
    }

    fun refresh() {
        _isRefreshing.value = true
        loadData()
        _isRefreshing.value = false
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories()
                .catch { emit(Resource.Error(it.localizedMessage ?: "Failed to load categories")) }
                .collect { res ->
                    if (res is Resource.Success) {
                        val currentSelected = _selectedCategoryId.value
                        val updated = res.data.map {
                            it.copy(isSelected = it.id.equals(currentSelected, ignoreCase = true))
                        }
                        _categories.value = updated
                    }
                }
        }
    }

    private fun loadFeatured() {
        viewModelScope.launch {
            repository.getFeaturedDestinations()
                .catch { emit(Resource.Error(it.localizedMessage ?: "Failed to load featured")) }
                .collect { _featured.value = it }
        }
    }

    private fun loadPopular() {
        viewModelScope.launch {
            repository.getPopularDestinations()
                .catch { emit(Resource.Error(it.localizedMessage ?: "Failed to load popular")) }
                .collect { _popular.value = it }
        }
    }

    private fun loadTrending() {
        viewModelScope.launch {
            repository.getTrendingDestinations()
                .catch { emit(Resource.Error(it.localizedMessage ?: "Failed to load trending")) }
                .collect { _trending.value = it }
        }
    }

    fun selectCategory(category: Category) {
        _selectedCategoryId.value = category.id
        _categories.value = _categories.value.map {
            it.copy(isSelected = it.id.equals(category.id, ignoreCase = true))
        }
    }

    fun toggleFavorite(destination: Destination) {
        viewModelScope.launch {
            repository.toggleFavorite(destination)
        }
    }
}
