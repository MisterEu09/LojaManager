package com.example.lojamamager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.ItemPedido
import java.text.NumberFormat
import java.util.Locale

class PedidoItemAdapter(
    private var items: List<ItemPedido>,
    private val onEditClick: (ItemPedido) -> Unit,
    private val onDeleteClick: (ItemPedido) -> Unit
) : RecyclerView.Adapter<PedidoItemAdapter.PedidoItemViewHolder>() {

    class PedidoItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeProduto: TextView = itemView.findViewById(R.id.textViewItemNomeProduto)
        val codigoProduto: TextView = itemView.findViewById(R.id.textViewItemCodigoProduto)
        val quantidade: TextView = itemView.findViewById(R.id.textViewItemQuantidade)
        val precoUnitario: TextView = itemView.findViewById(R.id.textViewItemPrecoUnitario)
        val subtotal: TextView = itemView.findViewById(R.id.textViewItemSubtotal)
        val editIcon: ImageView = itemView.findViewById(R.id.imageViewEditItem)
        val deleteIcon: ImageView = itemView.findViewById(R.id.imageViewDeleteItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido_produto, parent, false)
        return PedidoItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: PedidoItemViewHolder, position: Int) {
        val item = items[position]
        holder.nomeProduto.text = item.nomeProduto
        holder.codigoProduto.text = "Cód: ${item.codigoProduto ?: "N/A"}"
        holder.quantidade.text = "Qtd: ${item.quantidade}"
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        holder.precoUnitario.text = "Preço Unit: ${currencyFormat.format(item.precoUnitario)}"
        holder.subtotal.text = "Subtotal: ${currencyFormat.format(item.quantidade * item.precoUnitario)}"

        // Definindo os listeners para os ícones de editar e deletar
        holder.editIcon.setOnClickListener { onEditClick(item) }
        holder.deleteIcon.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ItemPedido>) {
        items = newItems
        notifyDataSetChanged()
    }
}