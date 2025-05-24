package com.rueda.supertaxi.view

import android.os.Bundle
import android.util.Log
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
        
        Log.d("IngresosActivity", "onCreate iniciado")
        
        setupToolbar()
        setupChipGroup()
        setupObservers()
        setupListeners()
        
        // Forzar la carga inicial
        viewModel.cambiarFiltro(FiltroTiempo.HOY)
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
            Log.d("IngresosActivity", "Chip HOY seleccionado")
            viewModel.cambiarFiltro(FiltroTiempo.HOY)
        }
        
        binding.chipSemana.setOnClickListener { 
            Log.d("IngresosActivity", "Chip SEMANA seleccionado")
            viewModel.cambiarFiltro(FiltroTiempo.SEMANA)
        }
        
        binding.chipMes.setOnClickListener { 
            Log.d("IngresosActivity", "Chip MES seleccionado")
            viewModel.cambiarFiltro(FiltroTiempo.MES)
        }
        
        binding.chipTodo.setOnClickListener { 
            Log.d("IngresosActivity", "Chip TODO seleccionado")
            viewModel.cambiarFiltro(FiltroTiempo.TODO)
        }
    }
    
    private fun setupObservers() {
        val decimalFormat = DecimalFormat("#,##0.00")
        val decimalFormatKm = DecimalFormat("#,##0.0")
        
        // Observar cambios en el filtro seleccionado
        viewModel.filtroActual.observe(this) { filtro ->
            Log.d("IngresosActivity", "Filtro cambiado a: $filtro")
            
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
            Log.d("IngresosActivity", "Cantidad servicios: $cantidad")
            binding.tvTotalServicios.text = cantidad.toString()
            
            // Mostrar mensaje si no hay datos
            if (cantidad == 0) {
                binding.tvNoData.visibility = View.VISIBLE
                binding.cardEstadisticas.visibility = View.GONE
                Log.d("IngresosActivity", "No hay servicios - mostrando mensaje")
            } else {
                binding.tvNoData.visibility = View.GONE
                binding.cardEstadisticas.visibility = View.VISIBLE
                Log.d("IngresosActivity", "Hay servicios - mostrando estadísticas")
            }
        }
        
        viewModel.totalIngresos.observe(this) { ingresos ->
            Log.d("IngresosActivity", "Total ingresos: $ingresos")
            binding.tvTotalIngresos.text = "${decimalFormat.format(ingresos)}€"
        }
        
        viewModel.totalKilometros.observe(this) { kilometros ->
            Log.d("IngresosActivity", "Total kilómetros: $kilometros")
            binding.tvTotalKm.text = "${decimalFormatKm.format(kilometros)}km"
        }
        
        // Observar datos para calcular promedios
        viewModel.serviciosFiltrados.observe(this) { servicios ->
            Log.d("IngresosActivity", "Servicios filtrados: ${servicios.size}")
            
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
                
                Log.d("IngresosActivity", "Promedios calculados - Ingreso: $ingresoPromedio, Km: $kmPromedio, Tiempo: $tiempoPromedio")
            } else {
                binding.tvIngresoPromedio.text = "0.00€"
                binding.tvKmPromedio.text = "0.0 km"
                binding.tvTiempoPromedio.text = "0 min"
                
                Log.d("IngresosActivity", "No hay servicios - promedios en cero")
            }
        }
        
        // Observar todos los servicios para debug
        viewModel.allServicios.observe(this) { servicios ->
            Log.d("IngresosActivity", "Total servicios en BD: ${servicios.size}")
        }
    }
    
    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            Log.d("IngresosActivity", "Swipe refresh ejecutado")
            binding.swipeRefresh.isRefreshing = false
        }
    }
} 