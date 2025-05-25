package com.rueda.supertaxi.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Añadir nuevos campos para jornada
        database.execSQL("""
            ALTER TABLE servicio 
            ADD COLUMN inicioJornada TEXT
        """)
        
        database.execSQL("""
            ALTER TABLE servicio 
            ADD COLUMN finJornada TEXT
        """)
        
        database.execSQL("""
            ALTER TABLE servicio 
            ADD COLUMN esUltimoServicioJornada INTEGER NOT NULL DEFAULT 0
        """)
        
        database.execSQL("""
            ALTER TABLE servicio 
            ADD COLUMN numeroServicioEnJornada INTEGER NOT NULL DEFAULT 0
        """)
    }
} 