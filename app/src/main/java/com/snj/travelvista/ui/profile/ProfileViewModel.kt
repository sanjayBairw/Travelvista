package com.snj.travelvista.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snj.travelvista.data.model.CurrencyConversionResult
import com.snj.travelvista.data.model.CurrencyRateInfo
import com.snj.travelvista.data.repository.GlobalTravelRepository
import com.snj.travelvista.ui.common.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val globalRepository: GlobalTravelRepository
) : ViewModel() {

    private val _currencyRate = MutableStateFlow<Resource<CurrencyRateInfo>?>(null)
    val currencyRate: StateFlow<Resource<CurrencyRateInfo>?> = _currencyRate.asStateFlow()

    private val _conversionResult = MutableStateFlow<Resource<CurrencyConversionResult>?>(null)
    val conversionResult: StateFlow<Resource<CurrencyConversionResult>?> = _conversionResult.asStateFlow()

    fun refreshExchangeRate(currencyCode: String) {
        viewModelScope.launch {
            _currencyRate.value = Resource.Loading
            _currencyRate.value = globalRepository.fetchUsdExchangeRate(currencyCode)
        }
    }

    fun calculateConversion(amount: Double, from: String, to: String) {
        viewModelScope.launch {
            _conversionResult.value = Resource.Loading
            _conversionResult.value = globalRepository.convertCurrency(amount, from, to)
        }
    }
}
