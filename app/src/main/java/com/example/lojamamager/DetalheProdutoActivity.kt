package com.example.lojamamager

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lojamamager.adapter.MaterialLinhaAdapter
import com.example.lojamamager.data.Fornecedor
import com.example.lojamamager.data.Produto
import com.example.lojamamager.data.MaterialInsumo
import com.example.lojamamager.data.MaterialProduto
import com.example.lojamamager.firebase.repository.CategoriaRepository
import com.example.lojamamager.firebase.repository.FornecedorRepository
import com.example.lojamamager.firebase.repository.MarcaRepository
import com.example.lojamamager.firebase.repository.ProdutoRepository
import com.example.lojamamager.util.com.example.lojamamager.firebase.repository.MaterialRepository
import java.text.DecimalFormat
import kotlinx.coroutines.launch

class DetalheProdutoActivity : BaseActivity() {

    private lateinit var categoriaRepository: CategoriaRepository
    private lateinit var marcaRepository: MarcaRepository
    private lateinit var fornecedorRepository: FornecedorRepository
    private lateinit var produtoRepository: ProdutoRepository
    private lateinit var materialRepository: MaterialRepository

    private lateinit var toolbarTitle: TextView
    private lateinit var imageViewBack: ImageView
    private lateinit var imageViewSave: ImageView
    private lateinit var imageViewDelete: ImageView

    private lateinit var editTextCodigo: EditText
    private lateinit var editTextNome: EditText
    private lateinit var editTextPrecoCusto: EditText
    private lateinit var editTextPrecoVenda: EditText
    private lateinit var editTextMarkup: EditText
    private lateinit var textViewMargemLucro: TextView
    private lateinit var layoutSelectFornecedor: LinearLayout
    private lateinit var textViewSelectedFornecedor: TextView
    private lateinit var layoutSelectCategoria: LinearLayout
    private lateinit var textViewSelectedCategoria: TextView
    private lateinit var layoutSelectMarca: LinearLayout
    private lateinit var textViewSelectedMarca: TextView
    private lateinit var editTextQuantidadeEstoque: EditText
    private lateinit var editTextImageUrl: EditText
    private lateinit var imageViewPreview: ImageView

    private lateinit var recyclerViewMateriais: RecyclerView
    private lateinit var btnAdicionarMaterial: Button
    private lateinit var materialLinhaAdapter: MaterialLinhaAdapter
    private val listaMateriaisVinculados = mutableListOf<MaterialProduto>()

    private var selectedFornecedorId: String? = null
    private var selectedCategoriaId: String? = null
    private var selectedMarcaId: String? = null
    private var produtoEmEdicao: Produto? = null
    private var isProdutoModificado = false
    private var isInitializingUI = true

    private val decimalFormat = DecimalFormat("0.00'%'")
    private var lastEditedField: FieldType = FieldType.NONE

    enum class FieldType {
        NONE, PRECO_CUSTO, PRECO_VENDA, MARKUP
    }

    private val fornecedorSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedFornecedor = result.data?.getParcelableExtra<Fornecedor>("selected_fornecedor")
            selectedFornecedor?.let {
                if (selectedFornecedorId != it.id) {
                    textViewSelectedFornecedor.text = it.razaoSocial
                    selectedFornecedorId = it.id
                    isProdutoModificado = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_produto)

        // Inicializar Views
        toolbarTitle = findViewById(R.id.textViewToolbarTitle)
        imageViewBack = findViewById(R.id.imageViewBack)
        imageViewSave = findViewById(R.id.imageViewSave)
        imageViewDelete = findViewById(R.id.imageViewDelete)
        editTextCodigo = findViewById(R.id.editTextCodigo)
        editTextNome = findViewById(R.id.editTextNome)
        editTextPrecoCusto = findViewById(R.id.editTextPrecoCusto)
        editTextPrecoVenda = findViewById(R.id.editTextPrecoVenda)
        editTextMarkup = findViewById(R.id.editTextMarkup)
        textViewMargemLucro = findViewById(R.id.textViewMargemLucro)
        layoutSelectFornecedor = findViewById(R.id.layoutSelectFornecedor)
        textViewSelectedFornecedor = findViewById(R.id.textViewSelectedFornecedor)
        layoutSelectCategoria = findViewById(R.id.layoutSelectCategoria)
        textViewSelectedCategoria = findViewById(R.id.textViewSelectedCategoria)
        layoutSelectMarca = findViewById(R.id.layoutSelectMarca)
        textViewSelectedMarca = findViewById(R.id.textViewSelectedMarca)
        editTextQuantidadeEstoque = findViewById(R.id.editTextQuantidadeEstoque)
        editTextImageUrl = findViewById(R.id.editTextImageUrl)
        imageViewPreview = findViewById(R.id.imageViewPreview)

        recyclerViewMateriais = findViewById(R.id.recyclerViewMateriaisDoProduto)
        btnAdicionarMaterial = findViewById(R.id.btnAdicionarMaterialAoProduto)

        // Inicializar os Repositórios
        produtoRepository = ProdutoRepository(applicationContext)
        categoriaRepository = CategoriaRepository(applicationContext)
        marcaRepository = MarcaRepository(applicationContext)
        fornecedorRepository = FornecedorRepository(applicationContext)
        materialRepository = MaterialRepository(applicationContext)

        // Configuração do RecyclerView e Adapter de Insumos
        recyclerViewMateriais.layoutManager = LinearLayoutManager(this)
        materialLinhaAdapter = MaterialLinhaAdapter(listaMateriaisVinculados) { posicao ->
            // Clique longo remove o insumo da lista
            listaMateriaisVinculados.removeAt(posicao)
            materialLinhaAdapter.notifyDataSetChanged()
            isProdutoModificado = true
            atualizarPrecoCustoBaseadoEmMateriais()
        }
        recyclerViewMateriais.adapter = materialLinhaAdapter

        produtoEmEdicao = intent.getParcelableExtra("produto") as? Produto

        if (produtoEmEdicao != null) {
            toolbarTitle.text = "Editar Produto"
            imageViewDelete.visibility = View.VISIBLE
            preencherCamposComProduto(produtoEmEdicao!!)
        } else {
            toolbarTitle.text = "Novo Produto"
            imageViewDelete.visibility = View.GONE
            setMarkupAndMargemLucro(0.0, 0.0)
            isInitializingUI = false
        }

        val allEditTexts = listOf(editTextCodigo, editTextNome, editTextPrecoCusto,
            editTextPrecoVenda, editTextMarkup, editTextQuantidadeEstoque, editTextImageUrl)

        allEditTexts.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!isInitializingUI) {
                        isProdutoModificado = true
                    }
                    if (editText == editTextPrecoCusto) {
                        if (editText.isFocused) {
                            lastEditedField = FieldType.PRECO_CUSTO
                            calcularValoresComBaseEmPrecoCusto()
                        }
                    } else if (editText == editTextPrecoVenda) {
                        if (editText.isFocused) {
                            lastEditedField = FieldType.PRECO_VENDA
                            calcularValoresComBaseEmPrecoVenda()
                        }
                    } else if (editText == editTextMarkup) {
                        if (editText.isFocused) {
                            lastEditedField = FieldType.MARKUP
                            calcularValoresComBaseEmMarkup()
                        }
                    } else if (editText == editTextImageUrl) {
                        val imageUrl = s?.toString()
                        if (!imageUrl.isNullOrEmpty()) {
                            Glide.with(this@DetalheProdutoActivity)
                                .load(imageUrl)
                                .placeholder(R.drawable.placeholder_image)
                                .error(R.drawable.placeholder_image)
                                .into(imageViewPreview)
                        } else {
                            imageViewPreview.setImageResource(R.drawable.placeholder_image)
                        }
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        btnAdicionarMaterial.setOnClickListener { abrirDialogoSelecaoMaterial() }

        layoutSelectFornecedor.setOnClickListener {
            val intent = Intent(this, ListaFornecedoresActivity::class.java)
            intent.putExtra("is_selection_mode", true)
            fornecedorSelectionLauncher.launch(intent)
        }

        layoutSelectCategoria.setOnClickListener {
            CategoriaSelectionDialog { selectedCategoria ->
                if (selectedCategoria?.id != selectedCategoriaId) {
                    textViewSelectedCategoria.text = selectedCategoria?.nome ?: "Selecione a Categoria"
                    selectedCategoriaId = selectedCategoria?.id
                    isProdutoModificado = true
                }
            }.show(supportFragmentManager, "CategoriaSelectionDialog")
        }

        layoutSelectMarca.setOnClickListener {
            MarcaSelectionDialog { selectedMarca ->
                if (selectedMarca?.id != selectedMarcaId) {
                    textViewSelectedMarca.text = selectedMarca?.nome ?: "Selecione a Marca"
                    selectedMarcaId = selectedMarca?.id
                    isProdutoModificado = true
                }
            }.show(supportFragmentManager, "MarcaSelectionDialog")
        }

        imageViewBack.setOnClickListener { handleBackPress() }
        imageViewSave.setOnClickListener { salvarProduto() }
        imageViewDelete.setOnClickListener { confirmarDelecao() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBackPress() }
        })
    }

    private fun preencherCamposComProduto(produto: Produto) {
        isInitializingUI = true

        editTextCodigo.setText(produto.codigo)
        editTextNome.setText(produto.nome)
        editTextPrecoCusto.setText(produto.precoCusto.toString())
        editTextPrecoVenda.setText(produto.precoVenda.toString())
        editTextQuantidadeEstoque.setText(produto.quantidadeEstoque.toString())
        editTextImageUrl.setText(produto.imageUrl)

        listaMateriaisVinculados.clear()
        listaMateriaisVinculados.addAll(produto.materiais)
        materialLinhaAdapter.notifyDataSetChanged()

        if (!produto.imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(produto.imageUrl).placeholder(R.drawable.placeholder_image).error(R.drawable.placeholder_image).into(imageViewPreview)
        } else {
            imageViewPreview.setImageResource(R.drawable.placeholder_image)
        }

        lifecycleScope.launch {
            try {
                if (produto.fornecedorId != null) {
                    val fornecedor = fornecedorRepository.getFornecedorByIdSingle(produto.fornecedorId)
                    textViewSelectedFornecedor.text = fornecedor?.razaoSocial ?: "Fabricação Própria"
                    selectedFornecedorId = fornecedor?.id
                } else {
                    textViewSelectedFornecedor.text = "Fabricação Própria"
                    selectedFornecedorId = null
                }

                val category = produto.categoriaId?.let { categoriaRepository.getCategoriaById(it) }
                textViewSelectedCategoria.text = category?.nome ?: "Selecione a Categoria"
                selectedCategoriaId = category?.id

                val brand = produto.marcaId?.let { marcaRepository.getMarcaById(it) }
                textViewSelectedMarca.text = brand?.nome ?: "Selecione a Marca"
                selectedMarcaId = brand?.id
            } catch (e: Exception) {
                Toast.makeText(this@DetalheProdutoActivity, "Erro ao carregar dados relacionados: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            } finally {
                isInitializingUI = false
                isProdutoModificado = false
            }
        }
    }

    private fun abrirDialogoSelecaoMaterial() {
        lifecycleScope.launch {
            val materiaisDisponiveis = materialRepository.getMateriaisSingle()

            if (materiaisDisponiveis.isEmpty()) {
                Toast.makeText(this@DetalheProdutoActivity, "Nenhum material base cadastrado!", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val nomes = materiaisDisponiveis.map { "${it.nome} (${currencyFormat.format(it.precoCusto)})" }.toTypedArray()

            AlertDialog.Builder(this@DetalheProdutoActivity)
                .setTitle("Escolha o Insumo")
                .setItems(nomes) { _, posicao ->
                    val materialEscolhido = materiaisDisponiveis[posicao]
                    solicitarQuantidadeDoMaterial(materialEscolhido)
                }
                .show()
        }
    }

    private fun solicitarQuantidadeDoMaterial(material: MaterialInsumo) {
        val input = EditText(this)

        // Configura o hint inteligente com base no tipo de medida do insumo
        input.hint = when (material.unidadeMedida.lowercase()) {
            "gramas", "g" -> "Ex: 50 ou 12.5 para gramas"
            "metros", "m" -> "Ex: 0.60 ou 1.5 para metros"
            "ml", "mililitros" -> "Ex: 15 ou 2.5 para ml"
            else -> "Ex: 2 para duas unidades"
        }

        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        AlertDialog.Builder(this)
            .setTitle("Quanto gasta de ${material.nome}?")
            .setMessage("Insumo controlado em: ${material.unidadeMedida.uppercase()}")
            .setView(input)
            .setPositiveButton("Adicionar") { _, _ ->
                val qtd = input.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                if (qtd > 0) {
                    // Cálculo: (Preço da caixa/rolo / Rendimento total da caixa/rolo) * quantidade real usada
                    val custoProporcional = (material.precoCusto / material.rendimentoTotal) * qtd

                    val materialVinculo = MaterialProduto(
                        materialId = material.id ?: "",
                        nomeMaterial = "${material.nome} (${material.unidadeMedida})",
                        quantidadeUsada = qtd,
                        custoCalculado = custoProporcional
                    )

                    listaMateriaisVinculados.add(materialVinculo)
                    materialLinhaAdapter.notifyDataSetChanged()
                    isProdutoModificado = true
                    atualizarPrecoCustoBaseadoEmMateriais()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun atualizarPrecoCustoBaseadoEmMateriais() {
        if (listaMateriaisVinculados.isNotEmpty()) {
            val somaCustos = listaMateriaisVinculados.sumOf { it.custoCalculado }
            isInitializingUI = true
            editTextPrecoCusto.setText("%.2f".format(somaCustos).replace(",", "."))
            isInitializingUI = false
            calcularValoresComBaseEmPrecoCusto()
        }
    }

    private fun setMarkupAndMargemLucro(markup: Double, margemLucro: Double) {
        if (markup.isNaN() || markup.isInfinite()) {
            editTextMarkup.setText("0.00")
        } else {
            val currentMarkupText = editTextMarkup.text.toString().replace(",", ".")
            val formattedMarkup = "%.2f".format(markup).replace(",", ".")
            if (currentMarkupText != formattedMarkup) {
                editTextMarkup.setText(formattedMarkup)
            }
        }
        editTextMarkup.setSelection(editTextMarkup.text.length)

        textViewMargemLucro.text = if (margemLucro.isNaN() || margemLucro.isInfinite()) {
            "0.00%"
        } else {
            decimalFormat.format(margemLucro)
        }
    }

    private fun calcularValoresComBaseEmPrecoCusto() {
        val custoStr = editTextPrecoCusto.text.toString().replace(",", ".")
        val custo = custoStr.toDoubleOrNull() ?: 0.0

        if (custo <= 0.0) {
            editTextPrecoCusto.error = "Preço de Custo deve ser maior que zero"
            editTextPrecoVenda.setText("")
            setMarkupAndMargemLucro(0.0, 0.0)
            return
        } else {
            editTextPrecoCusto.error = null
        }

        val vendaStr = editTextPrecoVenda.text.toString().replace(",", ".")
        val venda = vendaStr.toDoubleOrNull() ?: 0.0

        if (lastEditedField == FieldType.PRECO_CUSTO || lastEditedField == FieldType.NONE) {
            if (venda > 0) {
                val markup = ((venda / custo) - 1) * 100
                val margemLucro = ((venda - custo) / venda) * 100
                setMarkupAndMargemLucro(markup, margemLucro)
            } else {
                setMarkupAndMargemLucro(0.0, 0.0)
            }
        } else if (lastEditedField == FieldType.MARKUP) {
            calcularValoresComBaseEmMarkup()
        }
    }

    private fun calcularValoresComBaseEmPrecoVenda() {
        val custoStr = editTextPrecoCusto.text.toString().replace(",", ".")
        val vendaStr = editTextPrecoVenda.text.toString().replace(",", ".")
        val custo = custoStr.toDoubleOrNull() ?: 0.0
        val venda = vendaStr.toDoubleOrNull() ?: 0.0

        if (venda <= 0.0) {
            editTextPrecoVenda.error = "Preço de Venda deve ser maior que zero"
            setMarkupAndMargemLucro(0.0, 0.0)
            return
        } else {
            editTextPrecoVenda.error = null
        }

        if (custo > 0 && venda >= custo) {
            val markup = ((venda / custo) - 1) * 100
            val margemLucro = ((venda - custo) / venda) * 100
            setMarkupAndMargemLucro(markup, margemLucro)
        } else {
            setMarkupAndMargemLucro(0.0, 0.0)
        }
    }

    private fun calcularValoresComBaseEmMarkup() {
        val custoStr = editTextPrecoCusto.text.toString().replace(",", ".")
        val markupStr = editTextMarkup.text.toString().replace(",", ".")
        val custo = custoStr.toDoubleOrNull() ?: 0.0
        val markupPorcentagem = markupStr.toDoubleOrNull() ?: 0.0

        if (custo <= 0.0) {
            editTextPrecoCusto.error = "Preço de Custo deve ser maior que zero para calcular Markup/Venda"
            editTextPrecoVenda.setText("")
            setMarkupAndMargemLucro(0.0, 0.0)
            return
        } else {
            editTextPrecoCusto.error = null
        }

        if (markupPorcentagem < 0.0) {
            editTextMarkup.error = "Markup não pode ser negativo"
            editTextPrecoVenda.setText("")
            textViewMargemLucro.text = "0.00%"
            return
        } else {
            editTextMarkup.error = null
        }

        val precoVendaCalculado = custo * (1 + (markupPorcentagem / 100))
        editTextPrecoVenda.setText("%.2f".format(precoVendaCalculado).replace(",", "."))

        if (precoVendaCalculado > 0) {
            val margemLucro = ((precoVendaCalculado - custo) / precoVendaCalculado) * 100
            textViewMargemLucro.text = decimalFormat.format(margemLucro)
        } else {
            textViewMargemLucro.text = "0.00%"
        }
    }

    private fun salvarProduto() {
        val nome = editTextNome.text.toString().trim()
        val precoCustoStr = editTextPrecoCusto.text.toString().trim().replace(",", ".")
        val precoVendaStr = editTextPrecoVenda.text.toString().trim().replace(",", ".")
        val codigo = editTextCodigo.text.toString().trim().ifEmpty { null }
        val quantidadeEstoqueStr = editTextQuantidadeEstoque.text.toString().trim()
        val imageUrl = editTextImageUrl.text.toString().trim().ifEmpty { null }

        var isValid = true

        if (nome.isEmpty()) {
            editTextNome.error = "Nome é obrigatório"
            isValid = false
        }
        val precoCusto = precoCustoStr.toDoubleOrNull()
        if (precoCustoStr.isEmpty() || precoCusto == null || precoCusto <= 0) {
            editTextPrecoCusto.error = "Preço de Custo é obrigatório"
            isValid = false
        }

        val precoVenda = precoVendaStr.toDoubleOrNull()
        if (precoVendaStr.isEmpty() || precoVenda == null || precoVenda <= 0) {
            editTextPrecoVenda.error = "Preço de Venda é obrigatório"
            isValid = false
        }

        val quantidadeEstoque = quantidadeEstoqueStr.toIntOrNull()
        if (quantidadeEstoqueStr.isEmpty() || quantidadeEstoque == null || quantidadeEstoque < 0) {
            editTextQuantidadeEstoque.error = "Quantidade incorreta"
            isValid = false
        }

        if (selectedCategoriaId == null) {
            Toast.makeText(this, "Selecione uma categoria.", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (isValid) {
            val finalPrecoCusto = precoCusto ?: 0.0
            val finalPrecoVenda = precoVenda ?: 0.0
            val finalQuantidadeEstoque = quantidadeEstoque ?: 0

            val textoFornecedor = if (selectedFornecedorId != null) {
                textViewSelectedFornecedor.text.toString()
            } else {
                "Fabricação Própria"
            }

            lifecycleScope.launch {
                val produto: Produto = if (produtoEmEdicao != null) {
                    produtoEmEdicao!!.copy(
                        codigo = codigo,
                        nome = nome,
                        precoVenda = finalPrecoVenda,
                        precoCusto = finalPrecoCusto,
                        categoriaId = selectedCategoriaId!!,
                        fornecedorId = selectedFornecedorId,
                        marcaId = selectedMarcaId,
                        categoria = textViewSelectedCategoria.text.toString(),
                        fornecedor = textoFornecedor,
                        marca = textViewSelectedMarca.text.toString().let { if (it == "Selecione a Marca") null else it },
                        quantidadeEstoque = finalQuantidadeEstoque,
                        imageUrl = imageUrl,
                        materiais = listaMateriaisVinculados
                    )
                } else {
                    Produto(
                        id = null,
                        codigo = codigo,
                        nome = nome,
                        precoVenda = finalPrecoVenda,
                        precoCusto = finalPrecoCusto,
                        categoriaId = selectedCategoriaId!!,
                        fornecedorId = selectedFornecedorId,
                        marcaId = selectedMarcaId,
                        categoria = textViewSelectedCategoria.text.toString(),
                        fornecedor = textoFornecedor,
                        marca = textViewSelectedMarca.text.toString().let { if (it == "Selecione a Marca") null else it },
                        quantidadeEstoque = finalQuantidadeEstoque,
                        imageUrl = imageUrl,
                        materiais = listaMateriaisVinculados
                    )
                }

                try {
                    if (produtoEmEdicao != null) {
                        val success = produtoRepository.updateProduto(produto)
                        if (success) {
                            isProdutoModificado = false
                            finish()
                        }
                    } else {
                        val newId = produtoRepository.addProduto(produto)
                        if (newId != null) {
                            isProdutoModificado = false
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun confirmarDelecao() {
        produtoEmEdicao?.let { produto ->
            AlertDialog.Builder(this)
                .setTitle("Excluir Produto")
                .setPositiveButton("Sim") { _, _ ->
                    lifecycleScope.launch {
                        val success = produto.id?.let { produtoRepository.deleteProduto(it) } ?: false
                        if (success) {
                            isProdutoModificado = false
                            finish()
                        }
                    }
                }
                .setNegativeButton("Não", null)
                .show()
        }
    }

    private fun handleBackPress() {
        if (isProdutoModificado) {
            AlertDialog.Builder(this)
                .setTitle("Sair sem Salvar?")
                .setPositiveButton("Salvar") { _, _ -> salvarProduto() }
                .setNegativeButton("Não Salvar") { _, _ -> finish() }
                .show()
        } else {
            finish()
        }
    }

    private val currencyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
}