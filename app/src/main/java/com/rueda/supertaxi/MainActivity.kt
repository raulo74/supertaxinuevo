
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.rueda.supertaxi.R
import com.rueda.supertaxi.databinding.ActivityMainBinding
import com.rueda.supertaxi.model.TipoServicio
import com.rueda.supertaxi.util.LocationService
import com.rueda.supertaxi.viewmodel.MainViewModel
import com.rueda.supertaxi.view.DetalleActivity

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
            // Todos los permisos concedidos
            setupApp()
        } else {
            // Algún permiso denegado
            Toast.makeText(
                this,
                "Sin los permisos necesarios, la aplicación no funcionará correctamente",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
    }

    private fun setupApp() {
        setupSpinners()
        setupObservers()
        setupListeners()

        // Registrar el receptor de ubicación
        LocalBroadcastManager.getInstance(this).registerReceiver(
            locationReceiver,
            IntentFilter("LOCATION_UPDATE")
        )

        // Mostrar diálogo inicial solo la primera vez
        if (isFirstRun()) {
            showInitialDialog()
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        // Permisos específicos para Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        // Comprobar si tenemos todos los permisos
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            // Ya tenemos todos los permisos
            setupApp()
        } else {
            // Solicitar permisos
            permissionsLauncher.launch(permissionsToRequest)
        }
    }

    private fun setupSpinners() {
        // Configurar spinner de tipo de servicio
        tipoServicioAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            mutableListOf<String>()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
            android.R.layout.simple_spinner_item,
            tiposPago
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
        }

        // Observar estados de los botones
        viewModel.empezarEnabled.observe(this) { enabled ->
            binding.btnEmpezar.isEnabled = enabled
        }

        viewModel.inicioServicioEnabled.observe(this) { enabled ->
            binding.btnInicioServicio.isEnabled = enabled
        }

        viewModel.finServicioEnabled.observe(this) { enabled ->
            binding.btnFinServicio.isEnabled = enabled
            binding.editImporte.isEnabled = enabled
            binding.editComision.isEnabled = enabled
            binding.spinnerTipoPago.isEnabled = enabled
        }

        viewModel.resumenEnabled.observe(this) { enabled ->
            binding.btnResumenServicio.isEnabled = enabled
        }
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

        // Botón empezar
        binding.btnEmpezar.setOnClickListener {
            viewModel.empezarServicio()
        }

        // Botón inicio servicio
        binding.btnInicioServicio.setOnClickListener {
            viewModel.inicioServicio()
        }

        // Botón fin servicio
        binding.btnFinServicio.setOnClickListener {
            // Obtener datos de los campos
            val importeStr = binding.editImporte.text.toString()
            val comisionStr = binding.editComision.text.toString()
            val tipoPago = binding.spinnerTipoPago.selectedItem.toString()

            // Validar datos
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
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Valor numérico inválido", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón resumen servicio
        binding.btnResumenServicio.setOnClickListener {
            val intent = Intent(this, DetalleActivity::class.java).apply {
                putExtra("SERVICIO_ID", viewModel.currentServicioId.value)
            }
            startActivity(intent)
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
                // Guardar que ya se mostró este diálogo
                setFirstRunComplete()

                // Mostrar diálogo para añadir tipos de servicio
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