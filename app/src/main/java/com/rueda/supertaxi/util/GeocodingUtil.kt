package com.rueda.supertaxi.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.util.Locale

class GeocodingUtil(private val context: Context) {
    fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            
            if (addresses != null && addresses.isNotEmpty()) {
                val address: Address = addresses[0]
                val streetName = address.thoroughfare ?: ""
                val streetNumber = address.subThoroughfare ?: ""
                val city = address.locality ?: ""
                
                return "$streetName $streetNumber, $city".trim()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return "Dirección desconocida"
    }
} 