package com.example.lojamamager.data

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Categoria(
    @DocumentId
    var id: String? = null, // ID agora é String
    val nome: String = "",
    val empresaId: String? = null // *** NOVO CAMPO ***
) : Parcelable {
    override fun toString(): String = nome
}