package com.example.lojamamager

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.ProdutoSelecaoAdapter
import com.example.lojamamager.R
import com.example.lojamamager.data.ItemPedido
import com.example.lojamamager.data.Produto
import com.example.lojamamager.firebase.repository.CategoriaRepository
import com.example.lojamamager.firebase.repository.MarcaRepository
import com.example.lojamamager.firebase.repository.ProdutoRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class DetalheItemPedidoActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var produtoSelecaoAdapter: ProdutoSelecaoAdapter
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var imageViewBack: ImageView
    private lateinit var imageViewFilter: ImageView
    private lateinit var buttonConfirmarSelecao: Button

    private lateinit var produtoRepository: ProdutoRepository
    private lateinit var categoriaRepository: CategoriaRepository
    private lateinit var marcaRepository: MarcaRepository

    private var currentSearchQuery: String? = null // Usado para busca por nome
    private var selectedCodigoExato: String? = null // Usado para busca exata por código no filtro
    private var selectedCategoriaId: String? = null
    private var selectedMarcaId: String? = null

    private val selectedItemsMap: MutableMap<String, ItemPedido> = mutableMapOf()

    // A anotação @RequiresApi(Build.VERSION_CODES.TIRAMISU) pode ser removida da função onCreate
    // ou mantida se houver outras dependências de API 33+ que você não está verificando explicitamente.
    // Para a linha 71, a verificação interna já resolve.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_item_pedido)

        recyclerView = findViewById(R.id.recyclerViewItensDisponiveis)
        searchView = findViewById(R.id.searchViewItensPedido)
        imageViewBack = findViewById(R.id.imageViewBackItemPedido)
        imageViewFilter = findViewById(R.id.imageViewFilterItensPedido)
        buttonConfirmarSelecao = findViewById(R.id.buttonConfirmarSelecao)
        produtoRepository = ProdutoRepository(applicationContext)
        categoriaRepository = CategoriaRepository(applicationContext)
        marcaRepository = MarcaRepository(applicationContext)

        // --- INÍCIO DA CORREÇÃO NA LINHA 71 ---
        val existingItems = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("existing_items", ItemPedido::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("existing_items")
        }?.filterIsInstance<ItemPedido>() // Filtra e garante que são do tipo ItemPedido
        // --- FIM DA CORREÇÃO NA LINHA 71 ---

        existingItems?.forEach { item ->
            item.produtoId?.takeIf { it.isNotEmpty() }?.let { productId ->
                selectedItemsMap[productId] = item
            }
        }

        val itemQuantitiesMap: MutableMap<String, Int> = mutableMapOf()
        selectedItemsMap.values.forEach { item ->
            item.produtoId?.takeIf { it.isNotEmpty() }?.let { productId ->
                itemQuantitiesMap[productId] = item.quantidade
            }
        }

        produtoSelecaoAdapter = ProdutoSelecaoAdapter(
            emptyList(),
            onAddItem = { produto ->
                val productId = produto.id!!
                val currentQuantity = itemQuantitiesMap[productId] ?: 0
                val newQuantity = currentQuantity + 1
                itemQuantitiesMap[productId] = newQuantity
                produtoSelecaoAdapter.updateItemQuantity(productId, newQuantity)

                val existingItem = selectedItemsMap[productId]
                selectedItemsMap[productId] = ItemPedido(
                    id = existingItem?.id ?: "",
                    pedidoId = existingItem?.pedidoId ?: "",
                    produtoId = productId,
                    nomeProduto = produto.nome,
                    codigoProduto = produto.codigo,
                    quantidade = newQuantity,
                    precoUnitario = existingItem?.precoUnitario ?: produto.precoVenda
                )
            },
            onRemoveItem = { produto ->
                val productId = produto.id!!
                val currentQuantity = itemQuantitiesMap[productId] ?: 0
                if (currentQuantity > 0) {
                    val newQuantity = currentQuantity - 1
                    itemQuantitiesMap[productId] = newQuantity
                    produtoSelecaoAdapter.updateItemQuantity(productId, newQuantity)
                    if (newQuantity == 0) {
                        selectedItemsMap.remove(productId)
                    } else {
                        selectedItemsMap[productId]?.let {
                            selectedItemsMap[productId] = it.copy(quantidade = newQuantity)
                        }
                    }
                }
            },
            onItemClick = { produto ->
                showEditItemDialog(produto)
            },
            itemQuantities = itemQuantitiesMap
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = produtoSelecaoAdapter

        // Carrega os produtos iniciais com os filtros definidos (incluindo o código exato se houver)
        filterProdutos()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query.orEmpty().ifEmpty { null }
                selectedCodigoExato = null // Limpa o filtro de código exato ao usar a barra de busca
                filterProdutos()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText.orEmpty().ifEmpty { null }
                selectedCodigoExato = null // Limpa o filtro de código exato ao usar a barra de busca
                filterProdutos()
                return true
            }
        })

        imageViewBack.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        imageViewFilter.setOnClickListener {
            showFilterDialogItensPedido()
        }

        buttonConfirmarSelecao.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("selected_pedido_items", ArrayList(selectedItemsMap.values.filter { it.quantidade > 0 }))
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun filterProdutos() {
        lifecycleScope.launch {
            val queryTextForSearch = currentSearchQuery
            val isSearchingByCodeExact = !selectedCodigoExato.isNullOrBlank()

            val queryToRepo: String? = if (isSearchingByCodeExact) selectedCodigoExato else queryTextForSearch

            // Chamamos o searchProdutos com o novo parâmetro searchByCodeExact
            produtoRepository.searchProdutos(
                query = queryToRepo,
                categoriaId = selectedCategoriaId,
                marcaId = selectedMarcaId,
                fornecedorId = null, // Deixamos como null, pois você não tem filtro de fornecedor aqui
                searchByCodeExact = isSearchingByCodeExact // Passamos true se for busca por código exato
            ).collectLatest { produtos ->
                produtoSelecaoAdapter.updateProdutos(produtos)
            }
        }
    }


    private fun showEditItemDialog(produto: Produto) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_item_pedido, null)
        val textViewDialogProdutoCodigo: TextView = dialogView.findViewById(R.id.textViewDialogProdutoCodigo)
        val textViewDialogProdutoNome: TextView = dialogView.findViewById(R.id.textViewDialogProdutoNome)
        val editTextDialogPrecoVenda: EditText = dialogView.findViewById(R.id.editTextDialogPrecoVenda)
        val textViewPorcentagemAjuste: TextView = dialogView.findViewById(R.id.textViewPorcentagemAjuste)
        val editTextDialogQuantidade: EditText = dialogView.findViewById(R.id.editTextDialogQuantidade)
        val editTextDialogAcrescimo: EditText = dialogView.findViewById(R.id.editTextDialogAcrescimo)
        val editTextDialogDesconto: EditText = dialogView.findViewById(R.id.editTextDialogDesconto)
        val textViewDialogSubtotal: TextView = dialogView.findViewById(R.id.textViewDialogSubtotal)
        val buttonDialogVoltar: Button = dialogView.findViewById(R.id.buttonDialogVoltar)
        val buttonDialogSalvar: Button = dialogView.findViewById(R.id.buttonDialogSalvar)

        textViewDialogProdutoCodigo.text = "Código: ${produto.codigo ?: "N/A"}"
        textViewDialogProdutoNome.text = "Nome: ${produto.nome}"

        val productId = produto.id!!
        val existingItem = selectedItemsMap[productId]
        val precoVendaOriginalProduto = produto.precoVenda

        var currentPrice = existingItem?.precoUnitario ?: precoVendaOriginalProduto
        var currentQuantity = existingItem?.quantidade ?: 0

        editTextDialogPrecoVenda.setText(String.format(Locale.getDefault(), "%.2f", currentPrice))
        editTextDialogQuantidade.setText(currentQuantity.toString())

        val calculateAndDisplayAdjustmentPercentage = {
            val precoDigitadoStr = editTextDialogPrecoVenda.text.toString().trim()
            val precoDigitado = precoDigitadoStr.toDoubleOrNull() ?: 0.0

            if (precoVendaOriginalProduto > 0.0 && precoDigitado > 0.0) {
                if (precoDigitado != precoVendaOriginalProduto) {
                    val diferenca = precoDigitado - precoVendaOriginalProduto
                    val porcentagem = (diferenca / precoVendaOriginalProduto) * 100

                    if (porcentagem > 0) {
                        textViewPorcentagemAjuste.text = "Acréscimo: %.2f%%".format(Locale.getDefault(), abs(porcentagem))
                        textViewPorcentagemAjuste.setTextColor(getColor(R.color.md_theme_tertiary))
                    } else {
                        textViewPorcentagemAjuste.text = "Desconto: %.2f%%".format(Locale.getDefault(), abs(porcentagem))
                        textViewPorcentagemAjuste.setTextColor(getColor(R.color.md_theme_error))
                    }
                    textViewPorcentagemAjuste.visibility = View.VISIBLE
                } else {
                    textViewPorcentagemAjuste.text = ""
                    textViewPorcentagemAjuste.visibility = View.GONE
                }
            } else {
                textViewPorcentagemAjuste.text = ""
                textViewPorcentagemAjuste.visibility = View.GONE
            }
        }

        val calculateSubtotal = {
            val precoVendaStr = editTextDialogPrecoVenda.text.toString().trim()
            val quantidadeStr = editTextDialogQuantidade.text.toString().trim()
            val acrescimoStr = editTextDialogAcrescimo.text.toString().trim()
            val descontoStr = editTextDialogDesconto.text.toString().trim()

            var precoVenda = precoVendaStr.toDoubleOrNull() ?: 0.0
            val quantidade = quantidadeStr.toIntOrNull() ?: 0
            val acrescimoPercent = acrescimoStr.toDoubleOrNull() ?: 0.0
            val descontoPercent = descontoStr.toDoubleOrNull() ?: 0.0

            if (acrescimoPercent > 0) {
                precoVenda += precoVenda * (acrescimoPercent / 100)
                editTextDialogDesconto.setText("") // Limpa o campo de desconto
            }
            if (descontoPercent > 0) {
                precoVenda -= precoVenda * (descontoPercent / 100)
                editTextDialogAcrescimo.setText("") // Limpa o campo de acréscimo
            }

            val subtotal = precoVenda * quantidade
            textViewDialogSubtotal.text = "Subtotal do Item: ${NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(subtotal)}"
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateSubtotal()
                calculateAndDisplayAdjustmentPercentage()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        editTextDialogPrecoVenda.addTextChangedListener(textWatcher)
        editTextDialogQuantidade.addTextChangedListener(textWatcher)
        editTextDialogAcrescimo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true) {
                    editTextDialogDesconto.setText("")
                }
                calculateSubtotal()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        editTextDialogDesconto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true) {
                    editTextDialogAcrescimo.setText("")
                }
                calculateSubtotal()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        calculateSubtotal()
        calculateAndDisplayAdjustmentPercentage()

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        buttonDialogSalvar.setOnClickListener {
            val precoVendaStr = editTextDialogPrecoVenda.text.toString().trim()
            val quantidadeStr = editTextDialogQuantidade.text.toString().trim()
            val acrescimoStr = editTextDialogAcrescimo.text.toString().trim()
            val descontoStr = editTextDialogDesconto.text.toString().trim()

            val precoVendaBase = precoVendaStr.toDoubleOrNull()
            val quantidade = quantidadeStr.toIntOrNull()
            val acrescimoPercent = acrescimoStr.toDoubleOrNull() ?: 0.0
            val descontoPercent = descontoStr.toDoubleOrNull() ?: 0.0

            if (precoVendaBase == null || precoVendaBase <= 0) {
                Toast.makeText(this, "Preço de venda inválido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (quantidade == null || quantidade <= 0) {
                Toast.makeText(this, "Quantidade inválida.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var finalPrice = precoVendaBase
            if (acrescimoPercent > 0) {
                finalPrice += finalPrice * (acrescimoPercent / 100)
            }
            if (descontoPercent > 0) {
                finalPrice -= finalPrice * (descontoPercent / 100)
            }

            val item = ItemPedido(
                id = existingItem?.id ?: "",
                pedidoId = existingItem?.pedidoId ?: "",
                produtoId = productId,
                nomeProduto = produto.nome,
                codigoProduto = produto.codigo,
                quantidade = quantidade,
                precoUnitario = finalPrice
            )
            selectedItemsMap[productId] = item
            produtoSelecaoAdapter.updateItemQuantity(productId, quantidade)
            dialog.dismiss()
        }

        buttonDialogVoltar.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showFilterDialogItensPedido() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filter_itens_pedido, null)
        val editTextFilterCodigoExato: EditText = dialogView.findViewById(R.id.editTextFilterCodigoExato)
        val spinnerCategoria: Spinner = dialogView.findViewById(R.id.spinnerFilterCategoriaItemPedido)
        val spinnerMarca: Spinner = dialogView.findViewById(R.id.spinnerFilterMarcaItemPedido)
        val buttonClearItemFilters: Button = dialogView.findViewById(R.id.buttonClearItemFilters)

        // Preenche o campo de código exato com o valor atual
        editTextFilterCodigoExato.setText(selectedCodigoExato ?: "")

        lifecycleScope.launch {
            val categorias = categoriaRepository.getAllCategorias().first()
            val marcas = marcaRepository.getAllMarcas().first()

            val categoriaNames = listOf("Todas") + categorias.map { it.nome }
            val marcaNames = listOf("Todas") + marcas.map { it.nome }

            val categoriaAdapter = ArrayAdapter(this@DetalheItemPedidoActivity, android.R.layout.simple_spinner_item, categoriaNames)
            categoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategoria.adapter = categoriaAdapter
            selectedCategoriaId?.let { id ->
                val categoriaNome = categorias.find { it.id == id }?.nome
                val index = categoriaNames.indexOf(categoriaNome)
                if (index != -1) spinnerCategoria.setSelection(index)
            } ?: spinnerCategoria.setSelection(0)

            val marcaAdapter = ArrayAdapter(this@DetalheItemPedidoActivity, android.R.layout.simple_spinner_item, marcaNames)
            marcaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMarca.adapter = marcaAdapter
            selectedMarcaId?.let { id ->
                val marcaNome = marcas.find { it.id == id }?.nome
                val index = marcaNames.indexOf(marcaNome)
                if (index != -1) spinnerMarca.setSelection(index)
            } ?: spinnerMarca.setSelection(0)

            val dialog = AlertDialog.Builder(this@DetalheItemPedidoActivity)
                .setTitle("Filtrar Produtos")
                .setView(dialogView)
                .setPositiveButton("Aplicar") { _, _ ->
                    // Atualiza o selectedCodigoExato
                    selectedCodigoExato = editTextFilterCodigoExato.text.toString().trim().ifEmpty { null }
                    currentSearchQuery = null // Limpa a busca textual para que o filtro de código exato seja priorizado

                    selectedCategoriaId = if (spinnerCategoria.selectedItemPosition == 0) null else {
                        val categoriaNomeSelecionada = spinnerCategoria.selectedItem.toString()
                        categorias.find { it.nome == categoriaNomeSelecionada }?.id
                    }

                    selectedMarcaId = if (spinnerMarca.selectedItemPosition == 0) null else {
                        val marcaNomeSelecionada = spinnerMarca.selectedItem.toString()
                        marcas.find { it.id == marcaNomeSelecionada }?.id
                    }
                    filterProdutos() // Chama para aplicar os novos filtros
                }
                .setNegativeButton("Cancelar", null)
                .create()

            buttonClearItemFilters.setOnClickListener {
                selectedCodigoExato = null
                currentSearchQuery = null // Limpa também a query de busca textual
                selectedCategoriaId = null
                selectedMarcaId = null
                editTextFilterCodigoExato.setText("")
                spinnerCategoria.setSelection(0)
                spinnerMarca.setSelection(0)
                Toast.makeText(this@DetalheItemPedidoActivity, "Filtros de produtos limpos!", Toast.LENGTH_SHORT).show()
                filterProdutos() // Chama para recarregar sem filtros
                dialog.dismiss()
            }
            dialog.show()
        }
    }
}