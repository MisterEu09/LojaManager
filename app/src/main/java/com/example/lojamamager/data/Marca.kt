package com.example.lojamamager.data

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Marca(
    @DocumentId
    var id: String? = null,
    val nome: String = "",
    val empresaId: String = "" // <-- NOVO CAMPO
) : Parcelable