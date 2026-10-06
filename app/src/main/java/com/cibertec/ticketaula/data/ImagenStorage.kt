package com.cibertec.ticketaula.data

import android.content.Context
import android.net.Uri
import java.io.File

// Esta clase se ocupa exclusivamente de archivos. Separarla mantiene al Fragment
// concentrado en los eventos de la interfaz y ayuda a reconocer el modelo MVC.
class ImagenStorage(context: Context) {

    private val appContext = context.applicationContext

    fun guardarImagen(uriOrigen: Uri): String {
        // PRÁCTICA TEMA 6 - BLOQUE 1B:
        // 1. Crear dentro de filesDir el directorio de imágenes.
        // 2. Crear un archivo con nombre único usando System.currentTimeMillis().
        // 3. Abrir la Uri mediante contentResolver.openInputStream().
        // 4. Copiar el flujo al archivo con copyTo() y cerrar ambos mediante use.
        // 5. Retornar archivoDestino.absolutePath.
        // La cadena vacía mantiene el proyecto compilable antes de completar el bloque.
        val directorio = File(appContext.filesDir, DIRECTORIO_IMAGENES)
        if (!directorio.exists()){
            directorio.mkdirs()
        }
        val archivoDestino = File(directorio, "$PREFIJO_ARCHIVO${System.currentTimeMillis()}.img")
        val flujoEntrada = requireNotNull(appContext.contentResolver.apenImputStream(uriOrigen))

        //usa cierre cada flujo incluso s ocurre un error durante copyTo()
        flujoEntrada.use { entrada ->
            archivoDestino.outputStream().use { salida ->
                entrada.copyTo(salida)
            }
        }
        return archivoDestino.absolutePath
    }

    companion object {
        private const val DIRECTORIO_IMAGENES = "imagenes_solicitudes"
        private const val PREFIJO_ARCHIVO = "solicitud_"
    }
}
