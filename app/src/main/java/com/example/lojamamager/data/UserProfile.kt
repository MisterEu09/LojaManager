package com.example.lojamamager.data

import com.google.firebase.firestore.DocumentId

// Certifique-se de que os campos correspondem aos nomes no Firestore
data class UserProfile(
    @DocumentId
    var uid: String? = null,
    val email: String? = null,
    val nome: String? = null,
    val empresaId: String? = null, // ID da empresa à qual o usuário pertence
    val role: String? = null,      // Ex: "admin", "vendedor", "gerente"
    val profilePictureUrl: String? = null // NOVO CAMPO: URL da foto de perfil
)