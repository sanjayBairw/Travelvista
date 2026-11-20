package com.snj.travelvista.ui.destinations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snj.travelvista.data.local.entity.SearchHistoryEntity
import com.snj.travelvista.data.model.Category
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.data.model.SortOption
import com.snj.travelvista.data.repository.GlobalTravelRepository
import com.snj.travelvista.data.repository.TravelRepository
import com.snj.travelvista.ui.common.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class DestinationsViewModel(
    private val repository: TravelRepository,
    private val globalRepository: GlobalTravelRepository? = null
) : ViewModel() {

    private val _destinations = MutableStateFlow<Resource<List<Destination>>>(Resource.Loading)
    val destinations: StateFlow<Resource<List<Destination>>> = _destinations.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<SearchHistoryEntity>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryEntity>> = _searchHistory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow("all")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _selectedRegion = MutableStateFlow("All Regions")
    val selectedRegion: StateFlow<String> = _selectedRegion.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.RATING)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var searchJob: Job? = null
    private var fetchJob: Job? = null

    init {
        loadCategories()
        observeSearchHistory()
        fetchDestinations()
    }

    private fun observeSearchHistory() {
        val globalRepo = globalRepository ?: return
        viewModelScope.launch {
            globalRepo.getRecentSearches()
                .catch { emit(emptyList()) }
                .collect { _searchHistory.value = it }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories()
                .catch { emit(Resource.Error(it.localizedMessage ?: "Failed to load categories")) }
                .collect { res ->
                    if (res is Resource.Success) {
                        val current = _selectedCategoryId.value
                        val updated = res.data.map {
                            it.copy(isSelected = it.id.equals(current, ignoreCase = true))
                        }
                        _categories.value = updated
                    }
                }
        }
    }

    fun fetchDestinations() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            val cat = if (_selectedCategoryId.value.equals("all", ignoreCase = true)) null else _selectedCategoryId.value
            val query = _searchQuery.value.trim().ifEmpty { null }
            val sort = _sortOption.value
            val region = _selectedRegion.value.takeIf { !it.equals("All Regions", ignoreCase = true) }

            repository.getDestinations(
                category = cat,
                searchQuery = query,
                sortOption = sort,
                regionFilter = region
            ).catch {
                emit(Resource.Error(it.localizedMessage ?: "Failed to load destinations"))
            }.collect {
                _destinations.value = it
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        if (_searchQuery.value == newQuery) return
        _searchQuery.value = newQuery
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(450) // Polite debounce for Nominatim & Wikivoyage global search
            val clean = newQuery.trim()
            if (clean.length >= 2) {
                globalRepository?.saveSearchQuery(clean)
            }
            fetchDestinations()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            globalRepository?.clearSearchHistory()
        }
    }

    fun selectCategory(category: Category) {
        if (_selectedCategoryId.value.equals(category.id, ignoreCase = true)) return
        _selectedCategoryId.value = category.id
        _categories.value = _categories.value.map {
            it.copy(isSelected = it.id.equals(category.id, ignoreCase = true))
        }
        fetchDestinations()
    }

    fun setRegionFilter(region: String) {
        if (_selectedRegion.value == region) return
        _selectedRegion.value = region
        fetchDestinations()
    }

    fun setSortOption(option: SortOption) {
        if (_sortOption.value == option) return
        _sortOption.value = option
        fetchDestinations()
    }

    fun toggleFavorite(destination: Destination) {
        viewModelScope.launch {
            repository.toggleFavorite(destination)
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        fetchDestinations()
        _isRefreshing.value = false
    }

    companion object {
        val REGIONS = listOf("All Regions", "Europe", "Asia", "Americas", "Africa", "Oceania")
    }
}
