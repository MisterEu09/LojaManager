package com.example.lojamamager.util.com.example.lojamamager.firebase.repository

import android.content.Context
import com.example.lojamamager.data.MaterialInsumo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MaterialRepository(context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val collectionRef = firestore.collection("materiais")

    private suspend fun getEmpresaId(): String? {
        val uid = auth.currentUser?.uid ?: return null
        val userDoc = firestore.collection("users").document(uid).get().await()
        return userDoc.getString("empresaId")
    }

    fun getMateriais(): Flow<List<MaterialInsumo>> = callbackFlow {
        val empresaId = getEmpresaId()
        if (empresaId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = collectionRef
            .whereEqualTo("empresaId", empresaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(MaterialInsumo::class.java) ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addMaterial(material: MaterialInsumo): String? {
        val empresaId = getEmpresaId() ?: return null
        val docRef = collectionRef.document()
        val materialComEmpresa = material.copy(id = docRef.id, empresaId = empresaId)
        docRef.set(materialComEmpresa).await()
        return docRef.id
    }

    suspend fun updateMaterial(material: MaterialInsumo): Boolean {
        val id = material.id ?: return false
        return try {
            collectionRef.document(id).set(material).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteMaterial(id: String): Boolean {
        return try {
            collectionRef.document(id).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
    // Adicione esta função junto com as outras na sua classe MaterialRepository
    suspend fun getMateriaisSingle(): List<MaterialInsumo> {
        val empresaId = getEmpresaId() ?: return emptyList()
        return try {
            val snapshot = collectionRef
                .whereEqualTo("empresaId", empresaId)
                .get()
                .await()
            snapshot.toObjects(MaterialInsumo::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}