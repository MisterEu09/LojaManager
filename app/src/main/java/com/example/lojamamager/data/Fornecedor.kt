package com.example.lojamamager.data

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Fornecedor(
    @DocumentId
    var id: String? = null,
    val cnpjCpf: String? = null,
    val razaoSocial: String = "",
    val email: String? = null, // NOVO CAMPO: Adicione o email
    val cep: String? = null,
    val endereco: String? = null,
    val numero: String? = null,
    val complemento: String? = null,
    val bairro: String? = null,
    val cidade: String? = null,
    val uf: String? = null,
    val contato: String? = null,
    val telefone: String? = null,
    val empresaId: String? = null
) : Parcelable