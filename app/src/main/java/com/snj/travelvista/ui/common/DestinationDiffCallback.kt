package com.snj.travelvista.ui.common

import androidx.recyclerview.widget.DiffUtil
import com.snj.travelvista.data.model.Destination

class DestinationDiffCallback : DiffUtil.ItemCallback<Destination>() {

    override fun areItemsTheSame(oldItem: Destination, newItem: Destination): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Destination, newItem: Destination): Boolean {
        return oldItem == newItem
    }
}
