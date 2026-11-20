package com.snj.travelvista.ui.trips

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snj.travelvista.data.local.entity.TripEntity
import com.snj.travelvista.databinding.ItemTripBinding
import com.snj.travelvista.utils.GeoUtils
import kotlin.math.roundToInt

class TripAdapter(
    private val preferredCurrencyProvider: () -> String,
    private val exchangeRateProvider: () -> Double,
    private val onTripClick: (TripEntity) -> Unit,
    private val onDeleteClick: (TripEntity) -> Unit
) : ListAdapter<TripEntity, TripAdapter.TripViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TripViewHolder(
        private val binding: ItemTripBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(trip: TripEntity) {
            binding.tvTripName.text = trip.name
            binding.tvTripDestination.text = trip.destinationTitle
            binding.tvTripDates.text = "${trip.startDate} → ${trip.endDate} · ${trip.travelersCount} traveler(s)"

            val currency = preferredCurrencyProvider()
            val rate = exchangeRateProvider()
            binding.tvTripBudget.text = if (trip.estimatedBudgetUsd > 0) {
                val converted = (trip.estimatedBudgetUsd * rate).roundToInt()
                "Est. Budget: ${GeoUtils.currencySymbol(currency)}%,d".format(converted)
            } else {
                "Budget: Open"
            }

            binding.tvTripNotes.text = trip.notes.ifBlank {
                "Tap to manage day-wise itinerary, hotels, flights & share"
            }

            binding.root.setOnClickListener { onTripClick(trip) }
            binding.btnDeleteTrip.setOnClickListener { onDeleteClick(trip) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TripEntity>() {
            override fun areItemsTheSame(a: TripEntity, b: TripEntity) = a.id == b.id
            override fun areContentsTheSame(a: TripEntity, b: TripEntity) = a == b
        }
    }
}
