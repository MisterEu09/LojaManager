package com.example.lojamamager

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.DetalheItemPedidoActivity
import com.example.lojamamager.data.ItemPedido
import com.example.lojamamager.PedidoItemAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PedidoItemsFragment : Fragment() {

    private lateinit var recyclerViewPedidoItems: RecyclerView
    private lateinit var fabAddPedidoItem: FloatingActionButton
    private lateinit var _pedidoItemAdapter: PedidoItemAdapter

    // Getter para o adapter, garantindo que ele esteja inicializado
    val pedidoItemAdapter: PedidoItemAdapter
        get() {
            if (!this::_pedidoItemAdapter.isInitialized) {
                throw IllegalStateException("PedidoItemAdapter não foi inicializado. Certifique-se de que onViewCreated foi chamado antes de tentar acessar o adapter.")
            }
            return _pedidoItemAdapter
        }

    // Lista mutável de itens do pedido que este fragmento gerencia
    var currentItems: MutableList<ItemPedido> = mutableListOf()
        private set // Torna o setter privado para controlar as atualizações

    // Callback para notificar a Activity pai sobre mudanças nos itens
    var onItemsChanged: ((List<ItemPedido>) -> Unit)? = null

    // A anotação @RequiresApi(Build.VERSION_CODES.TIRAMISU) pode ser removida daqui também
    // ou mantida se houver outras dependências de API 33+ que você não está verificando explicitamente.
    // Para a linha 49, a verificação interna já resolve.
    private val selectItemsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Ao retornar da DetalheItemPedidoActivity, recebemos a lista atualizada de itens.
                // --- INÍCIO DA CORREÇÃO NA LINHA 49 ---
                val updatedItems = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableArrayListExtra("selected_pedido_items", ItemPedido::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableArrayListExtra("selected_pedido_items")
                }?.filterIsInstance<ItemPedido>()
                // --- FIM DA CORREÇÃO NA LINHA 49 ---

                updatedItems?.let { newItems ->
                    // Limpa a lista atual e adiciona os novos itens.
                    // Isso garante que 'currentItems' sempre reflita o estado mais recente.
                    currentItems.clear()
                    currentItems.addAll(newItems)

                    // Notifica o adapter e a Activity pai sobre a mudança completa na lista.
                    // Passamos uma cópia para garantir imutabilidade para o adapter.
                    pedidoItemAdapter.updateItems(currentItems.toList())
                    onItemsChanged?.invoke(currentItems.toList()) // Notifica a Activity pai com a lista atual
                }
            }
        }


    // Removendo @RequiresApi daqui, pois a verificação interna resolve o problema de compatibilidade.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pedido_items, container, false)
    }

    // Removendo @RequiresApi daqui, pois a verificação interna resolve o problema de compatibilidade.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewPedidoItems = view.findViewById(R.id.recyclerViewPedidoItems)
        fabAddPedidoItem = view.findViewById(R.id.fabAddPedidoItem)

        _pedidoItemAdapter = PedidoItemAdapter(
            currentItems.toList(), // Passa uma cópia inicial para o adapter
            onEditClick = { itemPedido ->
                val intent = Intent(requireContext(), DetalheItemPedidoActivity::class.java).apply {
                    putExtra("item_to_edit", itemPedido)
                    // Importante: Passar a lista atual completa para a DetalheItemPedidoActivity
                    // para que ela possa fazer a gestão de adição/edição de forma coerente,
                    // especialmente para lidar com múltiplos itens do mesmo produto.
                    putParcelableArrayListExtra("existing_items", ArrayList(currentItems))
                }
                selectItemsLauncher.launch(intent)
            },
            onDeleteClick = { itemPedido ->
                // Remove o item da lista mutável
                currentItems.remove(itemPedido)
                // Atualiza o adapter com a lista modificada (passa uma cópia)
                pedidoItemAdapter.updateItems(currentItems.toList())
                // Notifica a Activity pai sobre a mudança
                onItemsChanged?.invoke(currentItems.toList())
                Toast.makeText(
                    requireContext(),
                    "${itemPedido.nomeProduto} removido.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        recyclerViewPedidoItems.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewPedidoItems.adapter = _pedidoItemAdapter

        fabAddPedidoItem.setOnClickListener {
            val intent = Intent(activity, DetalheItemPedidoActivity::class.java).apply {
                // Ao adicionar um novo item, passe a lista atual de itens existentes
                // para que a DetalheItemPedidoActivity possa considerar esses itens (ex: para fusão)
                putParcelableArrayListExtra("existing_items", ArrayList(currentItems))
            }
            selectItemsLauncher.launch(intent)
        }
    }

    // Esta função é chamada pela DetalhePedidoActivity para preencher os itens inicialmente
    // ou quando o pedido é carregado/recarregado.
    fun updateItems(newItems: List<ItemPedido>) {
        currentItems.clear()
        currentItems.addAll(newItems)

        if (this::_pedidoItemAdapter.isInitialized) {
            pedidoItemAdapter.updateItems(currentItems.toList())
        }
        // Sempre notificar a Activity pai para que ela recalcule o total
        onItemsChanged?.invoke(currentItems.toList())
    }
}