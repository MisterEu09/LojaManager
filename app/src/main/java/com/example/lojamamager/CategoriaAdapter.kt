package com.example.lojamamager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.Categoria

class CategoriaAdapter(
    private var categorias: List<Categoria>,
    private val onItemClick: (Categoria) -> Unit, // Para selecionar a categoria
    private val onMoreOptionsClick: (Categoria, View) -> Unit // Para o menu de opções
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {

    class CategoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeCategoria: TextView = itemView.findViewById(R.id.textViewNomeCategoria)
        val moreOptions: ImageView = itemView.findViewById(R.id.imageViewMoreOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria, parent, false)
        return CategoriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        val categoria = categorias[position]
        holder.nomeCategoria.text = categoria.nome

        holder.itemView.setOnClickListener {
            onItemClick(categoria)
        }

        holder.moreOptions.setOnClickListener {
            onMoreOptionsClick(categoria, it) // Passa a view do ícone para o PopUpMenu
        }
    }

    override fun getItemCount(): Int = categorias.size

    fun updateCategorias(newCategorias: List<Categoria>) {
        categorias = newCategorias
        notifyDataSetChanged()
    }
}