package com.rueda.supertaxi.view

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.rueda.supertaxi.adapter.EstadisticaTipoAdapter
import com.rueda.supertaxi.databinding.ActivityIngresosBinding
import com.rueda.supertaxi.viewmodel.FiltroTiempo
import com.rueda.supertaxi.viewmodel.ResumenViewModel
import java.text.DecimalFormat

class IngresosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIngresosBinding
    private val viewModel: ResumenViewModel by viewModels()
    private lateinit var estadisticasDetalladasAdapter: EstadisticaTipoAdapter
    
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
        estadisticasDetalladasAdapter = EstadisticaTipoAdapter()
        binding.recyclerEstadisticasDetalladas.apply {
            layoutManager = LinearLayoutManager(this@IngresosActivity)
            adapter = estadisticasDetalladasAdapter
            setHasFixedSize(true)
            Log.d("IngresosActivity", "RecyclerView de estadísticas detalladas configurado")
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
        
        // VERIFICAR QUE EL FORMATO FUNCIONE
        Log.d("IngresosActivity", "Test DecimalFormat - 15.5 = ${decimalFormat.format(15.5)}")
        Log.d("IngresosActivity", "Test DecimalFormatKm - 23.7 = ${decimalFormatKm.format(23.7)}")
        
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
                binding.cardEstadisticasDetalladas.visibility = View.GONE
                Log.d("IngresosActivity", "OCULTANDO tarjetas - cantidad = 0")
            } else {
                binding.tvNoData.visibility = View.GONE
                binding.cardEstadisticas.visibility = View.VISIBLE
                binding.cardEstadisticasDetalladas.visibility = View.VISIBLE
                Log.d("IngresosActivity", "MOSTRANDO tarjetas - cantidad = $cantidad")
                
                // VERIFICAR que las tarjetas estén realmente visibles
                Log.d("IngresosActivity", "Visibilidad cardEstadisticas: ${binding.cardEstadisticas.visibility}")
                Log.d("IngresosActivity", "Visibilidad cardEstadisticasDetalladas: ${binding.cardEstadisticasDetalladas.visibility}")
            }
        }
        
        viewModel.totalIngresos.observe(this) { ingresos ->
            Log.d("IngresosActivity", "Total ingresos recibido: $ingresos")
            val textoFormateado = "${decimalFormat.format(ingresos)}€"
            binding.tvTotalIngresos.text = textoFormateado
            Log.d("IngresosActivity", "tvTotalIngresos actualizado a: $textoFormateado")
            
            runOnUiThread {
                binding.tvTotalIngresos.requestLayout()
                Log.d("IngresosActivity", "Forzando actualización de tvTotalIngresos en hilo principal")
            }
        }
        
        viewModel.totalKilometros.observe(this) { kilometros ->
            Log.d("IngresosActivity", "Total kilómetros recibido: $kilometros")
            val textoFormateado = "${decimalFormatKm.format(kilometros)}km"
            binding.tvTotalKm.text = textoFormateado
            Log.d("IngresosActivity", "tvTotalKm actualizado a: $textoFormateado")
            
            runOnUiThread {
                binding.tvTotalKm.requestLayout()
                Log.d("IngresosActivity", "Forzando actualización de tvTotalKm en hilo principal")
            }
        }
        
        // Observer para precio por hora promedio (primera fila)
        viewModel.precioHoraPromedio.observe(this) { precioHora ->
            Log.d("IngresosActivity", "Precio hora promedio recibido: $precioHora")
            val textoFormateado = "${decimalFormat.format(precioHora)}€/h"
            binding.tvPrecioHoraPromedio.text = textoFormateado
            Log.d("IngresosActivity", "tvPrecioHoraPromedio actualizado a: $textoFormateado")
            
            runOnUiThread {
                binding.tvPrecioHoraPromedio.requestLayout()
                Log.d("IngresosActivity", "Forzando actualización de tvPrecioHoraPromedio en hilo principal")
            }
        }
        
        // Observer para tiempo total trabajado
        viewModel.tiempoTotalTrabajado.observe(this) { tiempoHoras ->
            Log.d("IngresosActivity", "Tiempo total trabajado recibido: $tiempoHoras horas")
            val textoFormateado = "${decimalFormat.format(tiempoHoras)}h"
            binding.tvTiempoTrabajado.text = textoFormateado
            Log.d("IngresosActivity", "tvTiempoTrabajado actualizado a: $textoFormateado")
            
            runOnUiThread {
                binding.tvTiempoTrabajado.requestLayout()
                Log.d("IngresosActivity", "Forzando actualización de tvTiempoTrabajado en hilo principal")
            }
        }
        
        // Observer para eficiencia general
        viewModel.eficienciaGeneral.observe(this) { eficiencia ->
            Log.d("IngresosActivity", "Eficiencia general recibida: $eficiencia")
            val textoFormateado = "${eficiencia.toInt()}%"
            binding.tvEficienciaGeneral.text = textoFormateado
            Log.d("IngresosActivity", "tvEficienciaGeneral actualizado a: $textoFormateado")
            
            runOnUiThread {
                binding.tvEficienciaGeneral.requestLayout()
                Log.d("IngresosActivity", "Forzando actualización de tvEficienciaGeneral en hilo principal")
            }
        }
        
        // Observer para precio por hora mínimo
        viewModel.precioHoraMinimo.observe(this) { precioMin ->
            Log.d("IngresosActivity", "Precio hora mínimo recibido: $precioMin")
            val textoFormateado = "${decimalFormat.format(precioMin)}€/h"
            binding.tvPrecioHoraMinimo.text = textoFormateado
            Log.d("IngresosActivity", "tvPrecioHoraMinimo actualizado a: $textoFormateado")
            
            runOnUiThread {
                binding.tvPrecioHoraMinimo.requestLayout()
                Log.d("IngresosActivity", "Forzando actualización de tvPrecioHoraMinimo en hilo principal")
            }
        }
        
        // Observer para precio por hora máximo
        viewModel.precioHoraMaximo.observe(this) { precioMax ->
            Log.d("IngresosActivity", "Precio hora máximo recibido: $precioMax")
            val textoFormateado = "${decimalFormat.format(precioMax)}€/h"
            binding.tvPrecioHoraMaximo.text = textoFormateado
            Log.d("IngresosActivity", "tvPrecioHoraMaximo actualizado a: $textoFormateado")
            
            runOnUiThread {
                binding.tvPrecioHoraMaximo.requestLayout()
                Log.d("IngresosActivity", "Forzando actualización de tvPrecioHoraMaximo en hilo principal")
            }
        }
        
        // Observer para rango de precio por hora (máximo - mínimo)
        viewModel.precioHoraMaximo.observe(this) { precioMax ->
            viewModel.precioHoraMinimo.observe(this) { precioMin ->
                val rango = precioMax - precioMin
                Log.d("IngresosActivity", "Rango precio hora calculado: $rango (Max: $precioMax - Min: $precioMin)")
                val textoFormateado = "${decimalFormat.format(rango)}€/h"
                binding.tvRangoPrecioHora.text = textoFormateado
                Log.d("IngresosActivity", "tvRangoPrecioHora actualizado a: $textoFormateado")
                
                runOnUiThread {
                    binding.tvRangoPrecioHora.requestLayout()
                    Log.d("IngresosActivity", "Forzando actualización de tvRangoPrecioHora en hilo principal")
                }
            }
        }
        
        // Observar estadísticas detalladas por tipo
        viewModel.estadisticasPorTipo.observe(this) { estadisticas ->
            Log.d("IngresosActivity", "Estadísticas detalladas recibidas: ${estadisticas.size} tipos")
            
            if (estadisticas.isNotEmpty()) {
                estadisticasDetalladasAdapter.submitList(estadisticas) {
                    Log.d("IngresosActivity", "Lista de estadísticas detalladas actualizada")
                    binding.recyclerEstadisticasDetalladas.requestLayout()
                }
                
                // Log para debug de cada estadística
                estadisticas.forEach { estadistica ->
                    Log.d("IngresosActivity", """
                        Estadística detallada:
                        - Tipo: ${estadistica.tipoServicio}
                        - Servicios: ${estadistica.cantidadServicios}
                        - Ingresos: ${estadistica.totalIngresos}€
                        - Porcentaje: ${estadistica.porcentajeIngresos}%
                        - Km totales: ${estadistica.totalKilometros} km
                        - Promedio ingresos: ${estadistica.ingresoPromedio}€
                        - Promedio km: ${estadistica.kmPromedio} km
                    """.trimIndent())
                }
            } else {
                Log.d("IngresosActivity", "No hay estadísticas detalladas para mostrar")
                estadisticasDetalladasAdapter.submitList(emptyList())
            }
        }
        
        // DEBUG: Observar TODOS los servicios en la base de datos
        viewModel.allServicios.observe(this) { servicios ->
            Log.d("IngresosActivity", "=== DEBUG TODOS LOS SERVICIOS EN BD ===")
            Log.d("IngresosActivity", "Total servicios en BD: ${servicios.size}")
            
            servicios.forEach { servicio ->
                Log.d("IngresosActivity", "BD - ID: ${servicio.id}, Fecha: ${servicio.dia}, Importe: ${servicio.importe}€, Tipo: ${servicio.tipoServicio}")
            }
            Log.d("IngresosActivity", "=== FIN DEBUG BD ===")
        }
        
        // DEBUG: Observar datos para calcular promedios generales
        viewModel.serviciosFiltrados.observe(this) { servicios ->
            Log.d("IngresosActivity", "=== DEBUG SERVICIOS FILTRADOS ===")
            Log.d("IngresosActivity", "Cantidad servicios filtrados: ${servicios.size}")
            Log.d("IngresosActivity", "Filtro actual: ${viewModel.filtroActual.value}")
            
            // Log detallado de cada servicio filtrado
            servicios.forEachIndexed { index, servicio ->
                Log.d("IngresosActivity", """
                    --- Servicio filtrado $index ---
                    ID: ${servicio.id}
                    Día: ${servicio.dia}
                    Tipo: ${servicio.tipoServicio}
                    Importe: ${servicio.importe}€
                    Minutos totales: ${servicio.minutosTotales}
                    Precio por hora: ${servicio.precioHora}€
                    Km totales: ${servicio.kmTotales}
                    Comisión: ${servicio.comision}€
                """.trimIndent())
            }
            
            if (servicios.isNotEmpty()) {
                // Calcular ingreso promedio general
                val ingresoPromedio = servicios.map { it.importe }.average()
                Log.d("IngresosActivity", "ANTES de actualizar UI - Ingreso promedio: $ingresoPromedio")
                
                binding.tvIngresoPromedio.text = "${decimalFormat.format(ingresoPromedio)}€"
                Log.d("IngresosActivity", "DESPUÉS de actualizar tvIngresoPromedio: ${binding.tvIngresoPromedio.text}")
                
                // Calcular km promedio general
                val kmPromedio = servicios.map { it.kmTotales }.average()
                Log.d("IngresosActivity", "ANTES de actualizar UI - Km promedio: $kmPromedio")
                
                binding.tvKmPromedio.text = "${decimalFormatKm.format(kmPromedio)} km"
                Log.d("IngresosActivity", "DESPUÉS de actualizar tvKmPromedio: ${binding.tvKmPromedio.text}")
                
                // Calcular tiempo promedio general
                val tiempoPromedio = servicios.map { it.minutosTotales }.average()
                Log.d("IngresosActivity", "ANTES de actualizar UI - Tiempo promedio: $tiempoPromedio")
                
                binding.tvTiempoPromedio.text = "${tiempoPromedio.toInt()} min"
                Log.d("IngresosActivity", "DESPUÉS de actualizar tvTiempoPromedio: ${binding.tvTiempoPromedio.text}")
                
                // FORZAR ACTUALIZACIÓN DE LA UI
                runOnUiThread {
                    binding.tvIngresoPromedio.invalidate()
                    binding.tvKmPromedio.invalidate()
                    binding.tvTiempoPromedio.invalidate()
                    Log.d("IngresosActivity", "UI forzada a actualizarse en hilo principal")
                }
                
            } else {
                Log.d("IngresosActivity", "Lista vacía - poniendo valores en cero")
                binding.tvIngresoPromedio.text = "0.00€"
                binding.tvKmPromedio.text = "0.0 km"
                binding.tvTiempoPromedio.text = "0 min"
                
                runOnUiThread {
                    binding.tvIngresoPromedio.invalidate()
                    binding.tvKmPromedio.invalidate()
                    binding.tvTiempoPromedio.invalidate()
                    Log.d("IngresosActivity", "UI forzada a actualizarse (valores en cero) en hilo principal")
                }
            }
            
            Log.d("IngresosActivity", "=== FIN DEBUG SERVICIOS FILTRADOS ===")
        }
    }
    
    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            Log.d("IngresosActivity", "Swipe refresh ejecutado")
            binding.swipeRefresh.isRefreshing = false
        }
    }
} 