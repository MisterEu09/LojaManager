package com.example.lojamamager.data

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class ItemPedido(
    @DocumentId
    var id: String? = null,
    val pedidoId: String? = null,
    val produtoId: String? = null,
    val nomeProduto: String = "",
    val codigoProduto: String? = null,
    val quantidade: Int = 0,
    val precoUnitario: Double = 0.0,
    val empresaId: String = "" // <-- NOVO CAMPO
) : Parcelable