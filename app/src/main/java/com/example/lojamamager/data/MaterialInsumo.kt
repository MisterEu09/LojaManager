package com.example.lojamamager.data

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class MaterialInsumo(
    @DocumentId
    var id: String? = null,
    val codigo: String = "", // *** NOVO: Para buscar/atualizar via manutenção de CSV ***
    val nome: String = "",
    val precoCusto: Double = 0.0,
    val quantidadeEstoque: Double = 0.0,
    val unidadeMedida: String = "",
    val rendimentoTotal: Double = 1.0,
    val quantidadeMinima: Double = 0.0, // *** NOVO: Alinhado com seu CSV ***
    val empresaId: String? = null
) : Parcelable