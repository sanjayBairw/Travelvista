package com.snj.travelvista.ui.trips

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.snj.travelvista.R
import com.snj.travelvista.TravelApplication
import com.snj.travelvista.data.local.entity.TripEntity
import com.snj.travelvista.data.remote.provider.ActivitySearchQuery
import com.snj.travelvista.data.remote.provider.CabinClass
import com.snj.travelvista.data.remote.provider.FlightSearchQuery
import com.snj.travelvista.data.remote.provider.FlightTripType
import com.snj.travelvista.data.remote.provider.HotelSearchQuery
import com.snj.travelvista.data.remote.provider.ProviderBookingLinkBuilder
import com.snj.travelvista.databinding.FragmentTripsBinding
import com.snj.travelvista.ui.common.ViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class TripsFragment : Fragment() {

    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!

    private val app: TravelApplication
        get() = requireActivity().application as TravelApplication

    private val viewModel: TripsViewModel by viewModels {
        ViewModelFactory(app.repository, app.globalRepository)
    }

    private lateinit var tripAdapter: TripAdapter

    private var hotelCheckIn = ""
    private var hotelCheckOut = ""
    private var flightDepart = ""
    private var flightReturn = ""
    private var selectedCabin = CabinClass.ECONOMY
    private var selectedActivityCategory = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        setupMyTrips()
        setupHotelSearch()
        setupFlightSearch()
        setupActivitySearch()
        setupPackingChecklist()
        observeTrips()

        val initialTab = arguments?.getInt("initialTab", 0) ?: 0
        if (initialTab in 0..4) {
            binding.tabLayoutTrips.getTabAt(initialTab)?.select()
            showSection(initialTab)
        }
    }

    private fun setupTabs() {
        binding.tabLayoutTrips.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showSection(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showSection(position: Int) {
        binding.containerMyTrips.visibility = if (position == 0) View.VISIBLE else View.GONE
        binding.containerHotels.visibility = if (position == 1) View.VISIBLE else View.GONE
        binding.containerFlights.visibility = if (position == 2) View.VISIBLE else View.GONE
        binding.containerActivities.visibility = if (position == 3) View.VISIBLE else View.GONE
        binding.containerPacking.visibility = if (position == 4) View.VISIBLE else View.GONE
    }

    private fun setupMyTrips() {
        tripAdapter = TripAdapter(
            preferredCurrencyProvider = { app.userPreferences.preferredCurrency },
            exchangeRateProvider = { app.userPreferences.cachedUsdToPreferredRate },
            onTripClick = { trip -> showTripItineraryDialog(trip) },
            onDeleteClick = { trip ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Trip?")
                    .setMessage("Remove \"${trip.name}\" and all its itinerary items from this device?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteTrip(trip.id)
                        Snackbar.make(binding.root, "Trip removed", Snackbar.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        )
        binding.rvTrips.adapter = tripAdapter

        binding.btnCreateTrip.setOnClickListener {
            showCreateTripDialog()
        }
    }

    private fun observeTrips() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.trips.collect { list ->
                    tripAdapter.submitList(list)
                    binding.layoutTripsEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvTrips.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun showCreateTripDialog() {
        val ctx = requireContext()
        val pad = (16 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, pad / 2)
        }

        val etName = EditText(ctx).apply { hint = "Trip Name (e.g. Japan Autumn Tour)" }
        val etDest = EditText(ctx).apply { hint = "Destination(s) (e.g. Tokyo, Kyoto)" }
        val etStart = EditText(ctx).apply { hint = "Start Date (YYYY-MM-DD)" }
        val etEnd = EditText(ctx).apply { hint = "End Date (YYYY-MM-DD)" }
        val etTravelers = EditText(ctx).apply {
            hint = "Travelers"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("2")
        }
        val etBudget = EditText(ctx).apply {
            hint = "Est. Budget in USD (optional)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etNotes = EditText(ctx).apply { hint = "Travel Notes (optional)" }

        listOf(etName, etDest, etStart, etEnd, etTravelers, etBudget, etNotes).forEach {
            container.addView(it)
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Create New Offline Trip")
            .setView(container)
            .setPositiveButton("Save Trip") { _, _ ->
                val name = etName.text.toString().trim()
                val dest = etDest.text.toString().trim()
                if (name.isBlank() || dest.isBlank()) {
                    Snackbar.make(binding.root, "Please enter a trip name and destination", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.createTrip(
                    name = name,
                    destinationTitle = dest,
                    startDate = etStart.text.toString().ifBlank { "TBD" },
                    endDate = etEnd.text.toString().ifBlank { "TBD" },
                    travelersCount = etTravelers.text.toString().toIntOrNull() ?: 2,
                    estimatedBudgetUsd = etBudget.text.toString().toIntOrNull() ?: 0,
                    notes = etNotes.text.toString()
                )
                Snackbar.make(binding.root, "Trip \"$name\" saved offline!", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showTripItineraryDialog(trip: TripEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = viewModel.getItinerarySync(trip.id)
            val summary = buildString {
                append("Destination: ${trip.destinationTitle}\n")
                append("Dates: ${trip.startDate} → ${trip.endDate}\n")
                append("Travelers: ${trip.travelersCount}\n")
                if (trip.notes.isNotBlank()) append("Notes: ${trip.notes}\n")
                append("\nDay-wise Itinerary:\n")
                if (items.isEmpty()) {
                    append("• No itinerary items added yet. Tap 'Add Day Item' to plan sights, hotels, or flights.")
                } else {
                    items.forEach { item ->
                        val timePart = if (item.timeSlot.isNotBlank()) " (${item.timeSlot})" else ""
                        append("• Day ${item.dayNumber}$timePart [${item.itemType}]: ${item.title}\n")
                        if (item.notes.isNotBlank()) append("   ${item.notes}\n")
                    }
                }
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(trip.name)
                .setMessage(summary.trimEnd())
                .setPositiveButton("Add Day Item") { _, _ ->
                    showAddItineraryItemDialog(trip)
                }
                .setNeutralButton("Share Plan") { _, _ ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "TravelVista Itinerary: ${trip.name}")
                        putExtra(Intent.EXTRA_TEXT, "${trip.name}\n\n$summary\nShared via TravelVista")
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Trip Itinerary"))
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private fun showAddItineraryItemDialog(trip: TripEntity) {
        val ctx = requireContext()
        val pad = (16 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, pad / 2)
        }

        val etDay = EditText(ctx).apply {
            hint = "Day Number (e.g. 1)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("1")
        }
        val etTime = EditText(ctx).apply { hint = "Time Slot (e.g. 09:00 AM)" }
        val etTitle = EditText(ctx).apply { hint = "Activity, Hotel, or Flight Title" }
        val etType = EditText(ctx).apply {
            hint = "Type (SIGHTSEEING, HOTEL, FLIGHT, DINING)"
            setText("SIGHTSEEING")
        }
        val etNotes = EditText(ctx).apply { hint = "Notes or Booking Reference (optional)" }

        listOf(etDay, etTime, etTitle, etType, etNotes).forEach { container.addView(it) }

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Add to ${trip.name}")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isBlank()) return@setPositiveButton
                viewModel.addItineraryItem(
                    tripId = trip.id,
                    dayNumber = etDay.text.toString().toIntOrNull() ?: 1,
                    timeSlot = etTime.text.toString(),
                    title = title,
                    itemType = etType.text.toString().uppercase(Locale.US).ifBlank { "SIGHTSEEING" },
                    notes = etNotes.text.toString()
                )
                Snackbar.make(binding.root, "Added \"$title\" to Day ${etDay.text}", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // --- HOTELS EXTERNAL PROVIDER SEARCH ---

    private fun setupHotelSearch() {
        binding.btnHotelCheckin.setOnClickListener {
            pickDate { dateStr ->
                hotelCheckIn = dateStr
                binding.btnHotelCheckin.text = "In: $dateStr"
            }
        }

        binding.btnHotelCheckout.setOnClickListener {
            pickDate { dateStr ->
                hotelCheckOut = dateStr
                binding.btnHotelCheckout.text = "Out: $dateStr"
            }
        }

        binding.btnSearchBookingCom.setOnClickListener { launchHotelProvider(0) }
        binding.btnSearchGoogleHotels.setOnClickListener { launchHotelProvider(1) }
        binding.btnSearchAgoda.setOnClickListener { launchHotelProvider(2) }
        binding.btnSearchExpedia.setOnClickListener { launchHotelProvider(3) }
    }

    private fun launchHotelProvider(providerIndex: Int) {
        val city = binding.etHotelCity.text?.toString()?.trim().orEmpty()
        if (city.isBlank()) {
            Snackbar.make(binding.root, "Enter a city or destination to search hotels", Snackbar.LENGTH_SHORT).show()
            return
        }
        val query = HotelSearchQuery(
            destination = city,
            checkInDate = hotelCheckIn,
            checkOutDate = hotelCheckOut,
            adults = binding.etHotelAdults.text?.toString()?.toIntOrNull() ?: 2,
            rooms = binding.etHotelRooms.text?.toString()?.toIntOrNull() ?: 1
        )
        val links = ProviderBookingLinkBuilder.buildProviderSearchLinks(query)
        openExternalUrl(links[providerIndex].url)
    }

    // --- FLIGHTS EXTERNAL PROVIDER SEARCH ---

    private fun setupFlightSearch() {
        val savedHome = app.userPreferences.homeAirport.ifBlank { app.userPreferences.homeCity }
        if (savedHome.isNotBlank()) {
            binding.etFlightOrigin.setText(savedHome)
        }

        binding.rgFlightTripType.setOnCheckedChangeListener { _, checkedId ->
            val isRoundTrip = checkedId == binding.rbRoundTrip.id
            binding.btnFlightReturn.isEnabled = isRoundTrip
            binding.btnFlightReturn.alpha = if (isRoundTrip) 1.0f else 0.4f
        }

        binding.btnFlightDepart.setOnClickListener {
            pickDate { dateStr ->
                flightDepart = dateStr
                binding.btnFlightDepart.text = "Dep: $dateStr"
            }
        }

        binding.btnFlightReturn.setOnClickListener {
            pickDate { dateStr ->
                flightReturn = dateStr
                binding.btnFlightReturn.text = "Ret: $dateStr"
            }
        }

        binding.btnFlightCabin.setOnClickListener {
            val cabins = CabinClass.values()
            val labels = cabins.map { it.displayName }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Cabin Class")
                .setSingleChoiceItems(labels, cabins.indexOf(selectedCabin)) { dialog, which ->
                    selectedCabin = cabins[which]
                    binding.btnFlightCabin.text = "Cabin: ${selectedCabin.displayName}"
                    dialog.dismiss()
                }
                .show()
        }

        binding.btnSearchGoogleFlights.setOnClickListener { launchFlightProvider(0) }
        binding.btnSearchSkyscanner.setOnClickListener { launchFlightProvider(1) }
        binding.btnSearchKayak.setOnClickListener { launchFlightProvider(2) }
    }

    private fun launchFlightProvider(providerIndex: Int) {
        val origin = binding.etFlightOrigin.text?.toString()?.trim().orEmpty()
        val dest = binding.etFlightDestination.text?.toString()?.trim().orEmpty()
        if (origin.isBlank() || dest.isBlank()) {
            Snackbar.make(binding.root, "Enter both origin and destination airports/cities", Snackbar.LENGTH_SHORT).show()
            return
        }
        val tripType = if (binding.rbOneWay.isChecked) FlightTripType.ONE_WAY else FlightTripType.ROUND_TRIP
        val query = FlightSearchQuery(
            origin = origin,
            destination = dest,
            departureDate = flightDepart,
            returnDate = if (tripType == FlightTripType.ROUND_TRIP) flightReturn else null,
            passengers = binding.etFlightPassengers.text?.toString()?.toIntOrNull() ?: 1,
            tripType = tripType,
            cabinClass = selectedCabin
        )
        val links = ProviderBookingLinkBuilder.buildProviderSearchLinks(query)
        openExternalUrl(links[providerIndex].url)
    }

    // --- ACTIVITIES EXTERNAL PROVIDER SEARCH ---

    private fun setupActivitySearch() {
        val categories = arrayOf(
            "All", "Sightseeing", "Museums", "Guided Tours", "Adventure", "Theme Parks", "Food & Culture"
        )
        binding.btnActivityCategory.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Experience Category")
                .setItems(categories) { _, which ->
                    selectedActivityCategory = categories[which]
                    binding.btnActivityCategory.text = "Category: $selectedActivityCategory"
                }
                .show()
        }

        binding.btnSearchGetyourguide.setOnClickListener { launchActivityProvider(0) }
        binding.btnSearchViator.setOnClickListener { launchActivityProvider(1) }
        binding.btnSearchAirbnb.setOnClickListener { launchActivityProvider(2) }
    }

    private fun launchActivityProvider(providerIndex: Int) {
        val city = binding.etActivityCity.text?.toString()?.trim().orEmpty()
        if (city.isBlank()) {
            Snackbar.make(binding.root, "Enter a city or landmark to search tours", Snackbar.LENGTH_SHORT).show()
            return
        }
        val links = ProviderBookingLinkBuilder.buildProviderSearchLinks(
            ActivitySearchQuery(destination = city, category = selectedActivityCategory)
        )
        openExternalUrl(links[providerIndex].url)
    }

    private fun pickDate(onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onPicked(formatted)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun openExternalUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Unable to open browser", Snackbar.LENGTH_SHORT).show()
        }
    }

    // --- SMART PACKING CHECKLIST & ESSENTIALS ---

    private var activePackingCategory = "ALL"
    private val packingPrefs by lazy {
        requireContext().getSharedPreferences("travelvista_packing_prefs", android.content.Context.MODE_PRIVATE)
    }

    private data class PackingItem(val key: String, val title: String, val category: String)

    private val defaultPackingItems = listOf(
        // Documents & Visa
        PackingItem("doc_passport", "Passport & Visa Documents", "DOCS"),
        PackingItem("doc_insurance", "Travel Insurance Policy Card", "DOCS"),
        PackingItem("doc_idp", "International Driving Permit / License", "DOCS"),
        PackingItem("doc_confirmations", "Printed Flight & Hotel Vouchers", "DOCS"),
        PackingItem("doc_adapter", "Universal Travel Power Adapter", "DOCS"),
        // Beach & Coastal
        PackingItem("beach_swimwear", "Swimwear & Rash Guard", "BEACH"),
        PackingItem("beach_sunscreen", "Reef-Safe Sunscreen (SPF 50+)", "BEACH"),
        PackingItem("beach_sunglasses", "Polarized UV Sunglasses", "BEACH"),
        PackingItem("beach_towel", "Quick-Dry Sand-Free Microfiber Towel", "BEACH"),
        PackingItem("beach_pouch", "Waterproof Phone Lanyard & Pouch", "BEACH"),
        PackingItem("beach_hat", "Wide-Brim Sun Hat", "BEACH"),
        // Trekking & Mountain
        PackingItem("trek_boots", "Waterproof Hiking / Trekking Boots", "TREK"),
        PackingItem("trek_poles", "Collapsible Trekking Poles", "TREK"),
        PackingItem("trek_firstaid", "Blister Bandages & Wilderness First Aid Kit", "TREK"),
        PackingItem("trek_snacks", "High-Protein Energy Bars & Trail Nuts", "TREK"),
        PackingItem("trek_poncho", "Lightweight Breathable Rain Poncho", "TREK"),
        PackingItem("trek_bottle", "Insulated Water Bottle with Filter", "TREK"),
        // City Explorer
        PackingItem("city_daypack", "Anti-Theft RFID-Blocking Daypack", "CITY"),
        PackingItem("city_powerbank", "High-Capacity 20,000mAh Power Bank", "CITY"),
        PackingItem("city_shoes", "Ergonomic Walking Sneakers", "CITY"),
        PackingItem("city_umbrella", "Windproof Compact Folding Umbrella", "CITY"),
        PackingItem("city_metro", "Local Transit Card & Offline Maps", "CITY"),
        // Winter & Cold
        PackingItem("win_thermals", "Merino Wool Thermal Base Layer", "WINTER"),
        PackingItem("win_parka", "Heavy Down Parka / Insulated Jacket", "WINTER"),
        PackingItem("win_gloves", "Windproof Fleece Beanie & Touchscreen Gloves", "WINTER"),
        PackingItem("win_boots", "Thermal Insulated Snow Boots", "WINTER"),
        PackingItem("win_skincare", "Moisturizing Lip Balm & Cold Cream", "WINTER")
    )

    private fun setupPackingChecklist() {
        binding.btnPackAll.setOnClickListener { switchPackingFilter("ALL") }
        binding.btnPackBeach.setOnClickListener { switchPackingFilter("BEACH") }
        binding.btnPackMountain.setOnClickListener { switchPackingFilter("TREK") }
        binding.btnPackCity.setOnClickListener { switchPackingFilter("CITY") }
        binding.btnPackWinter.setOnClickListener { switchPackingFilter("WINTER") }
        binding.btnPackDocs.setOnClickListener { switchPackingFilter("DOCS") }

        binding.btnAddPackingItem.setOnClickListener { showAddPackingItemDialog() }
        binding.btnResetPacking.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset Checklist?")
                .setMessage("Uncheck all items and reset checklist?")
                .setPositiveButton("Reset") { _, _ ->
                    packingPrefs.edit().clear().apply()
                    renderPackingItems()
                    Snackbar.make(binding.root, "Packing list reset", Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        renderPackingItems()
    }

    private fun switchPackingFilter(category: String) {
        activePackingCategory = category
        renderPackingItems()
    }

    private fun showAddPackingItemDialog() {
        val input = EditText(requireContext()).apply {
            hint = "e.g. Action Camera & SD Cards"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(40, 30, 40, 30)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Custom Packing Item")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotBlank()) {
                    val customSet = packingPrefs.getStringSet("custom_packing_items", emptySet())?.toMutableSet() ?: mutableSetOf()
                    customSet.add(text)
                    packingPrefs.edit().putStringSet("custom_packing_items", customSet).apply()
                    renderPackingItems()
                    Snackbar.make(binding.root, "Added \"$text\" to checklist", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun renderPackingItems() {
        binding.layoutPackingItems.removeAllViews()

        val customSet = packingPrefs.getStringSet("custom_packing_items", emptySet()) ?: emptySet()
        val customItems = customSet.map { text ->
            PackingItem("custom_${text.hashCode()}", text, "ALL")
        }

        val allItems = defaultPackingItems + customItems
        val displayedItems = if (activePackingCategory == "ALL") {
            allItems
        } else {
            allItems.filter { it.category == activePackingCategory || it.category == "ALL" }
        }

        var checkedCount = 0
        displayedItems.forEach { item ->
            val isChecked = packingPrefs.getBoolean("item_${item.key}", false)
            if (isChecked) checkedCount++

            val checkBox = com.google.android.material.checkbox.MaterialCheckBox(requireContext()).apply {
                text = item.title
                this.isChecked = isChecked
                textSize = 14f
                setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.on_surface))
                setOnCheckedChangeListener { _, checked ->
                    packingPrefs.edit().putBoolean("item_${item.key}", checked).apply()
                    updatePackingProgress(displayedItems)
                }
            }
            binding.layoutPackingItems.addView(checkBox)
        }

        updatePackingProgress(displayedItems)
    }

    private fun updatePackingProgress(items: List<PackingItem>) {
        if (items.isEmpty()) {
            binding.progressPacking.progress = 0
            binding.tvPackingProgress.text = "0% Packed"
            return
        }
        val checkedCount = items.count { packingPrefs.getBoolean("item_${it.key}", false) }
        val percent = (checkedCount * 100) / items.size
        binding.progressPacking.progress = percent
        binding.tvPackingProgress.text = "$percent% Packed ($checkedCount/${items.size})"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
