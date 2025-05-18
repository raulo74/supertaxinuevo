package com.rueda.supertaxi.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rueda.supertaxi.model.Coordenada
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    
    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }
    }
    
    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? {
        return value?.format(DateTimeFormatter.ISO_LOCAL_TIME)
    }
    
    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it, DateTimeFormatter.ISO_LOCAL_TIME) }
    }
    
    @TypeConverter
    fun fromCoordenadaList(value: List<Coordenada>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toCoordenadaList(value: String?): List<Coordenada>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Coordenada>>() {}.type
        return gson.fromJson(value, listType)
    }
} 