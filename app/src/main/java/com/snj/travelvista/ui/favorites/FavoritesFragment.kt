package com.snj.travelvista.ui.favorites

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
import com.google.android.material.snackbar.Snackbar
import com.snj.travelvista.R
import com.snj.travelvista.TravelApplication
import com.snj.travelvista.databinding.FragmentFavoritesBinding
import com.snj.travelvista.ui.common.ViewModelFactory
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels {
        val app = requireActivity().application as TravelApplication
        ViewModelFactory(app.repository)
    }

    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(
            onDestinationClick = { destination ->
                val bundle = bundleOf(
                    "destinationId" to destination.id,
                    "destinationSlug" to destination.slug,
                    "destinationTitle" to destination.title
                )
                findNavController().navigate(R.id.action_favorites_to_detail, bundle)
            },
            onRemoveClick = { destination ->
                viewModel.removeFavorite(destination)
                Snackbar.make(binding.root, "${destination.title} removed from saved", Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) {
                        viewModel.restoreFavorite(destination)
                    }
                    .setActionTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent))
                    .show()
            }
        )
        binding.rvFavorites.adapter = favoritesAdapter
    }

    private fun setupListeners() {
        binding.btnExploreNow.setOnClickListener {
            findNavController().navigate(R.id.action_favorites_to_explore)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favorites.collect { list ->
                    favoritesAdapter.submitList(list)

                    if (list.isEmpty()) {
                        binding.layoutEmptyFavorites.visibility = View.VISIBLE
                        binding.rvFavorites.visibility = View.GONE
                        binding.tvFavoritesCount.text = "0 saved destinations"
                    } else {
                        binding.layoutEmptyFavorites.visibility = View.GONE
                        binding.rvFavorites.visibility = View.VISIBLE
                        val countText = if (list.size == 1) {
                            "1 saved destination"
                        } else {
                            "${list.size} saved destinations"
                        }
                        binding.tvFavoritesCount.text = countText
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
