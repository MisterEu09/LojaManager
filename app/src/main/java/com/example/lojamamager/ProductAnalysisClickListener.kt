package com.example.lojamamager

import com.example.lojamamager.data.ProductAnalysisItem
import com.example.lojamamager.data.Produto

// Defina esta interface para que a Activity possa reagir aos cliques nos itens de análise
interface ProductAnalysisClickListener {
    fun onProdutoClick(produto: Produto)
    fun onProductAnalysisClick(productId: String)
}