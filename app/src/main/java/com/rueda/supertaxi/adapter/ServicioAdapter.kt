package com.rueda.supertaxi.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rueda.supertaxi.R
import com.rueda.supertaxi.databinding.ItemServicioBinding
import com.rueda.supertaxi.model.Servicio
import java.text.DecimalFormat
import java.time.Duration
import java.time.format.DateTimeFormatter

class ServicioAdapter(
    private val onItemClick: (Servicio) -> Unit,
    private val onDetallesClick: (Servicio) -> Unit,
    private val onEliminarClick: (Servicio) -> Unit
) : ListAdapter<Servicio, ServicioAdapter.ServicioViewHolder>(ServicioDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicioViewHolder {
        val binding = ItemServicioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ServicioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServicioViewHolder, position: Int) {
        val servicio = getItem(position)
        holder.bind(servicio)
    }

    inner class ServicioViewHolder(private val binding: ItemServicioBinding) : 
            RecyclerView.ViewHolder(binding.root) {
        
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            
            binding.btnDetalles.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDetallesClick(getItem(position))
                }
            }
            
            binding.btnEliminar.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEliminarClick(getItem(position))
                }
            }
        }
        
        fun bind(servicio: Servicio) {
            val decimalFormat = DecimalFormat("#,##0.00")
            val decimalFormatKm = DecimalFormat("#,##0.0")
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            
            // Formatear fecha y hora
            val fechaStr = servicio.dia.format(dateFormatter)
            val horaStr = servicio.hora2.format(timeFormatter)
            binding.tvFecha.text = "$fechaStr - $horaStr"
            
            // Tipo de servicio
            binding.chipTipoServicio.text = servicio.tipoServicio
            
            // Direcciones
            binding.tvOrigen.text = servicio.calle2
            binding.tvDestino.text = servicio.calle3
            
            // Duración
            val minutos = calcularDuracion(servicio)
            binding.tvDuracion.text = binding.root.context.getString(
                R.string.minutos, 
                minutos.toInt()
            )
            
            // Importe
            binding.tvImporte.text = binding.root.context.getString(
                R.string.euros,
                servicio.importe
            )
            
            // Tipo de pago
            binding.tvTipoPago.text = servicio.tipoPago
            
            // Distancia
            binding.tvDistancia.text = binding.root.context.getString(
                R.string.kilometros,
                servicio.km2
            )
        }
        
        private fun calcularDuracion(servicio: Servicio): Long {
            return Duration.between(servicio.hora2, servicio.hora3).toMinutes()
        }
    }
}

class ServicioDiffCallback : DiffUtil.ItemCallback<Servicio>() {
    override fun areItemsTheSame(oldItem: Servicio, newItem: Servicio): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Servicio, newItem: Servicio): Boolean {
        return oldItem == newItem
    }
} 