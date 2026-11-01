package com.example.lojamamager.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MaterialProduto(
    val materialId: String = "",
    val nomeMaterial: String = "", // Salvar o nome evita ter que fazer um 'get' toda hora
    val quantidadeUsada: Double = 0.0, // Ex: 0.5 (meio metro) ou 2.0 (duas unidades)
    val custoCalculado: Double = 0.0 // (precoCusto do material / rendimento) * quantidadeUsada
) : Parcelable