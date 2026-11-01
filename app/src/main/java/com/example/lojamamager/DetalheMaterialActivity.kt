package com.example.lojamamager

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.lojamamager.data.MaterialInsumo
import com.example.lojamamager.util.com.example.lojamamager.firebase.repository.MaterialRepository
import kotlinx.coroutines.launch

class DetalheMaterialActivity : BaseActivity() {

    private lateinit var edtNome: EditText
    private lateinit var edtCusto: EditText
    private lateinit var edtEstoque: EditText
    private lateinit var edtUnidade: EditText
    private lateinit var btnSalvar: Button
    private lateinit var repository: MaterialRepository
    private lateinit var imageViewBack: ImageView
    private lateinit var imageViewDelete: ImageView // *** ADICIONADO ***
    private var materialEdicao: MaterialInsumo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_material)

        repository = MaterialRepository(applicationContext)
        edtNome = findViewById(R.id.editTextNomeMaterial)
        edtCusto = findViewById(R.id.editTextCustoMaterial)
        edtEstoque = findViewById(R.id.editTextEstoqueMaterial)
        edtUnidade = findViewById(R.id.editTextUnidadeMaterial)
        btnSalvar = findViewById(R.id.btnSalvarMaterial)
        imageViewBack = findViewById(R.id.imageViewBackMaterial)
        imageViewDelete = findViewById(R.id.imageViewDeleteMaterial) // *** ADICIONADO ***

        materialEdicao = intent.getParcelableExtra("material")

        materialEdicao?.let {
            edtNome.setText(it.nome)
            edtCusto.setText(it.precoCusto.toString())
            edtEstoque.setText(it.quantidadeEstoque.toString())
            edtUnidade.setText(it.unidadeMedida)

            // Se está editando, exibe o botão de lixeira
            imageViewDelete.visibility = View.VISIBLE
        }

        imageViewBack.setOnClickListener { finish() }

        imageViewDelete.setOnClickListener { confirmarExclusao() } // *** ADICIONADO ***

        btnSalvar.setOnClickListener { salvar() }
    }

    private fun confirmarExclusao() {
        val idParaDeletar = materialEdicao?.id ?: return

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Excluir Material")
            .setMessage("Tem certeza que deseja apagar permanentemente o insumo '${materialEdicao?.nome}'?")
            .setPositiveButton("Sim, excluir") { _, _ ->
                lifecycleScope.launch {
                    val deletado = repository.deleteMaterial(idParaDeletar)
                    if (deletado) {
                        Toast.makeText(this@DetalheMaterialActivity, "Insumo removido!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@DetalheMaterialActivity, "Erro ao deletar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }



    private fun salvar() {
        val nome = edtNome.text.toString().trim()
        val custo = edtCusto.text.toString().toDoubleOrNull() ?: 0.0
        val estoque = edtEstoque.text.toString().toDoubleOrNull() ?: 0.0
        val unidade = edtUnidade.text.toString().trim()

        if (nome.isEmpty() || unidade.isEmpty()) {
            Toast.makeText(this, "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }

        val material = MaterialInsumo(
            id = materialEdicao?.id,
            nome = nome,
            precoCusto = custo,
            quantidadeEstoque = estoque,
            unidadeMedida = unidade
        )

        lifecycleScope.launch {
            val sucesso = if (material.id == null) {
                repository.addMaterial(material) != null
            } else {
                repository.updateMaterial(material)
            }

            if (sucesso) {
                Toast.makeText(this@DetalheMaterialActivity, "Material Salvo!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@DetalheMaterialActivity, "Erro ao salvar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}