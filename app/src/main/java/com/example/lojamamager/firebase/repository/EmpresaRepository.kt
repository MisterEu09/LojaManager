package com.example.lojamamager.firebase.repository

import com.example.lojamamager.data.Empresa
import com.example.lojamamager.firebase.FirebaseClient
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.tasks.await

class EmpresaRepository {

    private val empresasCollection: CollectionReference by lazy {
        FirebaseClient.getFirestoreInstance().collection("empresas")
    }

    /**
     * Adiciona uma nova empresa ao Firestore e retorna o ID gerado.
     * Este ID será o empresaId a ser usado para todos os dados associados a esta empresa.
     */
    suspend fun addEmpresa(empresa: Empresa): String? {
        return try {
            val docRef = empresasCollection.add(empresa).await()
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Obtém uma empresa pelo seu ID.
     */
    suspend fun getEmpresaById(empresaId: String): Empresa? {
        return try {
            empresasCollection.document(empresaId).get().await().toObject(Empresa::class.java)?.apply {
                id = empresaId
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Atualiza os dados de uma empresa existente.
     */
    suspend fun updateEmpresa(empresa: Empresa): Boolean {
        return try {
            empresa.id?.let { id ->
                empresasCollection.document(id).set(empresa).await()
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Deleta uma empresa. CUIDADO: Deletar uma empresa significa que todos os dados
     * associados a ela (pedidos, clientes, produtos) ficarão órfãos ou inacessíveis,
     * a menos que você implemente uma exclusão em cascata (mais complexo e fora do escopo inicial).
     */
    suspend fun deleteEmpresa(empresaId: String): Boolean {
        return try {
            empresasCollection.document(empresaId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Você pode adicionar mais métodos aqui, como pesquisar empresas por nome, etc.
}