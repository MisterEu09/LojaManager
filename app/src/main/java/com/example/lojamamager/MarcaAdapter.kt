package com.example.lojamamager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.Marca

class MarcaAdapter(
    private var marcas: List<Marca>,
    private val onItemClick: (Marca) -> Unit,
    private val onMoreOptionsClick: (Marca, View) -> Unit
) : RecyclerView.Adapter<MarcaAdapter.MarcaViewHolder>() {

    class MarcaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeMarca: TextView = itemView.findViewById(R.id.textViewNomeMarca)
        val moreOptions: ImageView = itemView.findViewById(R.id.imageViewMoreOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarcaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_marca, parent, false)
        return MarcaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MarcaViewHolder, position: Int) {
        val marca = marcas[position]
        holder.nomeMarca.text = marca.nome

        holder.itemView.setOnClickListener {
            onItemClick(marca)
        }

        holder.moreOptions.setOnClickListener {
            onMoreOptionsClick(marca, it)
        }
    }

    override fun getItemCount(): Int = marcas.size

    fun updateMarcas(newMarcas: List<Marca>) {
        marcas = newMarcas
        notifyDataSetChanged()
    }
}