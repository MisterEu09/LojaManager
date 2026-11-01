package com.example.lojamamager

import ProdutosAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.Categoria
import com.example.lojamamager.data.Fornecedor
import com.example.lojamamager.data.Marca
import com.example.lojamamager.data.Produto
import com.example.lojamamager.firebase.repository.CategoriaRepository
import com.example.lojamamager.firebase.repository.FornecedorRepository
import com.example.lojamamager.firebase.repository.MarcaRepository
import com.example.lojamamager.firebase.repository.ProdutoRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProdutosActivity : BaseActivity() {

    private lateinit var produtoRepository: ProdutoRepository
    private lateinit var categoriaRepository: CategoriaRepository
    private lateinit var marcaRepository: MarcaRepository
    private lateinit var fornecedorRepository: FornecedorRepository
    private lateinit var imageViewDeleteSelecao: ImageView
    private lateinit var textViewTitle: TextView

    private var selectedCategory: Categoria? = null
    private var selectedMarca: Marca? = null
    private var selectedFornecedor: Fornecedor? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var produtosAdapter: ProdutosAdapter
    private lateinit var searchView: SearchView
    private lateinit var filterButton: ImageView
    private lateinit var fabAddProduto: FloatingActionButton
    private lateinit var imageViewBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_produtos)

        // 1. Inicializa views básicas primeiro
        recyclerView = findViewById(R.id.recyclerViewProdutos)
        searchView = findViewById(R.id.searchViewProdutos)
        filterButton = findViewById(R.id.imageViewFilter)
        fabAddProduto = findViewById(R.id.fabAddProduto)
        imageViewBack = findViewById(R.id.imageViewBackProdutos)
        imageViewDeleteSelecao = findViewById(R.id.imageViewDeleteSelecaoProdutos)

        // CORREÇÃO AQUI: Como a TextView está dentro do layout inflado da Activity,
        // você pode chamá-la diretamente pelo ID dela de forma limpa e segura!
        textViewTitle = findViewById(R.id.textViewTitleProdutos)

        produtoRepository = ProdutoRepository(applicationContext)
        categoriaRepository = CategoriaRepository(applicationContext)
        marcaRepository = MarcaRepository(applicationContext)
        fornecedorRepository = FornecedorRepository(applicationContext)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // 2. Inicializa o adapter
        produtosAdapter = ProdutosAdapter(emptyList(),
            onClick = { produto ->
                val intent = Intent(this, DetalheProdutoActivity::class.java)
                intent.putExtra("produto", produto)
                startActivity(intent)
            },
            onSelectionChanged = { total ->
                if (total > 0) {
                    textViewTitle.text = "$total selecionado(s)"
                    imageViewDeleteSelecao.visibility = View.VISIBLE
                    searchView.visibility = View.GONE
                } else {
                    textViewTitle.text = "Meus Produtos"
                    imageViewDeleteSelecao.visibility = View.GONE
                    searchView.visibility = View.VISIBLE
                    produtosAdapter.desativarModoSelecao()
                }
            }
        )
        recyclerView.adapter = produtosAdapter

        imageViewDeleteSelecao.setOnClickListener {
            confirmarExclusaoProdutosEmLote()
        }

        loadFilteredProdutos(null, null, null, null)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                loadFilteredProdutos(query.orEmpty(), selectedCategory?.id, selectedMarca?.id, selectedFornecedor?.id)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                loadFilteredProdutos(newText.orEmpty(), selectedCategory?.id, selectedMarca?.id, selectedFornecedor?.id)
                return true
            }
        })

        filterButton.setOnClickListener { showFilterDialog() }
        imageViewBack.setOnClickListener { finish() }
        fabAddProduto.setOnClickListener {
            startActivity(Intent(this, DetalheProdutoActivity::class.java))
        }
    }

    private fun loadFilteredProdutos(query: String?, categoriaId: String?, marcaId: String?, fornecedorId: String?) {
        lifecycleScope.launch {
            produtoRepository.searchProdutos(query, categoriaId, marcaId, fornecedorId, searchByCodeExact = false).collectLatest { produtos ->
                produtosAdapter.updateProdutos(produtos)
            }
        }
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filter_produtos, null)
        val textViewFilterCategory: TextView = dialogView.findViewById(R.id.textViewFilterCategory)
        val textViewFilterMarca: TextView = dialogView.findViewById(R.id.textViewFilterMarca)
        val textViewFilterFornecedor: TextView = dialogView.findViewById(R.id.textViewFilterFornecedor)
        val buttonClearAllFilters: Button = dialogView.findViewById(R.id.buttonClearAllFilters)

        textViewFilterCategory.text = selectedCategory?.nome ?: "Selecionar Categoria"
        textViewFilterMarca.text = selectedMarca?.nome ?: "Selecionar Marca"
        textViewFilterFornecedor.text = selectedFornecedor?.razaoSocial ?: "Selecionar Fornecedor"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Filtrar Produtos")
            .setView(dialogView)
            .setPositiveButton("Aplicar") { _, _ ->
                loadFilteredProdutos(searchView.query.toString(), selectedCategory?.id, selectedMarca?.id, selectedFornecedor?.id)
            }
            .setNegativeButton("Cancelar", null)
            .create()

        textViewFilterCategory.setOnClickListener {
            dialog.dismiss()
            CategoriaSelectionDialog { category ->
                selectedCategory = category
                showFilterDialog()
            }.show(supportFragmentManager, "FilterCategorySelectionDialog")
        }

        textViewFilterMarca.setOnClickListener {
            dialog.dismiss()
            MarcaSelectionDialog { marca ->
                selectedMarca = marca
                showFilterDialog()
            }.show(supportFragmentManager, "FilterMarcaSelectionDialog")
        }

        textViewFilterFornecedor.setOnClickListener {
            dialog.dismiss()
            FornecedorSelectionDialog { fornecedor ->
                selectedFornecedor = fornecedor
                showFilterDialog()
            }.show(supportFragmentManager, "FilterFornecedorSelectionDialog")
        }

        buttonClearAllFilters.setOnClickListener {
            selectedCategory = null
            selectedMarca = null
            selectedFornecedor = null
            Toast.makeText(this, "Filtros limpos!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            loadFilteredProdutos(searchView.query.toString(), null, null, null)
        }

        dialog.show()
    }

    private fun confirmarExclusaoProdutosEmLote() {
        val ids = produtosAdapter.getProdutosSelecionados()
        AlertDialog.Builder(this)
            .setTitle("Excluir Produtos")
            .setMessage("Confirma a exclusão permanente de ${ids.size} produtos selecionados?")
            .setPositiveButton("Sim, remover") { _, _ ->
                lifecycleScope.launch {
                    var contagemErros = 0
                    for (id in ids) {
                        val deletouComSucesso = produtoRepository.deleteProduto(id)
                        if (!deletouComSucesso) {
                            contagemErros++
                        }
                    }

                    textViewTitle.text = "Meus Produtos"
                    imageViewDeleteSelecao.visibility = View.GONE
                    searchView.visibility = View.VISIBLE
                    produtosAdapter.desativarModoSelecao()

                    if (contagemErros == 0) {
                        Toast.makeText(this@ProdutosActivity, "Produtos removidos com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProdutosActivity, "Falha ao remover $contagemErros produto(s).", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadFilteredProdutos(searchView.query.toString(), selectedCategory?.id, selectedMarca?.id, selectedFornecedor?.id)
    }
}