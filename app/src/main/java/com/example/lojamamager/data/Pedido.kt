package com.example.lojamamager.data

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pedido(
    @DocumentId
    var id: String? = null,
    val codigoPedido: String = "",
    val clienteId: String? = null,
    val dataCompra: Long = System.currentTimeMillis(),
    val dataEntregaPrevista: Long? = null,
    val valorTotal: Double = 0.0,
    val situacao: String = "Pendente",
    val condicaoPagamento: String = "",
    val empresaId: String? = null // *** NOVO CAMPO ***
) : Parcelable