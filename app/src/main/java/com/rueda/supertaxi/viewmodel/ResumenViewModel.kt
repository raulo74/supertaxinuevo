package com.rueda.supertaxi.viewmodel

import android.app.Application
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
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class FiltroTiempo {
    HOY, SEMANA, MES, TODO
}

class ResumenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ServicioRepository
    private val allServicios: LiveData<List<Servicio>>
    
    private val _filtroActual = MutableLiveData<FiltroTiempo>(FiltroTiempo.HOY)
    val filtroActual: LiveData<FiltroTiempo> = _filtroActual
    
    val serviciosFiltrados: LiveData<List<Servicio>>
    
    val cantidadServicios: LiveData<Int>
    val totalIngresos: LiveData<Double>
    val totalKilometros: LiveData<Double>
    
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
            var total = 0.0
            for (servicio in servicios) {
                total += servicio.importe
            }
            total
        }
        
        totalKilometros = serviciosFiltrados.map { servicios ->
            var total = 0.0
            for (servicio in servicios) {
                total += servicio.kmTotales
            }
            total
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