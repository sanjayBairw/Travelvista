package com.snj.travelvista

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.data.remote.MockTravelInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockInterceptorTest {

    private lateinit var client: OkHttpClient
    private val gson = Gson()

    @Before
    fun setUp() {
        client = OkHttpClient.Builder()
            .addInterceptor(MockTravelInterceptor())
            .build()
    }

    @Test
    fun testGetDestinationsReturns200AndValidList() {
        val request = Request.Builder()
            .url("https://api.travelvista.internal/destinations")
            .build()

        val response = client.newCall(request).execute()
        assertEquals(200, response.code)

        val json = response.body?.string()
        assertNotNull(json)

        val listType = object : TypeToken<List<Destination>>() {}.type
        val destinations: List<Destination> = gson.fromJson(json, listType)
        assertTrue(destinations.isNotEmpty())
    }

    @Test
    fun testCategoryFilterMountains() {
        val request = Request.Builder()
            .url("https://api.travelvista.internal/destinations?category=Mountains")
            .build()

        val response = client.newCall(request).execute()
        assertEquals(200, response.code)

        val json = response.body?.string()
        val listType = object : TypeToken<List<Destination>>() {}.type
        val destinations: List<Destination> = gson.fromJson(json, listType)

        assertTrue(destinations.all { it.category.equals("Mountains", ignoreCase = true) })
    }

    @Test
    fun testSearchQuery() {
        val request = Request.Builder()
            .url("https://api.travelvista.internal/destinations?search=Santorini")
            .build()

        val response = client.newCall(request).execute()
        assertEquals(200, response.code)

        val json = response.body?.string()
        val listType = object : TypeToken<List<Destination>>() {}.type
        val destinations: List<Destination> = gson.fromJson(json, listType)

        assertTrue(destinations.isNotEmpty())
        assertTrue(destinations.first().title.contains("Santorini"))
    }

    @Test
    fun testGetDestinationById() {
        val request = Request.Builder()
            .url("https://api.travelvista.internal/destinations/1")
            .build()

        val response = client.newCall(request).execute()
        assertEquals(200, response.code)

        val json = response.body?.string()
        val destination: Destination = gson.fromJson(json, Destination::class.java)
        assertEquals(1L, destination.id)
    }

    @Test
    fun testGetDestinationByIdNotFound() {
        val request = Request.Builder()
            .url("https://api.travelvista.internal/destinations/99999")
            .build()

        val response = client.newCall(request).execute()
        assertEquals(404, response.code)
    }
}
