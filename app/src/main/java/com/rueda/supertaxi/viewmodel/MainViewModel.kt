package com.rueda.supertaxi.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.*
import com.rueda.supertaxi.database.AppDatabase
import com.rueda.supertaxi.model.Coordenada
import com.rueda.supertaxi.model.Servicio
import com.rueda.supertaxi.model.TipoServicio
import com.rueda.supertaxi.model.JornadaCompleta
import com.rueda.supertaxi.model.EstadisticasJornada
import com.rueda.supertaxi.model.PrecioHoraEnCurso
import com.rueda.supertaxi.repository.ServicioRepository
import com.rueda.supertaxi.repository.TipoServicioRepository
import com.rueda.supertaxi.util.DistanceCalculator
import com.rueda.supertaxi.util.GeocodingUtil
import com.rueda.supertaxi.util.LocationService
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class MainViewModel(application: Application) : AndroidViewModel(application) {
    // Repositorios
    private val servicioRepository: ServicioRepository
    private val tipoServicioRepository: TipoServicioRepository

    // Datos de tipos de servicio
    val allTiposServicio: LiveData<List<TipoServicio>>

    // Variables para el servicio actual
    private val _currentServicioId = MutableLiveData<Long>()
    val currentServicioId: LiveData<Long> = _currentServicioId

    private val _tipoServicio = MutableLiveData<String>()
    val tipoServicio: LiveData<String> = _tipoServicio

    // Variable para trackear si hay un servicio en progreso
    private val _servicioEnProgreso = MutableLiveData<Boolean>(false)
    val servicioEnProgreso: LiveData<Boolean> = _servicioEnProgreso

    // Variable para trackear si el tipo de servicio cambió durante el servicio
    private val _tipoServicioCambiado = MutableLiveData<Boolean>(false)
    val tipoServicioCambiado: LiveData<Boolean> = _tipoServicioCambiado

    // Variable para guardar el tipo de servicio original cuando se inició
    private var tipoServicioOriginal: String = ""

    private val _dia = MutableLiveData<LocalDate>()
    val dia: LiveData<LocalDate> = _dia

    private val _hora1 = MutableLiveData<LocalTime>()
    val hora1: LiveData<LocalTime> = _hora1

    private val _hora2 = MutableLiveData<LocalTime>()
    val hora2: LiveData<LocalTime> = _hora2

    private val _hora3 = MutableLiveData<LocalTime>()
    val hora3: LiveData<LocalTime> = _hora3

    private val _importe = MutableLiveData<Double>()
    val importe: LiveData<Double> = _importe

    private val _comision = MutableLiveData<Double>()
    val comision: LiveData<Double> = _comision

    private val _tipoPago = MutableLiveData<String>()
    val tipoPago: LiveData<String> = _tipoPago

    private val _ruta1 = MutableLiveData<MutableList<Coordenada>>(mutableListOf())
    val ruta1: LiveData<MutableList<Coordenada>> = _ruta1

    private val _ruta2 = MutableLiveData<MutableList<Coordenada>>(mutableListOf())
    val ruta2: LiveData<MutableList<Coordenada>> = _ruta2

    // Datos calculados
    private val _km1 = MutableLiveData<Double>()
    val km1: LiveData<Double> = _km1

    private val _km2 = MutableLiveData<Double>()
    val km2: LiveData<Double> = _km2

    private val _calle1 = MutableLiveData<String>()
    val calle1: LiveData<String> = _calle1

    private val _calle2 = MutableLiveData<String>()
    val calle2: LiveData<String> = _calle2

    private val _calle3 = MutableLiveData<String>()
    val calle3: LiveData<String> = _calle3

    private val _porcentaje = MutableLiveData<Double>()
    val porcentaje: LiveData<Double> = _porcentaje

    private val _minutosTotales = MutableLiveData<Double>()
    val minutosTotales: LiveData<Double> = _minutosTotales

    private val _precioHora = MutableLiveData<Double>()
    val precioHora: LiveData<Double> = _precioHora

    private val _kmTotales = MutableLiveData<Double>()
    val kmTotales: LiveData<Double> = _kmTotales

    private val _precioKm = MutableLiveData<Double>()
    val precioKm: LiveData<Double> = _precioKm

    // Estado de los botones
    private val _empezarEnabled = MutableLiveData<Boolean>(false)
    val empezarEnabled: LiveData<Boolean> = _empezarEnabled

    private val _inicioServicioEnabled = MutableLiveData<Boolean>(false)
    val inicioServicioEnabled: LiveData<Boolean> = _inicioServicioEnabled

    private val _finServicioEnabled = MutableLiveData<Boolean>(false)
    val finServicioEnabled: LiveData<Boolean> = _finServicioEnabled

    private val _resumenEnabled = MutableLiveData<Boolean>(false)
    val resumenEnabled: LiveData<Boolean> = _resumenEnabled

    // NUEVAS PROPIEDADES PARA JORNADA
    private val _jornadaIniciada = MutableLiveData<Boolean>(false)
    val jornadaIniciada: LiveData<Boolean> = _jornadaIniciada

    private val _inicioJornadaHoy = MutableLiveData<LocalTime?>()
    val inicioJornadaHoy: LiveData<LocalTime?> = _inicioJornadaHoy

    private val _estadisticasJornadaHoy = MutableLiveData<JornadaCompleta?>()
    val estadisticasJornadaHoy: LiveData<JornadaCompleta?> = _estadisticasJornadaHoy

    // NUEVAS PROPIEDADES PARA TRACKING EN TIEMPO REAL
    private val _precioHoraEnCurso = MutableLiveData<PrecioHoraEnCurso>()
    val precioHoraEnCurso: LiveData<PrecioHoraEnCurso> = _precioHoraEnCurso

    private val _timerJornada = MutableLiveData<String>("00:00")
    val timerJornada: LiveData<String> = _timerJornada

    // Timer para actualizar estadísticas en tiempo real
    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null

    // Variables para el cronómetro grande
    private val _cronometroGrande = MutableLiveData<String>("00:00:00")
    val cronometroGrande: LiveData<String> = _cronometroGrande

    private val _cronometroGrandeVisible = MutableLiveData<Boolean>(false)
    val cronometroGrandeVisible: LiveData<Boolean> = _cronometroGrandeVisible

    private val _cardCronometroVisible = MutableLiveData<Boolean>(false)
    val cardCronometroVisible: LiveData<Boolean> = _cardCronometroVisible

    // Handler y Runnable para el cronómetro
    private var cronometroHandler: Handler? = null
    private var cronometroRunnable: Runnable? = null
    private var horaInicioCronometro: LocalTime? = null

    // Utilidades
    private val geocodingUtil = GeocodingUtil(application.applicationContext)

    // Servicio actual
    private var servicioActual: Servicio? = null

    init {
        val database = AppDatabase.getDatabase(application)
        servicioRepository = ServicioRepository(database.servicioDao(), application.applicationContext)
        tipoServicioRepository = TipoServicioRepository(database.tipoServicioDao())
        allTiposServicio = tipoServicioRepository.allTiposServicio

        resetVariables()
        
        // Verificar si ya hay una jornada iniciada hoy
        viewModelScope.launch {
            verificarJornadaDelDia()
        }
        
        iniciarTimerJornada()
    }

    // NUEVOS MÉTODOS PARA JORNADA

    private suspend fun verificarJornadaDelDia() {
        val serviciosHoy = servicioRepository.calcularJornadaCompleta(LocalDate.now())
        if (serviciosHoy != null && serviciosHoy.servicios.isNotEmpty()) {
            _jornadaIniciada.value = true
            _inicioJornadaHoy.value = serviciosHoy.inicioJornada
            _estadisticasJornadaHoy.value = serviciosHoy
        }
    }

    fun marcarFinJornada() {
        viewModelScope.launch {
            servicioRepository.marcarFinJornada(LocalDate.now(), LocalTime.now())
            _jornadaIniciada.value = false
            
            // Actualizar estadísticas finales de la jornada
            val jornadaFinal = servicioRepository.calcularJornadaCompleta(LocalDate.now())
            _estadisticasJornadaHoy.value = jornadaFinal
        }
    }

    fun obtenerEstadisticasJornada(fechaInicio: LocalDate, fechaFin: LocalDate, callback: (EstadisticasJornada) -> Unit) {
        viewModelScope.launch {
            val estadisticas = servicioRepository.calcularEstadisticasJornadas(fechaInicio, fechaFin)
            callback(estadisticas)
        }
    }

    // NUEVOS MÉTODOS PARA TRACKING EN TIEMPO REAL

    private fun iniciarTimerJornada() {
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                if (_jornadaIniciada.value == true) {
                    actualizarEstadisticasEnTiempoReal()
                    actualizarTimerDisplay()
                }
                timerHandler?.postDelayed(this, 30000) // Actualizar cada 30 segundos
            }
        }
        timerHandler?.post(timerRunnable!!)
    }

    private fun actualizarEstadisticasEnTiempoReal() {
        viewModelScope.launch {
            try {
                val estadisticasActuales = servicioRepository.calcularPrecioHoraProyectado(
                    LocalDate.now(), 
                    _servicioEnProgreso.value == true
                )
                _precioHoraEnCurso.postValue(estadisticasActuales)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error actualizando estadísticas en tiempo real", e)
            }
        }
    }

    private fun actualizarTimerDisplay() {
        _inicioJornadaHoy.value?.let { inicio ->
            val tiempoTranscurrido = Duration.between(inicio, LocalTime.now())
            val horas = tiempoTranscurrido.toHours()
            val minutos = tiempoTranscurrido.toMinutes() % 60
            _timerJornada.postValue(String.format("%02d:%02d", horas, minutos))
        }
    }

    // Método para obtener proyección de ingresos
    fun obtenerProyeccionIngresos(horasObjetivo: Double): Double {
        return _precioHoraEnCurso.value?.calcularProyeccionDiaria(horasObjetivo) ?: 0.0
    }

    // Métodos para interacción con la UI
    fun setTipoServicio(tipo: String) {
        Log.d("MainViewModel", "setTipoServicio llamado con: $tipo")
        
        // Si hay un servicio en progreso y el tipo es diferente al original
        if (_servicioEnProgreso.value == true && tipo != tipoServicioOriginal) {
            Log.d("MainViewModel", "Tipo de servicio cambiado durante servicio activo: $tipoServicioOriginal -> $tipo")
            _tipoServicioCambiado.value = true
        } else {
            _tipoServicioCambiado.value = false
        }
        
        _tipoServicio.value = tipo
        
        // FORZAR ocultación de tarjeta para CUALQUIER cambio de tipo
        Log.d("MainViewModel", "FORZANDO ocultación de tarjeta por cambio de tipo")
        _cardCronometroVisible.value = false
        _cronometroGrandeVisible.value = false
        
        // Si hay cronómetro activo, detenerlo
        if (cronometroRunnable != null) {
            Log.d("MainViewModel", "Deteniendo cronómetro existente")
            detenerCronometroParada()
        }
        
        // Debug después del cambio
        debugTarjetaStatus()
        
        // Solo cambiar el estado de los botones si NO hay un servicio en progreso
        if (_servicioEnProgreso.value != true) {
            // Lógica especial para tipo "Mano Alzada"
            if (tipo == "Mano Alzada") {
                _empezarEnabled.value = false
                _inicioServicioEnabled.value = true
                _finServicioEnabled.value = false
                _resumenEnabled.value = false
                
                _dia.value = LocalDate.now()
                _hora1.value = LocalTime.now()
                _ruta1.value = mutableListOf()
            } else {
                _empezarEnabled.value = true
                _inicioServicioEnabled.value = false
                _finServicioEnabled.value = false
                _resumenEnabled.value = false
            }
        }
        
        Log.d("MainViewModel", "setTipoServicio completado para: $tipo")
    }

    fun empezarServicio() {
        Log.d("MainViewModel", "empezarServicio iniciado con tipo: ${_tipoServicio.value}")
        
        // Marcar que hay un servicio en progreso
        _servicioEnProgreso.value = true
        tipoServicioOriginal = _tipoServicio.value ?: ""
        _tipoServicioCambiado.value = false
        
        _dia.value = LocalDate.now()
        _hora1.value = LocalTime.now()
        
        // Marcar inicio de jornada si es el primer servicio del día
        if (_jornadaIniciada.value != true) {
            _jornadaIniciada.value = true
            _inicioJornadaHoy.value = LocalTime.now()
        }
        
        // Controlar qué hacer según el tipo de servicio
        when (_tipoServicio.value) {
            "Mano Alzada" -> {
                Log.d("MainViewModel", "Iniciando servicio Mano Alzada - NO cronómetro")
                // Para Mano Alzada: no ruta1, no cronómetro, no tarjeta
                _ruta1.value = mutableListOf()
                _cardCronometroVisible.value = false
                _cronometroGrandeVisible.value = false
            }
            "Parada de taxis" -> {
                Log.d("MainViewModel", "Iniciando servicio Parada de taxis - SÍ cronómetro")
                // Para Parada de taxis: sí ruta1, sí cronómetro
                val intent = Intent(getApplication(), LocationService::class.java).apply {
                    putExtra("trackRoute1", true)
                    putExtra("trackRoute2", false)
                }
                getApplication<Application>().startService(intent)
                
                // Mostrar tarjeta e iniciar cronómetro SOLO para Parada de taxis
                iniciarCronometroParada()
            }
            else -> {
                Log.d("MainViewModel", "Iniciando servicio ${_tipoServicio.value} - NO cronómetro")
                // Para otros tipos de servicio: sí ruta1, no cronómetro, no tarjeta
                val intent = Intent(getApplication(), LocationService::class.java).apply {
                    putExtra("trackRoute1", true)
                    putExtra("trackRoute2", false)
                }
                getApplication<Application>().startService(intent)
                
                // Asegurar que no se muestre cronómetro para otros tipos
                _cardCronometroVisible.value = false
                _cronometroGrandeVisible.value = false
            }
        }
        
        // Actualizar estado de botones
        _empezarEnabled.value = false
        _inicioServicioEnabled.value = true
        _finServicioEnabled.value = false
        _resumenEnabled.value = false
        
        Log.d("MainViewModel", "Servicio iniciado. Tipo: ${_tipoServicio.value}, Tarjeta visible: ${_cardCronometroVisible.value}")
    }

    fun inicioServicio() {
        // Guardar hora2
        _hora2.value = LocalTime.now()
        
        // NUEVO: Detener cronómetro y ocultar tarjeta si estaba activo
        if (_cronometroGrandeVisible.value == true) {
            detenerCronometroParada()
        }
        
        // Detener tracking de ruta1 y comenzar ruta2
        val intent = Intent(getApplication(), LocationService::class.java).apply {
            putExtra("trackRoute1", false)
            putExtra("trackRoute2", true)
        }
        getApplication<Application>().startService(intent)
        
        // Actualizar estado de botones
        _empezarEnabled.value = false
        _inicioServicioEnabled.value = false
        _finServicioEnabled.value = true
        _resumenEnabled.value = false
    }

    fun finServicio() {
        // Guardar hora3
        _hora3.value = LocalTime.now()
        
        // Detener tracking de ubicación
        detenerServicioUbicacion()
        
        // Calcular datos finales
        calcularDatosFinales()
        
        // Guardar en base de datos y CSV
        guardarServicio()
        
        // Marcar que el servicio ya no está en progreso
        _servicioEnProgreso.value = false
        _tipoServicioCambiado.value = false
        
        // NUEVO: Limpiar estado en SharedPreferences
        val prefs = getApplication<Application>().getSharedPreferences("SuperTaxiPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("servicioEnProgreso", false).apply()
        
        // Actualizar estado de botones
        _empezarEnabled.value = false
        _inicioServicioEnabled.value = false
        _finServicioEnabled.value = false
        _resumenEnabled.value = true
    }

    private fun calcularDatosFinales() {
        viewModelScope.launch {
            // Calcular km1
            val distanciaRuta1 = DistanceCalculator.calculateDistance(ruta1.value ?: emptyList())
            _km1.value = distanciaRuta1

            // Calcular km2
            val distanciaRuta2 = DistanceCalculator.calculateDistance(ruta2.value ?: emptyList())
            _km2.value = distanciaRuta2

            // Calcular km totales
            _kmTotales.value = (km1.value ?: 0.0) + (km2.value ?: 0.0)

            // Obtener calles
            ruta1.value?.firstOrNull()?.let { coordenada ->
                _calle1.value = geocodingUtil.getAddressFromLocation(coordenada.latitud, coordenada.longitud)
            } ?: run {
                _calle1.value = "No disponible"
            }

            ruta2.value?.firstOrNull()?.let { coordenada ->
                _calle2.value = geocodingUtil.getAddressFromLocation(coordenada.latitud, coordenada.longitud)
            } ?: run {
                _calle2.value = "No disponible"
            }

            ruta2.value?.lastOrNull()?.let { coordenada ->
                _calle3.value = geocodingUtil.getAddressFromLocation(coordenada.latitud, coordenada.longitud)
            } ?: run {
                _calle3.value = "No disponible"
            }

            // Calcular porcentaje de comisión
            val importeVal = _importe.value ?: 0.0
            val comisionVal = _comision.value ?: 0.0

            // Manejar el caso cuando la comisión es 0
            if (comisionVal == 0.0) {
                _porcentaje.value = 0.0
            } else if (importeVal + comisionVal > 0) {
                _porcentaje.value = (comisionVal * 100) / (comisionVal + importeVal)
            } else {
                _porcentaje.value = 0.0
            }

            // Calcular minutos totales y precio por hora
            val h1 = _hora1.value ?: LocalTime.MIN
            val h2 = _hora2.value ?: LocalTime.MIN
            val h3 = _hora3.value ?: LocalTime.MIN

            val minutosTotalesValue = if (_tipoServicio.value == "Mano Alzada") {
                // Para Mano Alzada: solo contar desde hora2 hasta hora3
                Duration.between(h2, h3).toMinutes().toDouble()
            } else {
                // Para otros tipos: suma de los dos intervalos
                val minutos1 = Duration.between(h1, h2).toMinutes().toDouble()
                val minutos2 = Duration.between(h2, h3).toMinutes().toDouble()
                minutos1 + minutos2
            }
            _minutosTotales.value = minutosTotalesValue

            if (minutosTotalesValue > 0) {
                val horasDecimales = minutosTotalesValue / 60.0
                _precioHora.value = if (horasDecimales > 0) importeVal / horasDecimales else 0.0
                Log.d("MainViewModel", "Precio por hora calculado: ${_precioHora.value} (${importeVal}€ / ${horasDecimales}h)")
            } else {
                _precioHora.value = 0.0
                Log.d("MainViewModel", "Precio por hora = 0 (sin tiempo)")
            }

            // Calcular precio por km
            if (_kmTotales.value != null && _kmTotales.value!! > 0) {
                _precioKm.value = importeVal / _kmTotales.value!!
            } else {
                _precioKm.value = 0.0
            }
        }
    }

    private fun guardarServicio() {
        viewModelScope.launch {
            val nuevoServicio = Servicio(
                dia = _dia.value ?: LocalDate.now(),
                tipoServicio = _tipoServicio.value ?: "",
                hora1 = _hora1.value ?: LocalTime.now(),
                km1 = _km1.value ?: 0.0,
                calle1 = _calle1.value ?: "",
                hora2 = _hora2.value ?: LocalTime.now(),
                km2 = _km2.value ?: 0.0,
                hora3 = _hora3.value ?: LocalTime.now(),
                calle2 = _calle2.value ?: "",
                calle3 = _calle3.value ?: "",
                importe = _importe.value ?: 0.0,
                comision = _comision.value ?: 0.0,
                porcentaje = _porcentaje.value ?: 0.0,
                tipoPago = _tipoPago.value ?: "",
                minutosTotales = _minutosTotales.value ?: 0.0,
                precioHora = _precioHora.value ?: 0.0,
                kmTotales = _kmTotales.value ?: 0.0,
                precioKm = _precioKm.value ?: 0.0,
                ruta1 = _ruta1.value?.toList() ?: emptyList(),
                ruta2 = _ruta2.value?.toList() ?: emptyList()
            )

            val id = servicioRepository.insert(nuevoServicio)
            _currentServicioId.value = id
            servicioActual = nuevoServicio.copy(id = id)
        }
    }

    fun addLocationToRoute(latitude: Double, longitude: Double, isRoute1: Boolean) {
        val coordenada = Coordenada(latitude, longitude)

        if (isRoute1) {
            val rutaActual = _ruta1.value ?: mutableListOf()
            rutaActual.add(coordenada)
            _ruta1.value = rutaActual
        } else {
            val rutaActual = _ruta2.value ?: mutableListOf()
            rutaActual.add(coordenada)
            _ruta2.value = rutaActual
        }
    }

    fun setImporte(valor: Double) {
        _importe.value = valor
    }

    fun setComision(valor: Double) {
        _comision.value = valor
    }

    fun setTipoPago(tipo: String) {
        _tipoPago.value = tipo
    }

    fun addTipoServicio(nombre: String) {
        viewModelScope.launch {
            tipoServicioRepository.insert(TipoServicio(nombre, false))
        }
    }

    fun resetVariables() {
        Log.d("MainViewModel", "Iniciando resetVariables()")
        
        // PRIMERO: Detener cronómetro si está activo
        if (_cronometroGrandeVisible.value == true) {
            detenerCronometroParada()
        }
        
        // SEGUNDO: Ocultar tarjeta de cronómetro completamente
        _cardCronometroVisible.value = false
        _cronometroGrandeVisible.value = false
        
        // Reiniciar variables del servicio (MANTENER _jornadaIniciada)
        _tipoServicio.value = ""
        _servicioEnProgreso.value = false
        _tipoServicioCambiado.value = false
        tipoServicioOriginal = ""
        _dia.value = LocalDate.now()
        _hora1.value = LocalTime.now()
        _hora2.value = LocalTime.now()
        _hora3.value = LocalTime.now()
        _importe.value = 0.0
        _comision.value = 0.0
        _tipoPago.value = ""
        _ruta1.value = mutableListOf()
        _ruta2.value = mutableListOf()
        _km1.value = 0.0
        _km2.value = 0.0
        _calle1.value = ""
        _calle2.value = ""
        _calle3.value = ""
        _porcentaje.value = 0.0
        _minutosTotales.value = 0.0
        _precioHora.value = 0.0
        _kmTotales.value = 0.0
        _precioKm.value = 0.0
        
        // Reiniciar estados de botones
        _empezarEnabled.value = false
        _inicioServicioEnabled.value = false
        _finServicioEnabled.value = false
        _resumenEnabled.value = false
        
        // Reiniciar ID del servicio actual
        _currentServicioId.value = 0
        servicioActual = null
        
        // Establecer "Parada de taxis" como tipo de servicio predeterminado
        _tipoServicio.value = "Parada de taxis"
        setTipoServicio("Parada de taxis")
        
        Log.d("MainViewModel", "resetVariables() completado. Tarjeta visible: ${_cardCronometroVisible.value}")
    }

    fun getServicioActual(): Servicio? {
        return servicioActual
    }

    // Método para detener el servicio de ubicación
    private fun detenerServicioUbicacion() {
        Log.d("MainViewModel", "Deteniendo servicio de ubicación")
        val intent = Intent(getApplication(), LocationService::class.java)
        getApplication<Application>().stopService(intent)
    }

    // Método para cancelar el servicio actual
    fun cancelarServicioActual() {
        Log.d("MainViewModel", "Cancelando servicio actual")
        
        // NUEVO: Detener cronómetro si está activo
        if (_cronometroGrandeVisible.value == true) {
            detenerCronometroParada()
        }
        
        // NUEVO: Limpiar estado en SharedPreferences
        val prefs = getApplication<Application>().getSharedPreferences("SuperTaxiPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("servicioEnProgreso", false).apply()
        
        // Detener cualquier tracking de ubicación activo
        detenerServicioUbicacion()
        
        // Resetear todas las variables al estado inicial
        resetVariables()
        
        Log.d("MainViewModel", "Servicio cancelado y variables reseteadas")
    }

    // Método para verificar si se puede volver atrás
    fun puedeVolverAtras(): Boolean {
        return _servicioEnProgreso.value == true
    }

    // Método para verificar si el tipo de servicio cambió
    fun tipoServicioHaCambiado(): Boolean {
        return _tipoServicioCambiado.value == true
    }

    // Nuevo método para finalizar jornada completamente
    fun finalizarJornadaCompleta() {
        marcarFinJornada()
        _jornadaIniciada.value = false
        _inicioJornadaHoy.value = null
        _estadisticasJornadaHoy.value = null
        _precioHoraEnCurso.value = PrecioHoraEnCurso() // Reset
        _timerJornada.value = "00:00"
        detenerTimer()
    }

    private fun detenerTimer() {
        timerRunnable?.let { timerHandler?.removeCallbacks(it) }
    }

    // Método para iniciar el cronómetro
    private fun iniciarCronometroParada() {
        Log.d("MainViewModel", "Iniciando cronómetro de parada")
        horaInicioCronometro = LocalTime.now()
        _cardCronometroVisible.value = true
        _cronometroGrandeVisible.value = true
        
        cronometroHandler = Handler(Looper.getMainLooper())
        cronometroRunnable = object : Runnable {
            override fun run() {
                actualizarCronometroParada()
                cronometroHandler?.postDelayed(this, 1000) // Actualizar cada segundo
            }
        }
        cronometroHandler?.post(cronometroRunnable!!)
    }

    // Método para actualizar el cronómetro
    private fun actualizarCronometroParada() {
        horaInicioCronometro?.let { inicio ->
            val tiempoTranscurrido = Duration.between(inicio, LocalTime.now())
            val horas = tiempoTranscurrido.toHours()
            val minutos = (tiempoTranscurrido.toMinutes() % 60)
            val segundos = (tiempoTranscurrido.seconds % 60)
            
            val tiempoFormateado = String.format("%02d:%02d:%02d", horas, minutos, segundos)
            _cronometroGrande.postValue(tiempoFormateado)
        }
    }

    // Método para detener el cronómetro
    private fun detenerCronometroParada() {
        Log.d("MainViewModel", "Deteniendo cronómetro de parada")
        cronometroRunnable?.let { cronometroHandler?.removeCallbacks(it) }
        
        // Ocultar completamente la tarjeta
        _cardCronometroVisible.value = false
        _cronometroGrandeVisible.value = false
        _cronometroGrande.value = "00:00:00"
        horaInicioCronometro = null
    }

    override fun onCleared() {
        super.onCleared()
        detenerTimer()
        // NUEVO: Limpiar cronómetro
        cronometroRunnable?.let { cronometroHandler?.removeCallbacks(it) }
    }

    // AÑADIR método para debug
    fun debugTarjetaStatus() {
        Log.d("MainViewModel", "=== DEBUG TARJETA STATUS ===")
        Log.d("MainViewModel", "cardCronometroVisible: ${_cardCronometroVisible.value}")
        Log.d("MainViewModel", "cronometroGrandeVisible: ${_cronometroGrandeVisible.value}")
        Log.d("MainViewModel", "tipoServicio: ${_tipoServicio.value}")
        Log.d("MainViewModel", "servicioEnProgreso: ${_servicioEnProgreso.value}")
        Log.d("MainViewModel", "============================")
    }
}