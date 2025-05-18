package com.rueda.supertaxi.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tipo_servicio")
data class TipoServicio(
    @PrimaryKey
    val nombre: String,
    val predefinido: Boolean = false
) 