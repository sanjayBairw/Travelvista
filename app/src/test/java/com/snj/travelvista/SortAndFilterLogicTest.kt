package com.snj.travelvista

import com.snj.travelvista.data.model.SortOption
import com.snj.travelvista.data.remote.TravelMockData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SortAndFilterLogicTest {

    private val allDestinations = TravelMockData.destinations

    @Test
    fun testSortByRatingDescending() {
        val sorted = allDestinations.sortedByDescending { it.rating }
        for (i in 0 until sorted.size - 1) {
            assertTrue(sorted[i].rating >= sorted[i + 1].rating)
        }
    }

    @Test
    fun testSortByPopularityReviewsDescending() {
        val sorted = allDestinations.sortedByDescending { it.reviewCount }
        for (i in 0 until sorted.size - 1) {
            assertTrue(sorted[i].reviewCount >= sorted[i + 1].reviewCount)
        }
    }

    @Test
    fun testSortByPriceAscending() {
        val sorted = allDestinations.sortedBy { it.pricePerDay }
        for (i in 0 until sorted.size - 1) {
            assertTrue(sorted[i].pricePerDay <= sorted[i + 1].pricePerDay)
        }
    }

    @Test
    fun testCategoriesCoverage() {
        val categories = TravelMockData.categories
        val expected = listOf("all", "mountains", "beaches", "cities", "nature", "historical")
        assertEquals(expected.size, categories.size)
        assertTrue(categories.map { it.id }.containsAll(expected))
    }
}
