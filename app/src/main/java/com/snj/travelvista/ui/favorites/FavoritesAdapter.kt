package com.snj.travelvista.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.databinding.ItemFavoriteBinding
import com.snj.travelvista.ui.common.DestinationDiffCallback
import com.snj.travelvista.utils.ImageLoader

class FavoritesAdapter(
    private val onDestinationClick: (Destination) -> Unit,
    private val onRemoveClick: (Destination) -> Unit
) : ListAdapter<Destination, FavoritesAdapter.FavoriteViewHolder>(DestinationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FavoriteViewHolder(
        private val binding: ItemFavoriteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(destination: Destination) {
            binding.tvTitle.text = destination.title
            binding.tvLocation.text = destination.location
            binding.tvCategory.text = destination.category
            binding.tvRating.text = destination.formattedRating
            binding.tvPrice.text = destination.formattedPrice

            ImageLoader.loadImage(binding.ivDestination, destination.imageUrl, destination.title, destination.category)

            binding.cardFavorite.setOnClickListener {
                onDestinationClick(destination)
            }

            binding.btnRemoveFavorite.setOnClickListener {
                onRemoveClick(destination)
            }
        }
    }
}
