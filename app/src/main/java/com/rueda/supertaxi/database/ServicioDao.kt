package com.rueda.supertaxi.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rueda.supertaxi.model.Servicio
import java.time.LocalDate
import java.time.LocalTime

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
    
    // NUEVAS CONSULTAS PARA JORNADAS
    @Query("SELECT * FROM servicio WHERE dia = :fecha ORDER BY hora1 ASC")
    suspend fun getServiciosPorFecha(fecha: LocalDate): List<Servicio>
    
    @Query("SELECT DISTINCT dia FROM servicio ORDER BY dia DESC")
    suspend fun getFechasConServicios(): List<LocalDate>
    
    @Query("SELECT * FROM servicio WHERE dia BETWEEN :fechaInicio AND :fechaFin ORDER BY dia DESC, hora1 ASC")
    suspend fun getServiciosPorPeriodo(fechaInicio: LocalDate, fechaFin: LocalDate): List<Servicio>
    
    @Query("UPDATE servicio SET finJornada = :finJornada, esUltimoServicioJornada = :esUltimo WHERE id = :servicioId")
    suspend fun actualizarFinJornada(servicioId: Long, finJornada: LocalTime, esUltimo: Boolean)
    
    @Query("UPDATE servicio SET inicioJornada = :inicioJornada WHERE dia = :fecha AND inicioJornada IS NULL")
    suspend fun actualizarInicioJornadaDelDia(fecha: LocalDate, inicioJornada: LocalTime)
} 