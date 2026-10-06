package com.cibertec.ticketaula.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.fragment.app.Fragment
import com.cibertec.ticketaula.MainActivity
import com.cibertec.ticketaula.R
import com.cibertec.ticketaula.data.ImagenStorage
import com.cibertec.ticketaula.data.SolicitudStore
import com.cibertec.ticketaula.databinding.FragmentRegistroBinding

class RegistroFragment : Fragment() {

    // El signo ? permite que _binding contenga null cuando la vista todavía no existe
    // o ya fue destruida. El guion bajo es una convención para la propiedad interna.
    private var _binding: FragmentRegistroBinding? = null

    // get() crea un getter personalizado. !! afirma que _binding no es null mientras
    // se usa la vista del Fragment; por eso se libera obligatoriamente en onDestroyView().
    private val binding get() = _binding!!

    private lateinit var solicitudStore: SolicitudStore
    private lateinit var imagenStorage: ImagenStorage

    // Uri identifica el contenido seleccionado sin asumir una ruta física pública.
    private var uriImagenSeleccionada: Uri? = null

    // registerForActivityResult reemplaza a startActivityForResult. El contrato abre
    // Photo Picker y entrega Uri?; null significa que la persona cerró el selector.
    private val selectorImagen = registerForActivityResult(PickVisualMedia()) { uri ->
        // PRÁCTICA TEMA 6 - BLOQUE 1A:
        // 1. Comprobar que uri no sea null.
        // 2. Guardarla en uriImagenSeleccionada.
        // 3. Quitar el padding de imageViewEvidencia.
        // 4. Cambiar scaleType a CENTER_CROP y mostrar la imagen con setImageURI().
        if (uri != null){
            uriImagenSeleccionada = uri
            _binding?.imageViewEvidencia?.apply {
                setPadding(0, 0,0, 0)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // applicationContext evita que estas clases conserven accidentalmente la vista.
        solicitudStore = SolicitudStore(requireContext().applicationContext)
        imagenStorage = ImagenStorage(requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarSelectorCategoria()
        configurarSemaforoPrioridad()
        binding.buttonSeleccionarImagen.setOnClickListener {
            selectorImagen.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
        binding.buttonAgregar.setOnClickListener {
            registrarSolicitud()
        }
    }

    private fun registrarSolicitud() {
        val nombre = binding.editTextNombre.text.toString().trim()
        val descripcion = binding.editTextDescripcion.text.toString().trim()
        // val crea referencias que no se reasignarán. Cada función devuelve el dato
        // seleccionado y el resultado queda guardado en la variable correspondiente.
        val categoria = obtenerCategoriaSeleccionada()
        // prioridad tiene tipo String? porque la función también puede devolver null.
        val prioridad = obtenerPrioridadSeleccionada()

        binding.inputLayoutNombre.error = null
        binding.inputLayoutDescripcion.error = null
        binding.inputLayoutCategoria.error = null
        binding.textViewErrorPrioridad.visibility = View.GONE

        var formularioValido = true

        if (nombre.isBlank()) {
            binding.inputLayoutNombre.error = getString(R.string.error_nombre)
            formularioValido = false
        }

        if (descripcion.length < LONGITUD_MINIMA_DESCRIPCION) {
            binding.inputLayoutDescripcion.error = getString(R.string.error_descripcion)
            formularioValido = false
        }

        if (categoria == null) {
            binding.inputLayoutCategoria.error = getString(R.string.error_categoria)
            formularioValido = false
        }

        if (prioridad == null) {
            binding.textViewErrorPrioridad.visibility = View.VISIBLE
            formularioValido = false
        }

        // ! niega el Boolean y || significa "o": basta una condición verdadera para salir.
        if (!formularioValido || categoria == null || prioridad == null) {
            return
        }

        binding.buttonAgregar.isEnabled = false
        binding.buttonAgregar.setText(R.string.guardando_solicitud)

        // Thread crea un hilo de trabajo. Copiar archivos y escribir en SQLite puede
        // tardar, por lo que no debe bloquear el hilo principal que dibuja la interfaz.
        Thread {
            val idGenerado = runCatching {
                val rutaImagen = uriImagenSeleccionada?.let { uri ->
                    imagenStorage.guardarImagen(uri)
                }
                solicitudStore.agregar(
                    nombre,
                    descripcion,
                    categoria,
                    prioridad,
                    rutaImagen
                )
            }.getOrDefault(-1L)

            // Las vistas solo pueden modificarse desde el hilo principal.
            activity?.runOnUiThread {
                val bindingActual = _binding ?: return@runOnUiThread
                bindingActual.buttonAgregar.isEnabled = true
                bindingActual.buttonAgregar.setText(R.string.boton_agregar)

                if (idGenerado == -1L) {
                    Toast.makeText(
                        requireContext(),
                        R.string.error_guardar_solicitud,
                        Toast.LENGTH_LONG
                    ).show()
                    return@runOnUiThread
                }

                limpiarFormulario()
                // as? intenta convertir activity a MainActivity sin provocar una excepción.
                (activity as? MainActivity)?.abrirLista()
            }
        }.start()
    }

    // String? expresa que el retorno puede ser un texto o null si no hay selección.
    private fun obtenerPrioridadSeleccionada(): String? {
        // when evalúa varios casos. -> separa cada condición del resultado que devuelve.
        return when (binding.radioGroupPrioridad.checkedRadioButtonId) {
            R.id.radioPrioridadBaja -> getString(R.string.prioridad_baja)
            R.id.radioPrioridadMedia -> getString(R.string.prioridad_media)
            R.id.radioPrioridadAlta -> getString(R.string.prioridad_alta)
            else -> null
        }
    }

    private fun obtenerCategoriaSeleccionada(): String? {
        // takeIf conserva el texto cuando la condición es verdadera; en caso contrario,
        // devuelve null. categoria -> separa el parámetro de la lambda de su operación.
        return binding.autoCompleteCategoria.text
            .toString()
            .takeIf { categoria -> categoria.isNotBlank() }
    }

    private fun configurarSelectorCategoria() {
        binding.autoCompleteCategoria.setSimpleItems(R.array.categorias)
        // El evento entrega cuatro parámetros. Cada _ indica que ese parámetro existe,
        // pero no se necesita dentro de esta lambda. -> marca el inicio de su cuerpo.
        binding.autoCompleteCategoria.setOnItemClickListener { _, _, _, _ ->
            binding.inputLayoutCategoria.error = null
        }
    }

    private fun configurarSemaforoPrioridad() {
        // Este evento entrega el grupo y el identificador marcado. Se usan _ porque
        // aquí basta consultar la selección mediante obtenerPrioridadSeleccionada().
        binding.radioGroupPrioridad.setOnCheckedChangeListener { _, _ ->
            // La llamada se ejecuta primero y su retorno se asigna a la val prioridad.
            val prioridad = obtenerPrioridadSeleccionada()
            if (prioridad != null) {
                actualizarSemaforo(prioridad)
                binding.textViewErrorPrioridad.visibility = View.GONE
            }
        }
    }

    private fun actualizarSemaforo(prioridad: String) {
        // El when produce un recurso drawable; ese resultado se almacena en fondoSemaforo.
        val fondoSemaforo = when (prioridad) {
            getString(R.string.prioridad_baja) -> R.drawable.bg_semaforo_verde
            getString(R.string.prioridad_media) -> R.drawable.bg_semaforo_amarillo
            getString(R.string.prioridad_alta) -> R.drawable.bg_semaforo_rojo
            else -> R.drawable.bg_semaforo_neutro
        }
        binding.viewSemaforo.setBackgroundResource(fondoSemaforo)
        binding.textViewSemaforoEstado.text =
            getString(R.string.semaforo_estado_formato, prioridad)
    }

    private fun limpiarFormulario() {
        // ?. ejecuta clear() solo si text no es null, evitando una excepción.
        binding.editTextNombre.text?.clear()
        binding.editTextDescripcion.text?.clear()
        binding.autoCompleteCategoria.setText(null, false)
        binding.radioGroupPrioridad.clearCheck()
        binding.viewSemaforo.setBackgroundResource(R.drawable.bg_semaforo_neutro)
        binding.textViewSemaforoEstado.text = getString(R.string.semaforo_estado_inicial)
        uriImagenSeleccionada = null
        binding.imageViewEvidencia.apply {
            setPadding(
                resources.getDimensionPixelSize(R.dimen.gap_large),
                resources.getDimensionPixelSize(R.dimen.gap_large),
                resources.getDimensionPixelSize(R.dimen.gap_large),
                resources.getDimensionPixelSize(R.dimen.gap_large)
            )
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setImageResource(R.drawable.ic_agregar_imagen)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // La vista terminó su ciclo de vida; liberar la referencia evita conservarla en memoria.
        _binding = null
    }

    // companion object agrupa constantes asociadas a la clase sin requerir una instancia.
    companion object {
        private const val LONGITUD_MINIMA_DESCRIPCION = 8
    }
}
