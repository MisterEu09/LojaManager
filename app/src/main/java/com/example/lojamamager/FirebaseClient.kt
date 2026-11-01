package com.example.lojamamager.firebase // Ou o pacote que preferir

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

object FirebaseClient {
    private val db: FirebaseFirestore by lazy {
        val firestore = FirebaseFirestore.getInstance()
        // Opcional: Ativar persistência offline se necessário
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Habilita o cache offline
            .build()
        firestore.firestoreSettings = settings
        firestore
    }

    fun getFirestoreInstance(): FirebaseFirestore {
        return db
    }
}