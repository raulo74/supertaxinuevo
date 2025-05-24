package com.rueda.supertaxi.view

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.rueda.supertaxi.adapter.IngresosPorTipoAdapter
import com.rueda.supertaxi.databinding.ActivityIngresosBinding
import com.rueda.supertaxi.viewmodel.FiltroTiempo
import com.rueda.supertaxi.viewmodel.ResumenViewModel
import java.text.DecimalFormat

class IngresosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIngresosBinding
    private val viewModel: ResumenViewModel by viewModels()
    private lateinit var ingresosPorTipoAdapter: IngresosPorTipoAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIngresosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Log.d("IngresosActivity", "onCreate iniciado")
        
        setupToolbar()
        setupRecyclerView()
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
    
    private fun setupRecyclerView() {
        ingresosPorTipoAdapter = IngresosPorTipoAdapter()
        binding.recyclerIngresosPorTipo.apply {
            layoutManager = LinearLayoutManager(this@IngresosActivity)
            adapter = ingresosPorTipoAdapter
            setHasFixedSize(true)
            Log.d("IngresosActivity", "RecyclerView configurado")
            
            // Forzar la actualización del layout
            requestLayout()
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
                binding.cardIngresosPorTipo.visibility = View.GONE
                Log.d("IngresosActivity", "No hay servicios - mostrando mensaje")
            } else {
                binding.tvNoData.visibility = View.GONE
                binding.cardEstadisticas.visibility = View.VISIBLE
                binding.cardIngresosPorTipo.visibility = View.VISIBLE
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
        
        // Observar ingresos por tipo
        viewModel.ingresosPorTipoServicio.observe(this) { ingresosPorTipo ->
            Log.d("IngresosActivity", "Ingresos por tipo recibidos: ${ingresosPorTipo.size} tipos")
            Log.d("IngresosActivity", "Contenido del mapa: $ingresosPorTipo")
            
            if (ingresosPorTipo.isNotEmpty()) {
                // Convertir el mapa a lista de pares para el adapter
                val listaIngresos = ingresosPorTipo.entries.map { entry ->
                    Pair(entry.key, entry.value)
                }.sortedByDescending { it.second } // Ordenar por ingresos de mayor a menor
                
                Log.d("IngresosActivity", "Lista de ingresos a enviar al adapter: $listaIngresos")
                ingresosPorTipoAdapter.submitList(listaIngresos) {
                    Log.d("IngresosActivity", "Lista actualizada en el adapter")
                    binding.recyclerIngresosPorTipo.requestLayout()
                }
                
                // Log para debug
                listaIngresos.forEach { (tipo, total) ->
                    Log.d("IngresosActivity", "Tipo: $tipo, Total: $total")
                }
            } else {
                Log.d("IngresosActivity", "No hay ingresos por tipo para mostrar")
                ingresosPorTipoAdapter.submitList(emptyList()) {
                    Log.d("IngresosActivity", "Lista vacía actualizada en el adapter")
                    binding.recyclerIngresosPorTipo.requestLayout()
                }
            }
        }
        
        // Observar estadísticas por tipo
        viewModel.estadisticasPorTipo.observe(this) { estadisticas ->
            Log.d("IngresosActivity", "Estadísticas por tipo recibidas: ${estadisticas.size} tipos")
            
            if (estadisticas.isNotEmpty()) {
                // Log para debug
                estadisticas.forEach { estadistica ->
                    Log.d("IngresosActivity", """
                        Tipo: ${estadistica.tipoServicio}
                        Cantidad: ${estadistica.cantidadServicios}
                        Total Ingresos: ${estadistica.totalIngresos}
                        Total Km: ${estadistica.totalKilometros}
                        Promedio Ingresos: ${estadistica.ingresoPromedio}
                        Promedio Km: ${estadistica.kmPromedio}
                    """.trimIndent())
                }
            } else {
                Log.d("IngresosActivity", "No hay estadísticas por tipo para mostrar")
            }
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