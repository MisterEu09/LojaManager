package com.example.lojamamager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.ProductAnalysisItem
import com.example.lojamamager.data.Produto
import com.example.lojamamager.firebase.repository.PedidoRepository
import com.example.lojamamager.firebase.repository.ProdutoRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale


class EstoqueActivity : BaseActivity(), EstoqueProdutoClickListener {

    private lateinit var imageViewBack: ImageView
    private lateinit var textViewTotalItensEstoque: TextView
    private lateinit var textViewValorTotalEstoque: TextView
    private lateinit var spinnerAnalysisPeriodFilter: Spinner
    private lateinit var recyclerViewProductAnalysisEstoque: RecyclerView
    private lateinit var recyclerViewEstoqueProdutos: RecyclerView
    private lateinit var fabAddProduto: FloatingActionButton

    private lateinit var produtoRepository: ProdutoRepository
    private lateinit var pedidoRepository: PedidoRepository
    private lateinit var productAnalysisAdapter: ProductAnalysisAdapter
    private lateinit var estoqueProdutoAdapter: EstoqueProdutoAdapter

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estoque)

        imageViewBack = findViewById(R.id.imageViewBackEstoque)
        textViewTotalItensEstoque = findViewById(R.id.textViewTotalItensEstoque)
        textViewValorTotalEstoque = findViewById(R.id.textViewValorTotalEstoque)
        spinnerAnalysisPeriodFilter = findViewById(R.id.spinnerAnalysisPeriodFilter)
        recyclerViewProductAnalysisEstoque = findViewById(R.id.recyclerViewProductAnalysisEstoque)
        recyclerViewEstoqueProdutos = findViewById(R.id.recyclerViewEstoqueProdutos)
        fabAddProduto = findViewById(R.id.fabAddProduto)

        produtoRepository = ProdutoRepository(applicationContext)
        pedidoRepository = PedidoRepository(applicationContext)

        // Adapters precisam ser instanciados com o listener (que é esta Activity)
        productAnalysisAdapter = ProductAnalysisAdapter(emptyList(), this)
        recyclerViewProductAnalysisEstoque.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false) // layout horizontal para a análise
        recyclerViewProductAnalysisEstoque.adapter = productAnalysisAdapter

        estoqueProdutoAdapter = EstoqueProdutoAdapter(emptyList(), this)
        recyclerViewEstoqueProdutos.layoutManager = LinearLayoutManager(this)
        recyclerViewEstoqueProdutos.adapter = estoqueProdutoAdapter

        setupListeners()
        setupAnalysisPeriodFilter()
    }

    override fun onResume() {
        super.onResume()
        loadEstoqueData()
    }

    private fun setupListeners() {
        imageViewBack.setOnClickListener {
            finish()
        }

        fabAddProduto.setOnClickListener {
            val intent = Intent(this, DetalheProdutoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupAnalysisPeriodFilter() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.analysis_period_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAnalysisPeriodFilter.adapter = adapter

        spinnerAnalysisPeriodFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                analyzeProductPerformance()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        val defaultSelection = resources.getStringArray(R.array.analysis_period_options).indexOf("Últimos 2 Meses")
        if (defaultSelection != -1) {
            spinnerAnalysisPeriodFilter.setSelection(defaultSelection)
        }
    }

    private fun loadEstoqueData() {
        lifecycleScope.launch {
            produtoRepository.getTotalProdutosEmEstoque().collectLatest { total ->
                textViewTotalItensEstoque.text = "Total de Itens: ${total}"
            }
        }

        lifecycleScope.launch {
            produtoRepository.getValorTotalEstoque().collectLatest { valor ->
                textViewValorTotalEstoque.text = "Valor Total: ${currencyFormat.format(valor)}"
            }
        }

        lifecycleScope.launch {
            produtoRepository.getProdutosEmEstoque().collectLatest { productsInStock ->
                estoqueProdutoAdapter.updateProdutos(productsInStock)
            }
        }

        analyzeProductPerformance()
    }

    private fun getPeriodTimestampsForAnalysis(): Pair<Long?, Long?> {
        val selectedPeriod = spinnerAnalysisPeriodFilter.selectedItem.toString()
        val calendar = Calendar.getInstance()
        var startTime: Long? = null
        var endTime: Long? = Calendar.getInstance().timeInMillis

        // Ajusta endTime para o final do dia atual, se não for "Todo o Período"
        if (selectedPeriod != "Todo o Período") {
            endTime?.let {
                calendar.timeInMillis = it
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                endTime = calendar.timeInMillis
            }
        } else {
            // Se "Todo o Período", o endTime também deve ser nulo
            endTime = null
        }

        when (selectedPeriod) {
            "Todo o Período" -> {
                startTime = null
                // endTime já é null
            }
            "Últimos 7 Dias" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTime = calendar.timeInMillis
            }
            "Últimos 30 Dias" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTime = calendar.timeInMillis
            }
            "Últimos 2 Meses" -> {
                calendar.add(Calendar.MONTH, -2)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTime = calendar.timeInMillis
            }
            "Este Mês" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTime = calendar.timeInMillis
            }
            "Este Ano" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTime = calendar.timeInMillis
            }
        }
        return Pair(startTime, endTime)
    }

    private fun analyzeProductPerformance() {
        lifecycleScope.launch {
            val (startDate, endDate) = getPeriodTimestampsForAnalysis()
            val allProducts = produtoRepository.getAllProdutos().first()
            val itemsInPeriod = pedidoRepository.getAllItemsPedidosByDateRange(startDate, endDate).first()

            val productSalesMap = mutableMapOf<String, Int>()

            for (item in itemsInPeriod) {
                // **CORREÇÃO AQUI:** Verifica se produtoId não é nulo antes de usar como chave
                item.produtoId?.let { productId ->
                    productSalesMap[productId] = (productSalesMap[productId] ?: 0) + item.quantidade
                }
            }

            val analysisList = mutableListOf<ProductAnalysisItem>()
            val salesThreshold = 3

            val productsInStock = allProducts.filter { it.quantidadeEstoque > 0 }

            for (product in productsInStock) {
                // **AJUSTE AQUI:** Use !! para garantir que product.id não é nulo (já tem @DocumentId)
                val quantitySold = productSalesMap[product.id!!] ?: 0

                val suggestion: String
                val selectedPeriod = spinnerAnalysisPeriodFilter.selectedItem.toString()

                if (selectedPeriod == "Últimos 2 Meses" && quantitySold < salesThreshold) {
                    suggestion = "REVER/TROCAR"
                } else if (quantitySold < salesThreshold && selectedPeriod != "Todo o Período") {
                    // Esta condição já estava no seu código. Garante que para qualquer período
                    // que não seja "Todo o Período", se as vendas forem baixas, a sugestão é "REVER/TROCAR".
                    suggestion = "REVER/TROCAR"
                }
                else {
                    suggestion = "MANTER"
                }

                analysisList.add(
                    ProductAnalysisItem(
                        productId = product.id!!, // ID do produto é String e não nulo aqui
                        productName = product.nome,
                        productCode = product.codigo,
                        quantitySold = quantitySold,
                        suggestion = suggestion
                    )
                )
            }
            val sortedAnalysisList = analysisList.sortedWith(compareByDescending<ProductAnalysisItem> { it.suggestion == "MANTER" }.thenBy { it.productName })

            productAnalysisAdapter.updateAnalysisItems(sortedAnalysisList)
        }
    }

    // --- MÉTODOS DE CLIQUE CORRIGIDOS ---

    /**
     * @Override
     * Método para lidar com o clique em produtos do estoque.
     * CORRIGIDO: Passa o objeto Produto inteiro para a próxima tela.
     */
    override fun onProdutoClick(produto: Produto) {
        val intent = Intent(this, DetalheProdutoActivity::class.java)
        intent.putExtra("produto", produto)
        startActivity(intent)
    }

    /**
     * @Override
     * Método para lidar com o clique no botão "Rever/Trocar" na análise.
     * Implementado para buscar o produto e abrir a tela de edição.
     */
    fun onProductAnalysisClick(productId: String) {
        lifecycleScope.launch {
            // Buscamos o produto usando o ID do item de análise
            val produto = produtoRepository.getProdutoByIdSingle(productId)
            if (produto != null) {
                val intent = Intent(this@EstoqueActivity, DetalheProdutoActivity::class.java)
                intent.putExtra("produto", produto) // Passa o objeto Produto para a Activity
                startActivity(intent)
            }
        }
    }
}