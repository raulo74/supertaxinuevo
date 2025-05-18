package com.rueda.supertaxi.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rueda.supertaxi.model.Servicio
import com.rueda.supertaxi.model.TipoServicio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Servicio::class, TipoServicio::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun servicioDao(): ServicioDao
    abstract fun tipoServicioDao(): TipoServicioDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "super_taxi_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        // Insertar tipos de servicio predefinidos
                        database.tipoServicioDao().insertTipoServicio(
                            TipoServicio("Parada de taxis", true)
                        )
                        database.tipoServicioDao().insertTipoServicio(
                            TipoServicio("Mano Alzada", true)
                        )
                    }
                }
            }
        }
    }
} 