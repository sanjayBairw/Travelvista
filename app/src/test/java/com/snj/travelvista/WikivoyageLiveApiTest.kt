package com.snj.travelvista

import com.snj.travelvista.data.remote.RetrofitClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WikivoyageLiveApiTest {

    @Test
    fun testLiveWikivoyageApiCallForKyoto() = runBlocking {
        // Direct live call to the production Wikivoyage REST API
        val service = RetrofitClient.wikivoyageApiService
        val response = service.getPageSummary("Kyoto")

        assertNotNull(response)
        assertEquals("Kyoto", response.title)
        assertNotNull(response.pageId)
        assertNotNull(response.extract)
        assertTrue(response.extract?.isNotBlank() == true)
        assertNotNull(response.bestImageUrl)
        assertTrue(response.bestImageUrl?.startsWith("https://") == true)
        println("LIVE API SUCCESS: Title=${response.title}, Image=${response.bestImageUrl}, PageID=${response.pageId}")
    }

    @Test
    fun testLiveWikivoyageApiCallForSantorini() = runBlocking {
        val service = RetrofitClient.wikivoyageApiService
        val response = service.getPageSummary("Santorini")

        assertNotNull(response)
        assertEquals("Santorini", response.title)
        assertNotNull(response.bestImageUrl)
        assertTrue(response.bestImageUrl?.contains("wikimedia.org") == true)
        println("LIVE API SUCCESS: Title=${response.title}, Image=${response.bestImageUrl}")
    }
}
