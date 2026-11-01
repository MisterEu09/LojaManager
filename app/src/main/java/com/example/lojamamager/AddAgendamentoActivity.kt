package com.example.lojamamager

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lojamamager.data.Cliente
import com.example.lojamamager.data.Pedido
import com.example.lojamamager.firebase.repository.ClienteRepository
import com.example.lojamamager.firebase.repository.PedidoRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// A classe Pedido já estava no seu código, a Parcelize é importante


// Assumi que você tem a classe ItemPedido e outras classes de data aqui, mas não as incluo na resposta para focar no problema principal.

class AddAgendamentoActivity : BaseActivity() {

private lateinit var imageViewBack: ImageView
private lateinit var textViewSelectedClient: TextView
private lateinit var layoutSelectClient: LinearLayout
private lateinit var textViewSelectedDeliveryDate: TextView
private lateinit var layoutSelectDeliveryDate: LinearLayout
private lateinit var editTextObservacoesAgendamento: EditText
private lateinit var buttonSaveAgendamento: Button

private lateinit var pedidoRepository: PedidoRepository
private lateinit var clienteRepository: ClienteRepository

private var selectedClienteId: String? = null
private var selectedDeliveryDate: Long? = null
private val calendar = Calendar.getInstance()

private val clientSelectionLauncher = registerForActivityResult(
ActivityResultContracts.StartActivityForResult()
) { result ->
if (result.resultCode == RESULT_OK) {
val selectedCliente = result.data?.getParcelableExtra("selected_cliente") as? Cliente
selectedCliente?.let {
textViewSelectedClient.text = it.nomeRazaoSocial
selectedClienteId = it.id
}
}
}

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContentView(R.layout.activity_add_agendamento)

imageViewBack = findViewById(R.id.imageViewBackAddAgendamento)
textViewSelectedClient = findViewById(R.id.textViewSelectedClient)
layoutSelectClient = findViewById(R.id.layoutSelectClient)
textViewSelectedDeliveryDate = findViewById(R.id.textViewSelectedDeliveryDate)
layoutSelectDeliveryDate = findViewById(R.id.layoutSelectDeliveryDate)
editTextObservacoesAgendamento = findViewById(R.id.editTextObservacoesAgendamento)
buttonSaveAgendamento = findViewById(R.id.buttonSaveAgendamento)

pedidoRepository = PedidoRepository(applicationContext)
clienteRepository = ClienteRepository(applicationContext)

setupListeners()
}

private fun setupListeners() {
imageViewBack.setOnClickListener {
finish()
}

layoutSelectClient.setOnClickListener {
val intent = Intent(this, ListaClientesActivity::class.java)
intent.putExtra("is_selection_mode", true)
clientSelectionLauncher.launch(intent)
}

layoutSelectDeliveryDate.setOnClickListener {
showDatePickerDialog()
}

buttonSaveAgendamento.setOnClickListener {
saveAgendamento()
}
}
    // ... (seu código anterior)

    private fun showDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Aplica o estilo customizado aqui

            val datePickerDialog = DatePickerDialog(this, {

             _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDeliveryDate = getStartOfDay(calendar.timeInMillis)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                textViewSelectedDeliveryDate.text = dateFormat.format(Date(selectedDeliveryDate!!))
            }, year, month, day
        )

        datePickerDialog.show()
    }

    // ... (resto do seu código)
// Função que garante que o timestamp é sempre do início do dia (00:00:00.000)
private fun getStartOfDay(timestamp: Long): Long {
return Calendar.getInstance().apply {
timeInMillis = timestamp
set(Calendar.HOUR_OF_DAY, 0)
set(Calendar.MINUTE, 0)
set(Calendar.SECOND, 0)
set(Calendar.MILLISECOND, 0)
}.timeInMillis
}

private fun saveAgendamento() {
val clienteId = selectedClienteId
val dataEntrega = selectedDeliveryDate
val observacoes = editTextObservacoesAgendamento.text.toString().trim() // Observações não salvas no Pedido

if (clienteId == null) {
Toast.makeText(this, "Selecione um cliente para o agendamento.", Toast.LENGTH_SHORT).show()
return
}
if (dataEntrega == null) {
Toast.makeText(this, "Selecione a data de entrega prevista.", Toast.LENGTH_SHORT).show()
return
}

lifecycleScope.launch {
val timestamp = System.currentTimeMillis()
val codigoPedido = "AGD-${timestamp.toString().takeLast(6)}"

val novoPedido = Pedido(
codigoPedido = codigoPedido,
clienteId = clienteId,
dataCompra = System.currentTimeMillis(), // Data de compra é agora
dataEntregaPrevista = dataEntrega, // Data de entrega já é o início do dia
valorTotal = 0.0,
situacao = "Pendente",
condicaoPagamento = "N/A"
)

try {
val newPedidoId = pedidoRepository.insertPedido(novoPedido)
if (newPedidoId != null) {
Toast.makeText(this@AddAgendamentoActivity, "Agendamento salvo com sucesso!", Toast.LENGTH_SHORT).show()
finish()
} else {
Toast.makeText(this@AddAgendamentoActivity, "Erro ao salvar agendamento: ID não gerado.", Toast.LENGTH_LONG).show()
}
} catch (e: Exception) {
Toast.makeText(this@AddAgendamentoActivity, "Erro ao salvar agendamento: ${e.message}", Toast.LENGTH_LONG).show()
e.printStackTrace()
}
}
}
}