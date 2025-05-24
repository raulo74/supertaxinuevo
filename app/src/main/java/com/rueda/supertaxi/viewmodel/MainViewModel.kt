package com.rueda.supertaxi.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import com.rueda.supertaxi.database.AppDatabase
import com.rueda.supertaxi.model.Coordenada
import com.rueda.supertaxi.model.Servicio
import com.rueda.supertaxi.model.TipoServicio
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

    // NUEVA VARIABLE: Para trackear si hay un servicio en progreso
    private val _servicioEnProgreso = MutableLiveData<Boolean>(false)
    val servicioEnProgreso: LiveData<Boolean> = _servicioEnProgreso

    // NUEVA VARIABLE: Para trackear si el tipo de servicio cambió durante el servicio
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
        
        // Solo cambiar el estado de los botones si NO hay un servicio en progreso
        if (_servicioEnProgreso.value != true) {
            // Lógica especial para tipo "Mano Alzada"
            if (tipo == "Mano Alzada") {
                // Para Mano Alzada, bloqueamos Empezar y habilitamos directamente Inicio de servicio
                _empezarEnabled.value = false
                _inicioServicioEnabled.value = true
                _finServicioEnabled.value = false
                _resumenEnabled.value = false
                
                // Establecer fecha y hora de inicio automáticamente
                _dia.value = LocalDate.now()
                _hora1.value = LocalTime.now()
                
                // No iniciamos tracking para ruta1 en este caso
                _ruta1.value = mutableListOf()
            } else {
                // Para el resto de tipos de servicio, seguimos el flujo normal
                _empezarEnabled.value = true
                _inicioServicioEnabled.value = false
                _finServicioEnabled.value = false
                _resumenEnabled.value = false
            }
        }
    }

    fun empezarServicio() {
        Log.d("MainViewModel", "empezarServicio iniciado")
        
        // Marcar que hay un servicio en progreso
        _servicioEnProgreso.value = true
        tipoServicioOriginal = _tipoServicio.value ?: ""
        _tipoServicioCambiado.value = false
        
        _dia.value = LocalDate.now()
        _hora1.value = LocalTime.now()
        
        // Si es "Mano Alzada", no se guarda ruta1
        if (_tipoServicio.value == "Mano Alzada") {
            _ruta1.value = mutableListOf()
        } else {
            // Iniciar servicio de ubicación para ruta1
            val intent = Intent(getApplication(), LocationService::class.java).apply {
                putExtra("trackRoute1", true)
                putExtra("trackRoute2", false)
            }
            getApplication<Application>().startService(intent)
        }
        
        // Actualizar estado de botones
        _empezarEnabled.value = false
        _inicioServicioEnabled.value = true
        _finServicioEnabled.value = false
        _resumenEnabled.value = false
        
        Log.d("MainViewModel", "Servicio marcado como en progreso")
    }

    fun inicioServicio() {
        // Guardar hora2
        _hora2.value = LocalTime.now()
        
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
                _precioHora.value = (importeVal / (minutosTotalesValue / 60))
            } else {
                _precioHora.value = 0.0
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
        
        // Reiniciar variables del servicio
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
        
        Log.d("MainViewModel", "resetVariables() completado - Tipo servicio: Parada de taxis")
    }

    fun getServicioActual(): Servicio? {
        return servicioActual
    }

    // NUEVO MÉTODO: Para detener el servicio de ubicación
    private fun detenerServicioUbicacion() {
        Log.d("MainViewModel", "Deteniendo servicio de ubicación")
        val intent = Intent(getApplication(), LocationService::class.java)
        getApplication<Application>().stopService(intent)
    }

    // NUEVO MÉTODO: Para cancelar el servicio actual
    fun cancelarServicioActual() {
        Log.d("MainViewModel", "Cancelando servicio actual")
        
        // Detener cualquier tracking de ubicación activo
        detenerServicioUbicacion()
        
        // Resetear todas las variables al estado inicial
        resetVariables()
        
        Log.d("MainViewModel", "Servicio cancelado y variables reseteadas")
    }

    // NUEVO MÉTODO: Para verificar si se puede volver atrás
    fun puedeVolverAtras(): Boolean {
        return _servicioEnProgreso.value == true
    }

    // NUEVO MÉTODO: Para verificar si el tipo de servicio cambió
    fun tipoServicioHaCambiado(): Boolean {
        return _tipoServicioCambiado.value == true
    }
}