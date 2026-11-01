package com.example.lojamamager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.ProductAnalysisItem



class ProductAnalysisAdapter(
    private var analysisItems: List<ProductAnalysisItem>,
    private val clickListener: EstoqueActivity // Adicione o click listener
) : RecyclerView.Adapter<ProductAnalysisAdapter.ProductAnalysisViewHolder>() {

    class ProductAnalysisViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.imageViewAnalysisIcon)
        val productNameTextView: TextView = itemView.findViewById(R.id.textViewAnalysisProductName)
        val productDetailsTextView: TextView = itemView.findViewById(R.id.textViewAnalysisProductDetails)
        val suggestionTextView: TextView = itemView.findViewById(R.id.textViewAnalysisSuggestion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductAnalysisViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_analysis, parent, false)
        return ProductAnalysisViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductAnalysisViewHolder, position: Int) {
        val item = analysisItems[position]

        holder.productNameTextView.text = item.productName
        holder.productDetailsTextView.text = "Vendas: ${item.quantitySold}"
        holder.suggestionTextView.text = item.suggestion

        // Lógica para mudar a cor do texto e do ícone de acordo com a sugestão
        when (item.suggestion) {
            "REVER/TROCAR" -> {
                holder.suggestionTextView.setTextColor(holder.itemView.context.resources.getColor(android.R.color.holo_red_dark, null))
                holder.iconImageView.setImageResource(R.drawable.ic_warning) // Assumindo que este ícone existe
            }
            "MANTER" -> {
                holder.suggestionTextView.setTextColor(holder.itemView.context.resources.getColor(android.R.color.holo_green_dark, null))
                holder.iconImageView.setImageResource(R.drawable.ic_check) // Assumindo que este ícone existe
            }
        }

        // NOVO CÓDIGO: Adiciona o clique no `suggestionTextView`
        holder.suggestionTextView.setOnClickListener {
            if (item.suggestion == "REVER/TROCAR") {
                clickListener.onProductAnalysisClick(item.productId)
            }
        }
    }

    override fun getItemCount(): Int = analysisItems.size

    fun updateAnalysisItems(newItems: List<ProductAnalysisItem>) {
        analysisItems = newItems
        notifyDataSetChanged()
    }
}