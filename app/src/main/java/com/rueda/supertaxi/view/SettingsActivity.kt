package com.rueda.supertaxi.view

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        database = AppDatabase.getDatabase(this)
        
        setupToolbar()
        setupDarkModeSettings()
        setupResetButtons()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
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