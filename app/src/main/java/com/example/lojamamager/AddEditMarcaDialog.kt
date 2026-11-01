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
import com.example.lojamamager.data.Marca
import com.example.lojamamager.firebase.repository.MarcaRepository
import kotlinx.coroutines.launch

class AddEditMarcaDialog(
    private val marca: Marca? = null,
    private val onDismiss: () -> Unit
) : DialogFragment() {

    // **MUDANÇA AQUI:** De MarcaDao para MarcaRepository
    private lateinit var marcaRepository: MarcaRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // **MUDANÇA AQUI:** Inicializar o novo MarcaRepository
        marcaRepository = MarcaRepository(requireActivity().applicationContext)// Não precisa mais de AppDatabase

        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_edit_marca, null)

        val dialogTitle: TextView = view.findViewById(R.id.textViewDialogTitle)
        val editTextMarcaName: EditText = view.findViewById(R.id.editTextMarcaName)
        val buttonCancel: Button = view.findViewById(R.id.buttonCancel)
        val buttonSave: Button = view.findViewById(R.id.buttonSave)

        if (marca != null) {
            dialogTitle.text = "Editar Marca"
            editTextMarcaName.setText(marca.nome)
        } else {
            dialogTitle.text = "Nova Marca"
        }

        buttonCancel.setOnClickListener {
            dismiss()
        }

        buttonSave.setOnClickListener {
            val nome = editTextMarcaName.text.toString().trim()
            if (nome.isEmpty()) {
                editTextMarcaName.error = "Nome da marca é obrigatório"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                if (marca != null) {
                    // **MUDANÇA AQUI:** O ID da Marca é String. Não precisa de ID para copy, se já tiver um ID
                    val updatedMarca = marca.copy(nome = nome)
                    // **MUDANÇA AQUI:** Chamar o método do MarcaRepository
                    val success = marcaRepository.updateMarca(updatedMarca)
                    if (success) {
                        Toast.makeText(requireContext(), "Marca atualizada!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Erro ao atualizar marca.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // **MUDANÇA AQUI:** O ID do construtor de Marca é String?
                    // Firestore gerará o ID automaticamente, então não passamos no construtor
                    val newMarca = Marca(id = null, nome = nome) // Passe id como null
                    // **MUDANÇA AQUI:** Chamar o método do MarcaRepository
                    val newId = marcaRepository.addMarca(newMarca)
                    if (newId != null) {
                        Toast.makeText(requireContext(), "Marca adicionada!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Erro ao adicionar marca.", Toast.LENGTH_SHORT).show()
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