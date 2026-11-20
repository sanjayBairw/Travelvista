package com.snj.travelvista.ui.map

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.snj.travelvista.R
import com.snj.travelvista.TravelApplication
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.data.remote.dto.NominatimPlaceDto
import com.snj.travelvista.data.remote.provider.ProviderBookingLinkBuilder
import com.snj.travelvista.databinding.FragmentMapDiscoveryBinding
import com.snj.travelvista.ui.common.ViewModelFactory
import com.snj.travelvista.utils.GeoUtils
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MapDiscoveryFragment : Fragment() {

    private var _binding: FragmentMapDiscoveryBinding? = null
    private val binding get() = _binding!!

    private val app: TravelApplication
        get() = requireActivity().application as TravelApplication

    private val viewModel: MapViewModel by viewModels {
        ViewModelFactory(app.repository, app.globalRepository)
    }

    private var selectedDestination: Destination? = null
    private var allDestinations: List<Destination> = emptyList()
    private var selectedMapCategory = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapDiscoveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
        setupListeners()
        observeViewModel()
    }

    private fun setupMap() {
        binding.osmMapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.osmMapView.setMultiTouchControls(true)

        val focusLat = arguments?.getDouble("focusLat", Double.NaN) ?: Double.NaN
        val focusLon = arguments?.getDouble("focusLon", Double.NaN) ?: Double.NaN
        val focusTitle = arguments?.getString("focusTitle")

        val controller = binding.osmMapView.controller
        if (!focusLat.isNaN() && !focusLon.isNaN()) {
            controller.setZoom(11.0)
            val point = GeoPoint(focusLat, focusLon)
            controller.setCenter(point)
            addFocusMarker(point, focusTitle ?: "Selected Location")
        } else {
            controller.setZoom(3.2)
            controller.setCenter(GeoPoint(25.0, 15.0))
        }
    }

    private fun addFocusMarker(point: GeoPoint, title: String) {
        val marker = Marker(binding.osmMapView).apply {
            position = point
            this.title = title
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        binding.osmMapView.overlays.add(marker)
        binding.osmMapView.invalidate()
    }

    private fun setupListeners() {
        binding.btnMapBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnMapSearchGo.setOnClickListener {
            val query = binding.etMapSearch.text?.toString().orEmpty()
            viewModel.searchMapLocation(query)
        }

        binding.etMapSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.searchMapLocation(binding.etMapSearch.text?.toString().orEmpty())
                true
            } else false
        }

        binding.chipMapAll.setOnClickListener { filterMapByCategory("All") }
        binding.chipMapMountains.setOnClickListener { filterMapByCategory("Mountains") }
        binding.chipMapBeaches.setOnClickListener { filterMapByCategory("Beaches") }
        binding.chipMapCities.setOnClickListener { filterMapByCategory("Cities") }
        binding.chipMapHistorical.setOnClickListener { filterMapByCategory("Historical") }
        binding.chipMapNature.setOnClickListener { filterMapByCategory("Nature") }

        binding.btnPreviewDetails.setOnClickListener {
            selectedDestination?.let { dest ->
                findNavController().navigate(
                    R.id.action_map_to_detail,
                    bundleOf(
                        "destinationId" to dest.id,
                        "destinationSlug" to dest.slug,
                        "destinationTitle" to dest.title
                    )
                )
            }
        }

        binding.btnPreviewDirections.setOnClickListener {
            selectedDestination?.let { dest ->
                val lat = dest.latitude
                val lon = dest.longitude
                if (lat != null && lon != null) {
                    val url = ProviderBookingLinkBuilder.buildDirectionsUrl(lat, lon, dest.title)
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        Snackbar.make(binding.root, "Unable to open maps app", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.destinations.collect { list ->
                        allDestinations = list
                        filterMapByCategory(selectedMapCategory)
                    }
                }

                launch {
                    viewModel.searchResults.collect { places ->
                        renderSearchDropdown(places)
                    }
                }
            }
        }
    }

    private fun filterMapByCategory(category: String) {
        selectedMapCategory = category
        val filtered = if (category.equals("All", ignoreCase = true)) {
            allDestinations
        } else {
            allDestinations.filter { it.category.contains(category, ignoreCase = true) }
        }
        renderDestinationPins(filtered)
    }

    private fun renderDestinationPins(destinations: List<Destination>) {
        binding.osmMapView.overlays.clear()
        destinations.forEach { dest ->
            val lat = dest.latitude ?: return@forEach
            val lon = dest.longitude ?: return@forEach
            val marker = Marker(binding.osmMapView).apply {
                position = GeoPoint(lat, lon)
                title = dest.title
                snippet = dest.location
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    showPreviewCard(dest)
                    binding.osmMapView.controller.animateTo(GeoPoint(lat, lon))
                    true
                }
            }
            binding.osmMapView.overlays.add(marker)
        }
        binding.osmMapView.invalidate()
    }

    private fun renderSearchDropdown(places: List<NominatimPlaceDto>) {
        binding.layoutMapSearchResults.removeAllViews()
        if (places.isEmpty()) {
            binding.cardMapResults.visibility = View.GONE
            return
        }
        binding.cardMapResults.visibility = View.VISIBLE
        val density = resources.displayMetrics.density

        places.take(5).forEach { place ->
            val itemTv = TextView(requireContext()).apply {
                text = "${place.resolvedTitle} — ${place.resolvedRegion}"
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))
                val pad = (10 * density).toInt()
                setPadding(pad, pad, pad, pad)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    binding.cardMapResults.visibility = View.GONE
                    viewModel.openGeocodedPlace(place) { dest ->
                        val lat = dest.latitude
                        val lon = dest.longitude
                        if (lat != null && lon != null) {
                            binding.osmMapView.controller.setZoom(10.5)
                            binding.osmMapView.controller.animateTo(GeoPoint(lat, lon))
                            showPreviewCard(dest)
                        }
                    }
                }
            }
            binding.layoutMapSearchResults.addView(itemTv)
        }
    }

    private fun showPreviewCard(destination: Destination) {
        selectedDestination = destination
        binding.cardMapPreview.visibility = View.VISIBLE
        binding.tvPreviewTitle.text = destination.title

        val center = binding.osmMapView.mapCenter
        val distText = if (destination.latitude != null && destination.longitude != null) {
            val km = GeoUtils.haversineDistanceKm(
                center.latitude,
                center.longitude,
                destination.latitude,
                destination.longitude
            )
            val formattedDist = GeoUtils.formatDistance(km * 1000.0, app.userPreferences.useMetricUnits)
            String.format(
                Locale.US,
                "%s · %.3f°, %.3f° · %s from view center",
                destination.location,
                destination.latitude,
                destination.longitude,
                formattedDist
            )
        } else {
            destination.location
        }
        binding.tvPreviewSubtitle.text = distText
    }

    override fun onResume() {
        super.onResume()
        binding.osmMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.osmMapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
