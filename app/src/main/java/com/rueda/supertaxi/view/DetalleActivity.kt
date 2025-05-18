package com.rueda.supertaxi.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.rueda.supertaxi.databinding.ActivityDetalleBinding
import com.rueda.supertaxi.viewmodel.DetalleViewModel
import java.text.DecimalFormat

class DetalleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetalleBinding
    private lateinit var viewModel: DetalleViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[DetalleViewModel::class.java]
        
        // Obtener el ID del servicio de los extras
        val servicioId = intent.getLongExtra("SERVICIO_ID", -1)
        if (servicioId != -1L) {
            viewModel.loadServicio(servicioId)
        }
        
        setupObservers()
        setupListeners()
    }
    
    private fun setupObservers() {
        viewModel.servicio.observe(this) { servicio ->
            servicio?.let {
                // Formatear números para visualización
                val decimalFormat = DecimalFormat("#,##0.00")
                
                // Mostrar datos en la UI usando los IDs correctos
                binding.tvDia.text = it.dia.toString()
                binding.tvTipoServicio.text = it.tipoServicio
                binding.tvHora1.text = it.hora1.toString()
                binding.tvKm1.text = "${decimalFormat.format(it.km1)} km"
                binding.tvCalle1.text = it.calle1
                binding.tvHora2.text = it.hora2.toString()
                binding.tvKm2.text = "${decimalFormat.format(it.km2)} km"
                binding.tvHora3.text = it.hora3.toString()
                binding.tvCalle2.text = it.calle2
                binding.tvCalle3.text = it.calle3
                binding.tvImporte.text = "${decimalFormat.format(it.importe)} €"
                binding.tvComision.text = "${decimalFormat.format(it.comision)} €"
                binding.tvTipoPago.text = it.tipoPago
                binding.tvPorcentaje.text = "${decimalFormat.format(it.porcentaje)} %"
                binding.tvMinutosTotales.text = "${decimalFormat.format(it.minutosTotales)} minutos"
                binding.tvPrecioHora.text = "${decimalFormat.format(it.precioHora)} €/hora"
                binding.tvKmTotales.text = "${decimalFormat.format(it.kmTotales)} km"
                binding.tvPrecioKm.text = "${decimalFormat.format(it.precioKm)} €/km"
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnSiguienteServicio.setOnClickListener {
            Log.d("DetalleActivity", "Botón Siguiente Servicio pulsado, enviando señal de reinicio")
            val intent = Intent()
            intent.putExtra("REINICIAR_SERVICIO", true)
            setResult(RESULT_OK, intent)
            finish()
        }
    }
} 