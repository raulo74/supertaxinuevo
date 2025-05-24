package com.rueda.supertaxi.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rueda.supertaxi.R
import com.rueda.supertaxi.database.AppDatabase
import com.rueda.supertaxi.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: AppDatabase
    
    companion object {
        const val PREFS_NAME = "SuperTaxiPrefs"
        const val KEY_DARK_MODE = "dark_mode"
        const val DARK_MODE_AUTO = 0
        const val DARK_MODE_DAY = 1
        const val DARK_MODE_NIGHT = 2
    }
    
    // Launcher para solicitar permisos múltiples
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        updatePermissionStatus()
        
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Todos los permisos han sido concedidos", Toast.LENGTH_SHORT).show()
        } else {
            val deniedPermissions = permissions.entries.filter { !it.value }.map { it.key }
            showPermissionsDeniedDialog(deniedPermissions)
        }
    }
    
    // Launcher para abrir configuración de la aplicación
    private val appSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Actualizar el estado de los permisos cuando el usuario regrese de configuración
        updatePermissionStatus()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        database = AppDatabase.getDatabase(this)
        
        setupToolbar()
        setupDarkModeSettings()
        setupPermissionsSection()
        setupResetButtons()
        
        // Actualizar el estado inicial de los permisos
        updatePermissionStatus()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupPermissionsSection() {
        // Botón para verificar permisos
        binding.btnCheckPermissions.setOnClickListener {
            checkAndShowPermissionStatus()
        }
        
        // Botón para solicitar permisos
        binding.btnRequestPermissions.setOnClickListener {
            requestAllPermissions()
        }
        
        // Botón para abrir configuración de la app
        binding.btnOpenAppSettings.setOnClickListener {
            openAppSettings()
        }
    }
    
    private fun updatePermissionStatus() {
        val permissions = getAllRequiredPermissions()
        var grantedCount = 0
        var totalCount = permissions.size
        
        val permissionDetails = StringBuilder()
        
        permissions.forEach { permission ->
            val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            if (isGranted) grantedCount++
            
            val permissionName = getPermissionDisplayName(permission)
            val status = if (isGranted) "✅ Concedido" else "❌ Denegado"
            permissionDetails.append("$permissionName: $status\n")
        }
        
        // Actualizar el texto de estado
        val statusText = "Permisos concedidos: $grantedCount de $totalCount"
        binding.tvPermissionStatus.text = statusText
        
        // Actualizar el detalle de permisos
        binding.tvPermissionDetails.text = permissionDetails.toString().trim()
        
        // Habilitar/deshabilitar botones según el estado
        val allGranted = grantedCount == totalCount
        binding.btnRequestPermissions.isEnabled = !allGranted
        binding.btnRequestPermissions.text = if (allGranted) {
            "Todos los permisos concedidos"
        } else {
            "Solicitar permisos faltantes"
        }
        
        // Cambiar color del indicador de estado
        val colorResId = if (allGranted) R.color.colorAccent else R.color.colorRed
        binding.tvPermissionStatus.setTextColor(ContextCompat.getColor(this, colorResId))
    }
    
    private fun getAllRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            // Permisos de ubicación (ESENCIALES)
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        // Permisos de almacenamiento según la versión de Android
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) { // Android 12L o menor
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) { // Android 10 o menor
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        // NOTA: Para Android 13+ no necesitamos permisos de media porque usamos getExternalFilesDir()
        
        // Permisos para notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Permisos para ubicación en segundo plano (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        
        return permissions
    }
    
    private fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> "Ubicación precisa"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Ubicación aproximada"
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "Ubicación en segundo plano"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Leer almacenamiento (para CSV)"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Escribir almacenamiento (para CSV)"
            Manifest.permission.POST_NOTIFICATIONS -> "Mostrar notificaciones"
            else -> permission.substringAfterLast(".")
        }
    }
    
    private fun checkAndShowPermissionStatus() {
        updatePermissionStatus()
        
        val permissions = getAllRequiredPermissions()
        val deniedPermissions = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (deniedPermissions.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Estado de permisos")
                .setMessage("✅ Todos los permisos necesarios están concedidos.\n\nLa aplicación funcionará correctamente.")
                .setPositiveButton("Entendido", null)
                .show()
        } else {
            val deniedList = deniedPermissions.joinToString("\n") { "• ${getPermissionDisplayName(it)}" }
            MaterialAlertDialogBuilder(this)
                .setTitle("Permisos faltantes")
                .setMessage("❌ Los siguientes permisos están denegados:\n\n$deniedList\n\n" +
                           "La aplicación puede no funcionar correctamente sin estos permisos.")
                .setPositiveButton("Solicitar permisos") { _, _ ->
                    requestAllPermissions()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
    
    private fun requestAllPermissions() {
        val permissions = getAllRequiredPermissions()
        val permissionsToRequest = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (permissionsToRequest.isEmpty()) {
            Toast.makeText(this, "Todos los permisos ya están concedidos", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Mostrar explicación antes de solicitar permisos
        val permissionNames = permissionsToRequest.joinToString("\n") { "• ${getPermissionDisplayName(it)}" }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Permisos necesarios")
            .setMessage("SuperTaxi necesita los siguientes permisos para funcionar correctamente:\n\n" +
                       "$permissionNames\n\n" +
                       "• Ubicación: Para rastrear las rutas de los servicios de taxi\n" +
                       "• Almacenamiento: Para guardar el archivo CSV con los datos de los servicios\n" +
                       "• Notificaciones: Para mostrar el estado del servicio mientras está activo\n" +
                       "• Ubicación en segundo plano: Para continuar el rastreo cuando cambias de app\n\n" +
                       "¿Deseas conceder estos permisos?")
            .setPositiveButton("Conceder permisos") { _, _ ->
                permissionsLauncher.launch(permissionsToRequest.toTypedArray())
            }
            .setNegativeButton("Ahora no", null)
            .show()
    }
    
    private fun showPermissionsDeniedDialog(deniedPermissions: List<String>) {
        val deniedList = deniedPermissions.joinToString("\n") { "• ${getPermissionDisplayName(it)}" }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Permisos denegados")
            .setMessage("Los siguientes permisos fueron denegados:\n\n$deniedList\n\n" +
                       "Para que la aplicación funcione correctamente, necesitas conceder estos permisos " +
                       "desde la configuración de la aplicación.")
            .setPositiveButton("Ir a configuración") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Más tarde", null)
            .show()
    }
    
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            appSettingsLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir la configuración de la aplicación", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupDarkModeSettings() {
        // Obtener el modo actual
        val currentMode = sharedPreferences.getInt(KEY_DARK_MODE, DARK_MODE_AUTO)
        
        // Establecer el estado inicial del RadioGroup
        when (currentMode) {
            DARK_MODE_AUTO -> binding.radioAuto.isChecked = true
            DARK_MODE_DAY -> binding.radioDay.isChecked = true
            DARK_MODE_NIGHT -> binding.radioNight.isChecked = true
        }
        
        // Configurar listeners para los RadioButtons
        binding.radioGroupDarkMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioAuto -> DARK_MODE_AUTO
                R.id.radioDay -> DARK_MODE_DAY
                R.id.radioNight -> DARK_MODE_NIGHT
                else -> DARK_MODE_AUTO
            }
            
            // Guardar la preferencia
            sharedPreferences.edit().putInt(KEY_DARK_MODE, mode).apply()
            
            // Aplicar el modo
            applyDarkMode(mode)
        }
    }
    
    private fun applyDarkMode(mode: Int) {
        when (mode) {
            DARK_MODE_AUTO -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
            DARK_MODE_DAY -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            DARK_MODE_NIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }
    
    private fun setupResetButtons() {
        // Botón para resetear CSV
        binding.btnResetCsv.setOnClickListener {
            showResetCsvDialog()
        }
        
        // Botón para resetear base de datos
        binding.btnResetDatabase.setOnClickListener {
            showResetDatabaseDialog()
        }
        
        // Botón para resetear todo
        binding.btnResetAll.setOnClickListener {
            showResetAllDialog()
        }
    }
    
    private fun showResetCsvDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Resetear archivo CSV")
            .setMessage("¿Estás seguro de que deseas eliminar el archivo CSV? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                resetCsvFile()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showResetDatabaseDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Resetear base de datos")
            .setMessage("¿Estás seguro de que deseas eliminar todos los servicios de la base de datos? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                resetDatabase()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showResetAllDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Resetear todo")
            .setMessage("¿Estás seguro de que deseas eliminar TODOS los datos (CSV y base de datos)? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar todo") { _, _ ->
                resetAll()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun resetCsvFile() {
        try {
            val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            }
            
            val file = File(directory, "Registroapp.csv")
            
            if (file.exists()) {
                if (file.delete()) {
                    Toast.makeText(this, "Archivo CSV eliminado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al eliminar el archivo CSV", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No se encontró el archivo CSV", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun resetDatabase() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Eliminar todos los servicios
                    database.servicioDao().deleteAllServicios()
                    
                    // Eliminar tipos de servicio no predefinidos
                    database.tipoServicioDao().deleteNonPredefinedTipos()
                }
                
                Toast.makeText(
                    this@SettingsActivity, 
                    "Base de datos reseteada correctamente", 
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@SettingsActivity, 
                    "Error al resetear la base de datos: ${e.message}", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun resetAll() {
        // Primero resetear el CSV
        resetCsvFile()
        
        // Luego resetear la base de datos
        resetDatabase()
        
        Toast.makeText(this, "Todos los datos han sido eliminados", Toast.LENGTH_LONG).show()
    }
} 