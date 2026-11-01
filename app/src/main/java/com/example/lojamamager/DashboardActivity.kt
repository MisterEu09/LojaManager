package com.example.lojamamager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.TopSellingProductData
import com.example.lojamamager.firebase.repository.ClienteRepository
import com.example.lojamamager.firebase.repository.PedidoRepository
import com.example.lojamamager.firebase.repository.ProdutoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class DashboardActivity : BaseActivity() {

    private lateinit var imageViewBack: ImageView
    private lateinit var spinnerPeriodFilter: Spinner
    private lateinit var imageViewRefreshDashboard: ImageView

    private lateinit var textViewTotalPedidos: TextView
    private lateinit var textViewTotalClientes: TextView
    private lateinit var textViewTotalVendas: TextView
    private lateinit var textViewCustoTotal: TextView
    private lateinit var textViewLucroTotal: TextView
    private lateinit var textViewProximasEntregas: TextView
    private lateinit var textViewTotalProdutosEstoque: TextView
    private lateinit var textViewValorTotalEstoque: TextView

    private lateinit var recyclerViewTopSellingProducts: RecyclerView
    private lateinit var topSellingAdapter: TopSellingProductAdapter

    private lateinit var pedidoRepository: PedidoRepository
    private lateinit var clienteRepository: ClienteRepository
    private lateinit var produtoRepository: ProdutoRepository

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        imageViewBack = findViewById(R.id.imageViewBackDashboard)
        spinnerPeriodFilter = findViewById(R.id.spinnerPeriodFilter)
        imageViewRefreshDashboard = findViewById(R.id.imageViewRefreshDashboard)

        textViewTotalPedidos = findViewById(R.id.textViewTotalPedidos)
        textViewTotalClientes = findViewById(R.id.textViewTotalClientes)
        textViewTotalVendas = findViewById(R.id.textViewTotalVendas)
        textViewCustoTotal = findViewById(R.id.textViewCustoTotal)
        textViewLucroTotal = findViewById(R.id.textViewLucroTotal)
        textViewProximasEntregas = findViewById(R.id.textViewProximasEntregas)
        textViewTotalProdutosEstoque = findViewById(R.id.textViewTotalProdutosEstoque)
        textViewValorTotalEstoque = findViewById(R.id.textViewValorTotalEstoque)

        recyclerViewTopSellingProducts = findViewById(R.id.recyclerViewTopSellingProducts)
        topSellingAdapter = TopSellingProductAdapter(emptyList())
        recyclerViewTopSellingProducts.layoutManager = LinearLayoutManager(this)
        recyclerViewTopSellingProducts.adapter = topSellingAdapter

        pedidoRepository = PedidoRepository(applicationContext)
        clienteRepository = ClienteRepository(applicationContext)
        produtoRepository = ProdutoRepository(applicationContext)
        findViewById<CardView>(R.id.cardMateriais).setOnClickListener {
            startActivity(Intent(this, ListaMateriaisActivity::class.java))
        }
        setupListeners()
        setupPeriodFilter()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun setupListeners() {
        imageViewBack.setOnClickListener {
            finish()
        }

        imageViewRefreshDashboard.setOnClickListener {
            loadDashboardData()
        }

        spinnerPeriodFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Aumentar o tempo de espera antes de carregar para evitar cliques muito rápidos
                // Também garante que a seleção é diferente da anterior.
                loadDashboardData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<CardView>(R.id.cardEstoque).setOnClickListener {
            val intent = Intent(this, EstoqueActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupPeriodFilter() {
        val periodOptions = arrayOf("Todo o Período", "Últimos 7 Dias", "Últimos 30 Dias", "Últimos 2 Meses", "Este Mês", "Este Ano")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periodOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPeriodFilter.adapter = adapter
    }

    private fun getPeriodTimestamps(): Pair<Long?, Long?> {
        val selectedPeriod = spinnerPeriodFilter.selectedItem.toString()
        val calendar = Calendar.getInstance()
        var startTime: Long? = null
        var endTime: Long? = null

        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 23)
        now.set(Calendar.MINUTE, 59)
        now.set(Calendar.SECOND, 59)
        now.set(Calendar.MILLISECOND, 999)
        endTime = now.timeInMillis

        when (selectedPeriod) {
            "Todo o Período" -> {
                startTime = null
                endTime = null
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

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val (startTime, endTime) = getPeriodTimestamps()

                // Envolve cada chamada de repositório em um bloco try-catch para isolar falhas
                try {
                    val totalPedidos = pedidoRepository.getTotalPedidosByDateRange(startTime, endTime).first()
                    textViewTotalPedidos.text = totalPedidos.toString()
                } catch (e: Exception) {
                    textViewTotalPedidos.text = "Falha"
                    Toast.makeText(this@DashboardActivity, "Erro ao carregar pedidos.", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }

                try {
                    val totalClientes = clienteRepository.getClientesCount().first()
                    textViewTotalClientes.text = totalClientes.toString()
                } catch (e: Exception) {
                    textViewTotalClientes.text = "Falha"
                    Toast.makeText(this@DashboardActivity, "Erro ao carregar clientes.", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }

                try {
                    var totalVendas = 0.0
                    var totalCusto = 0.0
                    val pedidosWithItems = pedidoRepository.getPedidosWithItemsByDateRange(startTime, endTime).first()

                    for (pedidoWithItems in pedidosWithItems) {
                        totalVendas += pedidoWithItems.pedido.valorTotal
                        for (item in pedidoWithItems.itens) {
                            item.produtoId?.let { productId ->
                                val produtoOriginal = produtoRepository.getProdutoByIdSingle(productId)
                                produtoOriginal?.let {
                                    totalCusto += it.precoCusto * item.quantidade
                                }
                            }
                        }
                    }
                    val lucroTotal = totalVendas - totalCusto

                    textViewTotalVendas.text = currencyFormat.format(totalVendas)
                    textViewCustoTotal.text = currencyFormat.format(totalCusto)
                    textViewLucroTotal.text = currencyFormat.format(lucroTotal)
                } catch (e: Exception) {
                    textViewTotalVendas.text = "Falha"
                    textViewCustoTotal.text = "Falha"
                    textViewLucroTotal.text = "Falha"
                    Toast.makeText(this@DashboardActivity, "Erro ao calcular vendas e custos.", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }

                try {
                    val now = System.currentTimeMillis()
                    val sevenDaysFromNow = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, 7)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    val proximasEntregasCount = pedidoRepository.getUpcomingDeliveries(now, sevenDaysFromNow).first()
                    textViewProximasEntregas.text = proximasEntregasCount.toString()
                } catch (e: Exception) {
                    textViewProximasEntregas.text = "Falha"
                    Toast.makeText(this@DashboardActivity, "Erro ao carregar entregas.", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }


                try {
                    val totalProdutosEmEstoque = produtoRepository.getTotalProdutosEmEstoque().first()
                    textViewTotalProdutosEstoque.text = (totalProdutosEmEstoque).toString()
                } catch (e: Exception) {
                    textViewTotalProdutosEstoque.text = "Falha"
                    Toast.makeText(this@DashboardActivity, "Erro ao carregar estoque.", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }


                try {
                    val valorTotalEstoque = produtoRepository.getValorTotalEstoque().first()
                    textViewValorTotalEstoque.text = currencyFormat.format(valorTotalEstoque)
                } catch (e: Exception) {
                    textViewValorTotalEstoque.text = "Falha"
                    Toast.makeText(this@DashboardActivity, "Erro ao carregar valor do estoque.", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }


                try {
                    val topSellingProductsData: List<TopSellingProductData> = pedidoRepository.getTopSellingProducts(5, startTime, endTime).first()
                    val mappedTopSelling = topSellingProductsData.mapIndexed { index, data ->
                        TopSellingProduct(
                            rank = index + 1,
                            productName = data.productName,
                            quantitySold = data.quantitySold,
                            totalValue = data.totalValue
                        )
                    }
                    topSellingAdapter.updateProducts(mappedTopSelling)
                } catch (e: Exception) {
                    topSellingAdapter.updateProducts(emptyList())
                    Toast.makeText(this@DashboardActivity, "Erro ao carregar produtos mais vendidos.", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }


            } catch (e: Exception) {
                // Este catch final é para qualquer erro que não foi capturado pelos blocos internos
                Toast.makeText(this@DashboardActivity, "Falha geral ao carregar dados.", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}