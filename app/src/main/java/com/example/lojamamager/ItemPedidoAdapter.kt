
package com.example.lojamamager



import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.ItemPedido

import java.text.NumberFormat

import java.util.Locale
import kotlin.text.format
import kotlin.times


class ItemPedidoAdapter(

    private var itens: List<ItemPedido>,

    private val onItemClick: (ItemPedido) -> Unit

) : RecyclerView.Adapter<ItemPedidoAdapter.ItemPedidoViewHolder>() {



    class ItemPedidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val produtoNome: TextView = itemView.findViewById(R.id.textViewItemProdutoNome)

        val produtoCodigo: TextView = itemView.findViewById(R.id.textViewItemProdutoCodigo)

        val quantidade: TextView = itemView.findViewById(R.id.textViewItemQuantidade)

        val precoUnitario: TextView = itemView.findViewById(R.id.textViewItemPrecoUnitario)

        val subtotal: TextView = itemView.findViewById(R.id.textViewItemSubtotal)

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemPedidoViewHolder {

        val view = LayoutInflater.from(parent.context)

            .inflate(R.layout.item_item_pedido, parent, false)

        return ItemPedidoViewHolder(view)

    }



    override fun onBindViewHolder(holder: ItemPedidoViewHolder, position: Int) {

        val item = itens[position]

        holder.produtoNome.text = item.nomeProduto

        holder.produtoCodigo.text = "Código: ${item.codigoProduto ?: "N/A"}"

        holder.quantidade.text = "Quantidade: ${item.quantidade}"



        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        holder.precoUnitario.text = "Preço Unitário: ${currencyFormat.format(item.precoUnitario)}"

        holder.subtotal.text = "Subtotal: ${currencyFormat.format(item.quantidade * item.precoUnitario)}"



        holder.itemView.setOnClickListener {

            onItemClick(item)

        }

    }



    override fun getItemCount(): Int = itens.size



    fun updateItens(newItems: List<ItemPedido>) {

        itens = newItems // Atualiza a lista interna do adapter

        notifyDataSetChanged() // Notifica o RecyclerView para se redesenhar

    }

}