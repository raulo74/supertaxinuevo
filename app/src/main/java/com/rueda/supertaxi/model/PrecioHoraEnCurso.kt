package com.rueda.supertaxi.model

import java.time.LocalTime

data class PrecioHoraEnCurso(
    val precioHoraActual: Double = 0.0,
    val ingresosHastaAhora: Double = 0.0,
    val tiempoTotalTranscurrido: Double = 0.0, // en minutos
    val tiempoProductivo: Double = 0.0, // tiempo con clientes
    val tiempoSinGanancias: Double = 0.0, // tiempo de búsqueda/espera
    val eficienciaActual: Double = 0.0, // % tiempo productivo
    val serviciosCompletados: Int = 0,
    val inicioJornada: LocalTime? = null,
    val proyeccionHoraria: Double = 0.0 // proyección si continúa así
) {
    fun calcularProyeccionDiaria(horasObjetivo: Double): Double {
        return if (tiempoTotalTranscurrido > 0) {
            val ratioActual = ingresosHastaAhora / (tiempoTotalTranscurrido / 60.0)
            ratioActual * horasObjetivo
        } else 0.0
    }
} 