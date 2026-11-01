package com.example.lojamamager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.Produto
import java.text.NumberFormat
import java.util.Locale

// Defina esta interface para que a Activity possa reagir aos cliques nos produtos
interface EstoqueProdutoClickListener {
    fun onProdutoClick(produto: Produto)
}

class EstoqueProdutoAdapter(
    private var produtos: List<Produto>,
    private val clickListener: EstoqueProdutoClickListener
) : RecyclerView.Adapter<EstoqueProdutoAdapter.EstoqueProdutoViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    class EstoqueProdutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewNomeProduto: TextView = itemView.findViewById(R.id.textViewNomeProduto)
        val textViewPrecoProduto: TextView = itemView.findViewById(R.id.textViewPrecoProduto)
        val textViewCategoriaProduto: TextView = itemView.findViewById(R.id.textViewCategoriaProduto)
        val textViewQuantidadeEstoque: TextView = itemView.findViewById(R.id.textViewQuantidadeEstoque)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EstoqueProdutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produto, parent, false) // Reutilizando o layout existente
        return EstoqueProdutoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EstoqueProdutoViewHolder, position: Int) {
        val produto = produtos[position]
        holder.textViewNomeProduto.text = produto.nome
        holder.textViewPrecoProduto.text = "Preço: ${currencyFormat.format(produto.precoVenda)}"
        holder.textViewCategoriaProduto.text = "Categoria: ${produto.categoria}"
        holder.textViewQuantidadeEstoque.text = "Estoque: ${produto.quantidadeEstoque} unidades"

        if (produto.quantidadeEstoque <= 5) {
            holder.textViewQuantidadeEstoque.setTextColor(holder.itemView.context.resources.getColor(android.R.color.holo_red_dark, null))
        } else {
            holder.textViewQuantidadeEstoque.setTextColor(holder.itemView.context.resources.getColor(R.color.md_theme_onPrimaryContainer, null))
        }

        holder.itemView.setOnClickListener {
            clickListener.onProdutoClick(produto)
        }
    }

    override fun getItemCount(): Int = produtos.size

    fun updateProdutos(newProducts: List<Produto>) {
        produtos = newProducts
        notifyDataSetChanged()
    }
}