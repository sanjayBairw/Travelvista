package com.snj.travelvista

import com.snj.travelvista.data.local.entity.DestinationEntity
import com.snj.travelvista.data.model.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationEntityTest {

    @Test
    fun testDestinationToEntityAndBack() {
        val original = Destination(
            id = 42L,
            title = "Matterhorn Summit",
            country = "Switzerland",
            city = "Zermatt",
            category = "Mountains",
            rating = 4.9,
            reviewCount = 2800,
            imageUrl = "https://example.com/matterhorn.jpg",
            description = "Iconic alpine peak.",
            highlights = listOf("Alpine Views", "Gornergrat", "Skiing"),
            travelTips = listOf("Bring warm clothes", "Acclimatize"),
            bestTimeToVisit = "July to September",
            pricePerDay = 240,
            isFeatured = true,
            isPopular = true,
            isTrending = true,
            isFavorite = true
        )

        val entity = DestinationEntity.fromDestination(original)
        assertEquals(42L, entity.id)
        assertEquals("Matterhorn Summit", entity.title)
        assertEquals("Switzerland", entity.country)
        assertEquals("Mountains", entity.category)

        val restored = entity.toDestination()
        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.highlights, restored.highlights)
        assertEquals(original.travelTips, restored.travelTips)
        assertEquals(original.pricePerDay, restored.pricePerDay)
        assertTrue(restored.isFavorite)
    }
}
