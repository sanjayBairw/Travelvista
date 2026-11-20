package com.snj.travelvista

import com.google.gson.Gson
import com.snj.travelvista.data.remote.dto.WikivoyageSearchResponseDto
import com.snj.travelvista.data.remote.dto.WikivoyageSummaryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WikivoyageApiParsingTest {

    private val gson = Gson()

    @Test
    fun testParseKyotoSummaryResponse() {
        val json = """
            {
                "type": "standard",
                "title": "Kyoto",
                "displaytitle": "<span lang=\"en\" dir=\"ltr\"><span class=\"mw-page-title-main\">Kyoto</span></span>",
                "pageid": 18579,
                "description": "city in the Kyōto Prefecture, Japan",
                "thumbnail": {
                    "source": "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/80/Kiyomizu_Temple_in_Kyoto.jpg/330px-Kiyomizu_Temple_in_Kyoto.jpg",
                    "width": 330,
                    "height": 220
                },
                "originalimage": {
                    "source": "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/80/Kiyomizu_Temple_in_Kyoto.jpg/3840px-Kiyomizu_Temple_in_Kyoto.jpg",
                    "width": 6016,
                    "height": 4016
                },
                "lang": "en",
                "coordinates": {
                    "lat": 35.011667,
                    "lon": 135.768333
                },
                "extract": "Kyōto was the capital of Japan for over a millennium, and carries a reputation as the nation's most beautiful city and its cultural capital."
            }
        """.trimIndent()

        val dto = gson.fromJson(json, WikivoyageSummaryDto::class.java)

        assertEquals("Kyoto", dto.title)
        assertEquals(18579L, dto.pageId)
        assertEquals("city in the Kyōto Prefecture, Japan", dto.description)
        assertNotNull(dto.thumbnail)
        assertEquals("https://thumb.wikimedia.org/wikipedia/commons/thumb/8/80/Kiyomizu_Temple_in_Kyoto.jpg/330px-Kiyomizu_Temple_in_Kyoto.jpg", dto.thumbnail?.source)
        assertNotNull(dto.originalImage)
        assertEquals(6016, dto.originalImage?.width)
        assertNotNull(dto.coordinates)
        assertEquals(35.011667, dto.coordinates?.latitude ?: 0.0, 0.0001)
        assertEquals(135.768333, dto.coordinates?.longitude ?: 0.0, 0.0001)
        assertTrue(dto.extract?.contains("cultural capital") == true)
        assertEquals("https://thumb.wikimedia.org/wikipedia/commons/thumb/8/80/Kiyomizu_Temple_in_Kyoto.jpg/3840px-Kiyomizu_Temple_in_Kyoto.jpg", dto.bestImageUrl)
    }

    @Test
    fun testParseSantoriniSummaryResponse() {
        val json = """
            {
                "type": "standard",
                "title": "Santorini",
                "pageid": 31515,
                "description": "Greek island in the Cyclades",
                "thumbnail": {
                    "source": "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a9/Greece-Santorini.jpg/330px-Greece-Santorini.jpg",
                    "width": 330,
                    "height": 345
                },
                "coordinates": {
                    "lat": 36.4067,
                    "lon": 25.4333
                },
                "extract": "Santorini is an archipelago of 5 volcanic islands in the Cyclades group of the Greek islands in the southern Aegean Sea."
            }
        """.trimIndent()

        val dto = gson.fromJson(json, WikivoyageSummaryDto::class.java)

        assertEquals("Santorini", dto.title)
        assertEquals(31515L, dto.pageId)
        assertNull(dto.originalImage)
        assertNotNull(dto.thumbnail)
        // Best image falls back to thumbnail when originalimage is absent
        assertEquals("https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a9/Greece-Santorini.jpg/330px-Greece-Santorini.jpg", dto.bestImageUrl)
        assertEquals(36.4067, dto.coordinates?.latitude ?: 0.0, 0.0001)
    }

    @Test
    fun testParseNullSafetyMissingOptionalFields() {
        val minimalJson = """
            {
                "title": "Hidden Valley"
            }
        """.trimIndent()

        val dto = gson.fromJson(minimalJson, WikivoyageSummaryDto::class.java)

        assertEquals("Hidden Valley", dto.title)
        assertNull(dto.pageId)
        assertNull(dto.description)
        assertNull(dto.extract)
        assertNull(dto.thumbnail)
        assertNull(dto.originalImage)
        assertNull(dto.coordinates)
        assertNull(dto.bestImageUrl)
    }

    @Test
    fun testParseSearchResponse() {
        val json = """
            {
                "pages": [
                    {
                        "id": 18579,
                        "key": "Kyoto",
                        "title": "Kyoto",
                        "excerpt": "Historic ancient capital of Japan",
                        "description": "city in the Kyōto Prefecture",
                        "thumbnail": {
                            "url": "//thumb.wikimedia.org/photo.jpg",
                            "width": 60,
                            "height": 40
                        }
                    }
                ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, WikivoyageSearchResponseDto::class.java)
        assertEquals(1, response.pages.size)
        assertEquals("Kyoto", response.pages[0].title)
        assertEquals(18579L, response.pages[0].id)
        assertEquals("//thumb.wikimedia.org/photo.jpg", response.pages[0].thumbnail?.url)
    }
}
