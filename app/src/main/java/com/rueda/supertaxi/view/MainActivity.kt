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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.rueda.supertaxi.R
import com.rueda.supertaxi.databinding.ActivityMainBinding
import com.rueda.supertaxi.viewmodel.MainViewModel
import com.rueda.supertaxi.util.LocationService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var locationService: LocationService
    
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
            // Verificar si necesitamos reiniciar el servicio
            if (result.data?.getBooleanExtra("REINICIAR_SERVICIO", false) == true) {
                Log.d("MainActivity", "Recibida señal de reinicio desde DetalleActivity")
                // Reiniciar todas las variables y configuración
                viewModel.resetVariables()
                
                // Restablecer UI
                binding.editImporte.setText("")
                binding.editComision.setText("")
                binding.spinnerTipoPago.setSelection(0)
                
                // Log para depuración
                Log.d("MainActivity", "Variables del servicio reiniciadas")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d("MainActivity", "onCreate iniciado")
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            checkPermissions()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en onCreate", e)
            Toast.makeText(this, "Error al iniciar la aplicación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupApp() {
        try {
            Log.d("MainActivity", "Iniciando setupApp")
            
            setupSpinners()
            setupObservers()
            setupListeners()
            
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
        tipoServicioAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            mutableListOf<String>()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerTipoServicio.adapter = tipoServicioAdapter
        
        val tiposPago = arrayOf(
            getString(R.string.efectivo),
            getString(R.string.tarjeta),
            getString(R.string.aplicacion)
        )
        tipoPagoAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tiposPago
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerTipoPago.adapter = tipoPagoAdapter
    }
    
    private fun setupObservers() {
        viewModel.allTiposServicio.observe(this) { tiposServicio ->
            val tiposServicioList = tiposServicio.map { it.nombre }.toMutableList()
            tiposServicioList.add("+ Añadir nuevo tipo")
            
            tipoServicioAdapter.clear()
            tipoServicioAdapter.addAll(tiposServicioList)
        }
        
        viewModel.empezarEnabled.observe(this) { enabled ->
            binding.btnEmpezar.isEnabled = enabled
            binding.btnEmpezar.alpha = if (enabled) 1.0f else 0.5f
        }
        
        viewModel.inicioServicioEnabled.observe(this) { enabled ->
            binding.btnInicioServicio.isEnabled = enabled
            binding.btnInicioServicio.alpha = if (enabled) 1.0f else 0.5f
        }
        
        viewModel.finServicioEnabled.observe(this) { enabled ->
            binding.btnFinServicio.isEnabled = enabled
            binding.btnFinServicio.alpha = if (enabled) 1.0f else 0.5f
            
            binding.editImporte.isEnabled = enabled
            binding.editComision.isEnabled = enabled
            binding.spinnerTipoPago.isEnabled = enabled
        }
        
        viewModel.resumenEnabled.observe(this) { enabled ->
            binding.btnResumenServicio.isEnabled = enabled
            binding.btnResumenServicio.alpha = if (enabled) 1.0f else 0.5f
        }
    }
    
    private fun setupListeners() {
        binding.spinnerTipoServicio.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position) as String
                if (selected == "+ Añadir nuevo tipo") {
                    showAddTipoServicioDialog()
                    if (parent.count > 1) {
                        parent.setSelection(0)
                    }
                } else {
                    viewModel.setTipoServicio(selected)
                    Log.d("MainActivity", "Tipo de servicio seleccionado: $selected")
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        binding.btnEmpezar.setOnClickListener {
            viewModel.empezarServicio()
            Log.d("MainActivity", "Botón Empezar pulsado")
        }
        
        binding.btnInicioServicio.setOnClickListener {
            viewModel.inicioServicio()
            Log.d("MainActivity", "Botón Inicio de Servicio pulsado")
        }
        
        binding.btnFinServicio.setOnClickListener {
            val importeStr = binding.editImporte.text.toString()
            val comisionStr = binding.editComision.text.toString()
            val tipoPago = binding.spinnerTipoPago.selectedItem.toString()
            
            if (importeStr.isBlank() || comisionStr.isBlank()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            try {
                val importe = importeStr.toDouble()
                val comision = comisionStr.toDouble()
                
                viewModel.setImporte(importe)
                viewModel.setComision(comision)
                viewModel.setTipoPago(tipoPago)
                viewModel.finServicio()
                
                Log.d("MainActivity", "Botón Fin de Servicio pulsado")
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Valor numérico inválido", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnResumenServicio.setOnClickListener {
            val intent = Intent(this, DetalleActivity::class.java).apply {
                putExtra("SERVICIO_ID", viewModel.currentServicioId.value)
            }
            detalleActivityLauncher.launch(intent)
            Log.d("MainActivity", "Botón Resumen de Servicio pulsado")
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
    
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
    }
} 