package com.example.lojamamager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.firebase.repository.ClienteRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ListaClientesActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var clienteAdapter: ClienteAdapter
    private lateinit var searchView: SearchView
    private lateinit var fabAddCliente: FloatingActionButton
    private lateinit var imageViewBack: ImageView
    private lateinit var textViewClientesCount: TextView
    private lateinit var imageViewFilter: ImageView

    private lateinit var clienteRepository: ClienteRepository

    private var currentSearchQuery: String? = null
    private var selectedCidade: String? = null
    private var selectedBairro: String? = null
    private var isSelectionMode: Boolean = false

    companion object {
        const val IS_SELECTION_MODE = "is_selection_mode"
        const val SELECTED_CLIENTE_KEY = "selected_cliente"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_clientes)

        isSelectionMode = intent.getBooleanExtra(IS_SELECTION_MODE, false)

        recyclerView = findViewById(R.id.recyclerViewClientes)
        searchView = findViewById(R.id.searchViewClientes)
        fabAddCliente = findViewById(R.id.fabAddCliente)
        imageViewBack = findViewById(R.id.imageViewBackClientes)
        textViewClientesCount = findViewById(R.id.textViewClientesCount)
        imageViewFilter = findViewById(R.id.imageViewFilterClientes)

        clienteRepository = ClienteRepository(applicationContext) // Passa o applicationContext aqui
        clienteAdapter = ClienteAdapter(emptyList()) { cliente ->
            if (isSelectionMode) {
                val resultIntent = Intent().apply {
                    putExtra(SELECTED_CLIENTE_KEY, cliente)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                val intent = Intent(this, DetalheClienteActivity::class.java)
                intent.putExtra("cliente", cliente)
                startActivity(intent)
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = clienteAdapter

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        filterClientes()
        observeClientesCount()
    }

    private fun setupListeners() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query.orEmpty().ifEmpty { null }
                filterClientes()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText.orEmpty().ifEmpty { null }
                filterClientes()
                return true
            }
        })

        if (isSelectionMode) {
            fabAddCliente.visibility = View.GONE
        } else {
            fabAddCliente.setOnClickListener {
                val intent = Intent(this, DetalheClienteActivity::class.java)
                startActivity(intent)
            }
        }

        imageViewBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        imageViewFilter.setOnClickListener {
            showFilterDialogClientes()
        }
    }

    private fun filterClientes() {
        lifecycleScope.launch {
            // A função searchClientes agora gerencia a espera pelo empresaId internamente no ClienteRepository
            clienteRepository.searchClientes(currentSearchQuery, selectedCidade, selectedBairro).collectLatest { clientes ->
                clienteAdapter.updateClientes(clientes)
            }
        }
    }

    private fun observeClientesCount() {
        lifecycleScope.launch {
            // A função getClientesCount agora gerencia a espera pelo empresaId internamente no ClienteRepository
            clienteRepository.getClientesCount().collectLatest { count -> // Usando getClientesCount para a contagem
                textViewClientesCount.text = "Total de Clientes: $count"
            }
        }
    }

    private fun showFilterDialogClientes() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filter_clientes, null)
        val editTextFilterCidade: EditText = dialogView.findViewById(R.id.editTextFilterCidade)
        val editTextFilterBairro: EditText = dialogView.findViewById(R.id.editTextFilterBairro)
        val buttonClearClientFilters: Button = dialogView.findViewById(R.id.buttonClearClientFilters)

        editTextFilterCidade.setText(selectedCidade)
        editTextFilterBairro.setText(selectedBairro)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Filtrar Clientes")
            .setView(dialogView)
            .setPositiveButton("Aplicar") { _, _ ->
                selectedCidade = editTextFilterCidade.text.toString().trim().ifEmpty { null }
                selectedBairro = editTextFilterBairro.text.toString().trim().ifEmpty { null }
                filterClientes()
                observeClientesCount()
            }
            .setNegativeButton("Cancelar", null)
            .create()

        buttonClearClientFilters.setOnClickListener {
            selectedCidade = null
            selectedBairro = null
            editTextFilterCidade.setText("")
            editTextFilterBairro.setText("")
            Toast.makeText(this, "Filtros de cliente limpos!", Toast.LENGTH_SHORT).show()
            filterClientes()
            observeClientesCount()
            dialog.dismiss()
        }
        dialog.show()
    }
}