package com.example.lojamamager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.PedidoComCliente
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PedidoAdapter(
    private var pedidos: List<PedidoComCliente>,
    private val onItemClick: (PedidoComCliente) -> Unit,
    private val onMoreOptionsClick: (PedidoComCliente, View) -> Unit
) : RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    class PedidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewCodigoPedido: TextView = itemView.findViewById(R.id.textViewCodigoPedido)
        val textViewClienteNome: TextView = itemView.findViewById(R.id.textViewClienteNome)
        val textViewDataCompraPedido: TextView = itemView.findViewById(R.id.textViewDataCompraPedido)
        val textViewStatus: TextView = itemView.findViewById(R.id.textViewStatus)
        val textViewValorTotal: TextView = itemView.findViewById(R.id.textViewValorTotal)
        val textViewDataEntregaPrevista: TextView = itemView.findViewById(R.id.textViewDataEntregaPrevista)
        val imageViewMoreOptions: ImageView = itemView.findViewById(R.id.imageViewMoreOptionsPedido)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pedido, parent, false)
        return PedidoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedidoComCliente = pedidos[position]
        val pedido = pedidoComCliente.pedido
        val cliente = pedidoComCliente.cliente

        holder.textViewCodigoPedido.text = "Pedido: #${pedido.codigoPedido}"
        holder.textViewClienteNome.text = "Cliente: ${cliente?.nomeRazaoSocial ?: "Cliente Desconhecido"}"
        holder.textViewStatus.text = "Situação: ${pedido.situacao}"

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.textViewDataCompraPedido.text = "Data da Compra: ${dateFormat.format(Date(pedido.dataCompra))}"

        pedido.dataEntregaPrevista?.let {
            holder.textViewDataEntregaPrevista.text = "Entrega Prevista: ${dateFormat.format(Date(it))}"
            holder.textViewDataEntregaPrevista.visibility = View.VISIBLE
        } ?: run {
            holder.textViewDataEntregaPrevista.visibility = View.GONE
        }

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        holder.textViewValorTotal.text = "Valor: ${currencyFormat.format(pedido.valorTotal)}"

        // Mantidas as cores existentes (preservando o que você já tem)
        when (pedido.situacao.uppercase(Locale.getDefault())) {
            "PENDENTE" -> {
                holder.textViewStatus.setBackgroundResource(R.drawable.rounded_background_pending)
                holder.textViewStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            }
            "PEDIDO" -> { // Se "Pedido" é um status válido e tem seu próprio drawable
                holder.textViewStatus.setBackgroundResource(R.drawable.rounded_background_pedido)
                holder.textViewStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            }
            "ENTREGUE" -> {
                holder.textViewStatus.setBackgroundResource(R.drawable.rounded_background_entregue)
                holder.textViewStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            }
            "PAGO" -> {
                holder.textViewStatus.setBackgroundResource(R.drawable.rounded_background_pago)
                holder.textViewStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            }
            "TROCA" -> { // Se "Troca" é um status válido e tem seu próprio drawable
                holder.textViewStatus.setBackgroundResource(R.drawable.rounded_background_troca)
                holder.textViewStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            }
            else -> { // Para outros status ou fallback
                holder.textViewStatus.setBackgroundResource(R.drawable.rounded_background_default)
                holder.textViewStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.black))
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(pedidoComCliente)
        }

        holder.imageViewMoreOptions.setOnClickListener {
            onMoreOptionsClick(pedidoComCliente, it)
        }
    }

    override fun getItemCount(): Int = pedidos.size

    fun updatePedidos(newPedidos: List<PedidoComCliente>) {
        // A ordenação já virá da Activity, o Adapter só precisa atualizar a lista
        pedidos = newPedidos
        notifyDataSetChanged()
    }
}