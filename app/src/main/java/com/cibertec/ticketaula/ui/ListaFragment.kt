package com.cibertec.ticketaula.ui

import android.app.job.JobScheduler
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cibertec.ticketaula.R
import com.cibertec.ticketaula.adapter.SolicitudAdapter
import com.cibertec.ticketaula.background.RespaldoScheduler
import com.cibertec.ticketaula.data.SolicitudStore
import com.cibertec.ticketaula.databinding.DialogEditarSolicitudBinding
import com.cibertec.ticketaula.databinding.DialogDetalleSolicitudBinding
import com.cibertec.ticketaula.databinding.FragmentListaBinding
import com.cibertec.ticketaula.model.Solicitud
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.DateFormat
import java.util.Date

class ListaFragment : Fragment() {

    // _binding es la referencia interna anulable. El guion bajo es una convención
    // para distinguirla de la propiedad binding utilizada dentro de la vista.
    private var _binding: FragmentListaBinding? = null

    // !! asegura que existe un binding durante el ciclo de vida válido de la vista.
    private val binding get() = _binding!!

    // lateinit permite inicializar el adaptador más adelante, en configurarRecyclerView().
    private lateinit var solicitudAdapter: SolicitudAdapter
    private lateinit var solicitudStore: SolicitudStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solicitudStore = SolicitudStore(requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarRecyclerView()
        binding.buttonRespaldo.setOnClickListener {
            programarRespaldo()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarSolicitudes()
        mostrarEstadoRespaldo()
    }

    private fun configurarRecyclerView() {
        // solicitud -> declara el parámetro recibido por la lambda. El cuerpo situado
        // después de -> se ejecutará cuando se pulse una tarjeta del RecyclerView.
        solicitudAdapter = SolicitudAdapter { solicitud ->
            mostrarDetalle(solicitud)
        }
        binding.recyclerViewSolicitudes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSolicitudes.adapter = solicitudAdapter
        configurarGestos()
    }

    private fun configurarGestos() {
        // SimpleCallback recibe primero las direcciones de movimiento vertical y luego
        // las direcciones de swipe. START es izquierda y END es derecha en español.
        val callback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.START or ItemTouchHelper.END
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val posicion = viewHolder.bindingAdapterPosition
                if (posicion == RecyclerView.NO_POSITION) return

                val solicitud = solicitudAdapter.obtenerSolicitud(posicion)
                if (direction == ItemTouchHelper.END) {
                    mostrarEdicion(solicitud, posicion)
                } else {
                    confirmarEliminacion(solicitud, posicion)
                }
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerViewSolicitudes)
    }

    private fun cargarSolicitudes() {
        binding.progressIndicator.visibility = View.VISIBLE
        binding.recyclerViewSolicitudes.visibility = View.GONE
        binding.textViewListaVacia.visibility = View.GONE

        // PRÁCTICA TEMA 7 - BLOQUE 5:
        // 1. Crear Thread { } y consultar solicitudStore.obtenerTodas().
        // 2. Dentro del hilo, llamar activity?.runOnUiThread { }.
        // 3. Actualizar el Adapter y las visibilidades desde el hilo principal.
        solicitudAdapter.actualizar(emptyList())
        binding.progressIndicator.visibility = View.GONE
        binding.recyclerViewSolicitudes.visibility = View.GONE
        binding.textViewListaVacia.visibility = View.VISIBLE
    }

    private fun mostrarDetalle(solicitud: Solicitud) {
        val dialogBinding = DialogDetalleSolicitudBinding.inflate(layoutInflater)
        dialogBinding.textViewTituloDialog.text =
            getString(R.string.dialogo_titulo_formato, solicitud.id)
        dialogBinding.textViewSolicitanteDialog.text = solicitud.nombre
        dialogBinding.textViewProblemaDialog.text = solicitud.descripcion
        dialogBinding.textViewCategoriaDialog.text = solicitud.categoria
        dialogBinding.textViewPrioridadDialog.text = solicitud.prioridad

        solicitud.rutaImagen
            ?.let(::File)
            ?.takeIf { archivo -> archivo.exists() }
            ?.also { archivo ->
                dialogBinding.imageViewDialog.visibility = View.VISIBLE
                dialogBinding.imageViewDialog.setImageURI(Uri.fromFile(archivo))
            }

        // Cada -> relaciona un valor de prioridad con el drawable que debe devolverse.
        val fondoSemaforo = when (solicitud.prioridad) {
            getString(R.string.prioridad_baja) -> R.drawable.bg_semaforo_verde
            getString(R.string.prioridad_media) -> R.drawable.bg_semaforo_amarillo
            getString(R.string.prioridad_alta) -> R.drawable.bg_semaforo_rojo
            else -> R.drawable.bg_semaforo_neutro
        }
        dialogBinding.viewSemaforoDialog.setBackgroundResource(fondoSemaforo)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialogo_boton_aceptar, null)
            .show()
    }

    private fun mostrarEdicion(solicitud: Solicitud, posicion: Int) {
        val dialogBinding = DialogEditarSolicitudBinding.inflate(layoutInflater)
        dialogBinding.editTextNombreEditar.setText(solicitud.nombre)
        dialogBinding.editTextDescripcionEditar.setText(solicitud.descripcion)
        dialogBinding.autoCompleteCategoriaEditar.setSimpleItems(R.array.categorias)
        dialogBinding.autoCompleteCategoriaEditar.setText(solicitud.categoria, false)

        val radioSeleccionado = when (solicitud.prioridad) {
            getString(R.string.prioridad_baja) -> R.id.radioPrioridadBajaEditar
            getString(R.string.prioridad_media) -> R.id.radioPrioridadMediaEditar
            else -> R.id.radioPrioridadAltaEditar
        }
        dialogBinding.radioGroupPrioridadEditar.check(radioSeleccionado)

        solicitud.rutaImagen
            ?.let(::File)
            ?.takeIf { archivo -> archivo.exists() }
            ?.also { archivo ->
                dialogBinding.imageViewEditar.visibility = View.VISIBLE
                dialogBinding.textViewImagenEditar.visibility = View.VISIBLE
                dialogBinding.imageViewEditar.setImageURI(Uri.fromFile(archivo))
            }

        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialogo_editar_titulo, solicitud.id))
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialogo_guardar, null)
            .setNegativeButton(R.string.dialogo_cancelar) { _, _ ->
                solicitudAdapter.restaurarTarjeta(posicion)
            }
            .setOnCancelListener {
                solicitudAdapter.restaurarTarjeta(posicion)
            }
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                guardarEdicion(dialogo, dialogBinding, solicitud, posicion)
            }
        }
        dialogo.show()
    }

    private fun guardarEdicion(
        dialogo: AlertDialog,
        dialogBinding: DialogEditarSolicitudBinding,
        solicitudOriginal: Solicitud,
        posicion: Int
    ) {
        val nombre = dialogBinding.editTextNombreEditar.text.toString().trim()
        val descripcion = dialogBinding.editTextDescripcionEditar.text.toString().trim()
        val categoria = dialogBinding.autoCompleteCategoriaEditar.text.toString().trim()
        val prioridad = when (dialogBinding.radioGroupPrioridadEditar.checkedRadioButtonId) {
            R.id.radioPrioridadBajaEditar -> getString(R.string.prioridad_baja)
            R.id.radioPrioridadMediaEditar -> getString(R.string.prioridad_media)
            R.id.radioPrioridadAltaEditar -> getString(R.string.prioridad_alta)
            else -> null
        }

        dialogBinding.inputLayoutNombreEditar.error = null
        dialogBinding.inputLayoutDescripcionEditar.error = null
        dialogBinding.inputLayoutCategoriaEditar.error = null
        dialogBinding.textViewErrorPrioridadEditar.visibility = View.GONE

        var formularioValido = true
        if (nombre.isBlank()) {
            dialogBinding.inputLayoutNombreEditar.error = getString(R.string.error_nombre)
            formularioValido = false
        }
        if (descripcion.length < LONGITUD_MINIMA_DESCRIPCION) {
            dialogBinding.inputLayoutDescripcionEditar.error =
                getString(R.string.error_descripcion)
            formularioValido = false
        }
        if (categoria.isBlank()) {
            dialogBinding.inputLayoutCategoriaEditar.error = getString(R.string.error_categoria)
            formularioValido = false
        }
        if (prioridad == null) {
            dialogBinding.textViewErrorPrioridadEditar.visibility = View.VISIBLE
            formularioValido = false
        }
        if (!formularioValido || prioridad == null) return

        // copy() conserva id y rutaImagen, y reemplaza únicamente los datos editables.
        val solicitudEditada = solicitudOriginal.copy(
            nombre = nombre,
            descripcion = descripcion,
            categoria = categoria,
            prioridad = prioridad
        )

        Thread {
            val filasActualizadas = solicitudStore.actualizar(solicitudEditada)
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                dialogo.dismiss()
                if (filasActualizadas == 1) {
                    cargarSolicitudes()
                } else {
                    solicitudAdapter.restaurarTarjeta(posicion)
                    Toast.makeText(
                        requireContext(),
                        R.string.error_actualizar_solicitud,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun confirmarEliminacion(solicitud: Solicitud, posicion: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialogo_eliminar_titulo)
            .setMessage(
                getString(
                    R.string.dialogo_eliminar_mensaje,
                    solicitud.id,
                    solicitud.nombre
                )
            )
            .setPositiveButton(R.string.dialogo_eliminar_confirmar) { _, _ ->
                eliminarSolicitud(solicitud, posicion)
            }
            .setNegativeButton(R.string.dialogo_cancelar) { _, _ ->
                solicitudAdapter.restaurarTarjeta(posicion)
            }
            .setOnCancelListener {
                solicitudAdapter.restaurarTarjeta(posicion)
            }
            .show()
    }

    private fun eliminarSolicitud(solicitud: Solicitud, posicion: Int) {
        Thread {
            val filasEliminadas = solicitudStore.eliminar(solicitud.id)
            if (filasEliminadas == 1) {
                // La imagen pertenece únicamente a esta solicitud; se borra después
                // de confirmar que la fila desapareció correctamente de SQLite.
                solicitud.rutaImagen?.let(::File)?.delete()
            }

            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                if (filasEliminadas == 1) {
                    cargarSolicitudes()
                } else {
                    solicitudAdapter.restaurarTarjeta(posicion)
                    Toast.makeText(
                        requireContext(),
                        R.string.error_eliminar_solicitud,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun programarRespaldo() {
        val resultado = RespaldoScheduler.programar(requireContext())
        val mensaje = if (resultado == JobScheduler.RESULT_SUCCESS) {
            R.string.respaldo_programado
        } else {
            R.string.respaldo_no_programado
        }
        binding.textViewEstadoRespaldo.setText(mensaje)
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()

        // La ejecución pertenece al sistema y puede demorarse. Esta comprobación breve
        // actualiza el estado si el trabajo terminó mientras la pantalla sigue abierta.
        binding.root.postDelayed({ mostrarEstadoRespaldo() }, COMPROBACION_RESPALDO_MS)
    }

    private fun mostrarEstadoRespaldo() {
        val bindingActual = _binding ?: return
        val preferencias = requireContext().getSharedPreferences(
            RespaldoScheduler.PREFERENCIAS_RESPALDO,
            android.content.Context.MODE_PRIVATE
        )
        val ultimoRespaldo = preferencias.getLong(
            RespaldoScheduler.CLAVE_ULTIMO_RESPALDO,
            SIN_RESPALDO
        )
        if (ultimoRespaldo != SIN_RESPALDO) {
            val fecha = DateFormat.getDateTimeInstance().format(Date(ultimoRespaldo))
            bindingActual.textViewEstadoRespaldo.text =
                getString(R.string.respaldo_ultimo_formato, fecha)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Se elimina la referencia a la vista, aunque el Fragment continúe existiendo.
        _binding = null
    }

    companion object {
        private const val COMPROBACION_RESPALDO_MS = 6_000L
        private const val SIN_RESPALDO = 0L
        private const val LONGITUD_MINIMA_DESCRIPCION = 8
    }
}
