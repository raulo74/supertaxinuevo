package com.rueda.supertaxi.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.rueda.supertaxi.R
import com.rueda.supertaxi.adapter.ServicioAdapter
import com.rueda.supertaxi.databinding.ActivityResumenBinding
import com.rueda.supertaxi.model.Servicio
import com.rueda.supertaxi.viewmodel.FiltroTiempo
import com.rueda.supertaxi.viewmodel.ResumenViewModel

class ResumenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResumenBinding
    private val viewModel: ResumenViewModel by viewModels()
    private lateinit var adapter: ServicioAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResumenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupChipGroup()
        setupObservers()
        setupListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = ServicioAdapter(
            onItemClick = { servicio ->
                mostrarDetalleServicio(servicio)
            },
            onDetallesClick = { servicio ->
                mostrarDetalleServicio(servicio)
            },
            onEliminarClick = { servicio ->
                confirmarEliminacion(servicio)
            }
        )
        
        binding.recyclerServicios.apply {
            layoutManager = LinearLayoutManager(this@ResumenActivity)
            adapter = this@ResumenActivity.adapter
        }
    }
    
    private fun setupChipGroup() {
        binding.chipHoy.setOnClickListener { 
            viewModel.cambiarFiltro(FiltroTiempo.HOY)
        }
        
        binding.chipSemana.setOnClickListener { 
            viewModel.cambiarFiltro(FiltroTiempo.SEMANA)
        }
        
        binding.chipMes.setOnClickListener { 
            viewModel.cambiarFiltro(FiltroTiempo.MES)
        }
        
        binding.chipTodo.setOnClickListener { 
            viewModel.cambiarFiltro(FiltroTiempo.TODO)
        }
    }
    
    private fun setupObservers() {
        // Observar cambios en el filtro seleccionado
        viewModel.filtroActual.observe(this) { filtro ->
            // Actualizar UI con el filtro seleccionado
            when (filtro) {
                FiltroTiempo.HOY -> binding.chipHoy.isChecked = true
                FiltroTiempo.SEMANA -> binding.chipSemana.isChecked = true
                FiltroTiempo.MES -> binding.chipMes.isChecked = true
                FiltroTiempo.TODO -> binding.chipTodo.isChecked = true
            }
            
            // Actualizar texto del filtro aplicado
            binding.tvFiltroAplicado.text = viewModel.obtenerTextoFiltro()
        }
        
        // Observar cambios en la lista de servicios filtrados
        viewModel.serviciosFiltrados.observe(this) { servicios ->
            adapter.submitList(servicios)
            
            // Mostrar mensaje si no hay servicios
            if (servicios.isEmpty()) {
                binding.tvNoData.visibility = View.VISIBLE
                binding.recyclerServicios.visibility = View.GONE
            } else {
                binding.tvNoData.visibility = View.GONE
                binding.recyclerServicios.visibility = View.VISIBLE
            }
        }
    }
    
    private fun setupListeners() {
        // Configurar swipe para refrescar
        binding.swipeRefresh.setOnRefreshListener {
            // La lista se actualizará automáticamente por los observadores
            binding.swipeRefresh.isRefreshing = false
        }
        
        // Configurar botón flotante para nuevo servicio
        binding.fabNuevoServicio.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun mostrarDetalleServicio(servicio: Servicio) {
        val intent = Intent(this, DetalleActivity::class.java).apply {
            putExtra("SERVICIO_ID", servicio.id)
        }
        startActivity(intent)
    }
    
    private fun confirmarEliminacion(servicio: Servicio) {
        AlertDialog.Builder(this)
            .setTitle(R.string.eliminar)
            .setMessage(R.string.confirmar_eliminar)
            .setPositiveButton(R.string.si) { _, _ ->
                viewModel.eliminarServicio(servicio)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
} 