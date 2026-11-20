package com.snj.travelvista.data.model

data class FoodSpecialty(
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val isMustTry: Boolean = true
)

data class MostVisitedPlace(
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val category: String = "Must-Visit"
)

data class DestinationCulinaryAndSights(
    val destinationTitle: String,
    val cuisineOverview: String,
    val famousFoods: List<FoodSpecialty>,
    val mostVisitedPlaces: List<MostVisitedPlace>,
    val diningTips: String = "",
    val sourceAttribution: String = "Culinary & Sights via Wikivoyage & Global Travel Guide"
)
