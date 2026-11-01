package com.example.lojamamager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.lojamamager.data.Cliente
import com.example.lojamamager.data.MaterialInsumo
import com.example.lojamamager.data.Pedido
import com.example.lojamamager.data.Produto
import com.example.lojamamager.firebase.repository.ClienteRepository
import com.example.lojamamager.firebase.repository.FornecedorRepository
import com.example.lojamamager.firebase.repository.PedidoRepository
import com.example.lojamamager.firebase.repository.ProdutoRepository
import com.example.lojamamager.util.com.example.lojamamager.firebase.repository.MaterialRepository
import com.google.firebase.auth.FirebaseAuth
import com.opencsv.CSVReaderBuilder
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.StringReader

class DataManagementActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var clienteRepository: ClienteRepository
    private lateinit var produtoRepository: ProdutoRepository
    private lateinit var pedidoRepository: PedidoRepository
    private lateinit var fornecedorRepository: FornecedorRepository
    // Adicione junto com os outros repositórios declarados no topo da classe
    private lateinit var materialRepository: MaterialRepository


    private lateinit var spinnerImportType: Spinner
    private lateinit var buttonSelectCsv: Button
    private lateinit var textViewSelectedFile: TextView
    private lateinit var buttonImportData: Button
    private lateinit var progressBarImport: ProgressBar
    private lateinit var textViewImportStatus: TextView

    private lateinit var editTextCsvContent: EditText
    private lateinit var buttonImportFromText: Button

    private lateinit var spinnerExportType: Spinner
    private lateinit var buttonExportData: Button
    private lateinit var buttonHelp: Button

    private var selectedFileUri: Uri? = null

    private val pickCsvFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                selectedFileUri = uri
                val fileName = getFileName(uri)
                textViewSelectedFile.text = "Arquivo selecionado: $fileName"
                buttonImportData.isEnabled = true
                textViewImportStatus.text = ""
                progressBarImport.visibility = View.GONE
            } else {
                textViewSelectedFile.text = "Nenhum arquivo selecionado"
                buttonImportData.isEnabled = false
                Toast.makeText(this, "Não foi possível selecionar o arquivo.", Toast.LENGTH_SHORT).show()
            }
        } else {
            textViewSelectedFile.text = "Nenhum arquivo selecionado"
            buttonImportData.isEnabled = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_management)

        auth = FirebaseAuth.getInstance()
        clienteRepository = ClienteRepository(this)
        produtoRepository = ProdutoRepository(this)
        pedidoRepository = PedidoRepository(this)
        fornecedorRepository = FornecedorRepository(this)
        // Cole logo após a inicialização do fornecedorRepository:
        materialRepository = MaterialRepository(this)
        spinnerImportType = findViewById(R.id.spinnerImportType)
        buttonSelectCsv = findViewById(R.id.buttonSelectCsv)
        textViewSelectedFile = findViewById(R.id.textViewSelectedFile)
        buttonImportData = findViewById(R.id.buttonImportData)
        progressBarImport = findViewById(R.id.progressBarImport)
        textViewImportStatus = findViewById(R.id.textViewImportStatus)

        editTextCsvContent = findViewById(R.id.editTextCsvContent)
        buttonImportFromText = findViewById(R.id.buttonImportFromText)

        spinnerExportType = findViewById(R.id.spinnerExportType)
        buttonExportData = findViewById(R.id.buttonExportData)
        buttonHelp = findViewById(R.id.buttonHelp)

        // CONFIGURAÇÃO DA TOOLBAR COM O BOTÃO VOLTAR
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarData)
        toolbar.setNavigationOnClickListener {
            finish() // Fecha esta Activity e volta automaticamente para a Main
        }

        setupSpinners()

        editTextCsvContent.doAfterTextChanged { text ->
            buttonImportFromText.isEnabled = !text.isNullOrBlank()
        }

        buttonSelectCsv.setOnClickListener {
            openFilePicker()
        }

        buttonImportData.setOnClickListener {
            selectedFileUri?.let { uri ->
                showImportConfirmationDialog(uri)
            } ?: run {
                Toast.makeText(this, "Por favor, selecione um arquivo CSV primeiro.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonImportFromText.setOnClickListener {
            val csvContent = editTextCsvContent.text.toString()
            if (csvContent.isNotBlank()) {
                showImportConfirmationDialogFromText(csvContent)
            } else {
                Toast.makeText(this, "Por favor, cole o conteúdo do CSV na caixa de texto.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonExportData.setOnClickListener {
            exportData()
        }

        buttonHelp.setOnClickListener {
            openHelpVideo()
        }
    }

    private fun setupSpinners() {
        val dataTypesAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.data_types_array,
            android.R.layout.simple_spinner_item
        )
        dataTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerImportType.adapter = dataTypesAdapter
        spinnerExportType.adapter = dataTypesAdapter
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "application/vnd.ms-excel"))
        }
        pickCsvFileLauncher.launch(intent)
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = cursor.getString(displayNameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    result = result?.substring(cut + 1)
                }
            }
        }
        return result ?: "arquivo_desconhecido.csv"
    }

    private fun showImportConfirmationDialog(uri: Uri) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Aviso de Importação")

        // Mensagem transparente e direta explicando a regra de negócio
        builder.setMessage("Você está importando dados para '${spinnerImportType.selectedItem}'.\n\n" +
                "⚠️ ATENÇÃO: Se um item do arquivo tiver o mesmo CÓDIGO ou NOME de um já cadastrado, as informações antigas (incluindo a quantidade em estoque) serão SUBSTITUÍDAS pelas novas deste arquivo.")

        builder.setPositiveButton("Substituir e Importar") { dialog, _ ->
            importDataFromFile(uri)
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showImportConfirmationDialogFromText(content: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmar Importação")
        builder.setMessage("Tem certeza que deseja importar dados do texto colado para '${spinnerImportType.selectedItem}'?")
        builder.setPositiveButton("Importar") { dialog, which ->
            importDataFromString(content)
        }
        builder.setNegativeButton("Cancelar") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun importDataFromFile(uri: Uri) {
        progressBarImport.visibility = View.VISIBLE
        textViewImportStatus.visibility = View.VISIBLE
        textViewImportStatus.text = "Iniciando importação do arquivo..."
        buttonImportData.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { it.readText() }
                    importDataFromString(content)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DataManagementActivity, "Erro ao ler arquivo: ${e.message}", Toast.LENGTH_LONG).show()
                    progressBarImport.visibility = View.GONE
                    buttonImportData.isEnabled = true
                }
            }
        }
    }

    private fun importDataFromString(csvContent: String) {
        val dataType = spinnerImportType.selectedItem.toString()
        progressBarImport.visibility = View.VISIBLE
        textViewImportStatus.visibility = View.VISIBLE
        textViewImportStatus.text = "Iniciando importação do texto..."
        buttonImportFromText.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<String>()
            var dataLines: List<Array<String>> = emptyList() // <<-- AQUI ESTÁ A CORREÇÃO

            try {
                val reader = CSVReaderBuilder(StringReader(csvContent)).build()
                val lines = reader.readAll()
                if (lines.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DataManagementActivity, "O conteúdo CSV está vazio.", Toast.LENGTH_SHORT).show()
                        progressBarImport.visibility = View.GONE
                        buttonImportFromText.isEnabled = true
                    }
                    return@launch
                }

                val header = lines.firstOrNull()
                dataLines = lines.drop(1)

                withContext(Dispatchers.Main) {
                    progressBarImport.max = dataLines.size
                    progressBarImport.progress = 0
                }

                for ((index, line) in dataLines.withIndex()) {
                    try {
                        when (dataType) {
                            "Clientes" -> {
                                val cliente = parseClienteFromCsv(header, line)
                                if (cliente != null) {
                                    val id = clienteRepository.addCliente(cliente)
                                    if (id != null) {
                                        successCount++
                                    } else {
                                        errorCount++
                                        errors.add("Linha ${index + 2}: Erro ao salvar cliente.")
                                    }
                                } else {
                                    errorCount++
                                    errors.add("Linha ${index + 2}: Formato inválido ou campos ausentes para cliente.")
                                }
                            }
                            "Produtos" -> {
                                val produtoNovo = parseProdutoFromCsv(header, line)
                                if (produtoNovo != null) {
                                    // Busca se já existe algum produto com o mesmo código (ou mesmo nome caso não tenha código)
                                    val produtosExistentes = produtoRepository.getAllProdutos().first()
                                    val produtoCorrespondente = produtosExistentes.find {
                                        (!produtoNovo.codigo.isNullOrBlank() && it.codigo == produtoNovo.codigo) ||
                                                (produtoNovo.codigo.isNullOrBlank() && it.nome.trim().equals(produtoNovo.nome.trim(), ignoreCase = true))
                                    }

                                    if (produtoCorrespondente != null) {
                                        // !!! ENCONTROU DUPLICADO: SUBSTITUI COM OS DADOS NOVOS !!!
                                        val produtoAtualizado = produtoNovo.copy(
                                            id = produtoCorrespondente.id,
                                            empresaId = produtoCorrespondente.empresaId
                                        )
                                        val atualizou = produtoRepository.updateProduto(produtoAtualizado)
                                        if (atualizou) successCount++ else errorCount++
                                    } else {
                                        // Item inédito: adiciona normalmente
                                        val id = produtoRepository.addProduto(produtoNovo)
                                        if (id != null) successCount++ else errorCount++
                                    }
                                } else {
                                    errorCount++
                                    errors.add("Linha ${index + 2}: Formato inválido para produto.")
                                }
                            }

                            "Materiais" -> {
                                val materialNovo = parseMaterialFromCsv(header, line)
                                if (materialNovo != null) {
                                    val materiaisExistentes = materialRepository.getMateriaisSingle()

                                    // Agora o app busca se o material já existe comparando CÓDIGO (se houver) ou o NOME
                                    val materialCorrespondente = materiaisExistentes.find {
                                        (!materialNovo.codigo.isBlank() && it.codigo == materialNovo.codigo) ||
                                                (materialNovo.codigo.isBlank() && it.nome.trim().equals(materialNovo.nome.trim(), ignoreCase = true))
                                    }

                                    if (materialCorrespondente != null) {
                                        // Se já existe, clona aproveitando o ID do Firebase e sobrescreve com os valores atualizados
                                        val materialAtualizado = materialNovo.copy(
                                            id = materialCorrespondente.id,
                                            empresaId = materialCorrespondente.empresaId
                                        )
                                        val atualizou = materialRepository.updateMaterial(materialAtualizado)
                                        if (atualizou) successCount++ else errorCount++
                                    } else {
                                        val id = materialRepository.addMaterial(materialNovo)
                                        if (id != null) successCount++ else errorCount++
                                    }
                                } else {
                                    errorCount++
                                }
                            }
                            "Pedidos" -> {
                                val pedido = parsePedidoFromCsv(header, line)
                                if (pedido != null) {
                                    val id = pedidoRepository.addOrUpdatePedido(pedido)
                                    if (id != null) {
                                        successCount++
                                    } else {
                                        errorCount++
                                        errors.add("Linha ${index + 2}: Erro ao salvar pedido.")
                                    }
                                } else {
                                    errorCount++
                                    errors.add("Linha ${index + 2}: Formato inválido ou campos ausentes para pedido.")
                                }
                            }


                                else -> {
                                    errors.add("Tipo de dado '$dataType' não suportado para importação.")
                                    break
                                }

                        }
                    } catch (e: Exception) {
                        errorCount++
                        errors.add("Linha ${index + 2}: Erro ao processar - ${e.message}")
                    }
                    withContext(Dispatchers.Main) {
                        progressBarImport.progress = index + 1
                        textViewImportStatus.text = "Importando... ${index + 1}/${dataLines.size} (${successCount} sucessos, ${errorCount} erros)"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DataManagementActivity, "Erro ao ler o conteúdo: ${e.message}", Toast.LENGTH_LONG).show()
                }
                errorCount = dataLines.size
            } finally {
                withContext(Dispatchers.Main) {
                    progressBarImport.visibility = View.GONE
                    buttonImportFromText.isEnabled = true
                    val message = "Importação concluída: $successCount registros importados, $errorCount erros."
                    textViewImportStatus.text = message
                    Toast.makeText(this@DataManagementActivity, message, Toast.LENGTH_LONG).show()
                    if (errors.isNotEmpty()) {
                        showErrorDetailsDialog(errors)
                    }
                }
            }
        }
    }


    private fun parseClienteFromCsv(header: Array<String>?, line: Array<String>): Cliente? {
        if (header == null || line.size != header.size) return null
        val map = header.zip(line).toMap()
        return try {
            Cliente(
                id = map["id"],
                codigo = map["codigo"] ?: map["Código"],
                cnpjCpf = map["cnpjCpf"] ?: map["CPF/CNPJ"] ?: map["cpfCnpj"],
                nomeRazaoSocial = map["nomeRazaoSocial"] ?: map["Nome/Razão Social"] ?: "",
                cep = map["cep"] ?: map["CEP"],
                endereco = map["endereco"] ?: map["Endereço"],
                numero = map["numero"] ?: map["Número"],
                complemento = map["complemento"] ?: map["Complemento"],
                bairro = map["bairro"] ?: map["Bairro"],
                cidade = map["cidade"] ?: map["Cidade"],
                uf = map["uf"] ?: map["UF"] ?: map["estado"] ?: map["Estado"],
                telefone = map["telefone"] ?: map["Telefone"],
                empresaId = auth.currentUser?.uid
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseProdutoFromCsv(header: Array<String>?, line: Array<String>): Produto? {
        if (header == null || line.size != header.size) return null
        val map = header.zip(line).toMap()

        val nomeProduto = map["nome"] ?: map["Nome"] ?: map["nome do produto"]
        val precoCustoString = map["precoCusto"] ?: map["Preço Custo"] ?: map["preco custo"]
        val precoVendaString = map["precoVenda"] ?: map["Preço Venda"] ?: map["preco venda"]
        val quantidadeEstoqueString = map["quantidadeEstoque"] ?: map["Quantidade Estoque"] ?: map["quantidade em estoque"]
        val unidadeMedida = map["unidadeMedida"] ?: map["Unidade de Medida"]
        val categoria = map["categoria"] ?: map["Categoria"]
        val categoriaId = map["categoriaId"] ?: map["ID Categoria"]
        val marca = map["marca"] ?: map["Marca"]
        val marcaId = map["marcaId"] ?: map["ID Marca"]
        val fornecedor = map["fornecedor"] ?: map["Fornecedor"]
        val fornecedorId = map["fornecedorId"] ?: map["ID Fornecedor"]
        val descricao = map["descricao"] ?: map["Descrição"]
        val imageUrl = map["imageUrl"] ?: map["URL Imagem"]

        if (nomeProduto.isNullOrEmpty()) {
            return null
        }

        return try {
            Produto(
                id = map["id"],
                codigo = map["codigo"] ?: map["Código"],
                nome = nomeProduto,
                nomeLowerCase = nomeProduto.toLowerCase(),
                precoCusto = precoCustoString?.replace(",", ".")?.toDoubleOrNull() ?: 0.0,
                precoVenda = precoVendaString?.replace(",", ".")?.toDoubleOrNull() ?: 0.0,
                quantidadeEstoque = quantidadeEstoqueString?.toIntOrNull() ?: 0,
                unidadeMedida = unidadeMedida,
                categoria = categoria ?: "",
                categoriaId = categoriaId,
                marca = marca,
                marcaId = marcaId,
                fornecedor = fornecedor ?: "",
                fornecedorId = fornecedorId,
                descricao = descricao,
                imageUrl = imageUrl,
                empresaId = auth.currentUser?.uid
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePedidoFromCsv(header: Array<String>?, line: Array<String>): Pedido? {
        if (header == null || line.size != header.size) return null
        val map = header.zip(line).toMap()
        return try {
            Pedido(
                id = map["id"],
                codigoPedido = map["codigoPedido"] ?: map["Código do Pedido"] ?: "",
                clienteId = map["clienteId"] ?: map["ID Cliente"],
                dataCompra = map["dataCompra"]?.toLongOrNull() ?: System.currentTimeMillis(),
                dataEntregaPrevista = map["dataEntregaPrevista"]?.toLongOrNull(),
                valorTotal = map["valorTotal"]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0,
                situacao = map["situacao"] ?: map["Situação"] ?: "Pendente",
                condicaoPagamento = map["condicaoPagamento"] ?: map["Condição de Pagamento"] ?: "",
                empresaId = auth.currentUser?.uid
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMaterialFromCsv(header: Array<String>?, line: Array<String>): MaterialInsumo? {
        if (header == null || line.size != header.size) return null

        // Cria um mapa associando dinamicamente o Cabeçalho ao Valor da célula
        val dataMap = header.zip(line).toMap()

        val nome = dataMap["nome"]?.trim() ?: dataMap["Nome"]?.trim() ?: return null
        if (nome.isEmpty()) return null

        // Mapeamentos inteligentes convertendo os nomes da tabela para as variáveis do Kotlin
        val precoCusto = (dataMap["precoCusto"] ?: dataMap["precoUnitario"] ?: dataMap["Preço Custo"])?.trim()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val quantidadeEstoque = (dataMap["quantidadeEstoque"] ?: dataMap["quantidade"] ?: dataMap["Quantidade"])?.trim()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val unidadeMedida = (dataMap["unidadeMedida"] ?: dataMap["Unidade de Medida"])?.trim() ?: "Unidades"
        val rendimentoTotal = (dataMap["rendimentoTotal"] ?: dataMap["entoTotal"])?.trim()?.toDoubleOrNull() ?: 1.0
        val quantidadeMinima = (dataMap["quantidadeMinima"] ?: dataMap["Quantidade Mínima"])?.trim()?.toDoubleOrNull() ?: 0.0

        // Se houver uma coluna "codigo" ou "Código" na tabela, o app lerá automaticamente.
        val codigo = dataMap["codigo"] ?: dataMap["Código"] ?: ""

        return MaterialInsumo(
            id = dataMap["id"],
            codigo = codigo.trim(),
            nome = nome,
            precoCusto = precoCusto,
            quantidadeEstoque = quantidadeEstoque,
            unidadeMedida = unidadeMedida,
            rendimentoTotal = rendimentoTotal,
            quantidadeMinima = quantidadeMinima,
            empresaId = auth.currentUser?.uid
        )
    }

    private fun showErrorDetailsDialog(errors: List<String>) {
        val message = errors.joinToString("\n")
        AlertDialog.Builder(this)
            .setTitle("Erros na Importação")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun exportData() {
        val dataType = spinnerExportType.selectedItem.toString()
        Toast.makeText(this, "Exportando $dataType...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            val empresaId = auth.currentUser?.uid ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DataManagementActivity, "Usuário não autenticado. Impossível exportar.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            try {
                val csvContent = when (dataType) {
                    "Clientes" -> {
                        val clientes = clienteRepository.getAllClientes().first()
                        generateClientesCsv(clientes)
                    }
                    "Produtos" -> {
                        val produtos = produtoRepository.getAllProdutos().first()
                        generateProdutosCsv(produtos)
                    }
                    "Pedidos" -> {
                        val pedidos = pedidoRepository.getPedidosForEmpresa(empresaId)
                        generatePedidosCsv(pedidos)
                    }
                    // ===== ADICIONE ESTE BLOCO AQUI =====
                    "Materiais" -> {
                        val materiais = materialRepository.getMateriaisSingle()
                        generateMateriaisCsv(materiais)
                    }
                    // =====================================


                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@DataManagementActivity, "Tipo de dado '$dataType' não suportado para exportação.", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }

                if (csvContent.isBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DataManagementActivity, "Não há dados para exportar.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val fileName = "${dataType.toLowerCase()}_${System.currentTimeMillis()}.csv"
                saveCsvFile(fileName, csvContent)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DataManagementActivity, "Erro ao exportar dados: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun generateClientesCsv(clientes: List<Cliente>): String {
        val stringWriter = java.io.StringWriter()
        val csvWriter = CSVWriter(stringWriter)
        val header = arrayOf(
            "id", "codigo", "cnpjCpf", "nomeRazaoSocial", "cep", "endereco",
            "numero", "complemento", "bairro", "cidade", "uf", "telefone", "empresaId"
        )
        csvWriter.writeNext(header)
        clientes.forEach { cliente ->
            csvWriter.writeNext(arrayOf(
                cliente.id ?: "",
                cliente.codigo ?: "",
                cliente.cnpjCpf ?: "",
                cliente.nomeRazaoSocial,
                cliente.cep ?: "",
                cliente.endereco ?: "",
                cliente.numero ?: "",
                cliente.complemento ?: "",
                cliente.bairro ?: "",
                cliente.cidade ?: "",
                cliente.uf ?: "",
                cliente.telefone ?: "",
                cliente.empresaId ?: ""
            ))
        }
        csvWriter.close()
        return stringWriter.toString()
    }

    private fun generateProdutosCsv(produtos: List<Produto>): String {
        val stringWriter = java.io.StringWriter()
        val csvWriter = CSVWriter(stringWriter)
        val header = arrayOf(
            "id", "codigo", "nome", "nomeLowerCase", "precoCusto", "precoVenda",
            "quantidadeEstoque", "unidadeMedida", "categoria", "categoriaId",
            "fornecedor", "fornecedorId", "marca", "marcaId", "descricao", "imageUrl", "empresaId"
        )
        csvWriter.writeNext(header)
        produtos.forEach { produto ->
            csvWriter.writeNext(arrayOf(
                produto.id ?: "",
                produto.codigo ?: "",
                produto.nome,
                produto.nomeLowerCase,
                produto.precoCusto.toString(),
                produto.precoVenda.toString(),
                produto.quantidadeEstoque.toString(),
                produto.unidadeMedida ?: "",
                produto.categoria,
                produto.categoriaId ?: "",
                produto.fornecedor,
                produto.fornecedorId ?: "",
                produto.marca ?: "",
                produto.marcaId ?: "",
                produto.descricao ?: "",
                produto.imageUrl ?: "",
                produto.empresaId ?: ""
            ))
        }
        csvWriter.close()
        return stringWriter.toString()
    }

    private fun generatePedidosCsv(pedidos: List<Pedido>): String {
        val stringWriter = java.io.StringWriter()
        val csvWriter = CSVWriter(stringWriter)
        val header = arrayOf(
            "id", "codigoPedido", "clienteId", "dataCompra", "dataEntregaPrevista",
            "valorTotal", "situacao", "condicaoPagamento", "empresaId"
        )
        csvWriter.writeNext(header)
        pedidos.forEach { pedido ->
            csvWriter.writeNext(arrayOf(
                pedido.id ?: "",
                pedido.codigoPedido,
                pedido.clienteId ?: "",
                pedido.dataCompra.toString(),
                pedido.dataEntregaPrevista?.toString() ?: "",
                pedido.valorTotal.toString(),
                pedido.situacao,
                pedido.condicaoPagamento,
                pedido.empresaId ?: ""
            ))
        }
        csvWriter.close()
        return stringWriter.toString()
    }
    private fun generateMateriaisCsv(materiais: List<MaterialInsumo>): String {
        val stringWriter = java.io.StringWriter()
        val csvWriter = CSVWriter(stringWriter)

        // Estrutura do cabeçalho mapeada conforme seu objeto original
        val header = arrayOf(
            "id", "nome", "precoCusto", "quantidadeEstoque", "unidadeMedida", "rendimentoTotal", "empresaId"
        )
        csvWriter.writeNext(header)

        materiais.forEach { material ->
            csvWriter.writeNext(arrayOf(
                material.id ?: "",
                material.nome,
                material.precoCusto.toString(),
                material.quantidadeEstoque.toString(),
                material.unidadeMedida,
                material.rendimentoTotal.toString(),
                material.empresaId ?: ""
            ))
        }
        csvWriter.close()
        return stringWriter.toString()
    }
    private suspend fun saveCsvFile(fileName: String, content: String) {
        withContext(Dispatchers.Main) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)

                FileWriter(file).use { writer ->
                    writer.write(content)
                }

                Toast.makeText(this@DataManagementActivity, "Arquivo salvo em Downloads/$fileName", Toast.LENGTH_LONG).show()

                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this@DataManagementActivity,
                    applicationContext.packageName + ".fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Compartilhar arquivo CSV"))

            } catch (e: Exception) {
                Toast.makeText(this@DataManagementActivity, "Erro ao salvar ou compartilhar arquivo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openHelpVideo() {
        val videoUrl = "https://www.youtube.com/watch?v=SEU_LINK_DO_VIDEO_AQUI"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Nenhum aplicativo para abrir vídeos encontrado.", Toast.LENGTH_SHORT).show()
        }
    }
}