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
import com.example.lojamamager.data.Fornecedor
import com.example.lojamamager.firebase.repository.FornecedorRepository
import kotlinx.coroutines.launch

class DetalheFornecedorActivity : BaseActivity() {

    private lateinit var toolbarTitle: TextView
    private lateinit var imageViewBack: ImageView
    private lateinit var imageViewSave: ImageView
    private lateinit var imageViewDelete: ImageView

    private lateinit var editTextCnpjCpf: EditText
    private lateinit var editTextRazaoSocial: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextCep: EditText
    private lateinit var buttonBuscarCep: Button
    private lateinit var editTextEndereco: EditText
    private lateinit var editTextNumero: EditText
    private lateinit var editTextComplemento: EditText
    private lateinit var editTextBairro: EditText
    private lateinit var editTextCidade: EditText
    private lateinit var editTextUf: EditText
    private lateinit var editTextContato: EditText
    private lateinit var editTextTelefone: EditText

    private var fornecedorEmEdicao: Fornecedor? = null
    private var isFornecedorModificado = false

    private lateinit var fornecedorRepository: FornecedorRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_fornecedor)

        // Inicializar Views
        toolbarTitle = findViewById(R.id.textViewToolbarTitleFornecedor)
        imageViewBack = findViewById(R.id.imageViewBackFornecedor)
        imageViewSave = findViewById(R.id.imageViewSaveFornecedor)
        imageViewDelete = findViewById(R.id.imageViewDeleteFornecedor)

        editTextCnpjCpf = findViewById(R.id.editTextCnpjCpf)
        editTextRazaoSocial = findViewById(R.id.editTextRazaoSocial)
        // CORREÇÃO: A linha 59 no seu código original é essa.
        // A correção consiste em garantir que esta View exista no layout XML.
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextCep = findViewById(R.id.editTextCep)
        buttonBuscarCep = findViewById(R.id.buttonBuscarCep)
        editTextEndereco = findViewById(R.id.editTextEndereco)
        editTextNumero = findViewById(R.id.editTextNumero)
        editTextComplemento = findViewById(R.id.editTextComplemento)
        editTextBairro = findViewById(R.id.editTextBairro)
        editTextCidade = findViewById(R.id.editTextCidade)
        editTextUf = findViewById(R.id.editTextUf)
        editTextContato = findViewById(R.id.editTextContato)
        editTextTelefone = findViewById(R.id.editTextTelefone)

        fornecedorRepository = FornecedorRepository(applicationContext)

        fornecedorEmEdicao = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("fornecedor", Fornecedor::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("fornecedor") as? Fornecedor
        }

        if (fornecedorEmEdicao != null) {
            toolbarTitle.text = "Editar Fornecedor"
            preencherCamposComFornecedor(fornecedorEmEdicao!!)
            imageViewDelete.visibility = View.VISIBLE
        } else {
            toolbarTitle.text = "Novo Fornecedor"
            imageViewDelete.visibility = View.GONE
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isFornecedorModificado = true
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        editTextCnpjCpf.addTextChangedListener(textWatcher)
        editTextRazaoSocial.addTextChangedListener(textWatcher)
        editTextEmail.addTextChangedListener(textWatcher)
        editTextCep.addTextChangedListener(textWatcher)
        editTextEndereco.addTextChangedListener(textWatcher)
        editTextNumero.addTextChangedListener(textWatcher)
        editTextComplemento.addTextChangedListener(textWatcher)
        editTextBairro.addTextChangedListener(textWatcher)
        editTextCidade.addTextChangedListener(textWatcher)
        editTextUf.addTextChangedListener(textWatcher)
        editTextContato.addTextChangedListener(textWatcher)
        editTextTelefone.addTextChangedListener(textWatcher)

        imageViewBack.setOnClickListener {
            handleBackPress()
        }

        imageViewSave.setOnClickListener {
            salvarFornecedor()
        }

        imageViewDelete.setOnClickListener {
            confirmarDelecao()
        }

        buttonBuscarCep.setOnClickListener {
            buscarCep()
        }
    }

    private fun preencherCamposComFornecedor(fornecedor: Fornecedor) {
        editTextCnpjCpf.setText(fornecedor.cnpjCpf)
        editTextRazaoSocial.setText(fornecedor.razaoSocial)
        editTextEmail.setText(fornecedor.email)
        editTextCep.setText(fornecedor.cep)
        editTextEndereco.setText(fornecedor.endereco)
        editTextNumero.setText(fornecedor.numero)
        editTextComplemento.setText(fornecedor.complemento)
        editTextBairro.setText(fornecedor.bairro)
        editTextCidade.setText(fornecedor.cidade)
        editTextUf.setText(fornecedor.uf)
        editTextContato.setText(fornecedor.contato)
        editTextTelefone.setText(fornecedor.telefone)
        isFornecedorModificado = false
    }

    private fun salvarFornecedor() {
        val razaoSocial = editTextRazaoSocial.text.toString().trim()
        val cnpjCpf = editTextCnpjCpf.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val cep = editTextCep.text.toString().trim()
        val endereco = editTextEndereco.text.toString().trim()
        val numero = editTextNumero.text.toString().trim()
        val complemento = editTextComplemento.text.toString().trim()
        val bairro = editTextBairro.text.toString().trim()
        val cidade = editTextCidade.text.toString().trim()
        val uf = editTextUf.text.toString().trim()
        val contato = editTextContato.text.toString().trim()
        val telefone = editTextTelefone.text.toString().trim()

        if (!validarCampos(razaoSocial, cnpjCpf, email, endereco, numero)) {
            return
        }

        val fornecedor = fornecedorEmEdicao?.copy(
            cnpjCpf = if (cnpjCpf.isEmpty()) null else cnpjCpf,
            razaoSocial = razaoSocial,
            email = if (email.isEmpty()) null else email,
            cep = if (cep.isEmpty()) null else cep,
            endereco = endereco,
            numero = numero,
            complemento = if (complemento.isEmpty()) null else complemento,
            bairro = if (bairro.isEmpty()) null else bairro,
            cidade = if (cidade.isEmpty()) null else cidade,
            uf = if (uf.isEmpty()) null else uf,
            contato = if (contato.isEmpty()) null else contato,
            telefone = if (telefone.isEmpty()) null else telefone
        ) ?: Fornecedor(
            id = null,
            cnpjCpf = if (cnpjCpf.isEmpty()) null else cnpjCpf,
            razaoSocial = razaoSocial,
            email = if (email.isEmpty()) null else email,
            cep = if (cep.isEmpty()) null else cep,
            endereco = endereco,
            numero = numero,
            complemento = if (complemento.isEmpty()) null else complemento,
            bairro = if (bairro.isEmpty()) null else bairro,
            cidade = if (cidade.isEmpty()) null else cidade,
            uf = if (uf.isEmpty()) null else uf,
            contato = if (contato.isEmpty()) null else contato,
            telefone = if (telefone.isEmpty()) null else telefone
        )

        lifecycleScope.launch {
            try {
                if (fornecedorEmEdicao != null) {
                    val success = fornecedorRepository.updateFornecedor(fornecedor)
                    if (success) {
                        Toast.makeText(this@DetalheFornecedorActivity, "Fornecedor atualizado: ${fornecedor.razaoSocial}", Toast.LENGTH_SHORT).show()
                        isFornecedorModificado = false
                        finish()
                    } else {
                        Toast.makeText(this@DetalheFornecedorActivity, "Erro ao atualizar fornecedor.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val newId = fornecedorRepository.addFornecedor(fornecedor)
                    if (newId != null) {
                        Toast.makeText(this@DetalheFornecedorActivity, "Fornecedor adicionado: ${fornecedor.razaoSocial}", Toast.LENGTH_SHORT).show()
                        isFornecedorModificado = false
                        finish()
                    } else {
                        Toast.makeText(this@DetalheFornecedorActivity, "Erro ao adicionar fornecedor.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@DetalheFornecedorActivity, "Erro ao salvar fornecedor: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun validarCampos(razaoSocial: String, cnpjCpf: String, email: String, endereco: String, numero: String): Boolean {
        if (razaoSocial.isEmpty()) {
            editTextRazaoSocial.error = "Razão Social é obrigatória"
            Toast.makeText(this, "Preencha a Razão Social obrigatória!", Toast.LENGTH_LONG).show()
            return false
        }

        val cnpjCpfRegex = "^\\d{11}$|^\\d{14}$".toRegex()
        if (cnpjCpf.isNotEmpty() && !cnpjCpf.matches(cnpjCpfRegex)) {
            editTextCnpjCpf.error = "CPF ou CNPJ inválido. Digite 11 (CPF) ou 14 (CNPJ) dígitos."
            Toast.makeText(this, "CPF ou CNPJ inválido!", Toast.LENGTH_LONG).show()
            return false
        }

        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.error = "Formato de e-mail inválido"
            Toast.makeText(this, "Formato de e-mail inválido!", Toast.LENGTH_LONG).show()
            return false
        }

        if (endereco.isEmpty()) {
            editTextEndereco.error = "Endereço é obrigatório"
            Toast.makeText(this, "Endereço é obrigatório!", Toast.LENGTH_LONG).show()
            return false
        }
        if (numero.isEmpty()) {
            editTextNumero.error = "Número é obrigatório"
            Toast.makeText(this, "Número do endereço é obrigatório!", Toast.LENGTH_LONG).show()
            return false
        }

            // Validação de email aprimorada com regex mais restrita
            val emailRegex = ("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$").toRegex()
            if (email.isNotEmpty() && !email.matches(emailRegex)) {
                editTextEmail.error = "Formato de e-mail inválido"
                Toast.makeText(this, "Formato de e-mail inválido!", Toast.LENGTH_LONG).show()
                return false
            }


        return true
    }

    private fun confirmarDelecao() {
        fornecedorEmEdicao?.let { fornecedor ->
            AlertDialog.Builder(this)
                .setTitle("Excluir Fornecedor")
                .setMessage("Tem certeza de que deseja excluir ${fornecedor.razaoSocial}?")
                .setPositiveButton("Sim") { dialog, which ->
                    lifecycleScope.launch {
                        try {
                            val success = fornecedorRepository.deleteFornecedor(fornecedor.id!!)
                            if (success) {
                                Toast.makeText(this@DetalheFornecedorActivity, "${fornecedor.razaoSocial} excluído!", Toast.LENGTH_SHORT).show()
                                isFornecedorModificado = false
                                finish()
                            } else {
                                Toast.makeText(this@DetalheFornecedorActivity, "Erro ao excluir fornecedor.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@DetalheFornecedorActivity, "Erro ao excluir fornecedor: ${e.message}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    }
                }
                .setNegativeButton("Não", null)
                .show()
        }
    }

    private fun handleBackPress() {
        if (isFornecedorModificado) {
            AlertDialog.Builder(this)
                .setTitle("Sair sem Salvar?")
                .setMessage("Você tem alterações não salvas. Deseja salvá-las?")
                .setPositiveButton("Salvar") { dialog, which ->
                    salvarFornecedor()
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
        val cep = editTextCep.text.toString().trim()
        // Valida se o CEP tem 8 dígitos, removendo qualquer formatação
        val cleanedCep = cep.replace("-", "")
        if (cleanedCep.length != 8) {
            editTextCep.error = "CEP inválido. Deve ter 8 dígitos."
            return
        }
        editTextCep.error = null

        Toast.makeText(this, "Buscando CEP...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // Chama o serviço de busca de CEP de forma segura (suspend function)
                val response = RetrofitClient.cepService.buscarEnderecoPorCep(cleanedCep)

                if (response.isSuccessful && response.body() != null) {
                    val enderecoResponse = response.body()
                    // A API do ViaCEP retorna um objeto com erro = true se o CEP não for encontrado
                    if (enderecoResponse?.erro == true) {
                        Toast.makeText(this@DetalheFornecedorActivity, "CEP não encontrado.", Toast.LENGTH_LONG).show()
                    } else {
                        // Preenche os campos do formulário com os dados do endereço
                        editTextEndereco.setText(enderecoResponse?.logradouro)
                        editTextBairro.setText(enderecoResponse?.bairro)
                        editTextCidade.setText(enderecoResponse?.localidade)
                        editTextUf.setText(enderecoResponse?.uf)
                        editTextComplemento.setText(enderecoResponse?.complemento)
                        isFornecedorModificado = true
                        Toast.makeText(this@DetalheFornecedorActivity, "CEP preenchido!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@DetalheFornecedorActivity, "Erro ao buscar CEP: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                // Em caso de erro de conexão ou outro problema
                Toast.makeText(this@DetalheFornecedorActivity, "Erro de rede: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}