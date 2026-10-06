package com.cibertec.ticketaula.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// SQLiteOpenHelper administra la creación y las futuras versiones de la base de datos.
// El constructor padre recibe contexto, nombre, fábrica de cursores y versión.
class TicketAulaDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    SolicitudContrato.NOMBRE_BASE_DATOS,
    null,
    SolicitudContrato.VERSION_BASE_DATOS
) {

    // onCreate() se ejecuta una sola vez, cuando la base todavía no existe.
    override fun onCreate(db: SQLiteDatabase) {
        // PRÁCTICA TEMA 6 - BLOQUE 2:
        // Ejecutar SQL_CREAR_TABLA mediante db.execSQL().
        // Este bloque crea físicamente la tabla al abrir la app por primera vez.

        db.execSQL(SolicitudContrato.SQL_CREAR_TABLA)
    }

    // onUpgrade() se ejecutará cuando VERSION_BASE_DATOS aumente. La versión inicial
    // no necesita migraciones; las siguientes añadirán aquí ALTER TABLE sin borrar datos.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No existe una versión anterior que migrar en esta primera implementación.
    }
}
