package com.cibertec.ticketaula

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cibertec.ticketaula.databinding.ActivityMainBinding
import com.cibertec.ticketaula.ui.ListaFragment
import com.cibertec.ticketaula.ui.RegistroFragment

class MainActivity : AppCompatActivity() {

    // lateinit indica que la variable se inicializará después, dentro de onCreate().
    // Permite declarar binding sin asignarle un valor nulo provisional.
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Las llaves contienen la lambda que se ejecuta cuando se pulsa el botón.
        binding.buttonRegistro.setOnClickListener {
            mostrarFragmento(RegistroFragment())
        }

        binding.buttonLista.setOnClickListener {
            abrirLista()
        }

        // == compara valores. Solo se carga el fragmento inicial si no existe un estado restaurado.
        if (savedInstanceState == null) {
            mostrarFragmento(RegistroFragment())
        }
    }

    fun abrirLista() {
        mostrarFragmento(ListaFragment())
    }

    private fun mostrarFragmento(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
