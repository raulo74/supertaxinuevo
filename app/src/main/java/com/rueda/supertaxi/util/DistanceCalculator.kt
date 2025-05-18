package com.rueda.supertaxi.util

import android.location.Location
import com.rueda.supertaxi.model.Coordenada

class DistanceCalculator {
    companion object {
        fun calculateDistance(route: List<Coordenada>): Double {
            if (route.size < 2) return 0.0
            
            var totalDistance = 0.0
            for (i in 0 until route.size - 1) {
                val startLocation = Location("").apply {
                    latitude = route[i].latitud
                    longitude = route[i].longitud
                }
                
                val endLocation = Location("").apply {
                    latitude = route[i + 1].latitud
                    longitude = route[i + 1].longitud
                }
                
                totalDistance += startLocation.distanceTo(endLocation)
            }
            
            // Convertir de metros a kilómetros
            return totalDistance / 1000
        }
    }
} 