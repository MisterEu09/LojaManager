package com.example.lojamamager

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.Categoria
import com.example.lojamamager.data.Fornecedor
import com.example.lojamamager.data.Marca
import com.example.lojamamager.data.Produto
import com.example.lojamamager.firebase.repository.ProdutoRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Importe os Selection Dialogs (presumindo que você já os tenha)
// import com.example.lojamamager.CategoriaSelectionDialog
// import com.example.lojamamager.MarcaSelectionDialog
// import com.example.lojamamager.FornecedorSelectionDialog


class CatalogoProdutosActivity : BaseActivity() {

    private lateinit var produtoRepository: ProdutoRepository
    // Não precisamos dos outros repositórios aqui, pois eles são usados apenas pelos Selection Dialogs
    // e não para carregar os nomes no CatalogoProdutoAdapter, que já recebe os nomes no objeto Produto.

    private var selectedCategory: Categoria? = null
    private var selectedMarca: Marca? = null
    private var selectedFornecedor: Fornecedor? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var catalogoProdutoAdapter: CatalogoProdutoAdapter
    private lateinit var searchView: SearchView
    private lateinit var filterButton: ImageView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalogo_produtos)

        // Inicializar Views
        recyclerView = findViewById(R.id.recyclerViewCatalogoProdutos)
        searchView = findViewById(R.id.searchViewCatalogoProdutos)
        filterButton = findViewById(R.id.imageViewFilterCatalogo)
        backButton = findViewById(R.id.imageViewBackCatalogo)

        // Inicializar o Repositório de Produtos
        produtoRepository = ProdutoRepository(applicationContext)

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        catalogoProdutoAdapter = CatalogoProdutoAdapter(emptyList())
        recyclerView.adapter = catalogoProdutoAdapter

        // Configurar clique nos itens da lista
        catalogoProdutoAdapter.onItemClick = { produto ->
            // Ao clicar, abrimos a DetalheCatalogoProdutoActivity
            val intent = Intent(this, DetalheCatalogoProdutoActivity::class.java)
            intent.putExtra("produto", produto)
            startActivity(intent)
        }

        // Carrega os produtos iniciais sem filtros
        loadFilteredProdutos(null, null, null, null)

        // Configurar SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                loadFilteredProdutos(query.orEmpty(), selectedCategory?.id, selectedMarca?.id, selectedFornecedor?.id)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Atualiza a lista em tempo real conforme o usuário digita
                loadFilteredProdutos(newText.orEmpty(), selectedCategory?.id, selectedMarca?.id, selectedFornecedor?.id)
                return true
            }
        })

        // Configurar Botão de Filtro
        filterButton.setOnClickListener {
            showFilterDialog()
        }

        // Configurar Botão de Voltar
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Simula o botão voltar do sistema
        }
    }

    /**
     * Carrega e filtra produtos do repositório, atualizando o adapter.
     */
    private fun loadFilteredProdutos(query: String?, categoriaId: String?, marcaId: String?, fornecedorId: String?) {
        lifecycleScope.launch {
            // Chamamos o searchProdutos do ProdutoRepository, que já ajustamos para o Firestore
            produtoRepository.searchProdutos(query, categoriaId, marcaId, fornecedorId).collectLatest { produtos ->
                // Filtramos produtos com quantidadeEstoque > 0 no CLIENTE se for um requisito para o catálogo
                // OU, você pode criar um método no ProdutoRepository que já faça esse filtro no Firestore.
                // Mas se precisar, a linha abaixo mostraria apenas produtos com estoque > 0:
                // val produtosDisponiveis = produtos.filter { it.quantidadeEstoque > 0 }
                // catalogoProdutoAdapter.updateProdutos(produtosDisponiveis)

                catalogoProdutoAdapter.updateProdutos(produtos)
            }
        }
    }

    /**
     * Mostra o diálogo de filtros para categoria, marca e fornecedor.
     */
    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filter_produtos, null)
        val textViewFilterCategory: TextView = dialogView.findViewById(R.id.textViewFilterCategory)
        val textViewFilterMarca: TextView = dialogView.findViewById(R.id.textViewFilterMarca)
        val textViewFilterFornecedor: TextView = dialogView.findViewById(R.id.textViewFilterFornecedor)
        val buttonClearAllFilters: Button = dialogView.findViewById(R.id.buttonClearAllFilters)

        // Preencher com as seleções atuais
        textViewFilterCategory.text = selectedCategory?.nome ?: "Selecionar Categoria"
        textViewFilterMarca.text = selectedMarca?.nome ?: "Selecionar Marca"
        textViewFilterFornecedor.text = selectedFornecedor?.razaoSocial ?: "Selecionar Fornecedor"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Filtrar Catálogo")
            .setView(dialogView)
            .setPositiveButton("Aplicar") { _, _ ->
                // Ao aplicar, chamamos a função de carregamento com os filtros atuais
                loadFilteredProdutos(searchView.query.toString(), selectedCategory?.id, selectedMarca?.id, selectedFornecedor?.id)
            }
            .setNegativeButton("Cancelar", null)
            .create()

        textViewFilterCategory.setOnClickListener {
            dialog.dismiss()
            // Supondo que CategoriaSelectionDialog retorna um objeto Categoria
            CategoriaSelectionDialog { category ->
                selectedCategory = category
                showFilterDialog() // Reabre o diálogo para atualizar o texto
            }.show(supportFragmentManager, "FilterCategorySelectionDialog")
        }

        textViewFilterMarca.setOnClickListener {
            dialog.dismiss()
            // Supondo que MarcaSelectionDialog retorna um objeto Marca
            MarcaSelectionDialog { marca ->
                selectedMarca = marca
                showFilterDialog() // Reabre o diálogo para atualizar o texto
            }.show(supportFragmentManager, "FilterMarcaSelectionDialog")
        }

        textViewFilterFornecedor.setOnClickListener {
            dialog.dismiss()
            // Supondo que FornecedorSelectionDialog retorna um objeto Fornecedor
            FornecedorSelectionDialog { fornecedor ->
                selectedFornecedor = fornecedor
                showFilterDialog() // Reabre o diálogo para atualizar o texto
            }.show(supportFragmentManager, "FilterFornecedorSelectionDialog")
        }

        buttonClearAllFilters.setOnClickListener {
            selectedCategory = null
            selectedMarca = null
            selectedFornecedor = null
            Toast.makeText(this, "Filtros limpos!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            // Recarrega sem filtros de categoria/marca/fornecedor
            loadFilteredProdutos(searchView.query.toString(), null, null, null)
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        // Recarrega os produtos sempre que a tela é retomada (após voltar de um detalhe, por exemplo)
        loadFilteredProdutos(searchView.query.toString(), selectedCategory?.id, selectedMarca?.id, selectedFornecedor?.id)
    }
}