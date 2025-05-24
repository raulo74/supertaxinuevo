package com.rueda.supertaxi.view

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.rueda.supertaxi.R
import com.rueda.supertaxi.databinding.ActivityMainBinding
import com.rueda.supertaxi.model.TipoServicio
import com.rueda.supertaxi.util.LocationService
import com.rueda.supertaxi.viewmodel.MainViewModel
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    private lateinit var tipoServicioAdapter: ArrayAdapter<String>
    private lateinit var tipoPagoAdapter: ArrayAdapter<String>
    
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val latitude = it.getDoubleExtra("latitude", 0.0)
                val longitude = it.getDoubleExtra("longitude", 0.0)
                val isTrackingRoute1 = it.getBooleanExtra("isTrackingRoute1", false)
                val isTrackingRoute2 = it.getBooleanExtra("isTrackingRoute2", false)
                
                if (isTrackingRoute1) {
                    viewModel.addLocationToRoute(latitude, longitude, true)
                } else if (isTrackingRoute2) {
                    viewModel.addLocationToRoute(latitude, longitude, false)
                }
                
                // Actualizar UI con la información de ruta si es necesario
                updateRouteUI(isTrackingRoute1, isTrackingRoute2)
            }
        }
    }
    
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d("MainActivity", "Todos los permisos concedidos")
            setupApp()
        } else {
            Log.e("MainActivity", "Algunos permisos fueron denegados")
            Toast.makeText(
                this,
                "Sin los permisos necesarios, la aplicación no funcionará correctamente",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
    
    private val detalleActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            if (result.data?.getBooleanExtra("REINICIAR_SERVICIO", false) == true) {
                Log.d("MainActivity", "Recibida señal de reinicio desde DetalleActivity")
                resetUI()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar el tema guardado antes de super.onCreate()
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val darkMode = prefs.getInt(SettingsActivity.KEY_DARK_MODE, SettingsActivity.DARK_MODE_AUTO)
        
        when (darkMode) {
            SettingsActivity.DARK_MODE_AUTO -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
            SettingsActivity.DARK_MODE_DAY -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            SettingsActivity.DARK_MODE_NIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
        
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Configurar la barra de navegación inferior
            setupBottomNavigation()
            
            checkPermissions()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en onCreate", e)
            Toast.makeText(this, "Error al iniciar la aplicación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_servicio -> {
                    // Ya estamos en la pantalla de servicio
                    true
                }
                R.id.nav_historial -> {
                    val intent = Intent(this, ResumenActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_ingresos -> {
                    Toast.makeText(this, "Ingresos - Funcionalidad pendiente", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_ajustes -> {
                    // Navegar a la pantalla de ajustes
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupApp() {
        try {
            Log.d("MainActivity", "Iniciando setupApp")
            
            setupSpinners()
            setupObservers()
            setupListeners()
            
            // Establecer "Parada de taxis" como predeterminado al iniciar la app
            viewModel.setTipoServicio("Parada de taxis")
            
            LocalBroadcastManager.getInstance(this).registerReceiver(
                locationReceiver,
                IntentFilter("LOCATION_UPDATE")
            )
            
            Log.d("MainActivity", "Configuración completada con éxito")
            
            if (isFirstRun()) {
                showInitialDialog()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en setupApp", e)
            Toast.makeText(this, "Error al configurar la aplicación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun checkPermissions() {
        // Lista base de permisos
        val basePermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        // Permisos de almacenamiento basados en la versión Android
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) { // Android 12L (32) o menor
            basePermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) { // Android 10 (29) o menor
                basePermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else { // Android 13+ (33+)
            basePermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            basePermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            basePermissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }
        
        // Permisos para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (33)
            basePermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Permisos para el servicio de ubicación en segundo plano
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+ (29+)
            basePermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        
        // Comprobar qué permisos necesitan ser solicitados
        val permissionsToRequest = basePermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isEmpty()) {
            Log.d("MainActivity", "Todos los permisos ya están concedidos")
            setupApp()
        } else {
            Log.d("MainActivity", "Solicitando permisos: ${permissionsToRequest.joinToString()}")
            permissionsLauncher.launch(permissionsToRequest)
        }
    }
    
    private fun setupSpinners() {
        // Configurar spinner de tipo de servicio
        tipoServicioAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            mutableListOf<String>()
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spinnerTipoServicio.adapter = tipoServicioAdapter
        
        // Configurar spinner de tipo de pago
        val tiposPago = arrayOf(
            getString(R.string.efectivo),
            getString(R.string.tarjeta),
            getString(R.string.aplicacion)
        )
        tipoPagoAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            tiposPago
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spinnerTipoPago.adapter = tipoPagoAdapter
    }
    
    private fun setupObservers() {
        // Observar tipos de servicio
        viewModel.allTiposServicio.observe(this) { tiposServicio ->
            val tiposServicioList = tiposServicio.map { it.nombre }.toMutableList()
            
            // Agregar opción para añadir nuevo tipo
            tiposServicioList.add("+ Añadir nuevo tipo")
            
            tipoServicioAdapter.clear()
            tipoServicioAdapter.addAll(tiposServicioList)
            
            // Si no hay selección actual o es la primera vez, seleccionar "Parada de taxis"
            if (binding.spinnerTipoServicio.selectedItemPosition < 0 || 
                viewModel.tipoServicio.value.isNullOrEmpty()) {
                val indexParadaTaxis = tiposServicioList.indexOf("Parada de taxis")
                if (indexParadaTaxis >= 0) {
                    binding.spinnerTipoServicio.setSelection(indexParadaTaxis)
                    viewModel.setTipoServicio("Parada de taxis")
                }
            }
        }
        
        // Observar estados de los botones con estilo mejorado
        viewModel.empezarEnabled.observe(this) { enabled ->
            binding.btnEmpezar.isEnabled = enabled
            updateButtonStyle(binding.btnEmpezar, enabled, R.color.colorPrimary)
        }
        
        viewModel.inicioServicioEnabled.observe(this) { enabled ->
            binding.btnInicioServicio.isEnabled = enabled
            updateButtonStyle(binding.btnInicioServicio, enabled, R.color.colorAccent)
            
            // Mostrar/ocultar sección de mapa
            binding.cardMapaPreview.isVisible = enabled || viewModel.finServicioEnabled.value == true
        }
        
        viewModel.finServicioEnabled.observe(this) { enabled ->
            binding.btnFinServicio.isEnabled = enabled
            updateButtonStyle(binding.btnFinServicio, enabled, R.color.colorRed)
            
            // Mostrar/ocultar controles de pago
            binding.layoutDatosPago.isVisible = enabled
            binding.editImporte.isEnabled = enabled
            binding.editComision.isEnabled = enabled
            binding.spinnerTipoPago.isEnabled = enabled
        }
        
        viewModel.resumenEnabled.observe(this) { enabled ->
            binding.btnResumenServicio.isEnabled = enabled
            updateButtonStyle(binding.btnResumenServicio, enabled, R.color.colorPurple)
        }
        
        // Observar datos de distancia y tiempo para mostrar en la UI
        viewModel.km1.observe(this) { km ->
            if (km > 0) {
                binding.tvDistanciaHastaCliente.text = String.format("%.1f km", km)
            }
        }
        
        viewModel.km2.observe(this) { km ->
            if (km > 0) {
                binding.tvDistanciaServicio.text = String.format("%.1f km", km)
            }
        }
        
        // Si tienes LiveData para los tiempos, también puedes observarlos
        viewModel.hora1.observe(this) { hora1 ->
            viewModel.hora2.observe(this) { hora2 ->
                if (hora1 != null && hora2 != null) {
                    // Calcular y mostrar el tiempo hasta la recogida
                    updateTiempoUI(hora1, hora2, binding.tvTiempoHastaCliente)
                }
            }
        }
        
        viewModel.hora2.observe(this) { hora2 ->
            viewModel.hora3.observe(this) { hora3 ->
                if (hora2 != null && hora3 != null) {
                    // Calcular y mostrar el tiempo del servicio
                    updateTiempoUI(hora2, hora3, binding.tvTiempoServicio)
                }
            }
        }
    }
    
    private fun updateButtonStyle(button: android.widget.Button, enabled: Boolean, colorResId: Int) {
        if (enabled) {
            button.setBackgroundResource(R.drawable.bg_button_enabled)
            button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            button.backgroundTintList = ContextCompat.getColorStateList(this, colorResId)
        } else {
            button.setBackgroundResource(R.drawable.bg_button_disabled)
            button.setTextColor(ContextCompat.getColor(this, R.color.colorTextDisabled))
            button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorDisabled)
        }
    }
    
    private fun updateTiempoUI(horaInicio: java.time.LocalTime, horaFin: java.time.LocalTime, textView: android.widget.TextView) {
        val minutos = java.time.Duration.between(horaInicio, horaFin).toMinutes()
        textView.text = "$minutos min"
    }
    
    private fun updateRouteUI(isTrackingRoute1: Boolean, isTrackingRoute2: Boolean) {
        binding.routeStatusText.text = when {
            isTrackingRoute1 -> "Ruta hasta el cliente..."
            isTrackingRoute2 -> "Ruta del servicio en curso..."
            else -> if (viewModel.resumenEnabled.value == true) "Servicio finalizado" else ""
        }
        
        // Mostrar/ocultar paneles según el estado del tracking
        binding.panelRutaCliente.isVisible = isTrackingRoute1 || (viewModel.km1.value ?: 0.0) > 0
        binding.panelRutaServicio.isVisible = isTrackingRoute2 || (viewModel.km2.value ?: 0.0) > 0
    }
    
    private fun setupListeners() {
        // Spinner tipo de servicio
        binding.spinnerTipoServicio.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position) as String
                if (selected == "+ Añadir nuevo tipo") {
                    showAddTipoServicioDialog()
                    
                    // Revertir a la selección anterior
                    if (parent.count > 1) {
                        parent.setSelection(0)
                    }
                } else {
                    viewModel.setTipoServicio(selected)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Botón empezar con nueva estética
        binding.btnEmpezar.setOnClickListener {
            viewModel.empezarServicio()
            
            // Animación para mostrar el panel de mapa
            binding.cardMapaPreview.alpha = 0f
            binding.cardMapaPreview.isVisible = true
            binding.cardMapaPreview.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
        
        // Botón inicio servicio
        binding.btnInicioServicio.setOnClickListener {
            viewModel.inicioServicio()
            
            // Animación para mostrar el panel de datos de pago
            binding.layoutDatosPago.alpha = 0f
            binding.layoutDatosPago.isVisible = true
            binding.layoutDatosPago.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
        
        // Botón fin servicio
        binding.btnFinServicio.setOnClickListener {
            // Obtener datos de los campos
            val importeStr = binding.editImporte.text.toString()
            val comisionStr = binding.editComision.text.toString()
            val tipoPago = binding.spinnerTipoPago.selectedItem.toString()
            
            // Validar datos
            if (importeStr.isBlank() || comisionStr.isBlank()) {
                // Utilizamos Material Design para el mensaje de error
                binding.editImporte.error = if (importeStr.isBlank()) "Campo requerido" else null
                binding.editComision.error = if (comisionStr.isBlank()) "Campo requerido" else null
                return@setOnClickListener
            }
            
            try {
                val importe = importeStr.toDouble()
                val comision = comisionStr.toDouble()
                
                viewModel.setImporte(importe)
                viewModel.setComision(comision)
                viewModel.setTipoPago(tipoPago)
                viewModel.finServicio()
                
                // Actualizar UI para mostrar "Servicio finalizado"
                binding.routeStatusText.text = "Servicio finalizado"
            } catch (e: NumberFormatException) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Error de formato")
                    .setMessage("Los valores de importe y comisión deben ser números válidos")
                    .setPositiveButton("Entendido", null)
                    .show()
            }
        }
        
        // Botón resumen servicio con transición moderna
        binding.btnResumenServicio.setOnClickListener {
            val intent = Intent(this, DetalleActivity::class.java).apply {
                putExtra("SERVICIO_ID", viewModel.currentServicioId.value)
            }
            
            // Uso de ActivityOptionsCompat para animación de transición
            val options = androidx.core.app.ActivityOptionsCompat.makeCustomAnimation(
                this,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            
            detalleActivityLauncher.launch(intent, options)
        }
    }
    
    private fun showAddTipoServicioDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tipo_servicio, null)
        val editTipoServicio = dialogView.findViewById<TextInputEditText>(R.id.editTipoServicio)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ingresar_tipo_servicio)
            .setView(dialogView)
            .setPositiveButton(R.string.aniadir) { _, _ ->
                val nuevoTipo = editTipoServicio.text.toString().trim()
                if (nuevoTipo.isNotEmpty()) {
                    viewModel.addTipoServicio(nuevoTipo)
                    
                    // Feedback visual
                    Toast.makeText(
                        this,
                        "Tipo de servicio '${nuevoTipo}' añadido",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }
    
    private fun showInitialDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Configuración inicial")
            .setMessage(R.string.definir_apps_trabajo)
            .setPositiveButton("Entendido") { _, _ ->
                setFirstRunComplete()
                showAddTipoServicioDialog()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun isFirstRun(): Boolean {
        val prefs = getSharedPreferences("SuperTaxiPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("isFirstRun", true)
    }
    
    private fun setFirstRunComplete() {
        val prefs = getSharedPreferences("SuperTaxiPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("isFirstRun", false).apply()
    }
    
    private fun resetUI() {
        // Reiniciar el ViewModel
        viewModel.resetVariables()
        
        // Limpiar campos de UI
        binding.editImporte.setText("")
        binding.editImporte.error = null
        binding.editComision.setText("")
        binding.editComision.error = null
        binding.spinnerTipoPago.setSelection(0)
        
        // Establecer "Parada de taxis" en el spinner
        val adapter = binding.spinnerTipoServicio.adapter
        if (adapter != null) {
            for (i in 0 until adapter.count) {
                if (adapter.getItem(i).toString() == "Parada de taxis") {
                    binding.spinnerTipoServicio.setSelection(i)
                    break
                }
            }
        }
        
        // Ocultar paneles que deberían estar ocultos al inicio
        binding.cardMapaPreview.isVisible = false
        binding.layoutDatosPago.isVisible = false
        binding.panelRutaCliente.isVisible = false
        binding.panelRutaServicio.isVisible = false
        binding.separadorRutas.isVisible = false
        
        // Resetear textos informativos
        binding.tvDistanciaHastaCliente.text = "0.0 km"
        binding.tvDistanciaServicio.text = "0.0 km"
        binding.tvTiempoHastaCliente.text = "0 min"
        binding.tvTiempoServicio.text = "0 min"
        binding.routeStatusText.text = ""
        
        Log.d("MainActivity", "UI reiniciada con Parada de taxis como predeterminado")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
    }
} 