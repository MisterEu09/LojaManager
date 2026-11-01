package com.example.lojamamager

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lojamamager.data.Cliente
import com.example.lojamamager.firebase.repository.ClienteRepository
import kotlinx.coroutines.launch
import com.example.lojamamager.RetrofitClient

class DetalheClienteActivity : BaseActivity() {

    private lateinit var toolbarTitle: TextView
    private lateinit var imageViewBack: ImageView
    private lateinit var imageViewSave: ImageView
    private lateinit var imageViewDelete: ImageView

    private lateinit var editTextCodigoCliente: EditText
    private lateinit var editTextCnpjCpfCliente: EditText
    private lateinit var buttonBuscarCnpj: Button
    private lateinit var editTextNomeRazaoSocial: EditText
    private lateinit var editTextCepCliente: EditText
    private lateinit var buttonBuscarCepCliente: Button
    private lateinit var editTextEnderecoCliente: EditText
    private lateinit var editTextNumeroCliente: EditText
    private lateinit var editTextComplementoCliente: EditText
    private lateinit var editTextBairroCliente: EditText
    private lateinit var editTextCidadeCliente: EditText
    private lateinit var editTextUfCliente: EditText
    private lateinit var editTextTelefoneCliente: EditText
    private lateinit var editTextEmailCliente: EditText

    private var clienteEmEdicao: Cliente? = null
    private var isClienteModificado = false

    private lateinit var clienteRepository: ClienteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_cliente)

        // Inicializar Views
        toolbarTitle = findViewById(R.id.textViewToolbarTitleCliente)
        imageViewBack = findViewById(R.id.imageViewBackCliente)
        imageViewSave = findViewById(R.id.imageViewSaveCliente)
        // LINHA CORRIGIDA
        imageViewDelete = findViewById(R.id.imageViewDeleteCliente)

        editTextCodigoCliente = findViewById(R.id.editTextCodigoCliente)
        editTextCnpjCpfCliente = findViewById(R.id.editTextCnpjCpfCliente)
        buttonBuscarCnpj = findViewById(R.id.buttonBuscarCnpj)
        editTextNomeRazaoSocial = findViewById(R.id.editTextNomeRazaoSocial)
        editTextCepCliente = findViewById(R.id.editTextCepCliente)
        buttonBuscarCepCliente = findViewById(R.id.buttonBuscarCepCliente)
        editTextEnderecoCliente = findViewById(R.id.editTextEnderecoCliente)
        editTextNumeroCliente = findViewById(R.id.editTextNumeroCliente)
        editTextComplementoCliente = findViewById(R.id.editTextComplementoCliente)
        editTextBairroCliente = findViewById(R.id.editTextBairroCliente)
        editTextCidadeCliente = findViewById(R.id.editTextCidadeCliente)
        editTextUfCliente = findViewById(R.id.editTextUfCliente)
        editTextTelefoneCliente = findViewById(R.id.editTextTelefoneCliente)
        editTextEmailCliente = findViewById(R.id.editTextEmailCliente)

        // Inicializar o ClienteRepository
        clienteRepository = ClienteRepository(applicationContext)

        // Obter o cliente do Intent
        clienteEmEdicao = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("cliente", Cliente::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("cliente") as? Cliente
        }

        if (clienteEmEdicao != null) {
            toolbarTitle.text = "Editar Cliente"
            preencherCamposComCliente(clienteEmEdicao!!)
            imageViewDelete.visibility = View.VISIBLE
        } else {
            toolbarTitle.text = "Novo Cliente"
            imageViewDelete.visibility = View.GONE
        }

        // Listener para marcar como modificado ao digitar
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isClienteModificado = true
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        editTextCodigoCliente.addTextChangedListener(textWatcher)
        editTextCnpjCpfCliente.addTextChangedListener(textWatcher)
        editTextNomeRazaoSocial.addTextChangedListener(textWatcher)
        editTextCepCliente.addTextChangedListener(textWatcher)
        editTextEnderecoCliente.addTextChangedListener(textWatcher)
        editTextNumeroCliente.addTextChangedListener(textWatcher)
        editTextComplementoCliente.addTextChangedListener(textWatcher)
        editTextBairroCliente.addTextChangedListener(textWatcher)
        editTextCidadeCliente.addTextChangedListener(textWatcher)
        editTextUfCliente.addTextChangedListener(textWatcher)
        editTextTelefoneCliente.addTextChangedListener(textWatcher)
        editTextEmailCliente.addTextChangedListener(textWatcher)

        // Listeners para os botões da Toolbar
        imageViewBack.setOnClickListener {
            handleBackPress()
        }

        imageViewSave.setOnClickListener {
            salvarCliente()
        }

        imageViewDelete.setOnClickListener {
            confirmarDelecao()
        }

        // Listener para o botão Buscar CEP
        buttonBuscarCepCliente.setOnClickListener {
            buscarCep()
        }

        // Listener para o botão Buscar CNPJ (atualmente oculto)
        buttonBuscarCnpj.setOnClickListener {
            Toast.makeText(this, "Funcionalidade de busca de CNPJ não implementada.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun preencherCamposComCliente(cliente: Cliente) {
        editTextCodigoCliente.setText(cliente.codigo)
        editTextCnpjCpfCliente.setText(cliente.cnpjCpf)
        editTextNomeRazaoSocial.setText(cliente.nomeRazaoSocial)
        editTextCepCliente.setText(cliente.cep)
        editTextEnderecoCliente.setText(cliente.endereco)
        editTextNumeroCliente.setText(cliente.numero)
        editTextComplementoCliente.setText(cliente.complemento)
        editTextBairroCliente.setText(cliente.bairro)
        editTextCidadeCliente.setText(cliente.cidade)
        editTextUfCliente.setText(cliente.uf)
        editTextTelefoneCliente.setText(cliente.telefone)
        editTextEmailCliente.setText(cliente.email)
        isClienteModificado = false
    }

    private fun salvarCliente() {
        val nomeRazaoSocial = editTextNomeRazaoSocial.text.toString().trim()
        val codigo = editTextCodigoCliente.text.toString().trim().ifEmpty { null }
        val cnpjCpf = editTextCnpjCpfCliente.text.toString().trim().ifEmpty { null }
        val cep = editTextCepCliente.text.toString().trim().ifEmpty { null }
        val endereco = editTextEnderecoCliente.text.toString().trim().ifEmpty { null }
        val numero = editTextNumeroCliente.text.toString().trim().ifEmpty { null }
        val complemento = editTextComplementoCliente.text.toString().trim().ifEmpty { null }
        val bairro = editTextBairroCliente.text.toString().trim().ifEmpty { null }
        val cidade = editTextCidadeCliente.text.toString().trim().ifEmpty { null }
        val uf = editTextUfCliente.text.toString().trim().ifEmpty { null }
        val telefone = editTextTelefoneCliente.text.toString().trim().ifEmpty { null }
        val email = editTextEmailCliente.text.toString().trim().ifEmpty { null }

        var isValid = true
        if (nomeRazaoSocial.isEmpty()) {
            editTextNomeRazaoSocial.error = "Nome / Razão Social é obrigatória"
            isValid = false
        }
        if (!cnpjCpf.isNullOrEmpty() && !isCpfCnpjValid(cnpjCpf)) {
            editTextCnpjCpfCliente.error = "CPF/CNPJ inválido"
            isValid = false
        }
        if (!email.isNullOrEmpty() && !isEmailValid(email)) {
            editTextEmailCliente.error = "Email inválido"
            isValid = false
        }

        if (isValid) {
            val cliente = clienteEmEdicao?.copy(
                codigo = codigo,
                cnpjCpf = cnpjCpf,
                nomeRazaoSocial = nomeRazaoSocial,
                cep = cep,
                endereco = endereco,
                numero = numero,
                complemento = complemento,
                bairro = bairro,
                cidade = cidade,
                uf = uf,
                telefone = telefone,
                email = email
            ) ?: Cliente(
                id = null,
                codigo = codigo,
                cnpjCpf = cnpjCpf,
                nomeRazaoSocial = nomeRazaoSocial,
                cep = cep,
                endereco = endereco,
                numero = numero,
                complemento = complemento,
                bairro = bairro,
                cidade = cidade,
                uf = uf,
                telefone = telefone,
                email = email
            )

            lifecycleScope.launch {
                try {
                    if (clienteEmEdicao != null) {
                        val success = clienteRepository.updateCliente(cliente)
                        if (success) {
                            Toast.makeText(this@DetalheClienteActivity, "Cliente atualizado: ${cliente.nomeRazaoSocial}", Toast.LENGTH_SHORT).show()
                            isClienteModificado = false
                            finish()
                        } else {
                            Toast.makeText(this@DetalheClienteActivity, "Erro ao atualizar cliente.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val newId = clienteRepository.addCliente(cliente)
                        if (newId != null) {
                            Toast.makeText(this@DetalheClienteActivity, "Cliente adicionado: ${cliente.nomeRazaoSocial}", Toast.LENGTH_SHORT).show()
                            isClienteModificado = false
                            finish()
                        } else {
                            Toast.makeText(this@DetalheClienteActivity, "Erro ao adicionar cliente: ID não gerado.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@DetalheClienteActivity, "Erro ao salvar cliente: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        } else {
            Toast.makeText(this, "Preencha os campos obrigatórios e corrija os erros!", Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmarDelecao() {
        clienteEmEdicao?.let { cliente ->
            AlertDialog.Builder(this)
                .setTitle("Excluir Cliente")
                .setMessage("Tem certeza de que deseja excluir ${cliente.nomeRazaoSocial}?")
                .setPositiveButton("Sim") { dialog, which ->
                    lifecycleScope.launch {
                        try {
                            val success = clienteRepository.deleteCliente(cliente.id!!)
                            if (success) {
                                Toast.makeText(this@DetalheClienteActivity, "${cliente.nomeRazaoSocial} excluído!", Toast.LENGTH_SHORT).show()
                                isClienteModificado = false
                                finish()
                            } else {
                                Toast.makeText(this@DetalheClienteActivity, "Erro ao excluir cliente.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@DetalheClienteActivity, "Erro ao excluir cliente: ${e.message}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    }
                }
                .setNegativeButton("Não", null)
                .show()
        }
    }

    private fun handleBackPress() {
        if (isClienteModificado) {
            AlertDialog.Builder(this)
                .setTitle("Sair sem Salvar?")
                .setMessage("Você tem alterações não salvas. Deseja salvá-las?")
                .setPositiveButton("Salvar") { dialog, which ->
                    salvarCliente()
                }
                .setNegativeButton("Não Salvar") { dialog, which ->
                    finish()
                }
                .setNeutralButton("Voltar") { dialog, which ->
                    dialog.dismiss()
                }
                .show()
        } else {
            finish()
        }
    }

    override fun onBackPressed() {
        handleBackPress()
    }

    private fun buscarCep() {
        val cep = editTextCepCliente.text.toString().trim()
        val cleanedCep = cep.replace("-", "")
        if (cleanedCep.length != 8) {
            editTextCepCliente.error = "CEP inválido. Deve ter 8 dígitos."
            return
        }
        editTextCepCliente.error = null

        Toast.makeText(this, "Buscando CEP...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.cepService.buscarEnderecoPorCep(cleanedCep)
                if (response.isSuccessful && response.body() != null) {
                    val enderecoResponse = response.body()
                    if (enderecoResponse?.erro == true) {
                        Toast.makeText(this@DetalheClienteActivity, "CEP não encontrado.", Toast.LENGTH_LONG).show()
                    } else {
                        editTextEnderecoCliente.setText(enderecoResponse?.logradouro)
                        editTextBairroCliente.setText(enderecoResponse?.bairro)
                        editTextCidadeCliente.setText(enderecoResponse?.localidade)
                        editTextUfCliente.setText(enderecoResponse?.uf)
                        editTextComplementoCliente.setText(enderecoResponse?.complemento)
                        isClienteModificado = true
                        Toast.makeText(this@DetalheClienteActivity, "CEP preenchido!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@DetalheClienteActivity, "Erro ao buscar CEP: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DetalheClienteActivity, "Erro de rede: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    // Validação de CPF/CNPJ
    private fun isCpfCnpjValid(cpfCnpj: String): Boolean {
        val cleaned = cpfCnpj.replace(Regex("[^0-9]"), "")
        return when (cleaned.length) {
            11 -> isCpfValid(cleaned)
            14 -> isCnpjValid(cleaned)
            else -> false
        }
    }

    // Validação de CPF
    private fun isCpfValid(cpf: String): Boolean {
        if (cpf.length != 11 || cpf.all { it == cpf[0] }) return false
        try {
            val numbers = cpf.map { it.toString().toInt() }.toIntArray()
            val dv1 = ((0..8).sumOf { numbers[it] * (10 - it) } * 10) % 11
            if (numbers[9] != dv1) return false
            val dv2 = ((0..9).sumOf { numbers[it] * (11 - it) } * 10) % 11
            return numbers[10] == dv2
        } catch (e: Exception) {
            return false
        }
    }

    // Validação de CNPJ
    private fun isCnpjValid(cnpj: String): Boolean {
        if (cnpj.length != 14 || cnpj.all { it == cnpj[0] }) return false
        try {
            val numbers = cnpj.map { it.toString().toInt() }.toIntArray()
            val weights1 = intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
            val dv1 = (11 - (weights1.mapIndexed { i, w -> numbers[i] * w }.sum() % 11)) % 11
            if (numbers[12] != dv1 && dv1 != 10 && dv1 != 11) return false
            val weights2 = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
            val dv2 = (11 - (weights2.mapIndexed { i, w -> numbers[i] * w }.sum() % 11)) % 11
            return numbers[13] == dv2
        } catch (e: Exception) {
            return false
        }
    }

    // Validação de e-mail aprimorada
    private fun isEmailValid(email: String): Boolean {
        // Regex para validar emails de forma mais rigorosa
        val emailRegex = ("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$").toRegex()
        return email.matches(emailRegex)
    }
}