package com.example.lojamamager.data

import com.example.lojamamager.data.ItemPedido
import com.example.lojamamager.data.Pedido

/**
 * Data class para combinar um Pedido com seus Itens de Pedido.
 *
 * Esta classe é usada principalmente na camada de UI/lógica (ex: ViewModel, Adapter)
 * para representar um pedido completo, incluindo seus itens.
 *
 * No contexto do Firebase Firestore, os relacionamentos não são tratados com anotações
 * como no Room. Em vez disso, o Pedido contém o ID do cliente, e os ItemPedido contêm
 * o ID do Pedido. Os repositórios são responsáveis por buscar e combinar esses dados.
 */
data class PedidoWithItems(
    // O pedido principal. Não há necessidade de @Embedded, pois não estamos mais usando Room.
    val pedido: Pedido,
    // A lista de itens associados a este pedido.
    // Não há necessidade de @Relation, pois não estamos mais usando Room.
    val itens: List<ItemPedido>
)