package com.cibertec.ticketaula.background

import android.app.job.JobParameters
import android.app.job.JobService
import androidx.core.content.edit
import com.cibertec.ticketaula.data.SolicitudStore
import java.io.File

// JobService representa una tarea que Android puede posponer y ejecutar cuando resulte
// conveniente. Sus callbacks llegan al hilo principal, por eso el archivo se crea en Thread.
class RespaldoJobService : JobService() {

    private var hiloRespaldo: Thread? = null

    override fun onStartJob(params: JobParameters): Boolean {
        hiloRespaldo = Thread {
            val resultado = runCatching {
                val solicitudes = SolicitudStore(applicationContext).obtenerTodas()
                val archivo = File(filesDir, NOMBRE_RESPALDO)

                // buildString crea todo el contenido antes de escribirlo en el archivo privado.
                val contenido = buildString {
                    appendLine(CABECERA_CSV)
                    solicitudes.forEach { solicitud ->
                        appendLine(
                            listOf(
                                solicitud.id.toString(),
                                solicitud.nombre,
                                solicitud.descripcion,
                                solicitud.categoria,
                                solicitud.prioridad,
                                solicitud.rutaImagen.orEmpty()
                            ).joinToString(SEPARADOR_CSV) { valor -> escaparCsv(valor) }
                        )
                    }
                }
                archivo.writeText(contenido)

                // SharedPreferences guarda un dato sencillo de estado; SQLite conserva
                // las solicitudes y Files conserva el respaldo y las imágenes.
                getSharedPreferences(
                    RespaldoScheduler.PREFERENCIAS_RESPALDO,
                    MODE_PRIVATE
                ).edit {
                    // La extensión edit aplica el cambio al terminar el bloque.
                    putLong(RespaldoScheduler.CLAVE_ULTIMO_RESPALDO, System.currentTimeMillis())
                }
            }

            if (!Thread.currentThread().isInterrupted) {
                jobFinished(params, resultado.isFailure)
            }
        }.apply {
            name = "respaldo-solicitudes"
            start()
        }

        // true informa que todavía existe trabajo ejecutándose después de onStartJob().
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        hiloRespaldo?.interrupt()
        // true solicita otro intento si el sistema interrumpió la tarea.
        return true
    }

    private fun escaparCsv(valor: String): String {
        return "\"${valor.replace("\"", "\"\"")}\""
    }

    companion object {
        private const val NOMBRE_RESPALDO = "respaldo_solicitudes.csv"
        private const val CABECERA_CSV = "id,nombre,descripcion,categoria,prioridad,ruta_imagen"
        private const val SEPARADOR_CSV = ","
    }
}
