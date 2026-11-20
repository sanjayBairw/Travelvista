package com.snj.travelvista.ui.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snj.travelvista.data.local.entity.RecentDestinationEntity
import com.snj.travelvista.databinding.ItemRecentDestinationBinding
import com.snj.travelvista.utils.ImageLoader

class RecentDestinationAdapter(
    private val onClick: (RecentDestinationEntity) -> Unit
) : ListAdapter<RecentDestinationEntity, RecentDestinationAdapter.RecentViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val binding = ItemRecentDestinationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecentViewHolder(
        private val binding: ItemRecentDestinationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecentDestinationEntity) {
            binding.tvRecentTitle.text = item.title
            binding.tvRecentLocation.text = item.location
            ImageLoader.loadImage(binding.ivRecentImage, item.imageUrl)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RecentDestinationEntity>() {
            override fun areItemsTheSame(a: RecentDestinationEntity, b: RecentDestinationEntity) =
                a.destinationId == b.destinationId

            override fun areContentsTheSame(a: RecentDestinationEntity, b: RecentDestinationEntity) =
                a == b
        }
    }
}
