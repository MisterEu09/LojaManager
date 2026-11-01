package com.example.lojamamager.data

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class TopSellingProductData(
    // Esta é a data class que vem diretamente do repositório,
    // com os dados brutos sem o "rank" de exibição.
    @DocumentId
    val productId: String,
    val productName: String,
    val quantitySold: Int,
    val totalValue: Double
) : Parcelable