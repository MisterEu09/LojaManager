package com.example.lojamamager.firebase.repository

import android.content.Context
import android.util.Log
import com.example.lojamamager.data.Fornecedor
import com.example.lojamamager.auth.AuthManager
import com.example.lojamamager.firebase.FirebaseClient
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.lang.IllegalStateException

class FornecedorRepository(private val context: Context) {
    private val authManager: AuthManager by lazy { AuthManager(context) }

    private val fornecedoresCollection: CollectionReference by lazy {
        FirebaseClient.getFirestoreInstance().collection("fornecedores")
    }

    suspend fun addFornecedor(fornecedor: Fornecedor): String? {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("FornecedorRepository", "Erro ao obter empresaId para adicionar fornecedor: ${e.message}")
            return null
        }
        // --- INÍCIO: LOGS PARA DIAGNÓSTICO ---
        Log.d("FornecedorRepository", "Empresa ID obtido do AuthManager: $empresaId")
        // --- FIM: LOGS PARA DIAGNÓSTICO ---

        return try {
            // Garante que o fornecedor sendo adicionado tenha o empresaId correto do usuário logado
            val fornecedorComEmpresaId = fornecedor.copy(empresaId = empresaId)
            // --- INÍCIO: LOGS PARA DIAGNÓSTICO ---
            Log.d("FornecedorRepository", "Dados do fornecedor a ser adicionado: $fornecedorComEmpresaId")
            // --- FIM: LOGS PARA DIAGNÓSTICO ---
            val docRef = fornecedoresCollection.add(fornecedorComEmpresaId).await()
            docRef.id
        } catch (e: Exception) {
            Log.e("FornecedorRepository", "Erro ao adicionar fornecedor: ${e.message}")
            null
        }



    }

    suspend fun updateFornecedor(fornecedor: Fornecedor): Boolean {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("FornecedorRepository", "Erro ao obter empresaId para atualizar fornecedor: ${e.message}")
            return false
        }
        // Verifica se o fornecedor que está sendo atualizado realmente pertence à empresa do usuário logado
        if (fornecedor.empresaId != empresaId) {
            Log.e("FornecedorRepository", "Erro: Acesso negado ou Empresa ID inconsistente ao atualizar fornecedor.")
            return false
        }
        return try {
            fornecedor.id?.let { id ->
                fornecedoresCollection.document(id).set(fornecedor).await()
                true
            } ?: false
        } catch (e: Exception) {
            Log.e("FornecedorRepository", "Erro ao atualizar fornecedor: ${e.message}")
            false
        }
    }

    suspend fun deleteFornecedor(fornecedorId: String): Boolean {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("FornecedorRepository", "Erro ao obter empresaId para deletar fornecedor: ${e.message}")
            return false
        }
        return try {
            val fornecedorToDelete = getFornecedorByIdSingle(fornecedorId)
            // Verifica se o fornecedor a ser deletado realmente pertence à empresa do usuário logado
            if (fornecedorToDelete?.empresaId != empresaId) {
                Log.e("FornecedorRepository", "Erro: Fornecedor não pertence à empresa do usuário logado. Acesso negado.")
                return false
            }
            fornecedoresCollection.document(fornecedorId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("FornecedorRepository", "Erro ao deletar fornecedor: ${e.message}")
            false
        }
    }

    suspend fun getFornecedorByIdSingle(fornecedorId: String): Fornecedor? {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("FornecedorRepository", "Erro ao obter empresaId para buscar fornecedor por ID: ${e.message}")
            return null
        }
        return try {
            // Consulta o documento e verifica o empresaId
            val documentSnapshot = fornecedoresCollection.document(fornecedorId).get().await()
            val fornecedor = documentSnapshot.toObject(Fornecedor::class.java)

            if (fornecedor != null && fornecedor.empresaId == empresaId) {
                fornecedor.id = documentSnapshot.id // Garante que o ID do documento é setado no objeto
                fornecedor
            } else {
                Log.e("FornecedorRepository", "Erro: Fornecedor com ID $fornecedorId não encontrado ou não pertence ao empresaId $empresaId.")
                null
            }
        } catch (e: Exception) {
            Log.e("FornecedorRepository", "Erro ao obter fornecedor por ID $fornecedorId: ${e.message}")
            null
        }
    }


    fun getAllFornecedores(): Flow<List<Fornecedor>> = callbackFlow {
        val empresaId = try {
            // Obtenha o empresaId ANTES de configurar o listener do Firestore
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("FornecedorRepository", "Erro: Empresa ID não encontrado para getAllFornecedores: ${e.message}")
            trySend(emptyList()) // Envia uma lista vazia para indicar que não há dados
            close(e) // Fecha o flow com o erro
            return@callbackFlow // Sai da coroutine callbackFlow
        }

        // Se o empresaId foi obtido com sucesso, configure o listener
        val subscription = fornecedoresCollection
            .whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
            .orderBy("razaoSocial")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FornecedorRepository", "Erro ao observar fornecedores: ${e.message}")
                    close(e) // Fecha o flow com o erro
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val fornecedores = snapshot.toObjects(Fornecedor::class.java)
                    trySend(fornecedores)
                } else {
                    trySend(emptyList())
                }
            }
        // awaitClose deve ser a ÚLTIMA instrução no bloco callbackFlow
        awaitClose { subscription.remove() }
    }

    fun searchFornecedores(query: String): Flow<List<Fornecedor>> = callbackFlow {
        val empresaId = try {
            // Obtenha o empresaId ANTES de configurar o listener do Firestore
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("FornecedorRepository", "Erro: Empresa ID não encontrado para searchFornecedores: ${e.message}")
            trySend(emptyList()) // Envia uma lista vazia
            close(e) // Fecha o flow com o erro
            return@callbackFlow // Sai da coroutine callbackFlow
        }

        val firebaseQuery = fornecedoresCollection
            .whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
            .whereGreaterThanOrEqualTo("razaoSocial", query)
            .whereLessThanOrEqualTo("razaoSocial", query + "\uf8ff")
            .orderBy("razaoSocial")

        val subscription = firebaseQuery.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("FornecedorRepository", "Erro ao observar fornecedores pesquisados: ${e.message}")
                close(e) // Fecha o flow com o erro
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fornecedores = snapshot.toObjects(Fornecedor::class.java)
                trySend(fornecedores)
            } else {
                trySend(emptyList())
            }
        }
        // awaitClose deve ser a ÚLTIMA instrução no bloco callbackFlow
        awaitClose { subscription.remove() }
    }

    suspend fun getFornecedorByCnpjCpf(cnpjCpf: String): Fornecedor? {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e(
                "FornecedorRepository",
                "Erro ao obter empresaId para buscar fornecedor por CNPJ/CPF: ${e.message}"
            )
            return null
        }
        return try {
            fornecedoresCollection.whereEqualTo("cnpjCpf", cnpjCpf)
                .whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject(Fornecedor::class.java)
        } catch (e: Exception) {
            Log.e(
                "FornecedorRepository",
                "Erro ao obter fornecedor por CNPJ/CPF '$cnpjCpf': ${e.message}"
            )
            null
        }
    }


}