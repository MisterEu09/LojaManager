package com.example.lojamamager

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.lojamamager.data.ItemPedido
import com.example.lojamamager.data.Pedido
import com.example.lojamamager.firebase.repository.ClienteRepository
import com.example.lojamamager.firebase.repository.PedidoRepository
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DetalhePedidoActivity : BaseActivity() {

    private lateinit var toolbarTitle: TextView
    private lateinit var imageViewBack: ImageView
    private lateinit var imageViewSave: ImageView
    private lateinit var imageViewDelete: ImageView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var textViewTotalPedido: TextView

    private lateinit var pedidoRepository: PedidoRepository
    private lateinit var clienteRepository: ClienteRepository

    var pedidoEmEdicao: Pedido? = null
    private var originalHeaderData: PedidoHeaderData? = null
    private var originalItems: List<ItemPedido>? = null

    private lateinit var headerFragment: PedidoHeaderFragment
    private lateinit var itemsFragment: PedidoItemsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_pedido)

        toolbarTitle = findViewById(R.id.textViewToolbarTitlePedido)
        imageViewBack = findViewById(R.id.imageViewBackPedido)
        imageViewSave = findViewById(R.id.imageViewSavePedido)
        imageViewDelete = findViewById(R.id.imageViewDeletePedido)
        tabLayout = findViewById(R.id.tabLayoutPedido)
        viewPager = findViewById(R.id.viewPagerPedido)
        textViewTotalPedido = findViewById(R.id.textViewTotalPedido)

        pedidoRepository = PedidoRepository(applicationContext)
        clienteRepository = ClienteRepository(applicationContext)
        pedidoEmEdicao = intent.getParcelableExtra("pedido") as? Pedido

        headerFragment = PedidoHeaderFragment.newInstance(pedidoEmEdicao)
        itemsFragment = PedidoItemsFragment()

        setupViewPager()
        setupListeners()

        // CORREÇÃO: Usar um listener para saber quando o fragmento está pronto
        headerFragment.onFragmentReady = {
            if (pedidoEmEdicao != null) {
                toolbarTitle.text = "Editar Pedido"
                imageViewDelete.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        val itensDoPedido = pedidoRepository.getItensPedidoForPedido(pedidoEmEdicao!!.id!!).first()
                        itemsFragment.updateItems(itensDoPedido)
                        // Agora a view do fragmento já existe, é seguro obter os dados
                        originalHeaderData = headerFragment.getHeaderData()
                        originalItems = itensDoPedido.toList()
                        Log.d("DetalhePedidoActivity", "Dados originais carregados. Itens: ${originalItems?.size}")

                    } catch (e: Exception) {
                        Log.e("DetalhePedidoActivity", "Erro ao carregar itens do pedido: ${e.message}")
                        Toast.makeText(this@DetalhePedidoActivity, "Erro ao carregar itens do pedido: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                toolbarTitle.text = "Novo Pedido"
                imageViewDelete.visibility = View.GONE
                calculateTotal()
                originalHeaderData = headerFragment.getHeaderData()
                originalItems = emptyList()
                Log.d("DetalhePedidoActivity", "Novo pedido iniciado. Dados originais são vazios.")
            }
        }

        itemsFragment.onItemsChanged = { updatedItems ->
            calculateTotal(updatedItems)
        }
    }

    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): androidx.fragment.app.Fragment {
                return when (position) {
                    0 -> headerFragment
                    1 -> itemsFragment
                    else -> throw IllegalArgumentException("Posição inválida no ViewPager")
                }
            }
        }
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Cabeçalho"
                1 -> "Itens"
                else -> ""
            }
        }.attach()
    }

    private fun setupListeners() {
        imageViewBack.setOnClickListener {
            handleBackPress()
        }

        imageViewSave.setOnClickListener {
            salvarPedido()
        }

        imageViewDelete.setOnClickListener {
            confirmarDelecao()
        }
    }

    private fun salvarPedido() {
        val cliente = headerFragment.selectedCliente
        // CORREÇÃO: Usar uma função de verificação para evitar NPE
        val status = headerFragment.getStatusNota()
        val dataEmissao = headerFragment.dataEmissao
        val dataEntregaPrevista = headerFragment.dataEntregaPrevista
        val condicaoPagamento = headerFragment.getCondicaoPagamento()

        var isValid = true
        if (cliente == null) {
            Toast.makeText(this, "Selecione o Cliente.", Toast.LENGTH_SHORT).show()
            isValid = false
            viewPager.currentItem = 0
        }
        if (status.isNullOrEmpty()) {
            Toast.makeText(this, "Selecione o Status da Nota.", Toast.LENGTH_SHORT).show()
            isValid = false
            viewPager.currentItem = 0
        }
        if (dataEmissao == null) {
            Toast.makeText(this, "Selecione a Data de Emissão.", Toast.LENGTH_SHORT).show()
            isValid = false
            viewPager.currentItem = 0
        }
        if (condicaoPagamento.isNullOrEmpty()) {
            Toast.makeText(this, "Selecione a Condição de Pagamento.", Toast.LENGTH_SHORT).show()
            isValid = false
            viewPager.currentItem = 0
        }
        if (itemsFragment.currentItems.isEmpty()) {
            Toast.makeText(this, "O pedido deve ter pelo menos um item.", Toast.LENGTH_SHORT).show()
            isValid = false
            viewPager.currentItem = 1
        }

        if (isValid) {
            val valorTotal = itemsFragment.currentItems.sumOf { it.quantidade * it.precoUnitario }

            lifecycleScope.launch {
                val novoOuEditadoPedido: Pedido
                if (pedidoEmEdicao != null) {
                    novoOuEditadoPedido = pedidoEmEdicao!!.copy(
                        clienteId = cliente!!.id!!,
                        dataCompra = dataEmissao!!,
                        dataEntregaPrevista = dataEntregaPrevista,
                        valorTotal = valorTotal,
                        situacao = status!!,
                        condicaoPagamento = condicaoPagamento ?: ""
                    )
                    val success = pedidoRepository.updatePedidoWithItems(novoOuEditadoPedido, itemsFragment.currentItems)
                    if (success) {
                        Toast.makeText(this@DetalhePedidoActivity, "Pedido #${novoOuEditadoPedido.codigoPedido} atualizado!", Toast.LENGTH_SHORT).show()
                        originalHeaderData = headerFragment.getHeaderData()
                        originalItems = itemsFragment.currentItems.toList()
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@DetalhePedidoActivity, "Erro ao atualizar pedido.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val newCodigoPedido = generateNewPedidoCode()
                    novoOuEditadoPedido = Pedido(
                        id = null,
                        codigoPedido = newCodigoPedido,
                        clienteId = cliente!!.id!!,
                        dataCompra = dataEmissao!!,
                        dataEntregaPrevista = dataEntregaPrevista,
                        valorTotal = valorTotal,
                        situacao = status!!,
                        condicaoPagamento = condicaoPagamento ?: ""
                    )

                    val newPedidoId = pedidoRepository.insertPedidoWithItems(novoOuEditadoPedido, itemsFragment.currentItems)

                    if (newPedidoId != null) {
                        Toast.makeText(this@DetalhePedidoActivity, "Pedido #$newCodigoPedido criado!", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@DetalhePedidoActivity, "Erro ao criar pedido.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun confirmarDelecao() {
        pedidoEmEdicao?.let { pedido ->
            AlertDialog.Builder(this)
                .setTitle("Excluir Pedido")
                .setMessage("Tem certeza de que deseja excluir o pedido #${pedido.codigoPedido}? Isso também excluirá todos os itens associados.")
                .setPositiveButton("Sim") { dialog, which ->
                    lifecycleScope.launch {
                        val success = pedido.id?.let { pedidoId -> pedidoRepository.deletePedido(pedidoId) } ?: false
                        if (success) {
                            Toast.makeText(this@DetalhePedidoActivity, "Pedido #${pedido.codigoPedido} excluído!", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this@DetalhePedidoActivity, "Erro ao excluir pedido.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Não", null)
                .show()
        }
    }

    private fun handleBackPress() {
        if (verificarModificacoes()) {
            AlertDialog.Builder(this)
                .setTitle("Sair sem Salvar?")
                .setMessage("Você tem alterações não salvas. Deseja salvá-las?")
                .setPositiveButton("Salvar") { dialog, which ->
                    salvarPedido()
                }
                .setNegativeButton("Não Salvar") { dialog, which ->
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                .setNeutralButton("Voltar") { dialog, which ->
                    dialog.dismiss()
                }
                .show()
        } else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onBackPressed() {
        handleBackPress()
    }

    private fun verificarModificacoes(): Boolean {
        // CORREÇÃO: Adicionar verificação de `isAdded` para garantir que o fragmento está no ciclo de vida correto
        if (!headerFragment.isAdded) {
            return false // Se o fragmento ainda não foi adicionado, não há modificações a verificar
        }

        val currentHeaderData = headerFragment.getHeaderData()
        val currentItems = itemsFragment.currentItems

        if (pedidoEmEdicao == null) {
            val isHeaderModified = currentHeaderData.clienteId != null ||
                    currentHeaderData.statusNota != "Pendente" ||
                    currentHeaderData.dataEmissao != headerFragment.dataEmissao
            val isItemsModified = currentItems.isNotEmpty()
            return isHeaderModified || isItemsModified
        }

        val originalItemsCopia = originalItems
        if (originalHeaderData == null || originalItemsCopia == null) {
            Log.d("DetalhePedidoActivity", "Dados originais não carregados. Retornando true para segurança.")
            return true
        }

        val headerModified = originalHeaderData != currentHeaderData
        val itemsModified = !originalItemsCopia.equals(currentItems)

        Log.d("DetalhePedidoActivity", "Verificando modificações: Header modificado: $headerModified, Itens modificados: $itemsModified")
        return headerModified || itemsModified
    }

    private suspend fun generateNewPedidoCode(): String {
        val allPedidos = pedidoRepository.getAllPedidosComCliente().first()
        var maxCodeNum = 0
        for (pedidoComCliente in allPedidos) {
            val codigo = pedidoComCliente.pedido.codigoPedido
            if (!codigo.isNullOrEmpty()) {
                val codeNum = codigo.toIntOrNull()
                if (codeNum != null && codeNum > maxCodeNum) {
                    maxCodeNum = codeNum
                }
            }
        }
        val nextNumber = maxCodeNum + 1
        return String.format("%04d", nextNumber)
    }

    private fun calculateTotal(items: List<ItemPedido> = itemsFragment.currentItems) {
        val total = items.sumOf { item ->
            item.quantidade * item.precoUnitario
        }
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        textViewTotalPedido.text = "Total do Pedido: ${currencyFormat.format(total)}"
    }
}