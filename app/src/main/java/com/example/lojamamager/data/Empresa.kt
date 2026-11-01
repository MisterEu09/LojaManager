package com.example.lojamamager.data

import com.google.firebase.firestore.DocumentId

data class Empresa(
    @DocumentId
    var id: String? = null,
    val nome: String = "",
    val cnpj: String? = null,
    val endereco: String? = null,
    val telefone: String? = null,
    val emailContato: String? = null,
    val dataCriacao: Long = System.currentTimeMillis()
    // Adicione outros campos relevantes para a sua empresa, como logo, etc.
)