package com.rueda.supertaxi.model

import java.time.LocalDate
import java.time.LocalTime

data class JornadaCompleta(
    val fecha: LocalDate,
    val inicioJornada: LocalTime,
    val finJornada: LocalTime,
    val servicios: List<Servicio>,
    val totalIngresos: Double,
    val totalMinutosTrabajados: Double,
    val precioHoraReal: Double,
    val totalKilometros: Double,
    val cantidadServicios: Int,
    val tiempoPromedioEntreServicios: Double,
    val eficienciaProductiva: Double // % tiempo con cliente vs tiempo total
)

data class EstadisticasJornada(
    val precioHoraPromedio: Double,
    val precioHoraMinimo: Double,
    val precioHoraMaximo: Double,
    val ingresosDiarioPromedio: Double,
    val horasTrabajadasPromedio: Double,
    val serviciosPorJornada: Double,
    val eficienciaPromedio: Double,
    val mejorJornada: JornadaCompleta?,
    val peorJornada: JornadaCompleta?
) 