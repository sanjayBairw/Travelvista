package com.snj.travelvista.data.remote.service

import com.snj.travelvista.data.remote.dto.RestCountryDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * REST Countries v3.1 API Service.
 * Returns real-world local currency, languages, time zones, calling codes, region, and capital city.
 */
interface CountryInfoService {

    @GET("name/{name}")
    suspend fun getCountryByName(
        @Path("name") countryName: String,
        @Query("fields") fields: String = "name,cca2,region,subregion,capital,languages,currencies,timezones,idd,flags"
    ): List<RestCountryDto>

    @GET("alpha/{code}")
    suspend fun getCountryByCode(
        @Path("code") isoCode: String,
        @Query("fields") fields: String = "name,cca2,region,subregion,capital,languages,currencies,timezones,idd,flags"
    ): RestCountryDto
}
