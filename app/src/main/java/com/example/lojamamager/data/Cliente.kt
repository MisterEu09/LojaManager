package com.example.lojamamager.data

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Cliente(
    @DocumentId
    var id: String? = null,
    val codigo: String? = null,
    val cnpjCpf: String? = null,
    val nomeRazaoSocial: String = "",
    val cep: String? = null,
    val endereco: String? = null,
    val numero: String? = null,
    val complemento: String? = null,
    val bairro: String? = null,
    val cidade: String? = null,
    val uf: String? = null,
    val telefone: String? = null,
    val email: String? = null, // NOVO CAMPO: Adiciona o e-mail na data class
    val empresaId: String? = null
) : Parcelable