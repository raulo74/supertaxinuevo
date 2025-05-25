package com.rueda.supertaxi.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import com.rueda.supertaxi.database.ServicioDao
import com.rueda.supertaxi.model.Servicio
import com.rueda.supertaxi.model.JornadaCompleta
import com.rueda.supertaxi.model.EstadisticasJornada
import com.rueda.supertaxi.model.PrecioHoraEnCurso
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.LocalTime
import java.time.Duration

class ServicioRepository(private val servicioDao: ServicioDao, private val context: Context) {
    
    val allServicios: LiveData<List<Servicio>> = servicioDao.getAllServicios()
    
    // Insertar un servicio con lógica de jornada
    suspend fun insert(servicio: Servicio): Long {
        val serviciosDelDia = servicioDao.getServiciosPorFecha(servicio.dia)
        
        val servicioConJornada = if (serviciosDelDia.isEmpty()) {
            // Primer servicio del día - marca inicio de jornada
            servicio.copy(
                inicioJornada = servicio.hora1,
                numeroServicioEnJornada = 1
            )
        } else {
            // Servicio adicional del día - usar inicio de jornada del primer servicio
            val inicioJornada = serviciosDelDia.first().inicioJornada ?: servicio.hora1
            servicio.copy(
                inicioJornada = inicioJornada,
                numeroServicioEnJornada = serviciosDelDia.size + 1
            )
        }
        
        val id = servicioDao.insertServicio(servicioConJornada)
        
        // Actualizar inicio de jornada para todos los servicios del día si es necesario
        servicioDao.actualizarInicioJornadaDelDia(servicio.dia, servicioConJornada.inicioJornada!!)
        
        // Crear una copia del servicio con el ID asignado
        val servicioConId = servicioConJornada.copy(id = id)
        writeToCSV(servicioConId)
        return id
    }
    
    // Marcar fin de jornada
    suspend fun marcarFinJornada(fecha: LocalDate, horaFin: LocalTime) {
        val serviciosDelDia = servicioDao.getServiciosPorFecha(fecha)
        if (serviciosDelDia.isNotEmpty()) {
            val ultimoServicio = serviciosDelDia.last()
            servicioDao.actualizarFinJornada(ultimoServicio.id, horaFin, true)
        }
    }
    
    // NUEVO MÉTODO - Calcular precio/hora proyectado en tiempo real
    suspend fun calcularPrecioHoraProyectado(fecha: LocalDate, servicioEnCurso: Boolean = false): PrecioHoraEnCurso {
        val servicios = servicioDao.getServiciosPorFecha(fecha)
        if (servicios.isEmpty()) return PrecioHoraEnCurso()
        
        val inicioJornada = servicios.first().inicioJornada ?: servicios.first().hora1
        val ahoraActual = LocalTime.now()
        
        // Ingresos solo de servicios completados
        val serviciosCompletados = servicios.filter { 
            // Un servicio está completado si tiene hora3 definida y importe > 0
            it.hora3 != null && it.importe > 0
        }
        val ingresosCompletados = serviciosCompletados.sumOf { it.importe }
        
        // Tiempo total transcurrido desde inicio de jornada hasta ahora
        val tiempoTotalTranscurrido = Duration.between(inicioJornada, ahoraActual).toMinutes().toDouble()
        
        // Tiempo productivo (solo con clientes)
        val tiempoProductivo = serviciosCompletados.sumOf { it.minutosTotales }
        
        // Tiempo sin ganancias (búsqueda, espera, etc.)
        val tiempoSinGanancias = tiempoTotalTranscurrido - tiempoProductivo
        
        // Calcular precio/hora actual
        val precioHoraActual = if (tiempoTotalTranscurrido > 0) {
            ingresosCompletados / (tiempoTotalTranscurrido / 60.0)
        } else 0.0
        
        // Calcular eficiencia actual (% tiempo productivo)
        val eficienciaActual = if (tiempoTotalTranscurrido > 0) {
            (tiempoProductivo / tiempoTotalTranscurrido) * 100
        } else 0.0
        
        // Calcular proyección horaria si continúa al ritmo actual
        val proyeccionHoraria = if (tiempoTotalTranscurrido > 0) {
            val ratioIngresos = ingresosCompletados / (tiempoTotalTranscurrido / 60.0)
            ratioIngresos
        } else 0.0
        
        return PrecioHoraEnCurso(
            precioHoraActual = precioHoraActual,
            ingresosHastaAhora = ingresosCompletados,
            tiempoTotalTranscurrido = tiempoTotalTranscurrido,
            tiempoProductivo = tiempoProductivo,
            tiempoSinGanancias = tiempoSinGanancias,
            eficienciaActual = eficienciaActual,
            serviciosCompletados = serviciosCompletados.size,
            inicioJornada = inicioJornada,
            proyeccionHoraria = proyeccionHoraria
        )
    }
    
    // Calcular jornada completa
    suspend fun calcularJornadaCompleta(fecha: LocalDate): JornadaCompleta? {
        val servicios = servicioDao.getServiciosPorFecha(fecha)
        if (servicios.isEmpty()) return null
        
        val inicioJornada = servicios.first().inicioJornada ?: servicios.first().hora1
        val finJornada = servicios.last().finJornada ?: servicios.last().hora3
        
        val totalIngresos = servicios.sumOf { it.importe }
        val totalMinutosTrabajados = Duration.between(inicioJornada, finJornada).toMinutes().toDouble()
        val precioHoraReal = if (totalMinutosTrabajados > 0) {
            totalIngresos / (totalMinutosTrabajados / 60.0)
        } else 0.0
        
        val totalKilometros = servicios.sumOf { it.kmTotales }
        
        // Calcular tiempo promedio entre servicios
        val tiempoPromedioEntreServicios = if (servicios.size > 1) {
            var tiemposEspera = mutableListOf<Double>()
            for (i in 0 until servicios.size - 1) {
                val finServicioActual = servicios[i].hora3
                val inicioSiguienteServicio = servicios[i + 1].hora1
                val minutosEspera = Duration.between(finServicioActual, inicioSiguienteServicio).toMinutes().toDouble()
                tiemposEspera.add(minutosEspera)
            }
            tiemposEspera.average()
        } else 0.0
        
        // Calcular eficiencia productiva (tiempo con cliente vs tiempo total)
        val tiempoConClientes = servicios.sumOf { it.minutosTotales }
        val eficienciaProductiva = if (totalMinutosTrabajados > 0) {
            (tiempoConClientes / totalMinutosTrabajados) * 100
        } else 0.0
        
        return JornadaCompleta(
            fecha = fecha,
            inicioJornada = inicioJornada,
            finJornada = finJornada,
            servicios = servicios,
            totalIngresos = totalIngresos,
            totalMinutosTrabajados = totalMinutosTrabajados,
            precioHoraReal = precioHoraReal,
            totalKilometros = totalKilometros,
            cantidadServicios = servicios.size,
            tiempoPromedioEntreServicios = tiempoPromedioEntreServicios,
            eficienciaProductiva = eficienciaProductiva
        )
    }
    
    // Calcular estadísticas de múltiples jornadas
    suspend fun calcularEstadisticasJornadas(fechaInicio: LocalDate, fechaFin: LocalDate): EstadisticasJornada {
        val fechasConServicios = servicioDao.getFechasConServicios()
            .filter { !it.isBefore(fechaInicio) && !it.isAfter(fechaFin) }
        
        val jornadas = fechasConServicios.mapNotNull { fecha ->
            calcularJornadaCompleta(fecha)
        }
        
        if (jornadas.isEmpty()) {
            return EstadisticasJornada(
                precioHoraPromedio = 0.0,
                precioHoraMinimo = 0.0,
                precioHoraMaximo = 0.0,
                ingresosDiarioPromedio = 0.0,
                horasTrabajadasPromedio = 0.0,
                serviciosPorJornada = 0.0,
                eficienciaPromedio = 0.0,
                mejorJornada = null,
                peorJornada = null
            )
        }
        
        val preciosHora = jornadas.map { it.precioHoraReal }.filter { it > 0 }
        
        return EstadisticasJornada(
            precioHoraPromedio = if (preciosHora.isNotEmpty()) preciosHora.average() else 0.0,
            precioHoraMinimo = preciosHora.minOrNull() ?: 0.0,
            precioHoraMaximo = preciosHora.maxOrNull() ?: 0.0,
            ingresosDiarioPromedio = jornadas.map { it.totalIngresos }.average(),
            horasTrabajadasPromedio = jornadas.map { it.totalMinutosTrabajados / 60.0 }.average(),
            serviciosPorJornada = jornadas.map { it.cantidadServicios.toDouble() }.average(),
            eficienciaPromedio = jornadas.map { it.eficienciaProductiva }.average(),
            mejorJornada = jornadas.maxByOrNull { it.precioHoraReal },
            peorJornada = jornadas.minByOrNull { it.precioHoraReal }
        )
    }
    
    // Obtener un servicio por ID
    suspend fun getById(id: Long): Servicio? {
        try {
            return servicioDao.getServicioById(id)
        } catch (e: Exception) {
            Log.e("ServicioRepository", "Error al obtener servicio por ID", e)
            throw e
        }
    }
    
    // Eliminar un servicio
    suspend fun delete(servicio: Servicio) {
        try {
            servicioDao.deleteServicio(servicio)
        } catch (e: Exception) {
            Log.e("ServicioRepository", "Error al eliminar servicio", e)
            throw e
        }
    }
    
    // Escribir a CSV
    private fun writeToCSV(servicio: Servicio) {
        try {
            // Para Android 10+ usamos getExternalFilesDir que no requiere permisos especiales
            val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            }
            
            // Asegurarnos de que el directorio exista
            directory?.mkdirs()
            
            val file = File(directory, "Registroapp.csv")
            val isNewFile = !file.exists()
            
            try {
                FileWriter(file, true).use { writer ->
                    // Escribir encabezados si es un archivo nuevo
                    if (isNewFile) {
                        writer.append(
                            "ID,Día,Tipo de Servicio,Hora1,Km1,Calle1,Hora2,Km2,Hora3,Calle2,Calle3," +
                            "Importe,Comisión,Porcentaje,Tipo de Pago,Minutos Totales,Precio por Hora," +
                            "Km Totales,Precio por Km,Ruta1,Ruta2,Inicio Jornada,Fin Jornada," +
                            "Último Servicio Jornada,Número Servicio Jornada\n"
                        )
                    }
                    
                    // Escribir datos
                    writer.append("${servicio.id},")
                    writer.append("${servicio.dia},")
                    writer.append("${servicio.tipoServicio},")
                    writer.append("${servicio.hora1},")
                    writer.append("${servicio.km1},")
                    writer.append("\"${servicio.calle1}\",")
                    writer.append("${servicio.hora2},")
                    writer.append("${servicio.km2},")
                    writer.append("${servicio.hora3},")
                    writer.append("\"${servicio.calle2}\",")
                    writer.append("\"${servicio.calle3}\",")
                    writer.append("${servicio.importe},")
                    writer.append("${servicio.comision},")
                    writer.append("${servicio.porcentaje},")
                    writer.append("${servicio.tipoPago},")
                    writer.append("${servicio.minutosTotales},")
                    writer.append("${servicio.precioHora},")
                    writer.append("${servicio.kmTotales},")
                    writer.append("${servicio.precioKm},")
                    
                    // Utiliza una representación simplificada de las rutas para CSV
                    val ruta1Str = servicio.ruta1.joinToString("|") { "${it.latitud},${it.longitud}" }
                    val ruta2Str = servicio.ruta2.joinToString("|") { "${it.latitud},${it.longitud}" }
                    
                    writer.append("\"$ruta1Str\",")
                    writer.append("\"$ruta2Str\",")
                    writer.append("${servicio.inicioJornada ?: ""},")
                    writer.append("${servicio.finJornada ?: ""},")
                    writer.append("${servicio.esUltimoServicioJornada},")
                    writer.append("${servicio.numeroServicioEnJornada}\n")
                }
            } catch (e: Exception) {
                Log.e("ServicioRepository", "Error escribiendo en CSV", e)
                throw e
            }
        } catch (e: Exception) {
            Log.e("ServicioRepository", "Error accediendo al directorio", e)
            throw e
        }
    }
    
    // NUEVAS FUNCIONES PARA JORNADAS
    suspend fun getFechasConServicios(): List<LocalDate> {
        return servicioDao.getFechasConServicios()
    }
    
    suspend fun getServiciosPorPeriodo(fechaInicio: LocalDate, fechaFin: LocalDate): List<Servicio> {
        return servicioDao.getServiciosPorPeriodo(fechaInicio, fechaFin)
    }
} 