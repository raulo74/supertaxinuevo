package com.rueda.supertaxi.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rueda.supertaxi.model.TipoServicio

@Dao
interface TipoServicioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTipoServicio(tipoServicio: TipoServicio)
    
    @Query("SELECT * FROM tipo_servicio ORDER BY nombre ASC")
    fun getAllTiposServicio(): LiveData<List<TipoServicio>>
    
    @Delete
    suspend fun deleteTipoServicio(tipoServicio: TipoServicio)
    
    @Query("DELETE FROM tipo_servicio WHERE predefinido = 0")
    suspend fun deleteNonPredefinedTipos()
} 