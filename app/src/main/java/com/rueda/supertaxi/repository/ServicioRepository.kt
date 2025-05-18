package com.rueda.supertaxi.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import com.rueda.supertaxi.database.ServicioDao
import com.rueda.supertaxi.model.Servicio
import java.io.File
import java.io.FileWriter

class ServicioRepository(private val servicioDao: ServicioDao, private val context: Context) {
    
    // Obtener todos los servicios
    val allServicios: LiveData<List<Servicio>> = servicioDao.getAllServicios()
    
    // Insertar un servicio
    suspend fun insert(servicio: Servicio): Long {
        val id = servicioDao.insertServicio(servicio)
        // Crear una copia del servicio con el ID asignado por Room
        val servicioConId = servicio.copy(id = id)
        writeToCSV(servicioConId)
        return id
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
                            "Km Totales,Precio por Km,Ruta1,Ruta2\n"
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
                    writer.append("\"$ruta2Str\"\n")
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
} 