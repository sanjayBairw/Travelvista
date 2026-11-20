package com.snj.travelvista.utils

import com.snj.travelvista.data.model.FoodSpecialty
import com.snj.travelvista.data.model.MostVisitedPlace
import com.snj.travelvista.data.remote.dto.WikivoyageSectionItemDto
import java.util.regex.Pattern

object WikivoyageSectionParser {

    private val HTML_TAG_PATTERN = Pattern.compile("<[^>]*>")
    private val IMG_SRC_PATTERN = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']")
    private val LI_PATTERN = Pattern.compile("<li[^>]*>(.*?)</li>", Pattern.DOTALL)
    private val P_PATTERN = Pattern.compile("<p[^>]*>(.*?)</p>", Pattern.DOTALL)
    private val BOLD_PATTERN = Pattern.compile("<(?:b|strong)[^>]*>(.*?)</(?:b|strong)>", Pattern.DOTALL)

    fun findSectionIndex(
        sections: List<WikivoyageSectionItemDto>?,
        vararg targetNames: String
    ): String? {
        if (sections.isNullOrEmpty()) return null
        for (target in targetNames) {
            val match = sections.find {
                it.line?.equals(target, ignoreCase = true) == true ||
                        it.anchor?.equals(target, ignoreCase = true) == true
            }
            if (match?.index != null) return match.index
        }
        // Partial match fallback (e.g. "Eat and drink" matches "Eat")
        for (target in targetNames) {
            val partial = sections.find {
                it.line?.contains(target, ignoreCase = true) == true
            }
            if (partial?.index != null) return partial.index
        }
        return null
    }

    fun cleanHtml(rawHtml: String?): String {
        if (rawHtml.isNullOrBlank()) return ""
        return HTML_TAG_PATTERN.matcher(rawHtml).replaceAll("")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("&#8212;", "—")
            .replace("&#8211;", "–")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun sanitizeImageUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        val trimmed = rawUrl.trim()
        return when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://") -> "https://${trimmed.removePrefix("http://")}"
            trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
    }

    fun extractImages(html: String): List<String> {
        val images = mutableListOf<String>()
        val matcher = IMG_SRC_PATTERN.matcher(html)
        while (matcher.find()) {
            val url = matcher.group(1)
            sanitizeImageUrl(url)?.let { images.add(it) }
        }
        return images
    }

    fun parseFoodFromHtml(
        html: String,
        destinationTitle: String
    ): Pair<String, List<FoodSpecialty>> {
        val images = extractImages(html)
        var imgIdx = 0

        // 1. Extract Overview from the first substantive <p>
        var overview = ""
        val pMatcher = P_PATTERN.matcher(html)
        while (pMatcher.find()) {
            val text = cleanHtml(pMatcher.group(1))
            if (text.length > 40 && !text.contains("Wikivoyage", ignoreCase = true)) {
                overview = text
                break
            }
        }
        if (overview.isBlank()) {
            overview = "Discover the vibrant local culinary traditions, regional delicacies, and staple ingredients of $destinationTitle."
        }

        // 2. Extract Dishes from <li> items or sentences containing bold dish names
        val foods = mutableListOf<FoodSpecialty>()
        val seenNames = mutableSetOf<String>()

        val liMatcher = LI_PATTERN.matcher(html)
        while (liMatcher.find() && foods.size < 6) {
            val rawLi = liMatcher.group(1)
            val cleaned = cleanHtml(rawLi)
            if (cleaned.length in 20..280) {
                val title = extractTitleFromItem(rawLi, cleaned)
                if (title.isNotBlank() && seenNames.add(title.lowercase())) {
                    val dishImg = if (imgIdx < images.size) images[imgIdx++] else null
                    foods.add(
                        FoodSpecialty(
                            name = title,
                            description = cleaned.removePrefix(title).removePrefix("—").removePrefix("-").trim().ifBlank {
                                "A celebrated culinary staple and favorite local specialty in $destinationTitle."
                            },
                            imageUrl = dishImg,
                            isMustTry = true
                        )
                    )
                }
            }
        }

        // 3. If no list items, parse bold tags inside paragraphs
        if (foods.isEmpty()) {
            val boldMatcher = BOLD_PATTERN.matcher(html)
            while (boldMatcher.find() && foods.size < 5) {
                val boldText = cleanHtml(boldMatcher.group(1)).trim()
                if (boldText.length in 3..35 && !boldText.equals("Eat", ignoreCase = true) && !boldText.equals("Contents", ignoreCase = true)) {
                    if (seenNames.add(boldText.lowercase())) {
                        val dishImg = if (imgIdx < images.size) images[imgIdx++] else null
                        foods.add(
                            FoodSpecialty(
                                name = boldText.replaceFirstChar { it.uppercase() },
                                description = "Iconic local delicacy highly recommended when traveling in $destinationTitle.",
                                imageUrl = dishImg,
                                isMustTry = true
                            )
                        )
                    }
                }
            }
        }

        return Pair(overview, foods)
    }

    fun parseSightsFromHtml(
        html: String,
        destinationTitle: String
    ): List<MostVisitedPlace> {
        val images = extractImages(html)
        var imgIdx = 0
        val sights = mutableListOf<MostVisitedPlace>()
        val seenNames = mutableSetOf<String>()

        val liMatcher = LI_PATTERN.matcher(html)
        while (liMatcher.find() && sights.size < 8) {
            val rawLi = liMatcher.group(1)
            val cleaned = cleanHtml(rawLi)
            if (cleaned.length in 25..320) {
                val name = extractTitleFromItem(rawLi, cleaned)
                if (name.isNotBlank() && seenNames.add(name.lowercase())) {
                    val sightImg = if (imgIdx < images.size) images[imgIdx++] else null
                    sights.add(
                        MostVisitedPlace(
                            name = name,
                            description = cleaned.removePrefix(name).removePrefix("—").removePrefix("-").trim().ifBlank {
                                "One of the most famous and highly visited attractions in $destinationTitle."
                            },
                            imageUrl = sightImg,
                            category = inferSightCategory(name, cleaned)
                        )
                    )
                }
            }
        }

        // Fallback to bold tags if list was empty
        if (sights.isEmpty()) {
            val boldMatcher = BOLD_PATTERN.matcher(html)
            while (boldMatcher.find() && sights.size < 6) {
                val boldText = cleanHtml(boldMatcher.group(1)).trim()
                if (boldText.length in 4..40 && !boldText.equals("See", ignoreCase = true) && !boldText.equals("Contents", ignoreCase = true)) {
                    if (seenNames.add(boldText.lowercase())) {
                        val sightImg = if (imgIdx < images.size) images[imgIdx++] else null
                        sights.add(
                            MostVisitedPlace(
                                name = boldText,
                                description = "Must-see landmark and iconic destination sight in $destinationTitle.",
                                imageUrl = sightImg,
                                category = inferSightCategory(boldText, "")
                            )
                        )
                    }
                }
            }
        }

        return sights
    }

    private fun extractTitleFromItem(rawHtml: String?, cleanText: String): String {
        if (!rawHtml.isNullOrBlank()) {
            val bMatcher = BOLD_PATTERN.matcher(rawHtml)
            if (bMatcher.find()) {
                val bold = cleanHtml(bMatcher.group(1)).trim()
                if (bold.length in 3..40) return bold
            }
        }
        val beforeDash = cleanText.substringBefore("—").substringBefore("-").substringBefore(":").trim()
        return if (beforeDash.length in 3..40) beforeDash else cleanText.take(30)
    }

    private fun inferSightCategory(title: String, desc: String): String {
        val combined = "$title $desc".lowercase()
        return when {
            listOf("beach", "bay", "cove", "coast", "shore").any { it in combined } -> "Scenic Beach"
            listOf("temple", "church", "cathedral", "shrine", "mosque", "monastery", "basilica").any { it in combined } -> "Sacred Landmark"
            listOf("fort", "castle", "palace", "ruins", "monument", "tower").any { it in combined } -> "Historic Monument"
            listOf("museum", "gallery", "exhibit").any { it in combined } -> "Museum & Culture"
            listOf("park", "garden", "wildlife", "sanctuary", "waterfall", "mountain", "caldera").any { it in combined } -> "Natural Wonder"
            else -> "Iconic Sights"
        }
    }
}
