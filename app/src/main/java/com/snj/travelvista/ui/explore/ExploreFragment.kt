package com.snj.travelvista.ui.explore

import android.os.Bundle
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
import androidx.recyclerview.widget.PagerSnapHelper
import com.google.android.material.snackbar.Snackbar
import com.snj.travelvista.R
import com.snj.travelvista.TravelApplication
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.databinding.FragmentExploreBinding
import com.snj.travelvista.ui.common.Resource
import com.snj.travelvista.ui.common.ViewModelFactory
import com.snj.travelvista.ui.destinations.DestinationListAdapter
import kotlinx.coroutines.launch

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExploreViewModel by viewModels {
        val app = requireActivity().application as TravelApplication
        ViewModelFactory(app.repository, app.globalRepository)
    }

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var featuredAdapter: FeaturedAdapter
    private lateinit var popularAdapter: PopularAdapter
    private lateinit var trendingAdapter: DestinationListAdapter
    private lateinit var recentAdapter: RecentDestinationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Recently Viewed Destinations
        recentAdapter = RecentDestinationAdapter { recent ->
            navigateToDetailById(recent.destinationId)
        }
        binding.rvRecentlyViewed.adapter = recentAdapter

        // Categories
        categoryAdapter = CategoryAdapter { category ->
            viewModel.selectCategory(category)
            // Navigate to Destinations list with this category filter preselected!
            val bundle = bundleOf("selectedCategory" to category.id)
            findNavController().navigate(R.id.action_explore_to_destinations, bundle)
        }
        binding.rvCategories.adapter = categoryAdapter

        // Featured Destinations with Snap Carousel
        featuredAdapter = FeaturedAdapter { destination ->
            navigateToDetail(destination)
        }
        binding.rvFeatured.adapter = featuredAdapter
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvFeatured)

        // Popular Destinations
        popularAdapter = PopularAdapter(
            onDestinationClick = { destination ->
                navigateToDetail(destination)
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
        binding.rvPopular.adapter = popularAdapter

        // Trending Destinations (Vertical list)
        trendingAdapter = DestinationListAdapter(
            onDestinationClick = { destination ->
                navigateToDetail(destination)
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
        binding.rvTrending.adapter = trendingAdapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.cardSearchTrigger.setOnClickListener {
            findNavController().navigate(R.id.action_explore_to_destinations)
        }

        binding.btnQuickMap.setOnClickListener {
            findNavController().navigate(R.id.action_explore_to_map)
        }

        binding.btnQuickHotels.setOnClickListener {
            findNavController().navigate(R.id.action_explore_to_trips, bundleOf("initialTab" to 1))
        }

        binding.btnQuickFlights.setOnClickListener {
            findNavController().navigate(R.id.action_explore_to_trips, bundleOf("initialTab" to 2))
        }

        binding.btnQuickActivities.setOnClickListener {
            findNavController().navigate(R.id.action_explore_to_trips, bundleOf("initialTab" to 3))
        }

        binding.btnViewAllPopular.setOnClickListener {
            findNavController().navigate(R.id.action_explore_to_destinations)
        }

        binding.btnViewAllTrending.setOnClickListener {
            findNavController().navigate(R.id.action_explore_to_destinations)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadData()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Recently Viewed Destinations
                launch {
                    viewModel.recentDestinations.collect { recents ->
                        if (recents.isEmpty()) {
                            binding.layoutRecentlyViewed.visibility = View.GONE
                        } else {
                            binding.layoutRecentlyViewed.visibility = View.VISIBLE
                            recentAdapter.submitList(recents)
                        }
                    }
                }

                // Categories
                launch {
                    viewModel.categories.collect { categories ->
                        categoryAdapter.submitList(categories)
                    }
                }

                // Featured Destinations
                launch {
                    viewModel.featured.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> {
                                if (!binding.swipeRefresh.isRefreshing) {
                                    binding.progressLoading.visibility = View.VISIBLE
                                }
                            }
                            is Resource.Success -> {
                                binding.progressLoading.visibility = View.GONE
                                binding.layoutError.visibility = View.GONE
                                binding.layoutContent.visibility = View.VISIBLE
                                featuredAdapter.submitList(resource.data)
                            }
                            is Resource.Error -> {
                                binding.progressLoading.visibility = View.GONE
                                binding.layoutError.visibility = View.VISIBLE
                                binding.tvErrorMessage.text = resource.message
                            }
                        }
                    }
                }

                // Popular Destinations
                launch {
                    viewModel.popular.collect { resource ->
                        if (resource is Resource.Success) {
                            popularAdapter.submitList(resource.data)
                        }
                    }
                }

                // Trending Destinations
                launch {
                    viewModel.trending.collect { resource ->
                        if (resource is Resource.Success) {
                            trendingAdapter.submitList(resource.data)
                        }
                    }
                }

                // Refreshing state
                launch {
                    viewModel.isRefreshing.collect { isRefreshing ->
                        binding.swipeRefresh.isRefreshing = isRefreshing
                    }
                }
            }
        }
    }

    private fun navigateToDetail(destination: Destination) {
        val bundle = bundleOf(
            "destinationId" to destination.id,
            "destinationSlug" to destination.slug,
            "destinationTitle" to destination.title
        )
        findNavController().navigate(R.id.action_explore_to_detail, bundle)
    }

    private fun navigateToDetailById(destinationId: Long) {
        val bundle = bundleOf("destinationId" to destinationId)
        findNavController().navigate(R.id.action_explore_to_detail, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
