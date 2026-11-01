package com.example.lojamamager.firebase.repository

import android.content.Context
import android.util.Log
import com.example.lojamamager.data.Categoria
import com.example.lojamamager.auth.AuthManager
import com.example.lojamamager.firebase.FirebaseClient
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.lang.IllegalStateException

class CategoriaRepository(private val context: Context) {
    private val authManager: AuthManager by lazy { AuthManager(context) }

    private val categoriasCollection: CollectionReference by lazy {
        FirebaseClient.getFirestoreInstance().collection("categorias")
    }

    suspend fun addCategoria(categoria: Categoria): String? {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("CategoriaRepository", "Erro ao obter empresaId para adicionar categoria: ${e.message}")
            return null
        }
        return try {
            val categoriaComEmpresaId = categoria.copy(empresaId = empresaId)
            val docRef = categoriasCollection.add(categoriaComEmpresaId).await()
            docRef.id
        } catch (e: Exception) {
            Log.e("CategoriaRepository", "Erro ao adicionar categoria: ${e.message}")
            null
        }
    }

    suspend fun updateCategoria(categoria: Categoria): Boolean {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("CategoriaRepository", "Erro ao obter empresaId para atualizar categoria: ${e.message}")
            return false
        }
        if (categoria.empresaId != empresaId) {
            Log.e("CategoriaRepository", "Erro: Acesso negado ou Empresa ID inconsistente ao atualizar categoria.")
            return false
        }
        return try {
            categoria.id?.let { id ->
                categoriasCollection.document(id).set(categoria).await()
                true
            } ?: false
        } catch (e: Exception) {
            Log.e("CategoriaRepository", "Erro ao atualizar categoria: ${e.message}")
            false
        }
    }

    suspend fun deleteCategoria(categoriaId: String): Boolean {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("CategoriaRepository", "Erro ao obter empresaId para deletar categoria: ${e.message}")
            return false
        }
        return try {
            val categoriaToDelete = getCategoriaById(categoriaId) // Usa o novo getCategoriaById que já filtra por empresaId
            if (categoriaToDelete?.empresaId != empresaId) {
                Log.e("CategoriaRepository", "Erro: Categoria não pertence à empresa do usuário logado. Acesso negado.")
                return false
            }
            categoriasCollection.document(categoriaId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("CategoriaRepository", "Erro ao deletar categoria: ${e.message}")
            false
        }
    }

    suspend fun getCategoriaById(categoriaId: String): Categoria? {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("CategoriaRepository", "Erro ao obter empresaId para buscar categoria por ID: ${e.message}")
            return null
        }
        return try {
            categoriasCollection.document(categoriaId).get().await().toObject(Categoria::class.java)?.apply {
                if (this.empresaId != empresaId) {
                    Log.e("CategoriaRepository", "Erro: Categoria com ID $categoriaId encontrada, mas não pertence ao empresaId $empresaId.")
                    return null // Garante que a categoria pertence à empresa
                }
            }
        } catch (e: Exception) {
            Log.e("CategoriaRepository", "Erro ao obter categoria por ID $categoriaId: ${e.message}")
            null
        }
    }

    // *** ALTERAÇÃO AQUI ***
    fun getAllCategorias(): Flow<List<Categoria>> = callbackFlow {
        val empresaId = try {
            // Await empresaId diretamente no escopo do callbackFlow
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("CategoriaRepository", "Erro: Empresa ID não encontrado para getAllCategorias: ${e.message}")
            close(e) // Encerra o fluxo com erro
            return@callbackFlow // Sai do bloco callbackFlow
        }

        val subscription = categoriasCollection
            .whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
            .orderBy("nome")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CategoriaRepository", "Erro ao observar categorias: ${e.message}")
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val categorias = snapshot.toObjects(Categoria::class.java)
                    trySend(categorias)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { subscription.remove() } // Agora awaitClose é a última instrução do callbackFlow
    }

    // *** ALTERAÇÃO AQUI ***
    fun searchCategorias(query: String): Flow<List<Categoria>> = callbackFlow {
        val empresaId = try {
            // Await empresaId diretamente no escopo do callbackFlow
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("CategoriaRepository", "Erro: Empresa ID não encontrado para searchCategorias: ${e.message}")
            close(e) // Encerra o fluxo com erro
            return@callbackFlow // Sai do bloco callbackFlow
        }

        val normalizedQuery = query.trim().lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val firebaseQuery = categoriasCollection
            .whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
            .whereGreaterThanOrEqualTo("nome", normalizedQuery)
            .whereLessThanOrEqualTo("nome", normalizedQuery + "\uf8ff")
            .orderBy("nome")

        val subscription = firebaseQuery.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("CategoriaRepository", "Erro ao observar categorias pesquisadas: ${e.message}")
                close(e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val categorias = snapshot.toObjects(Categoria::class.java)
                trySend(categorias)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { subscription.remove() } // Agora awaitClose é a última instrução do callbackFlow
    }

    suspend fun getCategoriaByName(nome: String): Categoria? {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("CategoriaRepository", "Erro ao obter empresaId para buscar categoria por nome: ${e.message}")
            return null
        }
        return try {
            categoriasCollection.whereEqualTo("nome", nome)
                .whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject(Categoria::class.java)
        } catch (e: Exception) {
            Log.e("CategoriaRepository", "Erro ao obter categoria por nome '$nome': ${e.message}")
            null
        }
    }
}