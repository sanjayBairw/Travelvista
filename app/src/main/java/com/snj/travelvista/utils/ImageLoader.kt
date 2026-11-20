package com.snj.travelvista.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.snj.travelvista.BuildConfig
import com.snj.travelvista.R

/**
 * Robust image loading engine for TravelVista.
 * Sanitizes Wikimedia Commons CDN links, upgrades low-res thumbnails to standard 500px,
 * filters out administrative map diagrams / flag SVGs in favor of scenic photography,
 * and attaches necessary user-agent headers per API policies.
 */
object ImageLoader {

    private val MAP_OR_DIAGRAM_KEYWORDS = listOf(
        "map-", "map_", "_map.", "_map_", "-map.", "locator_map", "location_map",
        "flag_of", "coat_of_arms", "arms_of", "emblem_of", "outline_map", "satellite_image", ".svg"
    )

    fun resolveValidImageUrl(
        url: String?,
        title: String? = null,
        category: String? = null
    ): String {
        val trimmed = url?.trim().orEmpty()
        val isMapOrDiagram = MAP_OR_DIAGRAM_KEYWORDS.any { trimmed.contains(it, ignoreCase = true) }

        if (trimmed.isNotBlank() && !isMapOrDiagram) {
            return sanitizeWikimediaUrl(trimmed)
        }

        // Curated high-resolution destination photography fallbacks
        val lowerTitle = title?.lowercase().orEmpty()
        val lowerCat = category?.lowercase().orEmpty()

        return when {
            lowerTitle.contains("north goa") ->
                "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e2/Chapora_Fort_Goa.jpg/500px-Chapora_Fort_Goa.jpg"
            lowerTitle.contains("south goa") ->
                "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Palolem_Beach_Goa.jpg/500px-Palolem_Beach_Goa.jpg"
            lowerTitle.contains("goa") ->
                "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f9/Basilica_of_Bom_Jesus_2012.jpg/500px-Basilica_of_Bom_Jesus_2012.jpg"
            lowerTitle.contains("delhi") || lowerTitle.contains("agra") || lowerTitle.contains("taj") ->
                "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=1200&q=80"
            lowerTitle.contains("jaipur") || lowerTitle.contains("rajasthan") ->
                "https://images.unsplash.com/photo-1599661046289-e31897846e41?auto=format&fit=crop&w=1200&q=80"
            lowerTitle.contains("paris") || lowerTitle.contains("france") ->
                "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=80"
            lowerTitle.contains("kyoto") || lowerTitle.contains("tokyo") || lowerTitle.contains("japan") ->
                "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&w=1200&q=80"
            lowerTitle.contains("bali") || lowerTitle.contains("indonesia") ->
                "https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1200&q=80"
            lowerCat.contains("beach") || lowerTitle.contains("beach") || lowerTitle.contains("island") ->
                "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80"
            lowerCat.contains("mountain") || lowerTitle.contains("mountain") || lowerTitle.contains("alps") ->
                "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=80"
            lowerCat.contains("historic") || lowerTitle.contains("temple") || lowerTitle.contains("fort") ->
                "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1200&q=80"
            lowerCat.contains("nature") || lowerTitle.contains("park") || lowerTitle.contains("lake") ->
                "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=1200&q=80"
            else ->
                "https://images.unsplash.com/photo-1477959858617-67f30bc75b82?auto=format&fit=crop&w=1200&q=80"
        }
    }

    fun sanitizeWikimediaUrl(rawUrl: String): String {
        var clean = if (rawUrl.startsWith("//")) "https:$rawUrl" else rawUrl
        // Remove tracking query strings (?utm_source=...)
        clean = clean.substringBefore("?")
        // Upgrade thumb host to upload host for reliable CDN retrieval
        clean = clean.replace("thumb.wikimedia.org", "upload.wikimedia.org")
        // Upgrade small thumbnails (e.g. 60px, 120px, 250px, 330px) to standard 500px
        clean = clean.replace(Regex("/\\d+px-"), "/500px-")
        return clean
    }

    fun loadImage(
        imageView: ImageView,
        url: String?,
        placeholderRes: Int,
        errorRes: Int = placeholderRes
    ) {
        loadImage(imageView, url, null, null, placeholderRes, errorRes)
    }

    fun loadImage(
        imageView: ImageView,
        url: String?,
        title: String? = null,
        category: String? = null,
        placeholderRes: Int = R.drawable.placeholder_destination,
        errorRes: Int = R.drawable.placeholder_destination
    ) {
        val resolvedUrl = resolveValidImageUrl(url, title, category)

        val model: Any = if (resolvedUrl.startsWith("http://") || resolvedUrl.startsWith("https://")) {
            GlideUrl(
                resolvedUrl,
                LazyHeaders.Builder()
                    .addHeader("User-Agent", BuildConfig.API_USER_AGENT)
                    .build()
            )
        } else {
            resolvedUrl
        }

        Glide.with(imageView.context)
            .load(model)
            .placeholder(placeholderRes)
            .error(errorRes)
            .transition(DrawableTransitionOptions.withCrossFade(250))
            .centerCrop()
            .into(imageView)
    }
}
