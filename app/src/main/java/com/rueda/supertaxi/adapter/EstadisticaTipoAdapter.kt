package com.rueda.supertaxi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rueda.supertaxi.databinding.ItemEstadisticaTipoBinding
import com.rueda.supertaxi.viewmodel.EstadisticaTipoServicio
import java.text.DecimalFormat

class EstadisticaTipoAdapter : ListAdapter<EstadisticaTipoServicio, EstadisticaTipoAdapter.EstadisticaTipoViewHolder>(EstadisticaTipoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EstadisticaTipoViewHolder {
        val binding = ItemEstadisticaTipoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EstadisticaTipoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EstadisticaTipoViewHolder, position: Int) {
        val estadistica = getItem(position)
        holder.bind(estadistica)
    }

    inner class EstadisticaTipoViewHolder(private val binding: ItemEstadisticaTipoBinding) : 
            RecyclerView.ViewHolder(binding.root) {
        
        fun bind(estadistica: EstadisticaTipoServicio) {
            val decimalFormat = DecimalFormat("#,##0.00")
            val decimalFormatKm = DecimalFormat("#,##0.0")
            
            binding.tvTipoServicio.text = estadistica.tipoServicio
            binding.tvCantidadServicios.text = estadistica.cantidadServicios.toString()
            binding.tvTotalIngresos.text = "${decimalFormat.format(estadistica.totalIngresos)}€"
            binding.tvTotalKilometros.text = "${decimalFormatKm.format(estadistica.totalKilometros)} km"
            binding.tvIngresoPromedio.text = "${decimalFormat.format(estadistica.ingresoPromedio)}€"
            binding.tvKmPromedio.text = "${decimalFormatKm.format(estadistica.kmPromedio)} km"
        }
    }
}

class EstadisticaTipoDiffCallback : DiffUtil.ItemCallback<EstadisticaTipoServicio>() {
    override fun areItemsTheSame(oldItem: EstadisticaTipoServicio, newItem: EstadisticaTipoServicio): Boolean {
        return oldItem.tipoServicio == newItem.tipoServicio
    }

    override fun areContentsTheSame(oldItem: EstadisticaTipoServicio, newItem: EstadisticaTipoServicio): Boolean {
        return oldItem == newItem
    }
} 