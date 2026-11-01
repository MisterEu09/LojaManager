package com.example.lojamamager.firebase.repository

import android.content.Context
import android.util.Log
import com.example.lojamamager.data.Marca // Certifique-se de que a classe Marca está importada
import com.example.lojamamager.auth.AuthManager
import com.example.lojamamager.firebase.FirebaseClient
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
// Remover import kotlinx.coroutines.launch se não for usado em outro lugar no arquivo.
// Nao precisamos de um launch aninhado aqui.
import kotlinx.coroutines.tasks.await
import java.lang.IllegalStateException

class MarcaRepository(private val context: Context) {
    private val authManager: AuthManager by lazy { AuthManager(context) }

    private val marcasCollection: CollectionReference by lazy {
        FirebaseClient.getFirestoreInstance().collection("marcas")
    }

    suspend fun addMarca(marca: Marca): String? {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("MarcaRepository", "Erro ao obter empresaId para adicionar marca: ${e.message}")
            return null
        }
        return try {
            val marcaComEmpresaId = marca.copy(empresaId = empresaId)
            val docRef = marcasCollection.add(marcaComEmpresaId).await()
            docRef.id
        } catch (e: Exception) {
            Log.e("MarcaRepository", "Erro ao adicionar marca: ${e.message}")
            null
        }
    }

    suspend fun updateMarca(marca: Marca): Boolean {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("MarcaRepository", "Erro ao obter empresaId para atualizar marca: ${e.message}")
            return false
        }
        // Verifica se a marca pertence à empresa do usuário logado
        if (marca.empresaId != empresaId) {
            Log.e("MarcaRepository", "Erro: Acesso negado ou Empresa ID inconsistente ao atualizar marca.")
            return false
        }
        return try {
            marca.id?.let { id ->
                marcasCollection.document(id).set(marca).await()
                true
            } ?: false
        } catch (e: Exception) {
            Log.e("MarcaRepository", "Erro ao atualizar marca: ${e.message}")
            false
        }
    }

    suspend fun deleteMarca(marcaId: String): Boolean {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("MarcaRepository", "Erro ao obter empresaId para deletar marca: ${e.message}")
            return false
        }
        return try {
            // Busca a marca para verificar se ela pertence à empresa do usuário logado
            val marcaToDelete = getMarcaById(marcaId) // Usando o método que já filtra por empresaId
            if (marcaToDelete?.empresaId != empresaId) {
                Log.e("MarcaRepository", "Erro: Marca não pertence à empresa do usuário logado. Acesso negado.")
                return false
            }
            marcasCollection.document(marcaId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("MarcaRepository", "Erro ao deletar marca: ${e.message}")
            false
        }
    }

    suspend fun getMarcaById(marcaId: String): Marca? {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("MarcaRepository", "Erro ao obter empresaId para buscar marca por ID: ${e.message}")
            return null
        }
        return try {
            marcasCollection.document(marcaId).get().await().toObject(Marca::class.java)?.apply {
                // Garante que a marca pertence à empresa do usuário logado
                if (this.empresaId != empresaId) {
                    Log.w("MarcaRepository", "Marca $marcaId encontrada, mas não pertence à empresa $empresaId.")
                    return null
                }
            }
        } catch (e: Exception) {
            Log.e("MarcaRepository", "Erro ao obter marca por ID $marcaId: ${e.message}")
            null
        }
    }

    // *** ALTERAÇÃO AQUI: getAllMarcas ***
    fun getAllMarcas(): Flow<List<Marca>> = callbackFlow {
        val empresaId = try {
            authManager.awaitEmpresaId() // Await empresaId diretamente no escopo do callbackFlow
        } catch (e: IllegalStateException) {
            Log.e("MarcaRepository", "Erro: Empresa ID não encontrado para getAllMarcas: ${e.message}")
            close(e) // Encerra o fluxo com erro
            return@callbackFlow // Sai do bloco callbackFlow
        }

        val subscription = marcasCollection
            .whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
            .orderBy("nome")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MarcaRepository", "Erro ao observar marcas: ${e.message}")
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val marcas = snapshot.toObjects(Marca::class.java)
                    trySend(marcas)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { subscription.remove() } // Agora awaitClose é a última instrução do callbackFlow
    }

    // *** ALTERAÇÃO AQUI: searchMarcas ***
    fun searchMarcas(query: String): Flow<List<Marca>> = callbackFlow {
        val empresaId = try {
            authManager.awaitEmpresaId() // Await empresaId diretamente no escopo do callbackFlow
        } catch (e: IllegalStateException) {
            Log.e("MarcaRepository", "Erro: Empresa ID não encontrado para searchMarcas: ${e.message}")
            trySend(emptyList())
            close(e)
            return@callbackFlow // Sai do bloco callbackFlow
        }

        val firebaseQuery = marcasCollection
            .whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
            .whereGreaterThanOrEqualTo("nome", query)
            .whereLessThanOrEqualTo("nome", query + "\uf8ff")
            .orderBy("nome")

        val subscription = firebaseQuery.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("MarcaRepository", "Erro ao observar marcas pesquisadas: ${e.message}")
                close(e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val marcas = snapshot.toObjects(Marca::class.java)
                trySend(marcas)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { subscription.remove() } // Agora awaitClose é a última instrução do callbackFlow
    }

    suspend fun getMarcaByName(nome: String): Marca? {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("MarcaRepository", "Erro ao obter empresaId para buscar marca por nome: ${e.message}")
            return null
        }
        return try {
            marcasCollection.whereEqualTo("nome", nome)
                .whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject(Marca::class.java)
        } catch (e: Exception) {
            Log.e("MarcaRepository", "Erro ao obter marca por nome '$nome': ${e.message}")
            null
        }
    }
}