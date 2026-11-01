package com.example.lojamamager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.Cliente

class ClienteAdapter(
    private var clientes: List<Cliente>,
    private val onItemClick: (Cliente) -> Unit
) : RecyclerView.Adapter<ClienteAdapter.ClienteViewHolder>() {

    class ClienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeRazaoSocial: TextView = itemView.findViewById(R.id.textViewNomeRazaoSocial)
        val cnpjCpf: TextView = itemView.findViewById(R.id.textViewCnpjCpfCliente)
        val telefone: TextView = itemView.findViewById(R.id.textViewTelefoneCliente)
        val endereco: TextView = itemView.findViewById(R.id.textViewEnderecoCliente)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente, parent, false)
        return ClienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        val cliente = clientes[position]
        holder.nomeRazaoSocial.text = cliente.nomeRazaoSocial
        holder.cnpjCpf.text = "CNPJ/CPF: ${cliente.cnpjCpf ?: "Não informado"}"
        holder.telefone.text = "Tel: ${cliente.telefone ?: "Não informado"}"

        val enderecoCompleto = if (!cliente.endereco.isNullOrEmpty() && !cliente.numero.isNullOrEmpty()) {
            "${cliente.endereco}, ${cliente.numero} - ${cliente.bairro ?: ""} - ${cliente.cidade ?: ""}/${cliente.uf ?: ""}"
        } else if (!cliente.endereco.isNullOrEmpty()) {
            "${cliente.endereco} - ${cliente.bairro ?: ""} - ${cliente.cidade ?: ""}/${cliente.uf ?: ""}"
        } else {
            "Endereço não informado"
        }
        holder.endereco.text = "Endereço: $enderecoCompleto"

        holder.itemView.setOnClickListener {
            onItemClick(cliente)
        }
    }

    override fun getItemCount(): Int = clientes.size

    fun updateClientes(newClientes: List<Cliente>) {
        clientes = newClientes
        notifyDataSetChanged()
    }
}