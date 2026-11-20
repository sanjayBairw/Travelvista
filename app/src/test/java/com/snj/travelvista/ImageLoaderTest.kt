package com.snj.travelvista

import com.snj.travelvista.utils.ImageLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageLoaderTest {

    @Test
    fun testSanitizeWikimediaUrlStripsQueryParamsAndUpgradesThumbnail() {
        val raw = "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/db/Map-India-Goa01.png/60px-Map-India-Goa01.png?utm_source=en.wikivoyage.org&utm_campaign=rest&utm_content=thumbnail"
        val sanitized = ImageLoader.sanitizeWikimediaUrl(raw)

        assertFalse(sanitized.contains("?utm_source"))
        assertFalse(sanitized.contains("thumb.wikimedia.org"))
        assertTrue(sanitized.startsWith("https://upload.wikimedia.org/"))
        assertTrue(sanitized.contains("/500px-"))
    }

    @Test
    fun testResolveValidImageUrlReplacesMapDiagramWithScenicDestinationPhoto() {
        val mapUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/db/Map-India-Goa01.png/500px-Map-India-Goa01.png"
        val resolvedGoa = ImageLoader.resolveValidImageUrl(mapUrl, "Goa", "Beaches")
        val resolvedNorthGoa = ImageLoader.resolveValidImageUrl(mapUrl, "North Goa", "Cities")
        val resolvedSouthGoa = ImageLoader.resolveValidImageUrl(mapUrl, "South Goa", "Cities")

        assertFalse(resolvedGoa.contains("Map-India"))
        assertTrue(resolvedGoa.contains("Basilica_of_Bom_Jesus"))

        assertFalse(resolvedNorthGoa.contains("Map-India"))
        assertTrue(resolvedNorthGoa.contains("Chapora_Fort"))

        assertFalse(resolvedSouthGoa.contains("Map-India"))
        assertTrue(resolvedSouthGoa.contains("Palolem_Beach"))
    }

    @Test
    fun testResolveValidImageUrlProvidesCategoryFallbacksForBlankUrls() {
        val beach = ImageLoader.resolveValidImageUrl("", "Tropical Island", "Beaches")
        assertTrue(beach.isNotBlank())
        assertTrue(beach.startsWith("https://"))

        val mountain = ImageLoader.resolveValidImageUrl(null, "Alpine Peak", "Mountains")
        assertTrue(mountain.isNotBlank())
        assertTrue(mountain.startsWith("https://"))
    }
}
