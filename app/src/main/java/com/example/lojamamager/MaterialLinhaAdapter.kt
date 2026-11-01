package com.example.lojamamager.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.R
import com.example.lojamamager.data.MaterialProduto
import java.text.NumberFormat
import java.util.Locale

class MaterialLinhaAdapter(
    private val materiais: List<MaterialProduto>,
    private val onLongClick: (Int) -> Unit // Para permitir remover o insumo se errar
) : RecyclerView.Adapter<MaterialLinhaAdapter.ViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNome: TextView = view.findViewById(R.id.txtNomeMaterial)
        val txtCusto: TextView = view.findViewById(R.id.txtCustoMaterial)
        val txtEstoque: TextView = view.findViewById(R.id.txtEstoqueMaterial) // Usaremos para a Qtde Usada
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_material_produto_linha, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = materiais[position]
        holder.txtNome.text = item.nomeMaterial
        holder.txtCusto.text = "Custo: ${currencyFormat.format(item.custoCalculado)}"

        // Aqui está o truque: formata o texto com a quantidade fracionada ou inteira
        holder.txtEstoque.text = "${item.quantidadeUsada} Usado(s)"

        holder.itemView.setOnLongClickListener {
            onLongClick(position)
            true
        }
    }

    override fun getItemCount() = materiais.size
}