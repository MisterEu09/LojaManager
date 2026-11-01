package com.example.lojamamager

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.MaterialInsumo
import java.text.NumberFormat
import java.util.Locale

class MaterialAdapter(
    private var listaExibida: List<MaterialInsumo>,
    private val onClick: (MaterialInsumo) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<MaterialAdapter.MaterialViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private var listaCompletaBackup: List<MaterialInsumo> = listaExibida

    // Rastreia os IDs dos itens marcados
    val itensSelecionados = mutableSetOf<String>()
    var modoSelecaoAtivo = false

    class MaterialViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMain: TextView = view.findViewById(android.R.id.text1)
        val txtSub: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return MaterialViewHolder(view)
    }

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        val material = listaExibida[position]
        holder.txtMain.text = material.nome
        holder.txtSub.text = "Estoque: ${material.quantidadeEstoque} ${material.unidadeMedida} | Custo: ${currencyFormat.format(material.precoCusto)}"

        // === AGORA: Usa o seu Selector XML Inteligente ===
        if (itensSelecionados.contains(material.id)) {
            holder.itemView.isActivated = true
            holder.itemView.setBackgroundResource(R.drawable.item_selecionado_background)
        } else {
            holder.itemView.isActivated = false
            holder.itemView.setBackgroundResource(R.drawable.item_selecionado_background)
        }

        holder.itemView.setOnClickListener {
            if (modoSelecaoAtivo) {
                toggleSelecao(material.id, position)
            } else {
                onClick(material)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!modoSelecaoAtivo) {
                modoSelecaoAtivo = true
            }
            toggleSelecao(material.id, position)
            true
        }
    }

    private fun toggleSelecao(id: String?, position: Int) {
        if (id == null) return
        if (itensSelecionados.contains(id)) {
            itensSelecionados.remove(id)
        } else {
            itensSelecionados.add(id)
        }
        notifyItemChanged(position)
        onSelectionChanged(itensSelecionados.size)
    }

    override fun getItemCount() = listaExibida.size

    fun getMateriaisSelecionados(): List<String> = itensSelecionados.toList()

    fun desativarModoSelecao() {
        modoSelecaoAtivo = false
        itensSelecionados.clear()
        notifyDataSetChanged()
    }

    fun updateData(novaLista: List<MaterialInsumo>) {
        this.listaExibida = novaLista
        this.listaCompletaBackup = novaLista
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        listaExibida = if (query.isEmpty()) {
            listaCompletaBackup
        } else {
            listaCompletaBackup.filter { it.nome.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }
}