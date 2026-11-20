package com.snj.travelvista.data.remote.service

import com.snj.travelvista.data.remote.dto.NominatimPlaceDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * OpenStreetMap Nominatim Geocoding Service.
 * Provides worldwide autocomplete & geocoding for any country, city, region, island, beach, mountain, or attraction.
 * Compliant with Nominatim Usage Policy via TravelVista User-Agent header and client-side search debouncing.
 */
interface GeocodingService {

    @GET("search")
    suspend fun searchWorldwide(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 8,
        @Query("accept-language") language: String = "en"
    ): List<NominatimPlaceDto>
}
