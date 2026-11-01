package com.example.lojamamager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log // Adicione o import para Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle // Adicione o import para Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle // Adicione o import para repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.auth.AuthManager // IMPORTANTE: Adicione o import para AuthManager
import com.example.lojamamager.data.Cliente
import com.example.lojamamager.data.Pedido
import com.example.lojamamager.data.PedidoComCliente
import com.example.lojamamager.firebase.repository.ClienteRepository
import com.example.lojamamager.firebase.repository.PedidoRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListaPedidosActivity : BaseActivity() {

    private lateinit var recyclerViewPedidos: RecyclerView
    private lateinit var pedidoAdapter: PedidoAdapter
    private lateinit var searchViewPedidos: SearchView
    private lateinit var imageViewAddPedido: ImageView
    private lateinit var imageViewFilterPedidos: ImageView
    private lateinit var textViewTotalPedidos: TextView
    private lateinit var imageViewBack: ImageView
    private lateinit var imageViewSortPedidos: ImageView

    private lateinit var pedidoRepository: PedidoRepository
    private lateinit var clienteRepository: ClienteRepository
    private lateinit var authManager: AuthManager // Adicionado: Instância do AuthManager

    private var currentSearchQuery: String? = null
    private var selectedStatusFilter: String? = null
    private var selectedClientFilter: Cliente? = null

    private var currentSortMode: SortMode = SortMode.CODIGO_PEDIDO_ASC

    private val REQUEST_DETALHE_PEDIDO = 1001
    private val REQUEST_SELECT_CLIENT = 1002

    private var filterDialog: AlertDialog? = null
    private var dialogTextViewFilterClient: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_pedidos)

        recyclerViewPedidos = findViewById(R.id.recyclerViewPedidos)
        searchViewPedidos = findViewById(R.id.searchViewPedidos)
        imageViewAddPedido = findViewById(R.id.fabAddPedido)
        imageViewFilterPedidos = findViewById(R.id.imageViewFilterPedidos)
        textViewTotalPedidos = findViewById(R.id.textViewPedidosCount)
        imageViewBack = findViewById(R.id.imageViewBackPedidos)
        imageViewSortPedidos = findViewById(R.id.imageViewSortPedidos)

        // Inicialize AuthManager primeiro, pois ele é crucial para o PedidoRepository
        authManager = AuthManager(applicationContext)
        pedidoRepository = PedidoRepository(applicationContext)
        clienteRepository = ClienteRepository(applicationContext)

        setupRecyclerView()
        setupListeners()
        observeAuthStatusAndLoadData() // Chamada para a nova função que observa o empresaId
    }

    override fun onResume() {
        super.onResume()
        // Removidas as chamadas diretas aqui. Elas serão feitas via observeAuthStatusAndLoadData()
        // quando o empresaId estiver pronto.
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DETALHE_PEDIDO && resultCode == Activity.RESULT_OK) {
            // Após a edição/criação de um pedido, podemos forçar uma re-observação
            // se o empresaId já estiver carregado.
            // A observação principal já é reativa, mas isso garante um refresh visual imediato.
            if (authManager.currentEmpresaId.value != null) {
                observePedidos()
                observePedidosCount()
            }
        } else if (requestCode == REQUEST_SELECT_CLIENT && resultCode == Activity.RESULT_OK) {
            selectedClientFilter = data?.getParcelableExtra("selected_cliente") as? Cliente
            dialogTextViewFilterClient?.text = selectedClientFilter?.nomeRazaoSocial ?: "Selecionar Cliente"
            // Re-observa os pedidos com o novo filtro de cliente, se o empresaId já estiver carregado
            if (authManager.currentEmpresaId.value != null) {
                observePedidos()
                observePedidosCount()
            }
        }
    }

    private fun setupRecyclerView() {
        pedidoAdapter = PedidoAdapter(
            emptyList(),
            onItemClick = { pedidoComCliente ->
                val intent = Intent(this, DetalhePedidoActivity::class.java)
                intent.putExtra("pedido", pedidoComCliente.pedido)
                startActivityForResult(intent, REQUEST_DETALHE_PEDIDO)
            },
            onMoreOptionsClick = { pedidoComCliente, anchorView ->
                showPedidoOptionsMenu(pedidoComCliente, anchorView)
            }
        )
        recyclerViewPedidos.layoutManager = LinearLayoutManager(this)
        recyclerViewPedidos.adapter = pedidoAdapter
    }

    private fun setupListeners() {
        imageViewAddPedido.setOnClickListener {
            val intent = Intent(this, DetalhePedidoActivity::class.java)
            startActivityForResult(intent, REQUEST_DETALHE_PEDIDO)
        }

        imageViewBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        searchViewPedidos.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query.orEmpty().ifEmpty { null }
                observePedidos()
                observePedidosCount()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText.orEmpty().ifEmpty { null }
                observePedidos() // Atualiza a busca em tempo real
                observePedidosCount()
                return true
            }
        })

        imageViewFilterPedidos.setOnClickListener {
            showFilterDialogPedidos()
        }

        imageViewSortPedidos.setOnClickListener {
            showSortOptionsMenu(it)
        }
    }

    /**
     * Observa o status de autenticação (especificamente o `empresaId`) do AuthManager.
     * Somente quando o `empresaId` estiver disponível (não nulo), as funções
     * `observePedidos()` e `observePedidosCount()` serão chamadas.
     * Isso resolve o problema de "Empresa ID não encontrado" ao garantir que
     * as operações do repositório só iniciem após a autenticação ser completa.
     */
    private fun observeAuthStatusAndLoadData() {
        lifecycleScope.launch {
            // Usa repeatOnLifecycle para coletar o flow apenas quando a Activity está STARTED
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authManager.currentEmpresaId.collectLatest { empresaId ->
                    if (empresaId != null) {
                        Log.d("ListaPedidosActivity", "Empresa ID disponível: $empresaId. Carregando dados...")
                        // Agora que temos o empresaId, podemos observar os pedidos com segurança
                        observePedidos()
                        observePedidosCount()
                    } else {
                        Log.d("ListaPedidosActivity", "Empresa ID não disponível ou usuário deslogado. Aguardando...")
                        // Limpa a lista e os contadores enquanto o empresaId não está disponível
                        pedidoAdapter.updatePedidos(emptyList())
                        textViewTotalPedidos.text = "Total de Pedidos: 0"
                    }
                }
            }
        }
    }

    private fun observePedidos() {
        lifecycleScope.launch {
            // Garante que o fluxo de coleta continue apenas enquanto a Activity estiver STARTED
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pedidoRepository.searchPedidos(
                    query = currentSearchQuery,
                    status = selectedStatusFilter,
                    clienteId = selectedClientFilter?.id
                ).collectLatest { pedidosComCliente ->
                    val sortedPedidos = when (currentSortMode) {
                        SortMode.CODIGO_PEDIDO_ASC -> pedidosComCliente.sortedBy { it.pedido.codigoPedido }
                        SortMode.DATA_ENTREGA_PROXIMA -> pedidosComCliente.sortedWith(compareBy { it.pedido.dataEntregaPrevista ?: Long.MAX_VALUE })
                        // Adicione outras lógicas de ordenação aqui, se necessário, usando os outros modos de ordenação
                        // que você tem no seu enum SortMode. Por exemplo:
                        // SortMode.CODIGO_PEDIDO_DESC -> pedidosComCliente.sortedByDescending { it.pedido.codigoPedido }
                        // SortMode.CLIENTE_ASC -> pedidosComCliente.sortedBy { it.cliente.nomeRazaoSocial }
                        // etc.
                    }
                    pedidoAdapter.updatePedidos(sortedPedidos)
                }
            }
        }
    }

    private fun observePedidosCount() {
        lifecycleScope.launch {
            // Garante que o fluxo de coleta continue apenas enquanto a Activity estiver STARTED
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pedidoRepository.searchPedidos(
                    query = currentSearchQuery,
                    status = selectedStatusFilter,
                    clienteId = selectedClientFilter?.id
                ).collectLatest { pedidos ->
                    textViewTotalPedidos.text = "Total de Pedidos: ${pedidos.size}"
                }
            }
        }
    }

    private fun showFilterDialogPedidos() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filter_pedidos, null)
        dialogTextViewFilterClient = dialogView.findViewById(R.id.textViewFilterClient)
        val spinnerStatus: Spinner = dialogView.findViewById(R.id.spinnerFilterStatus)
        val buttonClearFilters: Button = dialogView.findViewById(R.id.buttonClearFiltersPedidos)

        dialogTextViewFilterClient?.text = selectedClientFilter?.nomeRazaoSocial ?: "Selecionar Cliente"

        dialogTextViewFilterClient?.setOnClickListener {
            val intent = Intent(this, ListaClientesActivity::class.java)
            intent.putExtra("is_selection_mode", true)
            startActivityForResult(intent, REQUEST_SELECT_CLIENT)
        }

        val statusOptions = listOf("Todos", "Pendente", "Confirmado", "Entregue", "Pago", "Cancelado", "Falha na Entrega")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = statusAdapter

        selectedStatusFilter?.let { status ->
            val index = statusOptions.indexOf(status)
            if (index != -1) spinnerStatus.setSelection(index)
        } ?: spinnerStatus.setSelection(0)

        filterDialog = AlertDialog.Builder(this)
            .setTitle("Filtrar Pedidos")
            .setView(dialogView)
            .setPositiveButton("Aplicar") { dialog, _ ->
                selectedStatusFilter = if (spinnerStatus.selectedItemPosition == 0) null else spinnerStatus.selectedItem.toString()
                observePedidos()
                observePedidosCount()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        buttonClearFilters.setOnClickListener {
            selectedStatusFilter = null
            selectedClientFilter = null
            dialogTextViewFilterClient?.text = "Selecionar Cliente"
            spinnerStatus.setSelection(0)
            Toast.makeText(this, "Filtros de pedidos limpos!", Toast.LENGTH_SHORT).show()

            observePedidos()
            observePedidosCount()

            filterDialog?.dismiss()
        }
        filterDialog?.show()
    }

    private fun showSortOptionsMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        // Certifique-se de que os ordinals correspondem aos valores do seu enum SortMode
        popup.menu.add(0, SortMode.CODIGO_PEDIDO_ASC.ordinal, 0, "Código do Pedido (Crescente)")
        popup.menu.add(0, SortMode.DATA_ENTREGA_PROXIMA.ordinal, 1, "Data de Entrega (Mais Próxima)")
        // Adicione outras opções de ordenação aqui, se existirem no seu enum SortMode
        // popup.menu.add(0, SortMode.CODIGO_PEDIDO_DESC.ordinal, 2, "Código do Pedido (Decrescente)")
        // popup.menu.add(0, SortMode.CLIENTE_ASC.ordinal, 3, "Nome do Cliente (Crescente)")

        popup.menu.findItem(currentSortMode.ordinal)?.isChecked = true
        popup.menu.setGroupCheckable(0, true, true)

        popup.setOnMenuItemClickListener { menuItem ->
            currentSortMode = when (menuItem.itemId) {
                SortMode.CODIGO_PEDIDO_ASC.ordinal -> SortMode.CODIGO_PEDIDO_ASC
                SortMode.DATA_ENTREGA_PROXIMA.ordinal -> SortMode.DATA_ENTREGA_PROXIMA
                // Adicione outros cases para os seus modos de ordenação
                else -> SortMode.CODIGO_PEDIDO_ASC // Default
            }
            menuItem.isChecked = true
            observePedidos() // Re-observa os pedidos com a nova ordenação
            true
        }
        popup.show()
    }

    private fun showPedidoOptionsMenu(pedidoComCliente: PedidoComCliente, anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        // Certifique-se de que 'menu_pedido_item_options' existe em res/menu/
        popup.menuInflater.inflate(R.menu.menu_pedido_item_options, popup.menu)

        val checkDeliveryItem = popup.menu.findItem(R.id.action_check_delivery_date)
        checkDeliveryItem?.isVisible = pedidoComCliente.pedido.dataEntregaPrevista != null

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_view_client_details -> {
                    val cliente = pedidoComCliente.cliente
                    if (cliente != null) {
                        val intent = Intent(this@ListaPedidosActivity, DetalheClienteActivity::class.java)
                        intent.putExtra("cliente", cliente)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@ListaPedidosActivity, "Cliente não encontrado.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.action_check_delivery_date -> {
                    if (pedidoComCliente.pedido.dataEntregaPrevista != null) {
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val data = Date(pedidoComCliente.pedido.dataEntregaPrevista!!)
                        AlertDialog.Builder(this)
                            .setTitle("Data de Entrega Prevista")
                            .setMessage("O pedido ${pedidoComCliente.pedido.codigoPedido} tem entrega prevista para: ${dateFormat.format(data)}")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        Toast.makeText(this, "Não há data de entrega prevista para este pedido.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.action_edit_pedido -> {
                    val intent = Intent(this, DetalhePedidoActivity::class.java)
                    intent.putExtra("pedido", pedidoComCliente.pedido)
                    startActivityForResult(intent, REQUEST_DETALHE_PEDIDO)
                    true
                }
                R.id.action_delete_pedido -> {
                    confirmDeletePedido(pedidoComCliente.pedido)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun confirmDeletePedido(pedido: Pedido) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Pedido")
            .setMessage("Tem certeza de que deseja excluir o pedido #${pedido.codigoPedido}? Isso também excluirá todos os itens associados.")
            .setPositiveButton("Sim") { dialog, which ->
                lifecycleScope.launch {
                    val success = pedidoRepository.deletePedido(pedido.id!!)
                    if (success) {
                        Toast.makeText(this@ListaPedidosActivity, "Pedido #${pedido.codigoPedido} excluído!", Toast.LENGTH_SHORT).show()
                        // A observação já deve atualizar a lista automaticamente, mas um pequeno delay pode ser útil
                        // para a UI se atualizar.
                    } else {
                        Toast.makeText(this@ListaPedidosActivity, "Erro ao excluir pedido.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    // Certifique-se de que esta enum contém todos os modos de ordenação que você planeja usar
    enum class SortMode {
        CODIGO_PEDIDO_ASC,
        DATA_ENTREGA_PROXIMA
        // CODIGO_PEDIDO_DESC, CLIENTE_ASC, DATA_COMPRA_ASC, DATA_COMPRA_DESC, VALOR_TOTAL_ASC, VALOR_TOTAL_DESC
        // Se você quiser implementar mais, adicione-os aqui e na lógica de ordenação em observePedidos()
    }
}