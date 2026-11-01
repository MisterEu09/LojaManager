package com.example.lojamamager

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.Marca // Importe sua data class Marca
import com.example.lojamamager.firebase.repository.CategoriaRepository
import com.example.lojamamager.firebase.repository.MarcaRepository // **MUDANÇA AQUI:** Importar o novo MarcaRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MarcaSelectionDialog(
    private val onMarcaSelected: (Marca?) -> Unit
) : DialogFragment() {

    // **MUDANÇA AQUI:** De MarcaDao para MarcaRepository
    private lateinit var marcaRepository: MarcaRepository
    private lateinit var marcaAdapter: MarcaAdapter
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // **MUDANÇA AQUI:** Inicializar o novo MarcaRepository
        marcaRepository =  MarcaRepository(requireActivity().applicationContext)

        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_marca_selection, null)

        recyclerView = view.findViewById(R.id.recyclerViewMarcaList)
        searchView = view.findViewById(R.id.searchViewMarcas)
        val buttonAddNewMarca: Button = view.findViewById(R.id.buttonAddNewMarca)
        val buttonMarcaBack: Button = view.findViewById(R.id.buttonMarcaBack)
        val buttonClearMarca: Button = view.findViewById(R.id.buttonClearMarca)

        marcaAdapter = MarcaAdapter(
            emptyList(),
            onItemClick = { marca ->
                onMarcaSelected(marca)
                dismiss()
            },
            onMoreOptionsClick = { marca, anchorView ->
                showMarcaOptionsMenu(marca, anchorView)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = marcaAdapter

        // **MUDANÇA AQUI:** Usar marcaRepository.getAllMarcas()
        lifecycleScope.launch {
            marcaRepository.getAllMarcas().collectLatest { marcas ->
                marcaAdapter.updateMarcas(marcas)
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterMarcas(newText.orEmpty())
                return true
            }
        })

        buttonAddNewMarca.setOnClickListener {
            AddEditMarcaDialog(onDismiss = {
                // Após adicionar/editar, a lista de marcas deve ser atualizada automaticamente pelo Flow
                // Não precisa de ação específica aqui, pois o Flow já está observando.
            }).show(parentFragmentManager, "AddEditMarcaDialog")
        }

        buttonMarcaBack.setOnClickListener {
            dismiss()
        }

        buttonClearMarca.setOnClickListener {
            onMarcaSelected(null)
            dismiss()
        }

        builder.setView(view)
        return builder.create()
    }

    private fun filterMarcas(query: String) {
        lifecycleScope.launch {
            if (query.isEmpty()) {
                // **MUDANÇA AQUI:** Usar marcaRepository.getAllMarcas()
                marcaRepository.getAllMarcas().collectLatest { marcas ->
                    marcaAdapter.updateMarcas(marcas)
                }
            } else {
                // **MUDANÇA AQUI:** Usar marcaRepository.searchMarcas()
                marcaRepository.searchMarcas(query).collectLatest { marcas ->
                    marcaAdapter.updateMarcas(marcas)
                }
            }
        }
    }

    private fun showMarcaOptionsMenu(marca: Marca, anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        popup.menuInflater.inflate(R.menu.menu_item_options, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    AddEditMarcaDialog(marca, onDismiss = {
                        // Após editar, a lista de marcas deve ser atualizada automaticamente pelo Flow
                    }).show(parentFragmentManager, "AddEditMarcaDialog")
                    true
                }
                R.id.action_delete -> {
                    confirmDeleteMarca(marca)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun confirmDeleteMarca(marca: Marca) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Marca")
            .setMessage("Tem certeza que deseja excluir '${marca.nome}'? Isso não removerá a marca de produtos já associados.")
            .setPositiveButton("Sim") { dialog, which ->
                lifecycleScope.launch {
                    // **MUDANÇA AQUI:** Usar marcaRepository.deleteMarca() e passar o ID String
                    marca.id?.let { id ->
                        val success = marcaRepository.deleteMarca(id)
                        if (success) {
                            Toast.makeText(requireContext(), "'${marca.nome}' excluída!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Erro ao excluir '${marca.nome}'.", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Toast.makeText(requireContext(), "ID da marca não encontrado para exclusão.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }
}