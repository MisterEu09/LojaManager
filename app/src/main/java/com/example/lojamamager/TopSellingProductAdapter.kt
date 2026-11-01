package com.example.lojamamager

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.parcelize.Parcelize
import java.text.NumberFormat
import java.util.Locale

@Parcelize
data class TopSellingProduct(
    val rank: Int,
    val productName: String,
    val quantitySold: Int,
    val totalValue: Double
) : Parcelable

// **Já está correto:** Este adaptador agora espera e gerencia List<TopSellingProduct>
class TopSellingProductAdapter(private var products: List<TopSellingProduct>) :
    RecyclerView.Adapter<TopSellingProductAdapter.TopSellingProductViewHolder>() {

    class TopSellingProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankTextView: TextView = itemView.findViewById(R.id.textViewRank)
        val productNameTextView: TextView = itemView.findViewById(R.id.textViewTopProductName)
        val quantitySoldTextView: TextView = itemView.findViewById(R.id.textViewTopProductQuantity)
        val totalValueTextView: TextView = itemView.findViewById(R.id.textViewTopProductValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopSellingProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_selling_product, parent, false)
        return TopSellingProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopSellingProductViewHolder, position: Int) {
        val product = products[position]
        holder.rankTextView.text = "${product.rank}."
        holder.productNameTextView.text = product.productName
        holder.quantitySoldTextView.text = "Qtd Vendida: ${product.quantitySold}"
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        holder.totalValueTextView.text = currencyFormat.format(product.totalValue)
    }

    override fun getItemCount(): Int = products.size

    // **Já está correto:** O método updateProducts também espera List<TopSellingProduct>
    fun updateProducts(newProducts: List<TopSellingProduct>) {
        products = newProducts
        notifyDataSetChanged()
    }
}