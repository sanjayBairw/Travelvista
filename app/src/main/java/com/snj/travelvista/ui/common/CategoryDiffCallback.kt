package com.snj.travelvista.ui.common

import androidx.recyclerview.widget.DiffUtil
import com.snj.travelvista.data.model.Category

class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {

    override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem == newItem && oldItem.isSelected == newItem.isSelected
    }
}
