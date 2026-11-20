package com.snj.travelvista.ui.destinations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snj.travelvista.R
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.databinding.ItemDestinationVerticalBinding
import com.snj.travelvista.ui.common.DestinationDiffCallback
import com.snj.travelvista.utils.ImageLoader

class DestinationListAdapter(
    private val onDestinationClick: (Destination) -> Unit,
    private val onFavoriteClick: (Destination) -> Unit
) : ListAdapter<Destination, DestinationListAdapter.DestinationViewHolder>(DestinationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestinationViewHolder {
        val binding = ItemDestinationVerticalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DestinationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DestinationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DestinationViewHolder(
        private val binding: ItemDestinationVerticalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(destination: Destination) {
            binding.tvTitle.text = destination.title
            binding.tvLocation.text = destination.location
            binding.tvCategory.text = destination.category
            binding.tvRating.text = destination.formattedRating
            binding.tvReviews.text = destination.formattedReviews
            binding.tvPrice.text = destination.formattedPrice

            ImageLoader.loadImage(binding.ivDestination, destination.imageUrl, destination.title, destination.category)

            // Favorite Icon State
            val favoriteIcon = if (destination.isFavorite) {
                R.drawable.ic_favorite_filled
            } else {
                R.drawable.ic_favorite_border
            }
            binding.btnFavorite.setImageResource(favoriteIcon)

            val tintColor = if (destination.isFavorite) {
                ContextCompat.getColor(binding.root.context, R.color.heart_red)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.on_surface_variant)
            }
            binding.btnFavorite.setColorFilter(tintColor)

            binding.cardDestination.setOnClickListener {
                onDestinationClick(destination)
            }

            binding.btnFavorite.setOnClickListener {
                onFavoriteClick(destination)
            }
        }
    }
}
