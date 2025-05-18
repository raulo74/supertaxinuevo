package com.rueda.supertaxi.repository

import androidx.lifecycle.LiveData
import com.rueda.supertaxi.database.TipoServicioDao
import com.rueda.supertaxi.model.TipoServicio

class TipoServicioRepository(private val tipoServicioDao: TipoServicioDao) {
    val allTiposServicio: LiveData<List<TipoServicio>> = tipoServicioDao.getAllTiposServicio()
    
    suspend fun insert(tipoServicio: TipoServicio) {
        tipoServicioDao.insertTipoServicio(tipoServicio)
    }
    
    suspend fun delete(tipoServicio: TipoServicio) {
        if (!tipoServicio.predefinido) {
            tipoServicioDao.deleteTipoServicio(tipoServicio)
        }
    }
} 