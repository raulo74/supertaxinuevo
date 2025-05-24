package com.rueda.supertaxi.view

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.rueda.supertaxi.databinding.ActivityIngresosBinding
import com.rueda.supertaxi.viewmodel.FiltroTiempo
import com.rueda.supertaxi.viewmodel.ResumenViewModel
import java.text.DecimalFormat

class IngresosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIngresosBinding
    private val viewModel: ResumenViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIngresosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupChipGroup()
        setupObservers()
        setupListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupChipGroup() {
        binding.chipHoy.setOnClickListener { 
            viewModel.cambiarFiltro(FiltroTiempo.HOY)
        }
        
        binding.chipSemana.setOnClickListener { 
            viewModel.cambiarFiltro(FiltroTiempo.SEMANA)
        }
        
        binding.chipMes.setOnClickListener { 
            viewModel.cambiarFiltro(FiltroTiempo.MES)
        }
        
        binding.chipTodo.setOnClickListener { 
            viewModel.cambiarFiltro(FiltroTiempo.TODO)
        }
    }
    
    private fun setupObservers() {
        val decimalFormat = DecimalFormat("#,##0.00")
        val decimalFormatKm = DecimalFormat("#,##0.0")
        
        // Observar cambios en el filtro seleccionado
        viewModel.filtroActual.observe(this) { filtro ->
            when (filtro) {
                FiltroTiempo.HOY -> binding.chipHoy.isChecked = true
                FiltroTiempo.SEMANA -> binding.chipSemana.isChecked = true
                FiltroTiempo.MES -> binding.chipMes.isChecked = true
                FiltroTiempo.TODO -> binding.chipTodo.isChecked = true
            }
            
            binding.tvFiltroAplicado.text = viewModel.obtenerTextoFiltro()
        }
        
        // Observar estadísticas principales
        viewModel.cantidadServicios.observe(this) { cantidad ->
            binding.tvTotalServicios.text = cantidad.toString()
            
            // Mostrar mensaje si no hay datos
            if (cantidad == 0) {
                binding.tvNoData.visibility = View.VISIBLE
                binding.cardEstadisticas.visibility = View.GONE
            } else {
                binding.tvNoData.visibility = View.GONE
                binding.cardEstadisticas.visibility = View.VISIBLE
            }
        }
        
        viewModel.totalIngresos.observe(this) { ingresos ->
            binding.tvTotalIngresos.text = "${decimalFormat.format(ingresos)}€"
        }
        
        viewModel.totalKilometros.observe(this) { kilometros ->
            binding.tvTotalKm.text = "${decimalFormatKm.format(kilometros)}km"
        }
        
        // Observar datos para calcular promedios
        viewModel.serviciosFiltrados.observe(this) { servicios ->
            if (servicios.isNotEmpty()) {
                // Calcular ingreso promedio
                val ingresoPromedio = servicios.map { it.importe }.average()
                binding.tvIngresoPromedio.text = "${decimalFormat.format(ingresoPromedio)}€"
                
                // Calcular km promedio
                val kmPromedio = servicios.map { it.kmTotales }.average()
                binding.tvKmPromedio.text = "${decimalFormatKm.format(kmPromedio)} km"
                
                // Calcular tiempo promedio
                val tiempoPromedio = servicios.map { it.minutosTotales }.average()
                binding.tvTiempoPromedio.text = "${tiempoPromedio.toInt()} min"
            } else {
                binding.tvIngresoPromedio.text = "0.00€"
                binding.tvKmPromedio.text = "0.0 km"
                binding.tvTiempoPromedio.text = "0 min"
            }
        }
    }
    
    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }
    }
} 