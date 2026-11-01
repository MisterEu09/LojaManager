package com.example.lojamamager

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.lojamamager.data.Categoria
import com.example.lojamamager.firebase.repository.CategoriaRepository // Importar o novo CategoriaRepository
import kotlinx.coroutines.launch

class AddEditCategoryDialog(
    private val categoria: Categoria? = null,
    private val onDismiss: () -> Unit
) : DialogFragment() {

    // **MUDANÇA AQUI:** De CategoriaDao para CategoriaRepository
    private lateinit var categoriaRepository: CategoriaRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // **MUDANÇA AQUI:** Inicializar o CategoriaRepository
        categoriaRepository =  CategoriaRepository(requireActivity().applicationContext)

        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_edit_category, null)

        val dialogTitle: TextView = view.findViewById(R.id.textViewDialogTitle)
        val editTextCategoryName: EditText = view.findViewById(R.id.editTextCategoryName)
        val buttonCancel: Button = view.findViewById(R.id.buttonCancel)
        val buttonSave: Button = view.findViewById(R.id.buttonSave)

        if (categoria != null) {
            dialogTitle.text = "Editar Categoria"
            editTextCategoryName.setText(categoria.nome)
        } else {
            dialogTitle.text = "Nova Categoria"
        }

        buttonCancel.setOnClickListener {
            dismiss()
        }

        buttonSave.setOnClickListener {
            val nome = editTextCategoryName.text.toString().trim()
            if (nome.isEmpty()) {
                editTextCategoryName.error = "Nome da categoria é obrigatório"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                if (categoria != null) {
                    // Atualizar categoria existente
                    // **MUDANÇA AQUI:** Chamar o método do CategoriaRepository e passar o ID
                    val updatedCategoria = categoria.copy(nome = nome)
                    val success = categoriaRepository.updateCategoria(updatedCategoria)
                    if (success) {
                        Toast.makeText(requireContext(), "Categoria atualizada!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Erro ao atualizar categoria.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Inserir nova categoria
                    val newCategoria = Categoria(id = null, nome = nome) // ID será gerado pelo Firestore
                    // **MUDANÇA AQUI:** Chamar o método do CategoriaRepository
                    val newId = categoriaRepository.addCategoria(newCategoria)
                    if (newId != null) {
                        Toast.makeText(requireContext(), "Categoria adicionada!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Erro ao adicionar categoria.", Toast.LENGTH_SHORT).show()
                    }
                }
                dismiss()
            }
        }

        builder.setView(view)
        return builder.create()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDismiss()
    }
}