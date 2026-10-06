package com.cibertec.ticketaula.model

// data class representa datos y genera automáticamente funciones útiles como equals() y copy().
// Los parámetros declarados con val se convierten en propiedades de solo lectura.
data class Solicitud(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val categoria: String,
    val prioridad: String,
    // La ruta puede ser null porque adjuntar una imagen es opcional.
    val rutaImagen: String?
)
