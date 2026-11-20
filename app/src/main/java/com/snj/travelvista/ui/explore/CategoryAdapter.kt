package com.snj.travelvista.ui.explore

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snj.travelvista.R
import com.snj.travelvista.data.model.Category
import com.snj.travelvista.databinding.ItemCategoryBinding
import com.snj.travelvista.ui.common.CategoryDiffCallback

class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.tvCategoryName.text = category.name

            val iconRes = when (category.id) {
                "mountains" -> R.drawable.ic_mountain
                "beaches" -> R.drawable.ic_beach
                "cities" -> R.drawable.ic_city
                "nature" -> R.drawable.ic_nature
                "historical" -> R.drawable.ic_historical
                else -> R.drawable.ic_all
            }
            binding.ivCategoryIcon.setImageResource(iconRes)

            val context = binding.root.context
            val primaryColor = ContextCompat.getColor(context, R.color.primary)
            if (category.isSelected) {
                val onPrimaryColor = ContextCompat.getColor(context, R.color.on_primary)

                binding.layoutCategoryContent.setBackgroundColor(primaryColor)
                binding.tvCategoryName.setTextColor(onPrimaryColor)
                binding.ivCategoryIcon.imageTintList = ColorStateList.valueOf(onPrimaryColor)
                binding.cardCategory.strokeColor = primaryColor
                binding.cardCategory.strokeWidth = (1.5f * context.resources.displayMetrics.density).toInt()
            } else {
                val surfaceColor = ContextCompat.getColor(context, R.color.surface)
                val onSurfaceColor = ContextCompat.getColor(context, R.color.on_surface)
                val outlineColor = ContextCompat.getColor(context, R.color.outline)

                binding.layoutCategoryContent.setBackgroundColor(surfaceColor)
                binding.tvCategoryName.setTextColor(onSurfaceColor)
                binding.ivCategoryIcon.imageTintList = ColorStateList.valueOf(primaryColor)
                binding.cardCategory.strokeColor = outlineColor
                binding.cardCategory.strokeWidth = (1f * context.resources.displayMetrics.density).toInt()
            }

            binding.cardCategory.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }
}
