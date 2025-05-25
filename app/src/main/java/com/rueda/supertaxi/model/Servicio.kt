package com.rueda.supertaxi.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rueda.supertaxi.database.Converters
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "servicio")
@TypeConverters(Converters::class)
data class Servicio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dia: LocalDate,
    val tipoServicio: String,
    val hora1: LocalTime,
    val km1: Double,
    val calle1: String,
    val hora2: LocalTime,
    val km2: Double,
    val hora3: LocalTime,
    val calle2: String,
    val calle3: String,
    val importe: Double,
    val comision: Double,
    val porcentaje: Double,
    val tipoPago: String,
    val minutosTotales: Double,
    val precioHora: Double,
    val kmTotales: Double,
    val precioKm: Double,
    val ruta1: List<Coordenada>,
    val ruta2: List<Coordenada>,
    
    // NUEVOS CAMPOS PARA JORNADA
    val inicioJornada: LocalTime? = null,
    val finJornada: LocalTime? = null,
    val esUltimoServicioJornada: Boolean = false,
    val numeroServicioEnJornada: Int = 1
)

data class Coordenada(
    val latitud: Double,
    val longitud: Double
) 