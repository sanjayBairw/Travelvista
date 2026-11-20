package com.snj.travelvista

import com.snj.travelvista.data.model.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationModelTest {

    private fun createSampleDestination(
        id: Long = 1L,
        title: String = "Santorini Caldera",
        city: String = "Santorini",
        country: String = "Greece",
        rating: Double = 4.88,
        reviewCount: Int = 1250,
        pricePerDay: Int = 180,
        isFavorite: Boolean = false
    ): Destination {
        return Destination(
            id = id,
            title = title,
            country = country,
            city = city,
            category = "Beaches",
            rating = rating,
            reviewCount = reviewCount,
            imageUrl = "https://example.com/santorini.jpg",
            description = "A wonderful Greek island.",
            highlights = listOf("Sunset", "Caldera", "Beaches"),
            travelTips = listOf("Book early", "Wear sunscreen"),
            bestTimeToVisit = "May to October",
            pricePerDay = pricePerDay,
            isFavorite = isFavorite
        )
    }

    @Test
    fun testLocationFormatting() {
        val dest = createSampleDestination(city = "Kyoto", country = "Japan")
        assertEquals("Kyoto, Japan", dest.location)
    }

    @Test
    fun testFormattedPrice() {
        val dest = createSampleDestination(pricePerDay = 250)
        assertEquals("$250 / day", dest.formattedPrice)
    }

    @Test
    fun testFormattedRating() {
        val dest = createSampleDestination(rating = 4.9)
        assertEquals("4.9", dest.formattedRating)
    }

    @Test
    fun testFormattedReviews() {
        val dest = createSampleDestination(reviewCount = 3500)
        assertEquals("(3500)", dest.formattedReviews)
    }

    @Test
    fun testFavoriteToggle() {
        val dest = createSampleDestination(isFavorite = false)
        assertFalse(dest.isFavorite)

        val updated = dest.copy(isFavorite = true)
        assertTrue(updated.isFavorite)
    }
}
