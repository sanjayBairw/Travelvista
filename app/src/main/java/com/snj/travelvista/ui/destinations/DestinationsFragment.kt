package com.snj.travelvista.ui.destinations

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.snj.travelvista.R
import com.snj.travelvista.TravelApplication
import com.snj.travelvista.data.model.SortOption
import com.snj.travelvista.databinding.FragmentDestinationsBinding
import com.snj.travelvista.ui.common.Resource
import com.snj.travelvista.ui.common.ViewModelFactory
import com.snj.travelvista.ui.explore.CategoryAdapter
import kotlinx.coroutines.launch

class DestinationsFragment : Fragment() {

    private var _binding: FragmentDestinationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DestinationsViewModel by viewModels {
        val app = requireActivity().application as TravelApplication
        ViewModelFactory(app.repository, app.globalRepository)
    }

    private lateinit var destinationAdapter: DestinationListAdapter
    private lateinit var categoryFilterAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if a category was passed via arguments from ExploreFragment
        arguments?.getString("selectedCategory")?.let { passedCategory ->
            // If passed, apply it
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.categories.collect { cats ->
                    cats.find { it.id.equals(passedCategory, ignoreCase = true) }?.let {
                        viewModel.selectCategory(it)
                    }
                }
            }
        }

        setupRecyclerViews()
        setupSearch()
        setupSort()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Category Filters
        categoryFilterAdapter = CategoryAdapter { category ->
            viewModel.selectCategory(category)
        }
        binding.rvCategoryFilters.adapter = categoryFilterAdapter

        // Destinations List
        destinationAdapter = DestinationListAdapter(
            onDestinationClick = { destination ->
                val bundle = bundleOf(
                    "destinationId" to destination.id,
                    "destinationSlug" to destination.slug,
                    "destinationTitle" to destination.title
                )
                findNavController().navigate(R.id.action_destinations_to_detail, bundle)
            },
            onFavoriteClick = { destination ->
                viewModel.toggleFavorite(destination)
                val msg = if (!destination.isFavorite) {
                    getString(R.string.added_to_favorites)
                } else {
                    getString(R.string.removed_from_favorites)
                }
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
            }
        )
        binding.rvDestinations.adapter = destinationAdapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                viewModel.onSearchQueryChanged(query)
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }
    }

    private fun setupSort() {
        binding.chipSort.setOnClickListener {
            showSortDialog()
        }

        binding.chipRegion.setOnClickListener {
            showRegionDialog()
        }

        binding.chipMapView.setOnClickListener {
            findNavController().navigate(R.id.action_destinations_to_map)
        }
    }

    private fun showRegionDialog() {
        val regions = DestinationsViewModel.REGIONS.toTypedArray()
        val currentSelected = regions.indexOf(viewModel.selectedRegion.value).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by World Region")
            .setSingleChoiceItems(regions, currentSelected) { dialog, which ->
                viewModel.setRegionFilter(regions[which])
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showSortDialog() {
        val options = SortOption.values()
        val optionNames = options.map { it.displayName }.toTypedArray()
        val currentSelected = options.indexOf(viewModel.sortOption.value)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(optionNames, currentSelected) { dialog, which ->
                viewModel.setSortOption(options[which])
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupListeners() {
        binding.swipeRefreshDestinations.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.chipCredits.setOnClickListener {
            showImageCreditsDialog()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.fetchDestinations()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Search History Chips
                launch {
                    viewModel.searchHistory.collect { history ->
                        binding.chipGroupSearchHistory.removeAllViews()
                        if (history.isEmpty()) {
                            binding.hsvSearchHistory.visibility = View.GONE
                        } else {
                            binding.hsvSearchHistory.visibility = View.VISIBLE
                            history.forEach { entry ->
                                val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                                    text = entry.query
                                    isClickable = true
                                    setOnClickListener {
                                        binding.etSearch.setText(entry.query)
                                        binding.etSearch.setSelection(entry.query.length)
                                    }
                                }
                                binding.chipGroupSearchHistory.addView(chip)
                            }
                            val clearChip = com.google.android.material.chip.Chip(requireContext()).apply {
                                text = getString(R.string.clear_history)
                                isClickable = true
                                setOnClickListener { viewModel.clearSearchHistory() }
                            }
                            binding.chipGroupSearchHistory.addView(clearChip)
                        }
                    }
                }

                // Region Filter
                launch {
                    viewModel.selectedRegion.collect { region ->
                        binding.chipRegion.text = region
                    }
                }

                // Categories
                launch {
                    viewModel.categories.collect { categories ->
                        categoryFilterAdapter.submitList(categories)
                    }
                }

                // Sort option
                launch {
                    viewModel.sortOption.collect { sort ->
                        binding.chipSort.text = sort.displayName
                    }
                }

                // Destinations
                launch {
                    viewModel.destinations.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> {
                                if (!binding.swipeRefreshDestinations.isRefreshing) {
                                    binding.progressLoading.visibility = View.VISIBLE
                                }
                                binding.layoutErrorState.visibility = View.GONE
                            }
                            is Resource.Success -> {
                                binding.progressLoading.visibility = View.GONE
                                binding.layoutErrorState.visibility = View.GONE
                                val list = resource.data
                                destinationAdapter.submitList(list)

                                binding.tvResultsCount.text = getString(R.string.results_count, list.size)

                                if (list.isEmpty()) {
                                    binding.layoutEmptyState.visibility = View.VISIBLE
                                    binding.rvDestinations.visibility = View.GONE
                                } else {
                                    binding.layoutEmptyState.visibility = View.GONE
                                    binding.rvDestinations.visibility = View.VISIBLE
                                }
                            }
                            is Resource.Error -> {
                                binding.progressLoading.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.GONE
                                binding.layoutErrorState.visibility = View.VISIBLE
                                binding.tvErrorMessage.text = resource.message
                            }
                        }
                    }
                }

                // Refreshing state
                launch {
                    viewModel.isRefreshing.collect { isRefreshing ->
                        binding.swipeRefreshDestinations.isRefreshing = isRefreshing
                    }
                }
            }
        }
    }

    private fun showImageCreditsDialog() {
        val currentList = (viewModel.destinations.value as? Resource.Success)?.data ?: emptyList()
        val creditsBuilder = StringBuilder()

        creditsBuilder.append("Destination Photography Credits:\n\n")
        if (currentList.isNotEmpty()) {
            currentList.take(8).forEach { dest ->
                val author = dest.imageAuthor.ifBlank { "Wikimedia Commons Contributor" }
                val license = dest.imageLicense.ifBlank { "CC BY-SA" }
                creditsBuilder.append("• ${dest.title}:\n   Photo: $author\n   License: $license\n\n")
            }
            if (currentList.size > 8) {
                creditsBuilder.append("... and more licensed under Wikimedia Commons.")
            }
        } else {
            creditsBuilder.append("All photography is curated from Wikimedia Commons and Wikivoyage under respective Creative Commons and Public Domain licenses.")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.image_credits_title)
            .setMessage(creditsBuilder.toString().trimEnd())
            .setIcon(R.drawable.ic_info)
            .setPositiveButton("Open Wikimedia Commons") { _, _ ->
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://commons.wikimedia.org"))
                    startActivity(intent)
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Unable to open browser", Snackbar.LENGTH_SHORT).show()
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
