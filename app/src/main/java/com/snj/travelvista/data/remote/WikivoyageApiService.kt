package com.snj.travelvista.data.remote

import com.snj.travelvista.data.remote.dto.WikivoyageGeosearchResponseDto
import com.snj.travelvista.data.remote.dto.WikivoyageParseTextResponseDto
import com.snj.travelvista.data.remote.dto.WikivoyageSearchResponseDto
import com.snj.travelvista.data.remote.dto.WikivoyageSectionItemDto
import com.snj.travelvista.data.remote.dto.WikivoyageSectionsResponseDto
import com.snj.travelvista.data.remote.dto.WikivoyageSummaryDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WikivoyageApiService {

    /**
     * Fetches real destination summary from Wikivoyage API.
     * Provides title, extract (real traveler guide description),
     * coordinates, and real photograph URLs hosted on Wikimedia Commons.
     */
    @GET("page/summary/{title}")
    suspend fun getPageSummary(
        @Path("title") title: String
    ): WikivoyageSummaryDto

    /**
     * Searches Wikivoyage articles by keyword using the Wikimedia core search endpoint.
     */
    @GET("https://en.wikivoyage.org/w/rest.php/v1/search/page")
    suspend fun searchDestinations(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10
    ): WikivoyageSearchResponseDto

    /**
     * Finds real nearby Wikivoyage destinations/attractions around a given latitude|longitude.
     */
    @GET("https://en.wikivoyage.org/w/api.php")
    suspend fun getNearbyPlaces(
        @Query("action") action: String = "query",
        @Query("list") list: String = "geosearch",
        @Query("gscoord") coordinates: String,
        @Query("gsradius") radiusMeters: Int = 10000,
        @Query("gslimit") limit: Int = 8,
        @Query("format") format: String = "json"
    ): WikivoyageGeosearchResponseDto

    /**
     * Retrieves table of contents / sections for a Wikivoyage destination article.
     * Used to locate "Eat" (local food & cuisine) and "See" (most visited places & top attractions).
     */
    @GET("https://en.wikivoyage.org/w/api.php")
    suspend fun getPageSections(
        @Query("page") page: String,
        @Query("action") action: String = "parse",
        @Query("prop") prop: String = "sections",
        @Query("format") format: String = "json"
    ): WikivoyageSectionsResponseDto

    /**
     * Retrieves parsed HTML text of a specific section (such as Eat or See).
     */
    @GET("https://en.wikivoyage.org/w/api.php")
    suspend fun getSectionHtml(
        @Query("page") page: String,
        @Query("section") sectionIndex: String,
        @Query("action") action: String = "parse",
        @Query("prop") prop: String = "text",
        @Query("format") format: String = "json"
    ): WikivoyageParseTextResponseDto
}
