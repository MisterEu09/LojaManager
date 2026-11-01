package com.example.lojamamager

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.Cliente
import com.example.lojamamager.data.ItemPedido
import com.example.lojamamager.data.Pedido
import com.example.lojamamager.data.PedidoComCliente
import com.example.lojamamager.data.PedidoWithItems
import com.example.lojamamager.data.TopSellingProductData
import com.example.lojamamager.firebase.repository.ClienteRepository
import com.example.lojamamager.firebase.repository.PedidoRepository
import com.example.lojamamager.firebase.repository.ProdutoRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentId
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

class AgendaActivity : BaseActivity(), AgendaPedidoClickListener {

private lateinit var imageViewBack: ImageView
private lateinit var textViewToolbarTitle: TextView
private lateinit var textViewEntregasAgendadasCount: TextView
private lateinit var spinnerPeriodSelection: Spinner
private lateinit var imageViewPreviousPeriod: ImageView
private lateinit var textViewCurrentPeriod: TextView
private lateinit var imageViewNextPeriod: ImageView
private lateinit var recyclerViewAgendaPedidos: RecyclerView
private lateinit var fabAddEntrega: FloatingActionButton

private lateinit var pedidoRepository: PedidoRepository
private lateinit var agendaPedidosAdapter: AgendaPedidosAdapter

// Usar TimeZone.getDefault() para garantir consistência com a hora local do dispositivo
private val calendar = Calendar.getInstance(TimeZone.getDefault(), Locale("pt", "BR"))
private val dateFormatDisplay = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
private val dateFormatDailyQuery = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

private var currentPeriodMode: String = "Diário"

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContentView(R.layout.activity_agenda)

imageViewBack = findViewById(R.id.imageViewBackAgenda)
textViewToolbarTitle = findViewById(R.id.textViewToolbarTitleAgenda)
textViewEntregasAgendadasCount = findViewById(R.id.textViewPedidosPendentesCount)
spinnerPeriodSelection = findViewById(R.id.spinnerPeriodSelection)
imageViewPreviousPeriod = findViewById(R.id.imageViewPreviousPeriod)
textViewCurrentPeriod = findViewById(R.id.textViewCurrentPeriod)
imageViewNextPeriod = findViewById(R.id.imageViewNextPeriod)
recyclerViewAgendaPedidos = findViewById(R.id.recyclerViewAgendaPedidos)
fabAddEntrega = findViewById(R.id.fabAddEntrega)

pedidoRepository = PedidoRepository(applicationContext)
agendaPedidosAdapter = AgendaPedidosAdapter(emptyList(), this)
recyclerViewAgendaPedidos.layoutManager = LinearLayoutManager(this)
recyclerViewAgendaPedidos.adapter = agendaPedidosAdapter

setupListeners()
setupPeriodSpinner()

// Garante que o período inicial seja sempre o "Diário" e a data atual do dispositivo
// ao iniciar a Activity.
spinnerPeriodSelection.setSelection(0) // Seleciona "Diário" por padrão
resetCalendarToCurrentPeriod() // Reseta o calendário para o dia de hoje
updatePeriodDisplay()
loadPedidosAgenda()
}

override fun onResume() {
super.onResume()
// Após retornar à Activity (ex: de Detalhes ou Adicionar),
// garante que os dados são recarregados para o período atualmente selecionado.
loadPedidosAgenda()
}

/**
* Configura os listeners para os elementos de UI (botões, imagem de voltar).
*/
private fun setupListeners() {
imageViewBack.setOnClickListener {
finish() // Volta para a tela anterior
}

imageViewPreviousPeriod.setOnClickListener {
navigatePeriod(-1) // Navega para o período anterior
}

imageViewNextPeriod.setOnClickListener {
navigatePeriod(1) // Navega para o próximo período
}

fabAddEntrega.setOnClickListener {
// Inicia a Activity para adicionar um novo agendamento de entrega
val intent = Intent(this, AddAgendamentoActivity::class.java)
startActivity(intent)
}
}

/**
* Configura o Spinner para seleção do modo de visualização do período (Diário, Semanal, Mensal).
*/
private fun setupPeriodSpinner() {
val adapter = ArrayAdapter.createFromResource(
this,
R.array.period_options, // Array de strings definido no strings.xml
android.R.layout.simple_spinner_item
)
adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
spinnerPeriodSelection.adapter = adapter

spinnerPeriodSelection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
currentPeriodMode = parent?.getItemAtPosition(position).toString()
// **MUDANÇA CRUCIAL AQUI:**
// Ao mudar o modo, resetamos o calendário para o INÍCIO do período ATUAL (hoje, esta semana, este mês).
resetCalendarToCurrentPeriod()
updatePeriodDisplay() // Atualiza o texto do período na tela
loadPedidosAgenda() // Recarrega os pedidos com base no novo período
}
override fun onNothingSelected(parent: AdapterView<*>?) {
// Não faz nada quando nada é selecionado
}
}
}

/**
* Reseta o objeto Calendar para o início do período atual (hoje, esta semana ou este mês)
* baseado no `currentPeriodMode`.
*/
private fun resetCalendarToCurrentPeriod() {
calendar.timeInMillis = System.currentTimeMillis() // Começa sempre do "agora"
calendar.set(Calendar.HOUR_OF_DAY, 0)
calendar.set(Calendar.MINUTE, 0)
calendar.set(Calendar.SECOND, 0)
calendar.set(Calendar.MILLISECOND, 0)

when (currentPeriodMode) {
"Semanal" -> {
// Seta para o primeiro dia da semana atual
// Garante que a semana comece no Monday, se Calendar.firstDayOfWeek for diferente
// Ou usa calendar.firstDayOfWeek para ser consistente com a localidade
val firstDayOfWeekAdjusted = if (calendar.firstDayOfWeek == Calendar.SUNDAY) {
Calendar.SUNDAY // Se a semana começa no domingo, mantém.
} else {
Calendar.MONDAY // Senão, assume segunda como início da semana para consistência.
}
calendar.set(Calendar.DAY_OF_WEEK, firstDayOfWeekAdjusted)
// Ajusta novamente o tempo para garantir que é o início do dia do primeiro dia da semana.
calendar.set(Calendar.HOUR_OF_DAY, 0)
calendar.set(Calendar.MINUTE, 0)
calendar.set(Calendar.SECOND, 0)
calendar.set(Calendar.MILLISECOND, 0)
}
"Mensal" -> {
// Seta para o primeiro dia do mês atual
calendar.set(Calendar.DAY_OF_MONTH, 1)
// Ajusta novamente o tempo para garantir que é o início do dia do primeiro dia do mês.
calendar.set(Calendar.HOUR_OF_DAY, 0)
calendar.set(Calendar.MINUTE, 0)
calendar.set(Calendar.SECOND, 0)
calendar.set(Calendar.MILLISECOND, 0)
}
// "Diário" já está configurado para o início do dia atual pelo .timeInMillis e as ZERAGENS iniciais.
}
}

/**
* Navega o calendário para o período anterior ou próximo com base na direção.
* @param direction -1 para período anterior, 1 para próximo período.
*/
private fun navigatePeriod(direction: Int) {
when (currentPeriodMode) {
"Diário" -> calendar.add(Calendar.DAY_OF_YEAR, direction)
"Semanal" -> calendar.add(Calendar.WEEK_OF_YEAR, direction)
"Mensal" -> calendar.add(Calendar.MONTH, direction)
}
updatePeriodDisplay() // Atualiza o texto do período
loadPedidosAgenda() // Recarrega os pedidos para o novo período
}

/**
* Atualiza o TextView que exibe o período atual (Diário, Semanal ou Mensal).
*/
private fun updatePeriodDisplay() {
when (currentPeriodMode) {
"Diário" -> textViewCurrentPeriod.text = dateFormatDisplay.format(calendar.time)
"Semanal" -> {
val startOfWeek = calendar.clone() as Calendar
// Garante que o startOfWeek esteja no início da semana corrente
startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek)
// Zera as horas para a exibição do início da semana
startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
startOfWeek.set(Calendar.MINUTE, 0)
startOfWeek.set(Calendar.SECOND, 0)
startOfWeek.set(Calendar.MILLISECOND, 0)

val endOfWeek = startOfWeek.clone() as Calendar
endOfWeek.add(Calendar.DAY_OF_YEAR, 6)
// Define o fim do dia para a exibição do fim da semana
endOfWeek.set(Calendar.HOUR_OF_DAY, 23)
endOfWeek.set(Calendar.MINUTE, 59)
endOfWeek.set(Calendar.SECOND, 59)
endOfWeek.set(Calendar.MILLISECOND, 999)

textViewCurrentPeriod.text = "${dateFormatDailyQuery.format(startOfWeek.time)} - ${dateFormatDailyQuery.format(endOfWeek.time)}"
}
"Mensal" -> {
val monthFormat = SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))
textViewCurrentPeriod.text = monthFormat.format(calendar.time)
}
}
}

/**
* Carrega os pedidos da agenda do Firestore com base no período selecionado.
*/
private fun loadPedidosAgenda() {
lifecycleScope.launch {
val (startTime, endTime) = getPeriodRangeForQuery()

// Utiliza o novo método otimizado do PedidoRepository para obter pedidos "Pendente" ou "Pago"
pedidoRepository.getPedidosParaAgendaByDateRange(startTime, endTime).collectLatest { pedidosComCliente ->
// Filtra apenas os pedidos que ainda precisam ser entregues (não 'Entregue' ou 'Falha na Entrega')
val pedidosAbertos = pedidosComCliente.filter {
it.pedido.situacao !in listOf("Entregue", "Falha na Entrega")
}
agendaPedidosAdapter.updatePedidos(pedidosAbertos)
textViewEntregasAgendadasCount.text = "${pedidosAbertos.size} Entregas Agendadas"
}
}
}

/**
* Calcula o range de tempo (startTime e endTime em milissegundos) para a consulta ao Firestore,
* com base no modo de período atual (Diário, Semanal, Mensal).
* ESTA FUNÇÃO É CRÍTICA PARA A PRECISÃO DAS QUERIES.
*/
private fun getPeriodRangeForQuery(): Pair<Long?, Long?> {
val startCal = calendar.clone() as Calendar
val endCal = calendar.clone() as Calendar

var startTime: Long? = null
var endTime: Long? = null

when (currentPeriodMode) {
"Diário" -> {
// Início do dia atual (calendar já está setado para o início do dia atual ou dia navegado)
startCal.set(Calendar.HOUR_OF_DAY, 0)
startCal.set(Calendar.MINUTE, 0)
startCal.set(Calendar.SECOND, 0)
startCal.set(Calendar.MILLISECOND, 0)
startTime = startCal.timeInMillis

// Fim do dia atual
endCal.timeInMillis = startCal.timeInMillis // Começa do mesmo dia
endCal.set(Calendar.HOUR_OF_DAY, 23)
endCal.set(Calendar.MINUTE, 59)
endCal.set(Calendar.SECOND, 59)
endCal.set(Calendar.MILLISECOND, 999)
endTime = endCal.timeInMillis
}
"Semanal" -> {
// O 'calendar' já está posicionado no primeiro dia da semana (por resetCalendarToCurrentPeriod/navigatePeriod)
// Agora, garantimos que 'startCal' use esse primeiro dia, zerando as horas
startCal.set(Calendar.DAY_OF_WEEK, calendar.get(Calendar.DAY_OF_WEEK))
startCal.set(Calendar.HOUR_OF_DAY, 0)
startCal.set(Calendar.MINUTE, 0)
startCal.set(Calendar.SECOND, 0)
startCal.set(Calendar.MILLISECOND, 0)
startTime = startCal.timeInMillis

// Calcula o fim da semana a partir do startCal
endCal.timeInMillis = startCal.timeInMillis
endCal.add(Calendar.DAY_OF_YEAR, 6)
endCal.set(Calendar.HOUR_OF_DAY, 23)
endCal.set(Calendar.MINUTE, 59)
endCal.set(Calendar.SECOND, 59)
endCal.set(Calendar.MILLISECOND, 999)
endTime = endCal.timeInMillis
}
"Mensal" -> {
// O 'calendar' já está posicionado no primeiro dia do mês (por resetCalendarToCurrentPeriod/navigatePeriod)
// Agora, garantimos que 'startCal' use esse primeiro dia, zerando as horas
startCal.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
startCal.set(Calendar.HOUR_OF_DAY, 0)
startCal.set(Calendar.MINUTE, 0)
startCal.set(Calendar.SECOND, 0)
startCal.set(Calendar.MILLISECOND, 0)
startTime = startCal.timeInMillis

// Calcula o último dia do mês a partir do startCal
endCal.timeInMillis = startCal.timeInMillis
endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
endCal.set(Calendar.HOUR_OF_DAY, 23)
endCal.set(Calendar.MINUTE, 59)
endCal.set(Calendar.SECOND, 59)
endCal.set(Calendar.MILLISECOND, 999)
endTime = endCal.timeInMillis
}
}
return Pair(startTime, endTime)
}

// --- Implementação dos métodos da AgendaPedidoClickListener ---

/**
* Abre a tela de detalhes do pedido quando o usuário clica em "Ver Detalhes".
*/
override fun onVerDetalhesClick(pedidoComCliente: PedidoComCliente) {
val intent = Intent(this, DetalhePedidoActivity::class.java)
intent.putExtra("pedido", pedidoComCliente.pedido)
startActivity(intent)
}

/**
* Lida com a finalização de um pedido com sucesso.
* Atualiza a situação do pedido para "Entregue" no banco de dados.
*/
override fun onFinalizarComSucessoClick(pedidoComCliente: PedidoComCliente) {
AlertDialog.Builder(this)
.setTitle("Finalizar Entrega")
.setMessage("Confirmar entrega do pedido ${pedidoComCliente.pedido.codigoPedido} como 'Entregue'?")
.setPositiveButton("Sim") { dialog, which ->
lifecycleScope.launch {
val updatedPedido = pedidoComCliente.pedido.copy(situacao = "Entregue")
val success = pedidoRepository.updatePedido(updatedPedido)
if (success) {
Toast.makeText(this@AgendaActivity, "Pedido ${updatedPedido.codigoPedido} marcado como Entregue!", Toast.LENGTH_SHORT).show()
// A atualização da lista acontecerá automaticamente via Flow do repositório
} else {
Toast.makeText(this@AgendaActivity, "Erro ao finalizar pedido.", Toast.LENGTH_SHORT).show()
}
}
}
.setNegativeButton("Não", null)
.show()
}

/**
* Lida com a finalização de um pedido com falha.
* Atualiza a situação do pedido para "Falha na Entrega" no banco de dados.
*/
override fun onFinalizarSemSucessoClick(pedidoComCliente: PedidoComCliente) {
AlertDialog.Builder(this)
.setTitle("Finalizar Entrega")
.setMessage("Confirmar falha na entrega do pedido ${pedidoComCliente.pedido.codigoPedido}?")
.setPositiveButton("Sim") { dialog, which ->
lifecycleScope.launch {
val updatedPedido = pedidoComCliente.pedido.copy(situacao = "Falha na Entrega")
val success = pedidoRepository.updatePedido(updatedPedido)
if (success) {
Toast.makeText(this@AgendaActivity, "Pedido ${updatedPedido.codigoPedido} marcado como Falha na Entrega!", Toast.LENGTH_SHORT).show()
// A atualização da lista acontecerá automaticamente via Flow do repositório
} else {
Toast.makeText(this@AgendaActivity, "Erro ao finalizar pedido.", Toast.LENGTH_SHORT).show()
}
}
}
.setNegativeButton("Não", null)
.show()
}

/**
* Copia o endereço de entrega do cliente para a área de transferência.
*/
override fun onCopiarEnderecoClick(endereco: String) {
val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
val clip = ClipData.newPlainText("Endereço de Entrega", endereco)
clipboard.setPrimaryClip(clip)
Toast.makeText(this, "Endereço copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
}
}