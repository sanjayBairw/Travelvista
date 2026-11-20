package com.snj.travelvista.data.remote

import com.snj.travelvista.data.model.Category
import com.snj.travelvista.data.model.Destination
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TravelApiService {

    @GET("destinations")
    suspend fun getDestinations(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null
    ): List<Destination>

    @GET("destinations/{id}")
    suspend fun getDestinationById(@Path("id") id: Long): Destination

    @GET("categories")
    suspend fun getCategories(): List<Category>
}
