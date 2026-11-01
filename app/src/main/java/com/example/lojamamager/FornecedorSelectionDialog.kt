package com.example.lojamamager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.lojamamager.data.Fornecedor
import com.example.lojamamager.firebase.repository.FornecedorRepository // **MUDANÇA AQUI:** Importe o FornecedorRepository
import com.example.lojamamager.firebase.repository.MarcaRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FornecedorSelectionDialog(private val onFornecedorSelected: (Fornecedor?) -> Unit) : DialogFragment() {

    // **MUDANÇA AQUI:** De FornecedorDao para FornecedorRepository
    private lateinit var fornecedorRepository: FornecedorRepository
    private lateinit var listViewFornecedores: ListView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_selection_list, container, false)
        listViewFornecedores = view.findViewById(R.id.listViewSelection)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setTitle("Selecionar Fornecedor")

        // **MUDANÇA AQUI:** Inicialize FornecedorRepository


        fornecedorRepository =  FornecedorRepository(requireActivity().applicationContext)
        lifecycleScope.launch {
            // **MUDANÇA AQUI:** Chame o método do FornecedorRepository
            fornecedorRepository.getAllFornecedores().collectLatest { fornecedores ->
                val fornecedoresComOpcaoLimpar = mutableListOf<Fornecedor?>(null)
                fornecedoresComOpcaoLimpar.addAll(fornecedores)

                val fornecedorNames = fornecedoresComOpcaoLimpar.map { fornecedor ->
                    fornecedor?.razaoSocial ?: "Nenhum Fornecedor"
                }

                // **MUDANÇA AQUI:** Assegurar que o tipo do ArrayAdapter seja String
                val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, fornecedorNames)
                listViewFornecedores.adapter = adapter
            }
        }

        listViewFornecedores.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                onFornecedorSelected(null)
            } else {
                lifecycleScope.launch {
                    // **MUDANÇA AQUI:** Chame o método do FornecedorRepository
                    val allFornecedores = fornecedorRepository.getAllFornecedores().first()
                    val selectedFornecedor = allFornecedores[position - 1]
                    onFornecedorSelected(selectedFornecedor)
                }
            }
            dismiss()
        }
    }
}