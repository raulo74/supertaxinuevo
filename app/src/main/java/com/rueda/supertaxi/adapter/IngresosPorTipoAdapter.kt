package com.rueda.supertaxi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rueda.supertaxi.databinding.ItemIngresoPorTipoBinding
import java.text.DecimalFormat

class IngresosPorTipoAdapter : ListAdapter<Pair<String, Double>, IngresosPorTipoAdapter.IngresoViewHolder>(IngresoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngresoViewHolder {
        val binding = ItemIngresoPorTipoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IngresoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IngresoViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class IngresoViewHolder(private val binding: ItemIngresoPorTipoBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: Pair<String, Double>) {
            val decimalFormat = DecimalFormat("#,##0.00")
            
            // Establecer el nombre del tipo de servicio
            binding.tvTipoServicio.text = item.first
            
            // Establecer el ingreso total con formato
            binding.tvIngresoTotal.text = "${decimalFormat.format(item.second)}€"
            
            // Color diferente según el tipo de servicio
            when (item.first) {
                "Parada de taxis" -> {
                    binding.viewIndicadorColor.setBackgroundColor(
                        binding.root.context.getColor(android.R.color.holo_blue_light)
                    )
                }
                "Mano Alzada" -> {
                    binding.viewIndicadorColor.setBackgroundColor(
                        binding.root.context.getColor(android.R.color.holo_green_light)
                    )
                }
                else -> {
                    binding.viewIndicadorColor.setBackgroundColor(
                        binding.root.context.getColor(android.R.color.holo_orange_light)
                    )
                }
            }
        }
    }
}

class IngresoDiffCallback : DiffUtil.ItemCallback<Pair<String, Double>>() {
    override fun areItemsTheSame(oldItem: Pair<String, Double>, newItem: Pair<String, Double>): Boolean {
        return oldItem.first == newItem.first
    }

    override fun areContentsTheSame(oldItem: Pair<String, Double>, newItem: Pair<String, Double>): Boolean {
        return oldItem == newItem
    }
} 