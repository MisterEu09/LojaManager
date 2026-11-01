package com.example.lojamamager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.Produto
import java.text.NumberFormat
import java.util.Locale

class ProdutoSelecaoAdapter(
private var produtos: List<Produto>,
private val onAddItem: (Produto) -> Unit,
private val onRemoveItem: (Produto) -> Unit,
private val onItemClick: (Produto) -> Unit,
val itemQuantities: MutableMap<String, Int>
) : RecyclerView.Adapter<ProdutoSelecaoAdapter.ProdutoSelecaoViewHolder>() {

private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

class ProdutoSelecaoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
val nome: TextView = itemView.findViewById(R.id.textViewProdutoNome)
val codigo: TextView = itemView.findViewById(R.id.textViewProdutoCodigo)
val valorVenda: TextView = itemView.findViewById(R.id.textViewProdutoValorVenda)
val buttonAdd: Button = itemView.findViewById(R.id.buttonAddItem)
val buttonRemove: Button = itemView.findViewById(R.id.buttonRemoveItem)
val itemQuantity: TextView = itemView.findViewById(R.id.textViewItemQuantity)
}

override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdutoSelecaoViewHolder {
val view = LayoutInflater.from(parent.context)
.inflate(R.layout.item_produto_pedido_selection, parent, false)
return ProdutoSelecaoViewHolder(view)
}

override fun onBindViewHolder(holder: ProdutoSelecaoViewHolder, position: Int) {
val produto = produtos[position]
holder.nome.text = produto.nome
holder.codigo.text = "Código: ${produto.codigo ?: "N/A"}"
holder.valorVenda.text = "Valor de Venda: ${currencyFormat.format(produto.precoVenda)}"

// **AJUSTE AQUI:** Usando produto.id!!
val quantity = produto.id?.let { itemQuantities[it] } ?: 0
holder.itemQuantity.text = quantity.toString()

holder.buttonAdd.setOnClickListener {
onAddItem(produto)
}
holder.buttonRemove.setOnClickListener {
onRemoveItem(produto)
}
holder.itemView.setOnClickListener {
onItemClick(produto)
}
}

override fun getItemCount(): Int = produtos.size

fun updateProdutos(newProdutos: List<Produto>) {
produtos = newProdutos
notifyDataSetChanged()
}

fun updateItemQuantity(productId: String, quantity: Int) {
itemQuantities[productId] = quantity
val index = produtos.indexOfFirst { it.id == productId }
if (index != -1) {
notifyItemChanged(index)
}
}
}