package com.snj.travelvista.data.remote.service

import com.snj.travelvista.data.remote.dto.FrankfurterRatesDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Frankfurter Exchange Rate API Service.
 * Verified European Central Bank daily reference rates for multi-currency conversion.
 */
interface CurrencyService {

    @GET("latest")
    suspend fun getLatestRates(
        @Query("from") baseCurrency: String = "USD"
    ): FrankfurterRatesDto
}
