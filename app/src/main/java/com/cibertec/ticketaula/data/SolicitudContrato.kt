package com.cibertec.ticketaula.data

// El contrato centraliza los nombres usados por SQLite. Así se evita repetir
// cadenas en las consultas y se reduce el riesgo de escribir una columna distinta.
object SolicitudContrato {
    const val NOMBRE_BASE_DATOS = "ticket_aula.db"
    const val VERSION_BASE_DATOS = 1

    const val TABLA_SOLICITUDES = "solicitudes"
    const val COLUMNA_ID = "id"
    const val COLUMNA_NOMBRE = "nombre"
    const val COLUMNA_DESCRIPCION = "descripcion"
    const val COLUMNA_CATEGORIA = "categoria"
    const val COLUMNA_PRIORIDAD = "prioridad"
    const val COLUMNA_RUTA_IMAGEN = "ruta_imagen"

    // INTEGER PRIMARY KEY AUTOINCREMENT delega a SQLite la generación correlativa del id.
    // TEXT admite los datos escritos por el usuario y la ruta privada de la imagen.
    const val SQL_CREAR_TABLA = """
        CREATE TABLE $TABLA_SOLICITUDES (
            $COLUMNA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMNA_NOMBRE TEXT NOT NULL,
            $COLUMNA_DESCRIPCION TEXT NOT NULL,
            $COLUMNA_CATEGORIA TEXT NOT NULL,
            $COLUMNA_PRIORIDAD TEXT NOT NULL,
            $COLUMNA_RUTA_IMAGEN TEXT
        )
    """
}
