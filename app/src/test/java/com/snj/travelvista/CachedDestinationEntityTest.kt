package com.snj.travelvista

import com.snj.travelvista.data.local.entity.CachedDestinationEntity
import com.snj.travelvista.data.model.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CachedDestinationEntityTest {

    @Test
    fun testCachedDestinationRoundtrip() {
        val destination = Destination(
            id = 101L,
            title = "Amalfi Cliffs",
            country = "Italy",
            city = "Positano",
            category = "Beaches",
            rating = 4.85,
            reviewCount = 2100,
            imageUrl = "https://thumb.wikimedia.org/amalfi.jpg",
            description = "Stunning cliffside coastal village.",
            highlights = listOf("Cliff views", "Path of the Gods"),
            travelTips = listOf("Wear comfortable walking shoes"),
            bestTimeToVisit = "May to September",
            pricePerDay = 220,
            isFeatured = true,
            isPopular = true,
            isTrending = false,
            slug = "Amalfi_Coast",
            latitude = 40.6333,
            longitude = 14.6000,
            imageAuthor = "Jensens",
            imageLicense = "Public domain",
            imageCreditUrl = "https://commons.wikimedia.org/wiki/File:Pastena.jpg"
        )

        val entity = CachedDestinationEntity.fromDestination(destination, "Amalfi_Coast")

        assertEquals(101L, entity.id)
        assertEquals("Amalfi_Coast", entity.slug)
        assertEquals("Amalfi Cliffs", entity.title)
        assertEquals("Italy", entity.country)
        assertEquals("Beaches", entity.category)
        assertEquals("https://thumb.wikimedia.org/amalfi.jpg", entity.imageUrl)
        assertEquals(40.6333, entity.latitude ?: 0.0, 0.0001)
        assertEquals("Jensens", entity.imageAuthor)
        assertEquals("Public domain", entity.imageLicense)

        val restored = entity.toDestination()

        assertEquals(destination.id, restored.id)
        assertEquals(destination.title, restored.title)
        assertEquals(destination.country, restored.country)
        assertEquals(destination.city, restored.city)
        assertEquals(destination.category, restored.category)
        assertEquals(destination.highlights, restored.highlights)
        assertEquals(destination.travelTips, restored.travelTips)
        assertEquals(destination.slug, restored.slug)
        assertEquals(destination.latitude, restored.latitude)
        assertEquals(destination.longitude, restored.longitude)
        assertEquals(destination.imageAuthor, restored.imageAuthor)
        assertEquals(destination.imageLicense, restored.imageLicense)
        assertEquals(destination.imageCreditUrl, restored.imageCreditUrl)
        assertEquals(destination.formattedPrice, restored.formattedPrice)
        assertNotNull(restored.description)
    }
}
