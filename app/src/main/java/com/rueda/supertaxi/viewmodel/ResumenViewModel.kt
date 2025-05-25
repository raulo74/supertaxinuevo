package com.rueda.supertaxi.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.rueda.supertaxi.R
import com.rueda.supertaxi.database.AppDatabase
import com.rueda.supertaxi.model.Servicio
import com.rueda.supertaxi.repository.ServicioRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class FiltroTiempo {
    HOY, SEMANA, MES, TODO
}

// Clase para estadísticas por tipo de servicio
data class EstadisticaTipoServicio(
    val tipoServicio: String,
    val cantidadServicios: Int,
    val totalIngresos: Double,
    val totalKilometros: Double,
    val ingresoPromedio: Double,
    val kmPromedio: Double,
    val porcentajeIngresos: Double
)

class ResumenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ServicioRepository
    val allServicios: LiveData<List<Servicio>>
    
    private val _filtroActual = MutableLiveData<FiltroTiempo>(FiltroTiempo.HOY)
    val filtroActual: LiveData<FiltroTiempo> = _filtroActual
    
    val serviciosFiltrados: LiveData<List<Servicio>>
    
    val cantidadServicios: LiveData<Int>
    val totalIngresos: LiveData<Double>
    val totalKilometros: LiveData<Double>
    
    // Estadísticas por tipo de servicio
    val estadisticasPorTipo: LiveData<List<EstadisticaTipoServicio>>
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = ServicioRepository(database.servicioDao(), application.applicationContext)
        allServicios = repository.allServicios
        
        serviciosFiltrados = _filtroActual.switchMap { filtro ->
            allServicios.map { servicios ->
                aplicarFiltro(servicios, filtro)
            }
        }
        
        cantidadServicios = serviciosFiltrados.map { servicios -> 
            servicios.size 
        }
        
        totalIngresos = serviciosFiltrados.map { servicios ->
            servicios.sumOf { it.importe }
        }
        
        totalKilometros = serviciosFiltrados.map { servicios ->
            servicios.sumOf { it.kmTotales }
        }
        
        // Calcular estadísticas por tipo de servicio
        estadisticasPorTipo = serviciosFiltrados.map { servicios ->
            if (servicios.isEmpty()) {
                emptyList()
            } else {
                // Calcular el total de ingresos de todos los servicios para el porcentaje
                val totalIngresosGeneral = servicios.sumOf { it.importe }
                
                servicios.groupBy { it.tipoServicio }
                    .map { (tipoServicio, serviciosDelTipo) ->
                        val cantidadServicios = serviciosDelTipo.size
                        val totalIngresos = serviciosDelTipo.sumOf { it.importe }
                        val totalKilometros = serviciosDelTipo.sumOf { it.kmTotales }
                        val ingresoPromedio = if (cantidadServicios > 0) totalIngresos / cantidadServicios else 0.0
                        val kmPromedio = if (cantidadServicios > 0) totalKilometros / cantidadServicios else 0.0
                        
                        // Calcular porcentaje sobre el total
                        val porcentajeIngresos = if (totalIngresosGeneral > 0) {
                            (totalIngresos / totalIngresosGeneral) * 100
                        } else {
                            0.0
                        }
                        
                        EstadisticaTipoServicio(
                            tipoServicio = tipoServicio,
                            cantidadServicios = cantidadServicios,
                            totalIngresos = totalIngresos,
                            totalKilometros = totalKilometros,
                            ingresoPromedio = ingresoPromedio,
                            kmPromedio = kmPromedio,
                            porcentajeIngresos = porcentajeIngresos
                        )
                    }
                    .sortedByDescending { it.totalIngresos }
            }
        }
    }
    
    fun cambiarFiltro(filtro: FiltroTiempo) {
        _filtroActual.value = filtro
    }
    
    fun eliminarServicio(servicio: Servicio) {
        viewModelScope.launch {
            repository.delete(servicio)
        }
    }
    
    fun obtenerTextoFiltro(): String {
        val context = getApplication<Application>()
        val filtroTexto = when (_filtroActual.value) {
            FiltroTiempo.HOY -> context.getString(R.string.filtro_hoy)
            FiltroTiempo.SEMANA -> context.getString(R.string.filtro_semana)
            FiltroTiempo.MES -> context.getString(R.string.filtro_mes)
            FiltroTiempo.TODO -> context.getString(R.string.filtro_todo)
            else -> context.getString(R.string.filtro_hoy)
        }
        return context.getString(R.string.servicio_filtrado, filtroTexto)
    }
    
    private fun aplicarFiltro(servicios: List<Servicio>, filtro: FiltroTiempo): List<Servicio> {
        val hoy = LocalDate.now()
        
        return when (filtro) {
            FiltroTiempo.HOY -> {
                servicios.filter { it.dia == hoy }
            }
            FiltroTiempo.SEMANA -> {
                val inicioSemana = hoy.minusDays(hoy.dayOfWeek.value - 1L)
                val finSemana = inicioSemana.plusDays(6)
                servicios.filter { 
                    !it.dia.isBefore(inicioSemana) && !it.dia.isAfter(finSemana) 
                }
            }
            FiltroTiempo.MES -> {
                val inicioMes = hoy.withDayOfMonth(1)
                val finMes = hoy.with(TemporalAdjusters.lastDayOfMonth())
                servicios.filter { 
                    !it.dia.isBefore(inicioMes) && !it.dia.isAfter(finMes) 
                }
            }
            FiltroTiempo.TODO -> {
                servicios
            }
        }
    }
} 