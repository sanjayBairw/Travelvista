package com.snj.travelvista.ui.details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.snj.travelvista.R
import com.snj.travelvista.TravelApplication
import com.snj.travelvista.data.model.CountryTravelInfo
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.data.model.DestinationCulinaryAndSights
import com.snj.travelvista.data.model.DestinationWeather
import com.snj.travelvista.data.model.FoodSpecialty
import com.snj.travelvista.data.model.MostVisitedPlace
import com.snj.travelvista.data.model.NearbyAttraction
import com.snj.travelvista.data.remote.provider.ActivitySearchQuery
import com.snj.travelvista.data.remote.provider.ExternalProviderLink
import com.snj.travelvista.data.remote.provider.FlightSearchQuery
import com.snj.travelvista.data.remote.provider.HotelSearchQuery
import com.snj.travelvista.data.remote.provider.ProviderBookingLinkBuilder
import com.snj.travelvista.databinding.FragmentDestinationDetailBinding
import com.snj.travelvista.ui.common.Resource
import com.snj.travelvista.ui.common.ViewModelFactory
import com.snj.travelvista.utils.GeoUtils
import com.snj.travelvista.utils.ImageLoader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DestinationDetailFragment : Fragment() {

    private var _binding: FragmentDestinationDetailBinding? = null
    private val binding get() = _binding!!

    private val app: TravelApplication
        get() = requireActivity().application as TravelApplication

    private val viewModel: DetailViewModel by viewModels {
        ViewModelFactory(app.repository, app.globalRepository)
    }

    private var destinationId: Long = -1L
    private var destinationSlug: String = ""
    private var destinationTitle: String = ""
    private var currentDestination: Destination? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        destinationId = arguments?.getLong("destinationId", -1L) ?: -1L
        if (destinationId == -1L && arguments?.containsKey("destinationId") == true) {
            destinationId = arguments?.getLong("destinationId") ?: -1L
        }
        destinationSlug = arguments?.getString("destinationSlug").orEmpty()
        destinationTitle = arguments?.getString("destinationTitle").orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()

        if (destinationId != -1L || destinationSlug.isNotBlank() || destinationTitle.isNotBlank()) {
            viewModel.loadDestination(destinationId, destinationSlug, destinationTitle)
        } else {
            // Default fallback destination so screen is never blank if invoked without bundle
            viewModel.loadDestination(1L, "Kyoto", "Kyoto Zen Temples & Bamboo")
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRetryDetail.setOnClickListener {
            viewModel.loadDestination(destinationId, destinationSlug, destinationTitle)
        }

        binding.btnShare.setOnClickListener {
            shareDestination()
        }

        binding.fabFavorite.setOnClickListener {
            viewModel.toggleFavorite()
        }

        binding.btnPlanTrip.setOnClickListener {
            currentDestination?.let { destination ->
                showPlanTripBottomSheet(destination)
            }
        }

        binding.btnOpenMap.setOnClickListener {
            currentDestination?.let { dest ->
                val lat = dest.latitude
                val lon = dest.longitude
                if (lat != null && lon != null) {
                    val bundle = bundleOf(
                        "focusLat" to lat,
                        "focusLon" to lon,
                        "focusTitle" to dest.title
                    )
                    findNavController().navigate(R.id.action_detail_to_map, bundle)
                }
            }
        }

        binding.btnOpenDirections.setOnClickListener {
            currentDestination?.let { dest ->
                val lat = dest.latitude
                val lon = dest.longitude
                if (lat != null && lon != null) {
                    val url = ProviderBookingLinkBuilder.buildDirectionsUrl(lat, lon, dest.title)
                    openExternalUrl(url)
                }
            }
        }

        binding.btnBookHotels.setOnClickListener {
            currentDestination?.let { dest ->
                val links = ProviderBookingLinkBuilder.buildProviderSearchLinks(
                    HotelSearchQuery(destination = dest.title, checkInDate = "", checkOutDate = "")
                )
                showProviderChooserDialog("Search Hotels in ${dest.title}", links)
            }
        }

        binding.btnBookFlights.setOnClickListener {
            currentDestination?.let { dest ->
                val homeAirport = app.userPreferences.homeAirport.ifBlank { app.userPreferences.homeCity }
                val links = ProviderBookingLinkBuilder.buildProviderSearchLinks(
                    FlightSearchQuery(origin = homeAirport, destination = dest.title, departureDate = "")
                )
                showProviderChooserDialog("Search Flights to ${dest.title}", links)
            }
        }

        binding.btnBookActivities.setOnClickListener {
            currentDestination?.let { dest ->
                val links = ProviderBookingLinkBuilder.buildProviderSearchLinks(
                    ActivitySearchQuery(destination = dest.title)
                )
                showProviderChooserDialog("Find Tours & Activities in ${dest.title}", links)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Destination details
                launch {
                    viewModel.destination.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> {
                                if (currentDestination == null) {
                                    binding.progressDetail.visibility = View.VISIBLE
                                    binding.layoutErrorDetail.visibility = View.GONE
                                    binding.nestedScrollDetail.visibility = View.INVISIBLE
                                }
                            }
                            is Resource.Success -> {
                                binding.progressDetail.visibility = View.GONE
                                binding.layoutErrorDetail.visibility = View.GONE
                                binding.nestedScrollDetail.visibility = View.VISIBLE
                                val dest = resource.data
                                currentDestination = dest
                                populateDetails(dest)
                            }
                            is Resource.Error -> {
                                binding.progressDetail.visibility = View.GONE
                                if (currentDestination == null) {
                                    binding.layoutErrorDetail.visibility = View.VISIBLE
                                    binding.nestedScrollDetail.visibility = View.GONE
                                    binding.tvErrorMessage.text = resource.message
                                }
                                Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG)
                                    .setAction("Retry") {
                                        viewModel.loadDestination(destinationId, destinationSlug, destinationTitle)
                                    }
                                    .show()
                            }
                        }
                    }
                }

                // Favorite state
                launch {
                    viewModel.isFavorite.collect { isFav ->
                        updateFavoriteFab(isFav)
                    }
                }

                // Live Weather (Open-Meteo)
                launch {
                    viewModel.weather.collect { weatherRes ->
                        when (weatherRes) {
                            is Resource.Success -> bindWeather(weatherRes.data)
                            else -> Unit
                        }
                    }
                }

                // Live Country Info (REST Countries)
                launch {
                    viewModel.countryInfo.collect { countryRes ->
                        when (countryRes) {
                            is Resource.Success -> bindCountryInfo(countryRes.data)
                            else -> Unit
                        }
                    }
                }

                // Nearby Attractions (Wikivoyage Geosearch)
                launch {
                    viewModel.nearbyAttractions.collect { nearby ->
                        bindNearbyAttractions(nearby)
                    }
                }

                // Live Culinary Specialties & Most Visited Places
                launch {
                    viewModel.culinaryAndSights.collect { culinaryData ->
                        if (culinaryData != null) {
                            bindCulinaryAndSights(culinaryData)
                        }
                    }
                }
            }
        }
    }

    private fun populateDetails(destination: Destination) {
        binding.tvDetailTitle.text = destination.title
        binding.tvDetailLocation.text = destination.location
        binding.tvDetailCategory.text = destination.category
        binding.tvDetailRating.text = "${destination.formattedRating} ${destination.formattedReviews}".trim()

        val prefs = app.userPreferences
        val dailyPrice = if (destination.pricePerDay > 0) destination.pricePerDay else 135
        binding.tvDetailPrice.text = GeoUtils.formatConvertedBudget(
            dailyPrice,
            prefs.preferredCurrency,
            prefs.cachedUsdToPreferredRate
        )

        if (destination.latitude != null && destination.longitude != null) {
            binding.layoutMapActions.visibility = View.VISIBLE
            binding.tvCoordinates.visibility = View.VISIBLE
            binding.tvCoordinates.text = String.format(
                Locale.US,
                "Coordinates: %.4f°, %.4f°",
                destination.latitude,
                destination.longitude
            )
        } else {
            binding.layoutMapActions.visibility = View.GONE
            binding.tvCoordinates.visibility = View.GONE
        }

        binding.tvDataFreshness.text =
            "Sources: Wikivoyage · Wikimedia Commons · OpenStreetMap · Open-Meteo · REST Countries\n${GeoUtils.formatFreshness(System.currentTimeMillis())}"
        binding.tvDetailDescription.text = destination.description.ifBlank {
            "Explore ${destination.title}, a premier travel destination with rich cultural heritage, scenic landmarks, and memorable experiences."
        }
        binding.tvDetailBestTime.text = destination.bestTimeToVisit.ifBlank {
            "Spring and autumn generally offer pleasant weather and optimal conditions for sightseeing."
        }

        ImageLoader.loadImage(binding.ivDetailImage, destination.imageUrl, destination.title, destination.category)

        // Bind Image Attribution & License Information
        binding.tvImageAttribution.text = destination.attributionSummary
        binding.layoutImageAttribution.setOnClickListener {
            showImageAttributionDialog(destination)
        }

        // Populate Highlights as Premium Cards
        binding.layoutHighlightsContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val highlightsList = destination.highlights.ifEmpty {
            listOf(
                "Discover iconic landmarks and historic points of interest in ${destination.title}",
                "Immerse yourself in authentic local traditions and culture",
                "Explore panoramic scenic viewpoints and regional attractions",
                "Sample delicious regional cuisine and local specialties"
            )
        }
        for (highlight in highlightsList) {
            val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                radius = 14f * density
                cardElevation = 0f
                strokeWidth = (1 * density).toInt()
                setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.outline))
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_variant))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, (8 * density).toInt())
                }
            }

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val p = (12 * density).toInt()
                setPadding(p, p, p, p)
            }

            val icon = ImageView(requireContext()).apply {
                val size = (20 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(0, 0, (12 * density).toInt(), 0)
                }
                setImageResource(R.drawable.ic_flag)
                setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
            }

            val text = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                this.text = highlight
                textSize = 14f
                setLineSpacing(3f, 1f)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))
            }

            row.addView(icon)
            row.addView(text)
            card.addView(row)
            binding.layoutHighlightsContainer.addView(card)
        }

        // Populate Travel Tips as Premium Cards
        binding.layoutTipsContainer.removeAllViews()
        val tipsList = destination.travelTips.ifEmpty {
            listOf(
                "Plan and reserve popular attractions or transit tickets ahead of time.",
                "Wear comfortable footwear suitable for walking tours and excursions.",
                "Keep local currency or reliable payment cards accessible.",
                "Respect local customs and check weather forecasts before daily outings."
            )
        }
        for (tip in tipsList) {
            val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                radius = 14f * density
                cardElevation = 0f
                strokeWidth = (1 * density).toInt()
                setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.outline))
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_variant))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, (8 * density).toInt())
                }
            }

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val p = (12 * density).toInt()
                setPadding(p, p, p, p)
            }

            val icon = ImageView(requireContext()).apply {
                val size = (20 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(0, 0, (12 * density).toInt(), 0)
                }
                setImageResource(R.drawable.ic_lightbulb)
                setColorFilter(ContextCompat.getColor(requireContext(), R.color.secondary))
            }

            val text = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                this.text = tip
                textSize = 14f
                setLineSpacing(3f, 1f)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))
            }

            row.addView(icon)
            row.addView(text)
            card.addView(row)
            binding.layoutTipsContainer.addView(card)
        }
    }

    private fun updateFavoriteFab(isFavorite: Boolean) {
        if (isFavorite) {
            binding.fabFavorite.setImageResource(R.drawable.ic_favorite_filled)
            binding.fabFavorite.setColorFilter(ContextCompat.getColor(requireContext(), R.color.heart_red))
        } else {
            binding.fabFavorite.setImageResource(R.drawable.ic_favorite_border)
            binding.fabFavorite.setColorFilter(ContextCompat.getColor(requireContext(), R.color.on_surface))
        }
    }

    private fun shareDestination() {
        val dest = currentDestination ?: return
        val shareBody = getString(
            R.string.share_text,
            dest.title,
            dest.country,
            dest.description
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Travel to ${dest.title}")
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_title)))
    }

    private fun showPlanTripBottomSheet(destination: Destination) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_plan_trip, null)
        dialog.setContentView(sheetView)

        val tvTitle = sheetView.findViewById<TextView>(R.id.tv_sheet_title)
        val tvSubtitle = sheetView.findViewById<TextView>(R.id.tv_sheet_subtitle)
        val tvTravelers = sheetView.findViewById<TextView>(R.id.tv_travelers_count)
        val tvDays = sheetView.findViewById<TextView>(R.id.tv_days_count)
        val tvTotalCost = sheetView.findViewById<TextView>(R.id.tv_total_cost)
        val btnMinusTravelers = sheetView.findViewById<MaterialButton>(R.id.btn_minus_travelers)
        val btnPlusTravelers = sheetView.findViewById<MaterialButton>(R.id.btn_plus_travelers)
        val btnMinusDays = sheetView.findViewById<MaterialButton>(R.id.btn_minus_days)
        val btnPlusDays = sheetView.findViewById<MaterialButton>(R.id.btn_plus_days)
        val btnConfirm = sheetView.findViewById<MaterialButton>(R.id.btn_confirm_plan)

        val baseDailyPrice = if (destination.pricePerDay > 0) destination.pricePerDay else 135
        tvTitle.text = "Plan Your Trip to ${destination.title}"
        tvSubtitle.text = "Daily estimate: $${baseDailyPrice} per traveler"

        var travelers = 2
        var days = 5

        fun updateTotal() {
            tvTravelers.text = travelers.toString()
            tvDays.text = days.toString()
            val total = travelers * days * baseDailyPrice
            tvTotalCost.text = "$%,d".format(total)
        }

        updateTotal()

        btnMinusTravelers.setOnClickListener {
            if (travelers > 1) {
                travelers--
                updateTotal()
            }
        }

        btnPlusTravelers.setOnClickListener {
            if (travelers < 12) {
                travelers++
                updateTotal()
            }
        }

        btnMinusDays.setOnClickListener {
            if (days > 1) {
                days--
                updateTotal()
            }
        }

        btnPlusDays.setOnClickListener {
            if (days < 30) {
                days++
                updateTotal()
            }
        }

        btnConfirm.setOnClickListener {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()
            val startStr = fmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, days)
            val endStr = fmt.format(cal.time)

            viewModel.saveTripToPlanner(
                destination = destination,
                tripName = "${destination.title} Trip",
                startDate = startStr,
                endDate = endStr,
                travelersCount = travelers,
                days = days,
                notes = "Saved from ${destination.title} destination guide."
            ) {
                dialog.dismiss()
                val msg = getString(R.string.trip_booked, destination.title)
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
                    .setAction("View Trips") {
                        findNavController().navigate(R.id.action_detail_to_trips)
                    }
                    .show()
            }
        }

        dialog.show()
    }

    private fun bindWeather(weather: DestinationWeather) {
        val useMetric = app.userPreferences.useMetricUnits
        binding.cardLiveWeather.visibility = View.VISIBLE
        binding.tvWeatherTemp.text = GeoUtils.formatTemperature(weather.temperatureCelsius, useMetric)

        val hiLo = if (weather.dailyMaxCelsius != null && weather.dailyMinCelsius != null) {
            " · High ${GeoUtils.formatTemperature(weather.dailyMaxCelsius, useMetric)} / Low ${GeoUtils.formatTemperature(weather.dailyMinCelsius, useMetric)}"
        } else ""
        binding.tvWeatherCondition.text = "${weather.conditionText}$hiLo"

        val humidityPart = weather.humidityPercent?.let { " · Humidity: $it%" } ?: ""
        binding.tvWeatherDetails.text =
            "Wind: ${GeoUtils.formatWindSpeed(weather.windSpeedKmh, useMetric)}$humidityPart · TZ: ${weather.timezone}"
        binding.tvWeatherAttribution.text =
            "${weather.sourceAttribution} · ${GeoUtils.formatFreshness(weather.fetchedAtMillis)}"
    }

    private fun bindCountryInfo(info: CountryTravelInfo) {
        binding.cardCountryInfo.visibility = View.VISIBLE
        binding.tvCountryCurrency.text = "Local Currency: ${info.currencyDisplay}"
        binding.tvCountryLanguages.text = "Languages: ${info.languages}"
        binding.tvCountryTimezone.text = "Time Zone: ${info.timezone} · Dialing: ${info.dialingCode}"
        binding.tvCountryRegion.text = "Region: ${info.region} · Capital: ${info.capital}"
        binding.tvCountryAttribution.text =
            "${info.sourceAttribution} · ${GeoUtils.formatFreshness(info.fetchedAtMillis)}"
    }

    private fun bindNearbyAttractions(nearby: List<NearbyAttraction>) {
        binding.layoutNearbyContainer.removeAllViews()
        if (nearby.isEmpty()) {
            binding.layoutNearbySection.visibility = View.GONE
            return
        }
        binding.layoutNearbySection.visibility = View.VISIBLE
        val density = resources.displayMetrics.density
        val useMetric = app.userPreferences.useMetricUnits

        nearby.take(6).forEach { place ->
            val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                radius = 14f * density
                cardElevation = 0f
                strokeWidth = (1 * density).toInt()
                setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.outline))
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_variant))
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, (8 * density).toInt())
                }
                setOnClickListener {
                    viewModel.resolveNearbyPlace(place) { newDestId ->
                        findNavController().navigate(
                            R.id.action_detail_to_detail,
                            bundleOf("destinationId" to newDestId)
                        )
                    }
                }
            }

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val p = (12 * density).toInt()
                setPadding(p, p, p, p)
            }

            val titleTv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = place.title
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))
            }

            val distTv = TextView(requireContext()).apply {
                text = GeoUtils.formatDistance(place.distanceMeters, useMetric)
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            }

            row.addView(titleTv)
            row.addView(distTv)
            card.addView(row)
            binding.layoutNearbyContainer.addView(card)
        }
    }

    private fun bindCulinaryAndSights(data: DestinationCulinaryAndSights) {
        val density = resources.displayMetrics.density

        // --- BIND CUISINE OVERVIEW & DINING TIPS ---
        binding.tvCuisineOverview.text = data.cuisineOverview
        if (data.diningTips.isNotBlank()) {
            binding.cardDiningTips.visibility = View.VISIBLE
            binding.tvDiningTips.text = data.diningTips
        } else {
            binding.cardDiningTips.visibility = View.GONE
        }

        // --- POPULATE FAMOUS FOOD SPECIALTIES ---
        binding.layoutFoodContainer.removeAllViews()
        data.famousFoods.forEach { food ->
            val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                radius = 16f * density
                cardElevation = 0f
                strokeWidth = (1 * density).toInt()
                setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.outline))
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_variant))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, (10 * density).toInt())
                }
            }

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val p = (12 * density).toInt()
                setPadding(p, p, p, p)
            }

            val imgCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                radius = 12f * density
                cardElevation = 0f
                strokeWidth = 0
                val imgSize = (72 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(imgSize, imgSize).apply {
                    setMargins(0, 0, (12 * density).toInt(), 0)
                }
            }

            val ivFood = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = food.name
                if (!food.imageUrl.isNullOrBlank()) {
                    ImageLoader.loadImage(this, food.imageUrl, R.drawable.ic_restaurant, R.drawable.ic_restaurant)
                } else {
                    setImageResource(R.drawable.ic_restaurant)
                    val pad = (18 * density).toInt()
                    setPadding(pad, pad, pad, pad)
                    setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
                }
            }
            imgCard.addView(ivFood)

            val contentCol = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val titleTv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = food.name
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))
            }
            titleRow.addView(titleTv)

            if (food.isMustTry) {
                val badge = TextView(requireContext()).apply {
                    text = getString(R.string.badge_must_try)
                    textSize = 10f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setBackgroundResource(R.drawable.bg_tag_pill)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    val hPad = (8 * density).toInt()
                    val vPad = (3 * density).toInt()
                    setPadding(hPad, vPad, hPad, vPad)
                }
                titleRow.addView(badge)
            }
            contentCol.addView(titleRow)

            val descTv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, (4 * density).toInt(), 0, 0)
                }
                text = food.description
                textSize = 12.5f
                setLineSpacing(2f, 1f)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
            }
            contentCol.addView(descTv)

            row.addView(imgCard)
            row.addView(contentCol)
            card.addView(row)
            binding.layoutFoodContainer.addView(card)
        }

        // --- POPULATE MOST VISITED PLACES & TOP SIGHTS ---
        binding.layoutSightsContainer.removeAllViews()
        data.mostVisitedPlaces.forEach { sight ->
            val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                radius = 16f * density
                cardElevation = 0f
                strokeWidth = (1 * density).toInt()
                setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.outline))
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_variant))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, (10 * density).toInt())
                }
            }

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val p = (12 * density).toInt()
                setPadding(p, p, p, p)
            }

            val imgCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                radius = 12f * density
                cardElevation = 0f
                strokeWidth = 0
                val imgSize = (72 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(imgSize, imgSize).apply {
                    setMargins(0, 0, (12 * density).toInt(), 0)
                }
            }

            val ivSight = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = sight.name
                if (!sight.imageUrl.isNullOrBlank()) {
                    ImageLoader.loadImage(this, sight.imageUrl, R.drawable.ic_landmark, R.drawable.ic_landmark)
                } else {
                    setImageResource(R.drawable.ic_landmark)
                    val pad = (18 * density).toInt()
                    setPadding(pad, pad, pad, pad)
                    setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
                }
            }
            imgCard.addView(ivSight)

            val contentCol = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val nameTv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = sight.name
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))
            }
            titleRow.addView(nameTv)

            val catBadge = TextView(requireContext()).apply {
                text = sight.category
                textSize = 10f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setBackgroundResource(R.drawable.bg_tag_pill)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                val hPad = (8 * density).toInt()
                val vPad = (3 * density).toInt()
                setPadding(hPad, vPad, hPad, vPad)
            }
            titleRow.addView(catBadge)
            contentCol.addView(titleRow)

            val descTv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, (4 * density).toInt(), 0, (6 * density).toInt())
                }
                text = sight.description
                textSize = 12.5f
                setLineSpacing(2f, 1f)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
            }
            contentCol.addView(descTv)

            val mapActionTv = TextView(requireContext()).apply {
                text = "📍 Search on Map"
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                setOnClickListener {
                    val mapQuery = "${sight.name}, ${currentDestination?.title ?: ""}"
                    val url = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(mapQuery)
                    openExternalUrl(url)
                }
            }
            contentCol.addView(mapActionTv)

            row.addView(imgCard)
            row.addView(contentCol)
            card.addView(row)
            binding.layoutSightsContainer.addView(card)
        }
    }

    private fun showProviderChooserDialog(title: String, links: List<ExternalProviderLink>) {
        val names = links.map { "${it.providerName} (Official Website)" }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(names) { _, which ->
                openExternalUrl(links[which].url)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openExternalUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Unable to open browser", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showImageAttributionDialog(destination: Destination) {
        val authorText = destination.imageAuthor.ifBlank { "Wikimedia Commons Contributor" }
        val licenseText = destination.imageLicense.ifBlank { "Creative Commons (Wikimedia Commons)" }
        val creditUrl = destination.imageCreditUrl.ifBlank { "https://commons.wikimedia.org" }

        val message = """
            Destination: ${destination.title}

            Photographer / Artist:
            $authorText

            License:
            $licenseText

            Source:
            Wikimedia Commons / Wikivoyage
        """.trimIndent()

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.photo_attribution_title)
            .setMessage(message)
            .setIcon(R.drawable.ic_info)
            .setPositiveButton("Open Wikimedia Page") { _, _ ->
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(creditUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Unable to open link", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
