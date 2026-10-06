package com.cibertec.ticketaula.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cibertec.ticketaula.R
import com.cibertec.ticketaula.databinding.ItemSolicitudBinding
import com.cibertec.ticketaula.model.Solicitud
import java.io.File

class SolicitudAdapter(
    // (Solicitud) -> Unit es un tipo de función: recibe una Solicitud y no retorna un valor.
    // Quien crea el adaptador entrega la acción concreta que se ejecutará al pulsar.
    private val onSolicitudClick: (Solicitud) -> Unit
) : RecyclerView.Adapter<SolicitudAdapter.SolicitudViewHolder>() {

    // <Solicitud> restringe la lista para que solo pueda almacenar objetos Solicitud.
    private val solicitudes = mutableListOf<Solicitud>()

    fun actualizar(nuevasSolicitudes: List<Solicitud>) {
        val cantidadAnterior = solicitudes.size
        solicitudes.clear()
        if (cantidadAnterior > 0) {
            notifyItemRangeRemoved(0, cantidadAnterior)
        }

        solicitudes.addAll(nuevasSolicitudes)
        if (solicitudes.isNotEmpty()) {
            notifyItemRangeInserted(0, solicitudes.size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SolicitudViewHolder {
        val binding = ItemSolicitudBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SolicitudViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SolicitudViewHolder, position: Int) {
        holder.vincular(solicitudes[position])
    }

    override fun getItemCount(): Int {
        return solicitudes.size
    }

    fun obtenerSolicitud(position: Int): Solicitud {
        return solicitudes[position]
    }

    fun restaurarTarjeta(position: Int) {
        // El swipe desplaza visualmente la tarjeta. notifyItemChanged() solicita al
        // RecyclerView que vuelva a dibujarla cuando la acción se cancela.
        notifyItemChanged(position)
    }

    inner class SolicitudViewHolder(
        private val binding: ItemSolicitudBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun vincular(solicitud: Solicitud) {
            binding.textViewCodigo.text =
                binding.root.context.getString(R.string.codigo_formato, solicitud.id)
            binding.textViewNombre.text = solicitud.nombre
            binding.textViewDescripcion.text = solicitud.descripcion
            binding.textViewCategoria.text =
                binding.root.context.getString(R.string.categoria_formato, solicitud.categoria)
            binding.textViewPrioridad.text = solicitud.prioridad

            // let ejecuta el bloque cuando rutaImagen no es null. takeIf conserva el
            // archivo únicamente cuando sigue existiendo en el almacenamiento interno.
            val archivoImagen = solicitud.rutaImagen
                ?.let(::File)
                ?.takeIf { archivo -> archivo.exists() }
            if (archivoImagen == null) {
                binding.imageViewSolicitud.visibility = View.GONE
                binding.imageViewSolicitud.setImageDrawable(null)
            } else {
                binding.imageViewSolicitud.visibility = View.VISIBLE
                binding.imageViewSolicitud.setImageURI(Uri.fromFile(archivoImagen))
            }
            // En un when, -> separa cada valor evaluado del resultado correspondiente.
            val fondoSemaforo = when (solicitud.prioridad) {
                binding.root.context.getString(R.string.prioridad_baja) ->
                    R.drawable.bg_semaforo_verde
                binding.root.context.getString(R.string.prioridad_media) ->
                    R.drawable.bg_semaforo_amarillo
                binding.root.context.getString(R.string.prioridad_alta) ->
                    R.drawable.bg_semaforo_rojo
                else -> R.drawable.bg_semaforo_neutro
            }
            binding.viewSemaforoTarjeta.setBackgroundResource(fondoSemaforo)
            binding.root.setOnClickListener {
                // Se invoca la función recibida por el constructor y se entrega la solicitud.
                onSolicitudClick(solicitud)
            }
        }
    }
}
