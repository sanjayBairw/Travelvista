package com.snj.travelvista.ui.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snj.travelvista.R
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.databinding.ItemPopularDestinationBinding
import com.snj.travelvista.ui.common.DestinationDiffCallback
import com.snj.travelvista.utils.ImageLoader

class PopularAdapter(
    private val onDestinationClick: (Destination) -> Unit,
    private val onFavoriteClick: (Destination) -> Unit
) : ListAdapter<Destination, PopularAdapter.PopularViewHolder>(DestinationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularViewHolder {
        val binding = ItemPopularDestinationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PopularViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PopularViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PopularViewHolder(
        private val binding: ItemPopularDestinationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(destination: Destination) {
            binding.tvTitle.text = destination.title
            binding.tvLocation.text = destination.location
            binding.tvRating.text = destination.formattedRating
            binding.tvPrice.text = "From ${destination.formattedPrice}"

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
                ContextCompat.getColor(binding.root.context, R.color.on_surface)
            }
            binding.btnFavorite.setColorFilter(tintColor)

            binding.cardPopular.setOnClickListener {
                onDestinationClick(destination)
            }

            binding.btnFavorite.setOnClickListener {
                onFavoriteClick(destination)
            }
        }
    }
}
