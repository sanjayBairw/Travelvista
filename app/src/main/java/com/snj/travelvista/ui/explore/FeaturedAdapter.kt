package com.snj.travelvista.ui.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snj.travelvista.data.model.Destination
import com.snj.travelvista.databinding.ItemFeaturedDestinationBinding
import com.snj.travelvista.ui.common.DestinationDiffCallback
import com.snj.travelvista.utils.ImageLoader

class FeaturedAdapter(
    private val onDestinationClick: (Destination) -> Unit
) : ListAdapter<Destination, FeaturedAdapter.FeaturedViewHolder>(DestinationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedViewHolder {
        val binding = ItemFeaturedDestinationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeaturedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeaturedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FeaturedViewHolder(
        private val binding: ItemFeaturedDestinationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(destination: Destination) {
            binding.tvTitle.text = destination.title
            binding.tvLocation.text = destination.location
            binding.tvCategory.text = destination.category
            binding.tvRating.text = destination.formattedRating
            binding.tvPrice.text = destination.formattedPrice

            ImageLoader.loadImage(binding.ivDestination, destination.imageUrl, destination.title, destination.category)

            binding.cardFeatured.setOnClickListener {
                onDestinationClick(destination)
            }
        }
    }
}
