package com.rueda.supertaxi.util

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.rueda.supertaxi.R
import com.rueda.supertaxi.view.MainActivity

class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setWaitForAccurateLocation(false)
        .setMinUpdateIntervalMillis(2000)
        .build()
    
    private var isTrackingRoute1 = false
    private var isTrackingRoute2 = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d("LocationService", "onCreate iniciado")
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Configurar el callback de ubicación
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    Log.d("LocationService", "Nueva ubicación: lat=${location.latitude}, lng=${location.longitude}")
                    
                    // Enviar la ubicación a través de un broadcast local
                    val intent = Intent("LOCATION_UPDATE")
                    intent.putExtra("latitude", location.latitude)
                    intent.putExtra("longitude", location.longitude)
                    intent.putExtra("isTrackingRoute1", isTrackingRoute1)
                    intent.putExtra("isTrackingRoute2", isTrackingRoute2)
                    LocalBroadcastManager.getInstance(this@LocationService).sendBroadcast(intent)
                }
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationService", "onStartCommand iniciado")
        
        // Obtener parámetros del intent
        intent?.let {
            isTrackingRoute1 = it.getBooleanExtra("trackRoute1", false)
            isTrackingRoute2 = it.getBooleanExtra("trackRoute2", false)
            
            Log.d("LocationService", "Tracking route1: $isTrackingRoute1, route2: $isTrackingRoute2")
        }
        
        // Iniciar como servicio en primer plano
        startForegroundService()
        
        // Iniciar actualizaciones de ubicación
        startLocationUpdates()
        
        return START_STICKY
    }
    
    private fun startForegroundService() {
        val channelId = createNotificationChannel("location_service_channel", "Servicio de Ubicación")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SuperTaxi en funcionamiento")
            .setContentText("Rastreando servicio de taxi")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
    }
    
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(channel)
        }
        return channelId
    }
    
    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d("LocationService", "Actualizaciones de ubicación iniciadas")
        } catch (e: SecurityException) {
            Log.e("LocationService", "Error al solicitar actualizaciones de ubicación", e)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("LocationService", "onDestroy: Servicio detenido")
    }
    
    fun startTrackingRoute1() {
        isTrackingRoute1 = true
        isTrackingRoute2 = false
    }
    
    fun startTrackingRoute2() {
        isTrackingRoute1 = false
        isTrackingRoute2 = true
    }
    
    fun stopTracking() {
        isTrackingRoute1 = false
        isTrackingRoute2 = false
    }
} 