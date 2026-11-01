package com.example.lojamamager

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.lojamamager.data.Produto

class DetalheCatalogoProdutoActivity : BaseActivity() {

    private lateinit var toolbarTitle: TextView
    private lateinit var imageViewBack: ImageView
    private lateinit var imageViewProdutoDetalhe: ImageView
    private lateinit var textViewNomeDetalhe: TextView
    private lateinit var textViewPrecoDetalhe: TextView
    private lateinit var textViewCategoriaDetalhe: TextView
    private lateinit var textViewMarcaDetalhe: TextView
    private lateinit var textViewFornecedorDetalhe: TextView
    private lateinit var textViewDescricaoDetalhe: TextView
    private lateinit var textViewCodigoDetalhe: TextView
    private lateinit var textViewUnidadeMedidaDetalhe: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_catalogo_produto)

        // Inicializar Views
        toolbarTitle = findViewById(R.id.textViewToolbarTitleDetalheCatalogo)
        imageViewBack = findViewById(R.id.imageViewBackDetalheCatalogo)
        imageViewProdutoDetalhe = findViewById(R.id.imageViewProdutoDetalhe)
        textViewNomeDetalhe = findViewById(R.id.textViewNomeDetalhe)
        textViewPrecoDetalhe = findViewById(R.id.textViewPrecoDetalhe)
        textViewCategoriaDetalhe = findViewById(R.id.textViewCategoriaDetalhe)
        textViewMarcaDetalhe = findViewById(R.id.textViewMarcaDetalhe)
        textViewFornecedorDetalhe = findViewById(R.id.textViewFornecedorDetalhe)
        textViewDescricaoDetalhe = findViewById(R.id.textViewDescricaoDetalhe)
        textViewCodigoDetalhe = findViewById(R.id.textViewCodigoDetalhe)
        textViewUnidadeMedidaDetalhe = findViewById(R.id.textViewUnidadeMedidaDetalhe)

        // Configurar botão de voltar
        imageViewBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Recuperar o produto passado pela Intent
        val produto = intent.getParcelableExtra<Produto>("produto")

        produto?.let {
            toolbarTitle.text = it.nome // Título da toolbar com o nome do produto
            textViewNomeDetalhe.text = it.nome
            textViewPrecoDetalhe.text = "R$ %.2f".format(it.precoVenda)
            textViewCategoriaDetalhe.text = "Categoria: ${it.categoria}"

            // Exibir marca se disponível
            if (!it.marca.isNullOrBlank()) {
                textViewMarcaDetalhe.text = "Marca: ${it.marca}"
                textViewMarcaDetalhe.visibility = View.VISIBLE
            } else {
                textViewMarcaDetalhe.visibility = View.GONE
            }

            // Exibir fornecedor se disponível
            if (!it.fornecedor.isNullOrBlank()) {
                textViewFornecedorDetalhe.text = "Fornecedor: ${it.fornecedor}"
                textViewFornecedorDetalhe.visibility = View.VISIBLE
            } else {
                textViewFornecedorDetalhe.visibility = View.GONE
            }

            // Exibir descrição se disponível
            if (!it.descricao.isNullOrBlank()) {
                textViewDescricaoDetalhe.text = it.descricao
                textViewDescricaoDetalhe.visibility = View.VISIBLE
            } else {
                textViewDescricaoDetalhe.visibility = View.GONE
            }

            // Exibir código se disponível
            if (!it.codigo.isNullOrBlank()) {
                textViewCodigoDetalhe.text = "Código: ${it.codigo}"
                textViewCodigoDetalhe.visibility = View.VISIBLE
            } else {
                textViewCodigoDetalhe.visibility = View.GONE
            }

            // Exibir unidade de medida se disponível
            if (!it.unidadeMedida.isNullOrBlank()) {
                textViewUnidadeMedidaDetalhe.text = "Unidade: ${it.unidadeMedida}"
                textViewUnidadeMedidaDetalhe.visibility = View.VISIBLE
            } else {
                textViewUnidadeMedidaDetalhe.visibility = View.GONE
            }

            // Carregar imagem usando Glide
            if (!it.imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(it.imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(imageViewProdutoDetalhe)
            } else {
                imageViewProdutoDetalhe.setImageResource(R.drawable.placeholder_image)
            }
        } ?: run {
            Toast.makeText(this, "Produto não encontrado.", Toast.LENGTH_SHORT).show()
            finish() // Fecha a activity se o produto for nulo
        }
    }
}