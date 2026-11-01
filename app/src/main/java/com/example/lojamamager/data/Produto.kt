package com.example.lojamamager.data

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Produto(
    @DocumentId
    var id: String? = null,
    val codigo: String? = null,
    val nome: String = "",
    val nomeLowerCase: String = "",
    val precoVenda: Double = 0.0,
    val precoCusto: Double = 0.0,
    val categoria: String = "",
    val categoriaId: String? = null,
    val fornecedorId: String? = null,
    val marcaId: String? = null,
    val fornecedor: String = "",
    val marca: String? = null,
    val descricao: String? = null,
    val quantidadeEstoque: Int = 0,
    val unidadeMedida: String? = null,
    val imageUrl: String? = null,
    val empresaId: String? = null,

    // *** NOVO CAMPO PARA OS MATERIAIS ***
    val materiais: List<MaterialProduto> = emptyList()

) : Parcelable