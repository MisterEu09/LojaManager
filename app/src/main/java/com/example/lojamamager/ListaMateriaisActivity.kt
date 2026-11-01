package com.example.lojamamager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.MaterialInsumo
import com.example.lojamamager.util.com.example.lojamamager.firebase.repository.MaterialRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ListaMateriaisActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var imgBack: ImageView
    private lateinit var searchView: SearchView
    private lateinit var imgDeleteSelecao: ImageView
    private lateinit var txtTitulo: TextView
    private lateinit var repository: MaterialRepository
    private lateinit var materialAdapter: MaterialAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_materiais)

        repository = MaterialRepository(applicationContext)
        recyclerView = findViewById(R.id.recyclerViewMateriais)
        fab = findViewById(R.id.fabAddMaterial)
        imgBack = findViewById(R.id.imageViewBack)
        searchView = findViewById(R.id.searchViewMateriais)
        imgDeleteSelecao = findViewById(R.id.imageViewDeleteSelecaoMateriais)
        txtTitulo = findViewById(R.id.textViewTitleMateriais)

        imgBack.setOnClickListener {
            if (materialAdapter.modoSelecaoAtivo) {
                cancelarSelecao()
            } else {
                finish()
            }
        }

        fab.setOnClickListener {
            startActivity(Intent(this, DetalheMaterialActivity::class.java))
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

        materialAdapter = MaterialAdapter(emptyList(),
            onClick = { materialClicado ->
                val intent = Intent(this, DetalheMaterialActivity::class.java)
                intent.putExtra("material", materialClicado)
                startActivity(intent)
            },
            onSelectionChanged = { quantidadeSelecionada ->
                atualizarInterfaceSelecao(quantidadeSelecionada)
            }
        )
        recyclerView.adapter = materialAdapter

        imgDeleteSelecao.setOnClickListener {
            confirmarExclusaoLote()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                materialAdapter.filter(newText.orEmpty())
                return true
            }
        })

        lifecycleScope.launch {
            repository.getMateriais().collectLatest { lista ->
                materialAdapter.updateData(lista)
            }
        }
    }

    private fun atualizarInterfaceSelecao(total: Int) {
        if (total > 0) {
            txtTitulo.text = "$total selecionado(s)"
            imgDeleteSelecao.visibility = View.VISIBLE
            searchView.visibility = View.GONE
        } else {
            cancelarSelecao()
        }
    }

    private fun cancelarSelecao() {
        txtTitulo.text = "Meus Materiais"
        imgDeleteSelecao.visibility = View.GONE
        searchView.visibility = View.VISIBLE
        materialAdapter.desativarModoSelecao()
    }

    private fun confirmarExclusaoLote() {
        val itensParaDeletar = materialAdapter.getMateriaisSelecionados()
        AlertDialog.Builder(this)
            .setTitle("Excluir em Lote")
            .setMessage("Deseja realmente apagar os ${itensParaDeletar.size} insumos selecionados de uma vez?")
            .setPositiveButton("Sim, apagar") { _, _ ->
                lifecycleScope.launch {
                    var falhas = 0
                    itensParaDeletar.forEach { id ->
                        val deletado = repository.deleteMaterial(id)
                        if (!deletado) falhas++
                    }
                    cancelarSelecao()
                    if (falhas == 0) {
                        Toast.makeText(this@ListaMateriaisActivity, "Todos os itens apagados!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ListaMateriaisActivity, "Erro ao apagar $falhas item(ns).", View.VISIBLE).show()
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    override fun onBackPressed() {
        if (materialAdapter.modoSelecaoAtivo) {
            cancelarSelecao()
        } else {
            super.onBackPressed()
        }
    }
}