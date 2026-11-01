package com.example.lojamamager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.Fornecedor

class FornecedorAdapter(
    private var fornecedores: List<Fornecedor>,
    private val onItemClick: (Fornecedor) -> Unit // Lambda para lidar com cliques
) : RecyclerView.Adapter<FornecedorAdapter.FornecedorViewHolder>() {

    class FornecedorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val razaoSocial: TextView = itemView.findViewById(R.id.textViewRazaoSocial)
        val cnpjCpf: TextView = itemView.findViewById(R.id.textViewCnpjCpf)
        val telefone: TextView = itemView.findViewById(R.id.textViewTelefoneFornecedor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FornecedorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fornecedor, parent, false)
        return FornecedorViewHolder(view)
    }

    override fun onBindViewHolder(holder: FornecedorViewHolder, position: Int) {
        val fornecedor = fornecedores[position]
        holder.razaoSocial.text = fornecedor.razaoSocial
        holder.cnpjCpf.text = "CNPJ/CPF: ${fornecedor.cnpjCpf ?: "Não informado"}"
        holder.telefone.text = "Tel: ${fornecedor.telefone ?: "Não informado"}"

        holder.itemView.setOnClickListener {
            onItemClick(fornecedor) // Chama a lambda quando um item é clicado
        }
    }

    override fun getItemCount(): Int = fornecedores.size

    fun updateFornecedores(newFornecedores: List<Fornecedor>) {
        fornecedores = newFornecedores
        notifyDataSetChanged()
    }
}