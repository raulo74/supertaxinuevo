package com.rueda.supertaxi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rueda.supertaxi.database.AppDatabase
import com.rueda.supertaxi.model.Servicio
import com.rueda.supertaxi.repository.ServicioRepository
import kotlinx.coroutines.launch

class DetalleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ServicioRepository
    
    private val _servicio = MutableLiveData<Servicio?>()
    val servicio: LiveData<Servicio?> = _servicio
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = ServicioRepository(database.servicioDao(), application.applicationContext)
    }
    
    fun loadServicio(id: Long) {
        viewModelScope.launch {
            _servicio.value = repository.getById(id)
        }
    }
} 