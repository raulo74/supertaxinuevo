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
import androidx.activity.OnBackPressedCallback
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
import com.rueda.supertaxi.viewmodel.MainViewModel
import androidx.appcompat.app.AppCompatDelegate
import android.widget.TextView

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
            
            // ASEGURAR que la tarjeta esté oculta al inicio
            ocultarTarjetaCronometroCompletamente()
            
            // NUEVO: Configurar el manejo del botón de navegación hacia atrás
            setupOnBackPressedCallback()
            
            // Configurar la barra de navegación inferior
            setupBottomNavigation()
            
            checkPermissions()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en onCreate", e)
            Toast.makeText(this, "Error al iniciar la aplicación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // NUEVO MÉTODO: Configurar el callback para el botón de navegación hacia atrás
    private fun setupOnBackPressedCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("MainActivity", "Botón de navegación hacia atrás presionado")
                
                // Verificar si hay un servicio en progreso
                if (viewModel.puedeVolverAtras()) {
                    Log.d("MainActivity", "Hay un servicio en progreso, mostrando diálogo de confirmación")
                    mostrarDialogoCancelarServicio()
                } else {
                    Log.d("MainActivity", "No hay servicio en progreso, comportamiento normal")
                    // Comportamiento normal: cerrar la aplicación o minimizar
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    
    // NUEVO MÉTODO: Mostrar diálogo para confirmar cancelación del servicio
    private fun mostrarDialogoCancelarServicio() {
        val tipoServicioCambio = viewModel.tipoServicioHaCambiado()
        
        val titulo = if (tipoServicioCambio) {
            getString(R.string.aplicar_cambio_titulo)
        } else {
            getString(R.string.cancelar_servicio_titulo)
        }
        
        val mensaje = if (tipoServicioCambio) {
            getString(R.string.aplicar_cambio_mensaje)
        } else {
            getString(R.string.cancelar_servicio_mensaje)
        }
        
        val botonPositivo = if (tipoServicioCambio) {
            getString(R.string.si_aplicar_cambio)
        } else {
            getString(R.string.si_cancelar_servicio)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton(botonPositivo) { _, _ ->
                Log.d("MainActivity", "Usuario confirmó cancelación del servicio")
                cancelarServicioYReiniciar()
            }
            .setNegativeButton(getString(R.string.no_continuar_servicio)) { dialog, _ ->
                Log.d("MainActivity", "Usuario canceló la cancelación del servicio")
                dialog.dismiss()
            }
            .setNeutralButton(getString(R.string.salir_app)) { _, _ ->
                Log.d("MainActivity", "Usuario eligió salir de la aplicación")
                finish()
            }
            .setCancelable(false) // No permitir cancelar tocando fuera del diálogo
            .show()
    }
    
    // NUEVO MÉTODO: Cancelar servicio y reiniciar la aplicación
    private fun cancelarServicioYReiniciar() {
        Log.d("MainActivity", "Iniciando cancelación y reinicio del servicio")
        
        try {
            // Cancelar el servicio actual en el ViewModel
            viewModel.cancelarServicioActual()
            
            // Reiniciar la UI
            resetUI()
            
            // Mostrar confirmación al usuario
            Toast.makeText(
                this,
                getString(R.string.servicio_cancelado),
                Toast.LENGTH_LONG
            ).show()
            
            Log.d("MainActivity", "Cancelación y reinicio completados exitosamente")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error durante la cancelación del servicio", e)
            Toast.makeText(
                this,
                "Error al cancelar el servicio: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
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
                    // CAMBIO: Permitir navegación libre sin cancelar servicio
                    Log.d("MainActivity", "Navegando a Historial - Servicio en progreso: ${viewModel.servicioEnProgreso.value}")
                    val intent = Intent(this, ResumenActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_ingresos -> {
                    // CAMBIO: Permitir navegación libre sin cancelar servicio
                    Log.d("MainActivity", "Navegando a Ingresos - Servicio en progreso: ${viewModel.servicioEnProgreso.value}")
                    val intent = Intent(this, IngresosActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_ajustes -> {
                    // Para ajustes sí mantenemos la confirmación porque podría afectar permisos o configuración
                    if (viewModel.puedeVolverAtras()) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(getString(R.string.servicio_en_progreso))
                            .setMessage("¿Quieres ir a ajustes? El servicio actual continuará en segundo plano.")
                            .setPositiveButton("Sí, ir a ajustes") { _, _ ->
                                val intent = Intent(this, SettingsActivity::class.java)
                                startActivity(intent)
                            }
                            .setNegativeButton(getString(R.string.no_continuar_servicio), null)
                            .show()
                    } else {
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                    }
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
        
        // NUEVO OBSERVER: Observar cambios en el tipo de servicio durante un servicio activo
        viewModel.tipoServicioCambiado.observe(this) { cambiado ->
            if (cambiado) {
                // Cambiar el color de fondo del spinner para indicar que hay un cambio pendiente
                binding.spinnerTipoServicio.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.colorRed)
                )
                
                // Mostrar un mensaje sutil al usuario
                Toast.makeText(
                    this,
                    getString(R.string.tipo_servicio_cambiado),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // Restaurar el color normal del spinner
                binding.spinnerTipoServicio.setBackgroundColor(
                    ContextCompat.getColor(this, android.R.color.transparent)
                )
            }
        }
        
        // NUEVO OBSERVER: Observar el estado del servicio en progreso
        viewModel.servicioEnProgreso.observe(this) { enProgreso ->
            Log.d("MainActivity", "Servicio en progreso: $enProgreso")
            
            // Cambiar el título de la toolbar para indicar el estado
            if (enProgreso) {
                // Cambiar título para indicar servicio activo
                binding.toolbar.findViewById<TextView>(R.id.toolbar_title)?.apply {
                    text = "SuperTaxi - ⏱️ Servicio activo"
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.colorAccent))
                } ?: run {
                    // Si no encuentra el TextView, usar el método tradicional
                    supportActionBar?.title = "SuperTaxi - ⏱️ Servicio activo"
                }
            } else {
                // Restaurar título normal
                binding.toolbar.findViewById<TextView>(R.id.toolbar_title)?.apply {
                    text = getString(R.string.app_name)
                    setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
                } ?: run {
                    // Si no encuentra el TextView, usar el método tradicional
                    supportActionBar?.title = getString(R.string.app_name)
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
            
            // Mostrar/ocultar sección de cronómetro
            binding.cardCronometro.isVisible = enabled || viewModel.finServicioEnabled.value == true
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
            
            // También habilitar el botón de siguiente servicio
            binding.btnSiguienteServicio.isEnabled = enabled
            updateButtonStyle(binding.btnSiguienteServicio, enabled, R.color.colorAccent)
        }

        // Observer para la tarjeta del cronómetro CON CONTROL ESTRICTO
        viewModel.cardCronometroVisible.observe(this) { visible ->
            Log.d("MainActivity", "=== CONTROL TARJETA CRONOMETRO ===")
            Log.d("MainActivity", "Comando visible: $visible")
            Log.d("MainActivity", "Tipo servicio: ${viewModel.tipoServicio.value}")
            Log.d("MainActivity", "Servicio en progreso: ${viewModel.servicioEnProgreso.value}")
            
            // Control estricto: solo mostrar si es Parada de taxis Y visible es true
            val deberiaEstarVisible = visible && viewModel.tipoServicio.value == "Parada de taxis"
            
            binding.cardCronometro.visibility = if (deberiaEstarVisible) View.VISIBLE else View.GONE
            binding.layoutContenidoCronometro.visibility = if (deberiaEstarVisible) View.VISIBLE else View.GONE
            
            Log.d("MainActivity", "Resultado final tarjeta: ${if (deberiaEstarVisible) "VISIBLE" else "GONE"}")
            Log.d("MainActivity", "==================================")
        }

        // Observer para el tipo de servicio que FUERZA ocultación
        viewModel.tipoServicio.observe(this) { tipo ->
            Log.d("MainActivity", "=== CAMBIO TIPO SERVICIO ===")
            Log.d("MainActivity", "Nuevo tipo: $tipo")
            
            // FORZAR ocultación si no es Parada de taxis
            if (tipo != "Parada de taxis") {
                Log.d("MainActivity", "FORZANDO ocultación - tipo no es Parada de taxis")
                binding.cardCronometro.visibility = View.GONE
                binding.layoutContenidoCronometro.visibility = View.GONE
            }
            
            Log.d("MainActivity", "============================")
        }

        // Observer para el cronómetro grande
        viewModel.cronometroGrandeVisible.observe(this) { visible ->
            binding.layoutCronometroGrande.visibility = if (visible) View.VISIBLE else View.GONE
            Log.d("MainActivity", "Cronómetro grande visible: $visible")
        }

        viewModel.cronometroGrande.observe(this) { tiempo ->
            binding.tvCronometroGrande.text = tiempo
            if (binding.layoutCronometroGrande.visibility == View.VISIBLE) {
                Log.d("MainActivity", "Cronómetro actualizado: $tiempo")
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
    
    private fun setupListeners() {
        // Spinner tipo de servicio CON CONTROL ADICIONAL
        binding.spinnerTipoServicio.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position) as String
                Log.d("MainActivity", "Spinner seleccionado: $selected")
                
                if (selected == "+ Añadir nuevo tipo") {
                    showAddTipoServicioDialog()
                    
                    // Revertir a la selección anterior
                    if (parent.count > 1) {
                        parent.setSelection(0)
                    }
                } else {
                    Log.d("MainActivity", "Llamando setTipoServicio con: $selected")
                    viewModel.setTipoServicio(selected)
                    
                    // CONTROL ADICIONAL: Si no es Parada de taxis, ocultar inmediatamente
                    if (selected != "Parada de taxis") {
                        ocultarTarjetaCronometroCompletamente()
                    }
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("MainActivity", "Spinner: nada seleccionado")
                // Por seguridad, ocultar tarjeta
                ocultarTarjetaCronometroCompletamente()
            }
        }
        
        // Botón empezar con nueva estética
        binding.btnEmpezar.setOnClickListener {
            viewModel.empezarServicio()
            
            // Animación para mostrar el panel de cronómetro
            binding.cardCronometro.alpha = 0f
            binding.cardCronometro.isVisible = true
            binding.cardCronometro.animate()
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
            
            // Validar solo el importe (la comisión puede estar vacía)
            if (importeStr.isBlank()) {
                binding.editImporte.error = "Campo requerido"
                return@setOnClickListener
            }
            
            try {
                val importe = importeStr.toDouble()
                // Si la comisión está vacía, usar 0.0
                val comision = if (comisionStr.isBlank()) 0.0 else comisionStr.toDouble()
                
                // Limpiar cualquier error previo en el campo comisión
                binding.editComision.error = null
                
                viewModel.setImporte(importe)
                viewModel.setComision(comision)
                viewModel.setTipoPago(tipoPago)
                viewModel.finServicio()
            } catch (e: NumberFormatException) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Error de formato")
                    .setMessage("El valor del importe debe ser un número válido")
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

        // Botón siguiente servicio
        binding.btnSiguienteServicio.setOnClickListener {
            Log.d("MainActivity", "Botón Siguiente Servicio pulsado")
            
            // Verificar que tenemos datos para guardar
            val servicioId = viewModel.currentServicioId.value
            if (servicioId == null || servicioId == 0L) {
                Log.e("MainActivity", "No hay servicio actual")
                Toast.makeText(this, "Error: No hay datos de servicio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Reiniciar directamente para el siguiente servicio
            resetUI()
            Toast.makeText(this, "Listo para el siguiente servicio", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "UI reiniciada para nuevo servicio")
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
        
        // FORZAR ocultación de tarjeta cronómetro
        ocultarTarjetaCronometroCompletamente()
        
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
        binding.layoutDatosPago.isVisible = false
        
        // Resetear ambos botones finales
        binding.btnResumenServicio.isEnabled = false
        binding.btnSiguienteServicio.isEnabled = false
        updateButtonStyle(binding.btnResumenServicio, false, R.color.colorPurple)
        updateButtonStyle(binding.btnSiguienteServicio, false, R.color.colorAccent)
        
        Log.d("MainActivity", "UI reiniciada con Parada de taxis como predeterminado")
    }
    
    // AÑADIR método para ocultar forzosamente la tarjeta
    private fun ocultarTarjetaCronometroCompletamente() {
        binding.cardCronometro.visibility = View.GONE
        binding.layoutContenidoCronometro.visibility = View.GONE
        binding.layoutCronometroGrande.visibility = View.GONE
        Log.d("MainActivity", "Tarjeta cronómetro FORZADAMENTE oculta")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
    }
} 