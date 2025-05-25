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
    val porcentajeIngresos: Double,
    // NUEVOS CAMPOS PARA PRECIO/HORA
    val precioHoraPromedio: Double,
    val precioHoraMinimo: Double,
    val precioHoraMaximo: Double,
    val tiempoTotalMinutos: Double,
    val eficienciaPromedio: Double
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
    
    // NUEVAS PROPIEDADES PARA PRECIO/HORA
    val precioHoraPromedio: LiveData<Double>
    val precioHoraMinimo: LiveData<Double>
    val precioHoraMaximo: LiveData<Double>
    val tiempoTotalTrabajado: LiveData<Double> // en horas
    val eficienciaGeneral: LiveData<Double>
    
    // Estadísticas por tipo de servicio (actualizada)
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
        
        // NUEVOS CÁLCULOS PARA PRECIO/HORA
        precioHoraPromedio = serviciosFiltrados.map { servicios ->
            if (servicios.isNotEmpty()) {
                val preciosHora = servicios.map { it.precioHora }.filter { it > 0 }
                if (preciosHora.isNotEmpty()) preciosHora.average() else 0.0
            } else 0.0
        }
        
        precioHoraMinimo = serviciosFiltrados.map { servicios ->
            if (servicios.isNotEmpty()) {
                val preciosHora = servicios.map { it.precioHora }.filter { it > 0 }
                preciosHora.minOrNull() ?: 0.0
            } else 0.0
        }
        
        precioHoraMaximo = serviciosFiltrados.map { servicios ->
            if (servicios.isNotEmpty()) {
                val preciosHora = servicios.map { it.precioHora }.filter { it > 0 }
                preciosHora.maxOrNull() ?: 0.0
            } else 0.0
        }
        
        tiempoTotalTrabajado = serviciosFiltrados.map { servicios ->
            servicios.sumOf { it.minutosTotales } / 60.0 // convertir a horas
        }
        
        eficienciaGeneral = serviciosFiltrados.map { servicios ->
            if (servicios.isNotEmpty()) {
                // Calcular eficiencia como ratio tiempo productivo vs tiempo total
                val tiempoProductivo = servicios.sumOf { it.minutosTotales }
                val tiempoTotal = calcularTiempoTotalJornadas(servicios)
                if (tiempoTotal > 0) (tiempoProductivo / tiempoTotal) * 100 else 0.0
            } else 0.0
        }
        
        // Calcular estadísticas por tipo de servicio (ACTUALIZADA CON PRECIO/HORA)
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
                        
                        // NUEVOS CÁLCULOS PARA PRECIO/HORA POR TIPO
                        val preciosHora = serviciosDelTipo.map { it.precioHora }.filter { it > 0 }
                        val precioHoraPromedio = if (preciosHora.isNotEmpty()) preciosHora.average() else 0.0
                        val precioHoraMinimo = preciosHora.minOrNull() ?: 0.0
                        val precioHoraMaximo = preciosHora.maxOrNull() ?: 0.0
                        val tiempoTotalMinutos = serviciosDelTipo.sumOf { it.minutosTotales }
                        
                        // Eficiencia para este tipo de servicio
                        val tiempoTotalJornadasTipo = calcularTiempoTotalJornadasPorTipo(serviciosDelTipo)
                        val eficienciaPromedio = if (tiempoTotalJornadasTipo > 0) {
                            (tiempoTotalMinutos / tiempoTotalJornadasTipo) * 100
                        } else 0.0
                        
                        EstadisticaTipoServicio(
                            tipoServicio = tipoServicio,
                            cantidadServicios = cantidadServicios,
                            totalIngresos = totalIngresos,
                            totalKilometros = totalKilometros,
                            ingresoPromedio = ingresoPromedio,
                            kmPromedio = kmPromedio,
                            porcentajeIngresos = porcentajeIngresos,
                            precioHoraPromedio = precioHoraPromedio,
                            precioHoraMinimo = precioHoraMinimo,
                            precioHoraMaximo = precioHoraMaximo,
                            tiempoTotalMinutos = tiempoTotalMinutos,
                            eficienciaPromedio = eficienciaPromedio
                        )
                    }
                    .sortedByDescending { it.totalIngresos }
            }
        }
    }
    
    // NUEVO MÉTODO: Calcular tiempo total de jornadas
    private fun calcularTiempoTotalJornadas(servicios: List<Servicio>): Double {
        // Agrupar servicios por día
        val serviciosPorDia = servicios.groupBy { it.dia }
        
        var tiempoTotalJornadas = 0.0
        
        serviciosPorDia.forEach { (_, serviciosDelDia) ->
            if (serviciosDelDia.isNotEmpty()) {
                val serviciosOrdenados = serviciosDelDia.sortedBy { it.hora1 }
                val inicioJornada = serviciosOrdenados.first().inicioJornada ?: serviciosOrdenados.first().hora1
                val finJornada = serviciosOrdenados.last().finJornada ?: serviciosOrdenados.last().hora3
                
                val minutosJornada = java.time.Duration.between(inicioJornada, finJornada).toMinutes().toDouble()
                tiempoTotalJornadas += minutosJornada
            }
        }
        
        return tiempoTotalJornadas
    }
    
    // NUEVO MÉTODO: Calcular tiempo total de jornadas por tipo
    private fun calcularTiempoTotalJornadasPorTipo(serviciosDelTipo: List<Servicio>): Double {
        // Para simplificar, usamos la suma de minutos totales de cada servicio
        // En una implementación más compleja, calcularíamos el tiempo real de jornada por tipo
        return serviciosDelTipo.sumOf { it.minutosTotales }
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
        Log.d("ResumenViewModel", "Aplicando filtro $filtro. Fecha actual: $hoy. Total servicios: ${servicios.size}")
        
        val resultado = when (filtro) {
            FiltroTiempo.HOY -> {
                val filtrados = servicios.filter { it.dia == hoy }
                Log.d("ResumenViewModel", "Filtro HOY: ${filtrados.size} servicios encontrados")
                filtrados.forEach { 
                    Log.d("ResumenViewModel", "Servicio fecha: ${it.dia} (${if(it.dia == hoy) "COINCIDE" else "NO COINCIDE"})")
                }
                filtrados
            }
            FiltroTiempo.SEMANA -> {
                val inicioSemana = hoy.minusDays(hoy.dayOfWeek.value - 1L)
                val finSemana = inicioSemana.plusDays(6)
                Log.d("ResumenViewModel", "Filtro SEMANA: $inicioSemana a $finSemana")
                servicios.filter { 
                    !it.dia.isBefore(inicioSemana) && !it.dia.isAfter(finSemana) 
                }
            }
            FiltroTiempo.MES -> {
                val inicioMes = hoy.withDayOfMonth(1)
                val finMes = hoy.with(TemporalAdjusters.lastDayOfMonth())
                Log.d("ResumenViewModel", "Filtro MES: $inicioMes a $finMes")
                servicios.filter { 
                    !it.dia.isBefore(inicioMes) && !it.dia.isAfter(finMes) 
                }
            }
            FiltroTiempo.TODO -> {
                Log.d("ResumenViewModel", "Filtro TODO: mostrando todos los servicios")
                servicios
            }
        }
        
        Log.d("ResumenViewModel", "Resultado filtro: ${resultado.size} servicios")
        return resultado
    }
} 