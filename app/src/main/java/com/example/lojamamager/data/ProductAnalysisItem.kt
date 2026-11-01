package com.example.lojamamager.data

data class ProductAnalysisItem(
    // **MUDANÇA AQUI:** productId agora é String
    val productId: String,
    val productName: String,
    val productCode: String?,
    val quantitySold: Int,
    val suggestion: String // "MANTER" ou "REVER/TROCAR"
)