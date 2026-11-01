package com.example.lojamamager

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Importe o Glide
import com.example.lojamamager.data.Produto

// Certifique-se de adicionar a dependência do Glide no seu build.gradle (Module: app):
// implementation 'com.github.bumptech.glide:glide:4.12.0'
// annotationProcessor 'com.github.bumptech.glide:compiler:4.12.0'

class CatalogoProdutoAdapter(private var produtos: List<Produto>) :
    RecyclerView.Adapter<CatalogoProdutoAdapter.CatalogoProdutoViewHolder>() {

    // Interface para lidar com cliques nos itens
    var onItemClick: ((Produto) -> Unit)? = null

    class CatalogoProdutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewProduto: ImageView = itemView.findViewById(R.id.imageViewProdutoCatalogo)
        val nomeProduto: TextView = itemView.findViewById(R.id.textViewNomeProdutoCatalogo)
        val precoProduto: TextView = itemView.findViewById(R.id.textViewPrecoProdutoCatalogo)
        val categoriaProduto: TextView = itemView.findViewById(R.id.textViewCategoriaProdutoCatalogo)
        val marcaProduto: TextView = itemView.findViewById(R.id.textViewMarcaProdutoCatalogo) // Adicionado
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogoProdutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_catalogo_produto, parent, false)
        return CatalogoProdutoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CatalogoProdutoViewHolder, position: Int) {
        val produto = produtos[position]
        holder.nomeProduto.text = produto.nome
        holder.precoProduto.text = "R$ %.2f".format(produto.precoVenda)
        holder.categoriaProduto.text = "Categoria: ${produto.categoria}"

        // Exibe a marca se disponível
        if (!produto.marca.isNullOrBlank()) {
            holder.marcaProduto.text = "Marca: ${produto.marca}"
            holder.marcaProduto.visibility = View.VISIBLE
        } else {
            holder.marcaProduto.visibility = View.GONE
        }

        // Carregar imagem usando Glide
        if (!produto.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(produto.imageUrl)
                .placeholder(R.drawable.placeholder_image) // Imagem de placeholder
                .error(R.drawable.placeholder_image) // Imagem de erro
                .into(holder.imageViewProduto)
        } else {
            // Se não houver URL de imagem, mostra o placeholder
            holder.imageViewProduto.setImageResource(R.drawable.placeholder_image)
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(produto)
        }
    }

    override fun getItemCount(): Int = produtos.size

    fun updateProdutos(newProdutos: List<Produto>) {
        produtos = newProdutos
        notifyDataSetChanged()
    }
}