package com.example.lojamamager

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.lojamamager.data.Cliente
import com.example.lojamamager.data.Pedido
import com.example.lojamamager.firebase.repository.ClienteRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PedidoHeaderData(
    val clienteId: String?,
    val statusNota: String?,
    val dataEmissao: Long?,
    val dataEntregaPrevista: Long?,
    val condicaoPagamento: String?
)

class PedidoHeaderFragment : Fragment() {

    private lateinit var textViewSelectedClient: TextView
    private lateinit var spinnerStatusNota: Spinner
    private lateinit var textViewDataEmissao: TextView
    private lateinit var textViewDataEntregaPrevista: TextView
    private lateinit var buttonCalcularEntrega: Button
    private lateinit var spinnerCondicaoPagamento: Spinner

    var selectedCliente: Cliente? = null
    var dataEmissao: Long? = null
    var dataEntregaPrevista: Long? = null

    // CORREÇÃO: Listener para notificar a activity quando o fragmento estiver pronto
    var onFragmentReady: (() -> Unit)? = null

    private val REQUEST_SELECT_CLIENTE = 2001
    private lateinit var clienteRepository: ClienteRepository

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    companion object {
        private const val ARG_PEDIDO = "pedido"
        fun newInstance(pedido: Pedido?): PedidoHeaderFragment {
            val fragment = PedidoHeaderFragment()
            val args = Bundle()
            args.putParcelable(ARG_PEDIDO, pedido)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pedido_header, container, false)

        textViewSelectedClient = view.findViewById(R.id.textViewSelectedClient)
        spinnerStatusNota = view.findViewById(R.id.spinnerStatusNota)
        textViewDataEmissao = view.findViewById(R.id.textViewDataEmissao)
        textViewDataEntregaPrevista = view.findViewById(R.id.textViewDataEntregaPrevista)
        buttonCalcularEntrega = view.findViewById(R.id.buttonCalcularEntrega)
        spinnerCondicaoPagamento = view.findViewById(R.id.spinnerCondicaoPagamento)

        clienteRepository = ClienteRepository(requireActivity().applicationContext)

        setupSpinners()
        setupClickListeners()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getParcelable<Pedido>(ARG_PEDIDO)?.let { pedido ->
            dataEmissao = pedido.dataCompra
            dataEntregaPrevista = pedido.dataEntregaPrevista

            // CORREÇÃO: Verificação de null-safety para o adapter
            (spinnerStatusNota.adapter as? ArrayAdapter<String>)?.let { statusAdapter ->
                val statusIndex = statusAdapter.getPosition(pedido.situacao)
                if (statusIndex != -1) {
                    spinnerStatusNota.setSelection(statusIndex)
                } else {
                    val defaultStatusIndex = statusAdapter.getPosition("Pendente")
                    if (defaultStatusIndex != -1) spinnerStatusNota.setSelection(defaultStatusIndex)
                }
            }

            (spinnerCondicaoPagamento.adapter as? ArrayAdapter<String>)?.let { condicaoAdapter ->
                val condicaoIndex = condicaoAdapter.getPosition(pedido.condicaoPagamento)
                if (condicaoIndex != -1) {
                    spinnerCondicaoPagamento.setSelection(condicaoIndex)
                } else {
                    if (condicaoAdapter.count > 0) spinnerCondicaoPagamento.setSelection(0)
                }
            }

            lifecycleScope.launch {
                pedido.clienteId?.let { clienteIdString ->
                    selectedCliente = clienteRepository.getClienteById(clienteIdString)
                }
                updateUiFromData()
                onFragmentReady?.invoke()
            }
        } ?: run {
            dataEmissao = Calendar.getInstance().timeInMillis
            updateUiFromData()
            onFragmentReady?.invoke()
        }
    }

    private fun setupSpinners() {
        val statusOptions = arrayOf("Pendente", "Confirmado", "Entregue", "Pago", "Cancelado", "Troca", "Falha na Entrega")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatusNota.adapter = statusAdapter

        val condicaoPagamentoOptions = arrayOf("À Vista", "Cartão de Crédito", "Cartão de Débito", "Boleto", "Pix")
        val condicaoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, condicaoPagamentoOptions)
        condicaoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCondicaoPagamento.adapter = condicaoAdapter
    }

    private fun setupClickListeners() {
        textViewSelectedClient.setOnClickListener {
            val intent = Intent(activity, ListaClientesActivity::class.java).apply {
                putExtra(ListaClientesActivity.IS_SELECTION_MODE, true)
            }
            startActivityForResult(intent, REQUEST_SELECT_CLIENTE)
        }

        textViewDataEmissao.setOnClickListener {
            showDatePicker(textViewDataEmissao) { timestamp ->
                dataEmissao = timestamp
                updateUiFromData()
            }
        }

        textViewDataEntregaPrevista.setOnClickListener {
            showDatePicker(textViewDataEntregaPrevista) { timestamp ->
                dataEntregaPrevista = timestamp
                updateUiFromData()
            }
        }

        buttonCalcularEntrega.setOnClickListener {
            calcularDataEntregaUtil()
        }
    }

    fun updateUiFromData() {
        selectedCliente?.let {
            textViewSelectedClient.text = it.nomeRazaoSocial
            textViewSelectedClient.setTextColor(resources.getColor(android.R.color.black, null))
        } ?: run {
            textViewSelectedClient.text = "Selecionar Cliente"
            textViewSelectedClient.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        dataEmissao?.let {
            textViewDataEmissao.text = dateFormat.format(Date(it))
            textViewDataEmissao.setTextColor(resources.getColor(android.R.color.black, null))
        } ?: run {
            textViewDataEmissao.text = "Selecione a Data"
            textViewDataEmissao.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        dataEntregaPrevista?.let {
            textViewDataEntregaPrevista.text = dateFormat.format(Date(it))
            textViewDataEntregaPrevista.setTextColor(resources.getColor(android.R.color.black, null))
        } ?: run {
            textViewDataEntregaPrevista.text = "Selecione ou Calcule"
            textViewDataEntregaPrevista.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }
    }

    private fun showDatePicker(textView: TextView, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        if (textView.text != "Selecione a Data" && textView.text != "Selecione ou Calcule") {
            try {
                val date = dateFormat.parse(textView.text.toString())
                date?.let { calendar.time = it }
            } catch (e: Exception) {
                // Ignora, usa a data atual se a conversão falhar
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                val selectedDateMillis = selectedCalendar.timeInMillis
                onDateSelected(selectedDateMillis)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun calcularDataEntregaUtil() {
        if (dataEmissao == null) {
            Toast.makeText(requireContext(), "Selecione a Data de Emissão primeiro.", Toast.LENGTH_SHORT).show()
            return
        }

        val input = AlertDialog.Builder(requireContext())
            .setTitle("Calcular Entrega")
            .setMessage("Quantos dias úteis a partir da data de emissão?")
            .setView(R.layout.dialog_input_days)
            .setPositiveButton("Calcular") { dialogInterface, _ ->
                val alertDialog = dialogInterface as AlertDialog
                val daysEditText = alertDialog.findViewById<EditText>(R.id.editTextDays)
                val daysString = daysEditText?.text.toString()
                if (daysString.isNotEmpty()) {
                    val daysToAdd = daysString.toInt()
                    lifecycleScope.launch {
                        val cal = Calendar.getInstance().apply { timeInMillis = dataEmissao!! }
                        var count = 0
                        while (count < daysToAdd) {
                            cal.add(Calendar.DAY_OF_MONTH, 1)
                            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                                count++
                            }
                        }
                        dataEntregaPrevista = cal.timeInMillis
                        updateUiFromData()
                        Toast.makeText(requireContext(), "$daysToAdd dias úteis calculados.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Por favor, insira o número de dias.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        input.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_CLIENTE && resultCode == Activity.RESULT_OK) {
            selectedCliente = data?.getParcelableExtra(ListaClientesActivity.SELECTED_CLIENTE_KEY) as? Cliente
            updateUiFromData()
        }
    }

    // CORREÇÃO: Funções públicas para acessar os valores do Spinner com segurança
    fun getStatusNota(): String? {
        return spinnerStatusNota.selectedItem?.toString()
    }

    fun getCondicaoPagamento(): String? {
        return spinnerCondicaoPagamento.selectedItem?.toString()
    }

    fun getHeaderData(): PedidoHeaderData {
        return PedidoHeaderData(
            clienteId = selectedCliente?.id,
            statusNota = getStatusNota(),
            dataEmissao = dataEmissao,
            dataEntregaPrevista = dataEntregaPrevista,
            condicaoPagamento = getCondicaoPagamento()
        )
    }
}