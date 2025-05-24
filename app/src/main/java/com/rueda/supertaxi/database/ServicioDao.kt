package com.rueda.supertaxi.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rueda.supertaxi.model.Servicio

@Dao
interface ServicioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServicio(servicio: Servicio): Long
    
    @Query("SELECT * FROM servicio ORDER BY id DESC")
    fun getAllServicios(): LiveData<List<Servicio>>
    
    @Query("SELECT * FROM servicio WHERE id = :id")
    suspend fun getServicioById(id: Long): Servicio?
    
    @Delete
    suspend fun deleteServicio(servicio: Servicio)
    
    @Query("DELETE FROM servicio")
    suspend fun deleteAllServicios()
} 