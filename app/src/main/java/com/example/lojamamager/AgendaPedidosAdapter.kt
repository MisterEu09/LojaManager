package com.example.lojamamager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.PedidoComCliente
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Interface para lidar com cliques nos itens e no menu
interface AgendaPedidoClickListener {
fun onVerDetalhesClick(pedidoComCliente: PedidoComCliente)
fun onFinalizarComSucessoClick(pedidoComCliente: PedidoComCliente)
fun onFinalizarSemSucessoClick(pedidoComCliente: PedidoComCliente)
fun onCopiarEnderecoClick(endereco: String) // Método para copiar endereço
}

class AgendaPedidosAdapter(
private var pedidos: List<PedidoComCliente>,
private val clickListener: AgendaPedidoClickListener // Adiciona o listener
) : RecyclerView.Adapter<AgendaPedidosAdapter.AgendaPedidoViewHolder>() {

private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

// ViewHolder para cachear as referências das views de cada item
class AgendaPedidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
val textViewDeliveryDate: TextView = itemView.findViewById(R.id.textViewDeliveryDate)
val textViewClientName: TextView = itemView.findViewById(R.id.textViewClientName)
val textViewClientCode: TextView = itemView.findViewById(R.id.textViewClientCode)
val textViewSituacaoPedido: TextView = itemView.findViewById(R.id.textViewSituacaoPedido) // Novo TextView
val imageViewMenuOptions: ImageView = itemView.findViewById(R.id.imageViewMenuOptions)
val buttonFinalizarSemSucesso: Button = itemView.findViewById(R.id.buttonFinalizarSemSucesso)
val buttonFinalizarComSucesso: Button = itemView.findViewById(R.id.buttonFinalizarComSucesso)
}

override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendaPedidoViewHolder {
// Infla o layout do item da agenda
val view = LayoutInflater.from(parent.context)
.inflate(R.layout.item_agenda_pedido, parent, false)
return AgendaPedidoViewHolder(view)
}

override fun onBindViewHolder(holder: AgendaPedidoViewHolder, position: Int) {
val pedidoComCliente = pedidos[position]
val pedido = pedidoComCliente.pedido
val cliente = pedidoComCliente.cliente

// Lógica para determinar a situação de exibição e aplicar cores
val today = Calendar.getInstance().apply {
set(Calendar.HOUR_OF_DAY, 0)
set(Calendar.MINUTE, 0)
set(Calendar.SECOND, 0)
set(Calendar.MILLISECOND, 0)
}.timeInMillis

val deliveryDate = pedido.dataEntregaPrevista ?: 0L

val situacaoDisplay: String
val textColor: Int

// 1. Prioridade: Entregas Atrasadas (não entregues e data prevista < hoje)
if (deliveryDate > 0 && deliveryDate < today && pedido.situacao != "Entregue" && pedido.situacao != "Falha na Entrega") {
situacaoDisplay = "Entrega Atrasada"
textColor = ContextCompat.getColor(holder.itemView.context, R.color.colorEntregaAtrasada)
holder.buttonFinalizarComSucesso.visibility = View.VISIBLE
holder.buttonFinalizarSemSucesso.visibility = View.VISIBLE
} else {
// 2. Outras situações (Pendente, Pago, Entregue, Falha na Entrega)
when (pedido.situacao) {
"Pendente" -> {
situacaoDisplay = "A Confirmar Pagamento e Entrega"
textColor = ContextCompat.getColor(holder.itemView.context, R.color.colorEntregaPendente)
holder.buttonFinalizarComSucesso.visibility = View.VISIBLE
holder.buttonFinalizarSemSucesso.visibility = View.VISIBLE
}
"Pago" -> {
situacaoDisplay = "Pago para Entregar"
textColor = ContextCompat.getColor(holder.itemView.context, R.color.colorEntregaPaga)
holder.buttonFinalizarComSucesso.visibility = View.VISIBLE
holder.buttonFinalizarSemSucesso.visibility = View.VISIBLE
}
"Entregue" -> {
situacaoDisplay = "Entregue"
textColor = ContextCompat.getColor(holder.itemView.context, R.color.colorEntregaSucesso)
holder.buttonFinalizarComSucesso.visibility = View.GONE
holder.buttonFinalizarSemSucesso.visibility = View.GONE
}
"Falha na Entrega" -> {
situacaoDisplay = "Falha na Entrega"
textColor = ContextCompat.getColor(holder.itemView.context, R.color.colorEntregaFalha)
holder.buttonFinalizarComSucesso.visibility = View.GONE
holder.buttonFinalizarSemSucesso.visibility = View.GONE
}
else -> {
situacaoDisplay = "Situação Desconhecida"
textColor = ContextCompat.getColor(holder.itemView.context, R.color.colorTextPrimary)
holder.buttonFinalizarComSucesso.visibility = View.GONE
holder.buttonFinalizarSemSucesso.visibility = View.GONE
}
}
}

// Preenche o TextView da situação com o texto e cor determinados
holder.textViewSituacaoPedido.text = situacaoDisplay
holder.textViewSituacaoPedido.setTextColor(textColor)


// Preenchimento dos outros campos de texto
pedido.dataEntregaPrevista?.let {
holder.textViewDeliveryDate.text = "Entrega: ${dateFormat.format(it)}"
} ?: run {
holder.textViewDeliveryDate.text = "Entrega: Não Definida"
}

holder.textViewClientName.text = "Cliente: ${cliente?.nomeRazaoSocial ?: "Não Encontrado"}"
holder.textViewClientCode.text = "Cód. Cliente: ${cliente?.codigo ?: "N/A"}"


// --- Configuração do Menu de 3 Pontinhos ---
holder.imageViewMenuOptions.setOnClickListener { view ->
PopupMenu(view.context, view).apply {
menuInflater.inflate(R.menu.menu_agenda_item_options, menu) // Infla o menu definido no XML

// Define o listener para os cliques nos itens do menu
setOnMenuItemClickListener { menuItem ->
when (menuItem.itemId) {
R.id.action_view_order -> { // ID do XML do menu que você forneceu
clickListener.onVerDetalhesClick(pedidoComCliente)
true
}
R.id.action_copy_address -> { // Novo ID para copiar endereço
// Constrói a string completa do endereço usando os campos do Cliente
val enderecoCompleto = cliente?.let {
val endereco = "${it.endereco ?: ""}, ${it.numero ?: ""}".trim().trimEnd(',')
val complemento = if (!it.complemento.isNullOrBlank()) ", ${it.complemento}" else ""
val bairro = if (!it.bairro.isNullOrBlank()) ", ${it.bairro}" else ""
val cidadeEstado = if (!it.cidade.isNullOrBlank() && !it.uf.isNullOrBlank()) "${it.cidade} - ${it.uf}" else it.cidade ?: it.uf ?: ""
val cep = if (!it.cep.isNullOrBlank()) ", CEP: ${it.cep}" else ""

"$endereco$complemento$bairro, $cidadeEstado$cep".trim().trimStart(',').trim()
} ?: "Endereço não disponível"
clickListener.onCopiarEnderecoClick(enderecoCompleto)
true
}
else -> false
}
}
show() // Exibe o menu
}
}

// --- Configuração dos Botões de Finalizar ---
holder.buttonFinalizarComSucesso.setOnClickListener {
clickListener.onFinalizarComSucessoClick(pedidoComCliente)
}

holder.buttonFinalizarSemSucesso.setOnClickListener {
clickListener.onFinalizarSemSucessoClick(pedidoComCliente)
}
}

override fun getItemCount(): Int = pedidos.size

/**
* Atualiza a lista de pedidos exibida no RecyclerView e a ordena.
* Pedidos atrasados são priorizados no topo da lista.
* @param newPedidos A nova lista de PedidoComCliente.
*/
fun updatePedidos(newPedidos: List<PedidoComCliente>) {
val now = Calendar.getInstance().apply {
set(Calendar.HOUR_OF_DAY, 0)
set(Calendar.MINUTE, 0)
set(Calendar.SECOND, 0)
set(Calendar.MILLISECOND, 0)
}.timeInMillis

// Separa os pedidos em "atrasados" e "não atrasados"
val (atrasados, naoAtrasados) = newPedidos.partition {
val deliveryDate = it.pedido.dataEntregaPrevista ?: 0L
// Um pedido é atrasado se a data de entrega prevista passou e ele não foi entregue/falhou
deliveryDate > 0 && deliveryDate < now && it.pedido.situacao != "Entregue" && it.pedido.situacao != "Falha na Entrega"
}

// Ordena os atrasados: os mais antigos primeiro
val sortedAtrasados = atrasados.sortedBy { it.pedido.dataEntregaPrevista }
// Ordena os não atrasados: os mais próximos primeiro
val sortedNaoAtrasados = naoAtrasados.sortedBy { it.pedido.dataEntregaPrevista }

// Combina as listas: atrasados primeiro, depois os não atrasados
pedidos = sortedAtrasados + sortedNaoAtrasados
notifyDataSetChanged() // Notifica o RecyclerView para redesenhar a lista
}
}