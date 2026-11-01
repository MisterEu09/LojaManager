package com.example.lojamamager.firebase.repository

import android.content.Context
import android.util.Log
import com.example.lojamamager.data.Cliente
import com.example.lojamamager.auth.AuthManager
import com.example.lojamamager.firebase.FirebaseClient
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine // Importar combine
import kotlinx.coroutines.launch // Manter para o combine em searchClientes
import kotlinx.coroutines.tasks.await

class ClienteRepository(private val context: Context) {
    private val authManager: AuthManager by lazy { AuthManager(context) }

    private val clientesCollection: CollectionReference by lazy {
        FirebaseClient.getFirestoreInstance().collection("clientes")
    }

    suspend fun addCliente(cliente: Cliente): String? {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("ClienteRepository", "Erro ao obter empresaId para adicionar cliente: ${e.message}")
            return null
        }
        return try {
            val clienteComEmpresaId = cliente.copy(empresaId = empresaId)
            val docRef = if (clienteComEmpresaId.id.isNullOrEmpty()) {
                clientesCollection.add(clienteComEmpresaId).await()
            } else {
                clientesCollection.document(clienteComEmpresaId.id!!).set(clienteComEmpresaId).await()
                clientesCollection.document(clienteComEmpresaId.id!!) // Retornar a referência do documento existente
            }
            docRef.id
        } catch (e: Exception) {
            Log.e("ClienteRepository", "Erro ao adicionar/atualizar cliente: ${e.message}")
            null
        }
    }

    suspend fun updateCliente(cliente: Cliente): Boolean {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("ClienteRepository", "Erro ao obter empresaId para atualizar cliente: ${e.message}")
            return false
        }
        if (cliente.empresaId != empresaId) {
            Log.e("ClienteRepository", "Erro: Acesso negado ou Empresa ID inconsistente ao atualizar cliente.")
            return false
        }
        return try {
            cliente.id?.let { id ->
                clientesCollection.document(id).set(cliente).await()
                true
            } ?: false
        } catch (e: Exception) {
            Log.e("ClienteRepository", "Erro ao atualizar cliente: ${e.message}")
            false
        }
    }

    suspend fun deleteCliente(clienteId: String): Boolean {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("ClienteRepository", "Erro ao obter empresaId para deletar cliente: ${e.message}")
            return false
        }
        return try {
            val clienteToDelete = getClienteById(clienteId) // Usa o novo getClienteById que já filtra por empresaId
            if (clienteToDelete?.empresaId != empresaId) {
                Log.e("ClienteRepository", "Erro: Cliente não pertence à empresa do usuário logado. Acesso negado.")
                return false
            }
            clientesCollection.document(clienteId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("ClienteRepository", "Erro ao deletar cliente: ${e.message}")
            false
        }
    }

    suspend fun getClienteById(clienteId: String): Cliente? {
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("ClienteRepository", "Erro ao obter empresaId para buscar cliente por ID: ${e.message}")
            return null
        }
        return try {
            clientesCollection.document(clienteId).get().await().toObject(Cliente::class.java)?.apply {
                id = clienteId
                if (this.empresaId != empresaId) {
                    Log.e("ClienteRepository", "Erro: Cliente com ID $clienteId encontrada, mas não pertence ao empresaId $empresaId.")
                    return null // Garante que o cliente pertence à empresa
                }
            }
        } catch (e: Exception) {
            Log.e("ClienteRepository", "Erro ao obter cliente por ID $clienteId: ${e.message}")
            null
        }
    }

    // --- Métodos de Leitura com Flow (CORRIGIDOS) ---

    fun getAllClientes(): Flow<List<Cliente>> = callbackFlow {
        // Remover o 'launch' aninhado. A obtenção de empresaId ocorre diretamente aqui.
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("ClienteRepository", "Erro: Empresa ID não encontrado para getAllClientes: ${e.message}")
            trySend(emptyList())
            close(e)
            return@callbackFlow // Sai da callbackFlow
        }

        val subscription = clientesCollection
            .whereEqualTo("empresaId", empresaId)
            .orderBy("nomeRazaoSocial")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ClienteRepository", "Erro ao observar clientes: ${e.message}")
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val clientes = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Cliente::class.java)?.apply { id = doc.id }
                    }
                    trySend(clientes)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose {
            Log.d("ClienteRepository", "getAllClientes: Listener removido.")
            subscription.remove()
        }
    }

    fun searchClientes(searchQuery: String?, cidade: String?, bairro: String?): Flow<List<Cliente>> {
        return callbackFlow {
            val empresaId = try {
                authManager.awaitEmpresaId()
            } catch (e: IllegalStateException) {
                Log.e("ClienteRepository", "Erro: Empresa ID não encontrado para searchClientes: ${e.message}")
                trySend(emptyList())
                close(e)
                return@callbackFlow
            }

            // Os flows internos são OK, mas o `launch` ao redor deles precisa ser removido
            // e a combinação deve ser feita e coletada DENTRO do callbackFlow principal.
            val nameSearchFlow = callbackFlow<List<Cliente>> {
                var nameQuery: Query = clientesCollection.whereEqualTo("empresaId", empresaId)

                if (!searchQuery.isNullOrBlank()) {
                    nameQuery = nameQuery
                        .whereGreaterThanOrEqualTo("nomeRazaoSocial", searchQuery)
                        .whereLessThanOrEqualTo("nomeRazaoSocial", searchQuery + "\uf8ff")
                }

                if (!cidade.isNullOrBlank()) {
                    nameQuery = nameQuery.whereEqualTo("cidade", cidade)
                }
                if (!bairro.isNullOrBlank()) {
                    nameQuery = nameQuery.whereEqualTo("bairro", bairro)
                }

                nameQuery = nameQuery.orderBy("nomeRazaoSocial")

                val subscription = nameQuery.addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        close(e)
                        return@addSnapshotListener
                    }
                    val clientes = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Cliente::class.java)?.apply { id = doc.id }
                    } ?: emptyList()
                    trySend(clientes)
                }
                awaitClose { subscription.remove() }
            }

            val codeSearchFlow = callbackFlow<List<Cliente>> {
                var codeQuery: Query = clientesCollection.whereEqualTo("empresaId", empresaId)

                if (!searchQuery.isNullOrBlank()) {
                    codeQuery = codeQuery
                        .whereGreaterThanOrEqualTo("codigo", searchQuery)
                        .whereLessThanOrEqualTo("codigo", searchQuery + "\uf8ff")
                }

                if (!cidade.isNullOrBlank()) {
                    codeQuery = codeQuery.whereEqualTo("cidade", cidade)
                }
                if (!bairro.isNullOrBlank()) {
                    codeQuery = codeQuery.whereEqualTo("bairro", bairro)
                }

                codeQuery = codeQuery.orderBy("codigo")

                val subscription = codeQuery.addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        close(e)
                        return@addSnapshotListener
                    }
                    val clientes = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Cliente::class.java)?.apply { id = doc.id }
                    } ?: emptyList()
                    trySend(clientes)
                }
                awaitClose { subscription.remove() }
            }

            // Coletar os resultados dos fluxos internos e combiná-los aqui no callbackFlow principal
            // O `launch` aqui serve para iniciar a coleta dos sub-fluxos sem bloquear o callbackFlow principal
            launch {
                nameSearchFlow.combine(codeSearchFlow) { nameResults, codeResults ->
                    (nameResults + codeResults).distinctBy { it.id }
                }.collect { combinedList ->
                    trySend(combinedList)
                }
            }
            // O awaitClose do callbackFlow principal deve ser o ÚLTIMO,
            // então não deve haver nada depois da combinação/coleta.
            // No caso de múltiplos listeners como este, o awaitClose deve gerenciar todos eles.
            // Para `combine`, o awaitClose do `callbackFlow` principal irá cancelar os fluxos internos
            // quando o flow principal for cancelado.
            awaitClose {
                // Não é necessário remover listeners aqui para nameSearchFlow e codeSearchFlow
                // pois seus próprios awaitClose dentro de seus respectivos callbackFlows cuidarão disso.
                // Este awaitClose é para o callbackFlow externo, se ele tivesse um listener próprio.
                // Como não tem, ele simplesmente espera o cancelamento.
                Log.d("ClienteRepository", "searchClientes: Listener principal (combinado) removido.")
            }
        }
    }

    fun getClientesCount(): Flow<Int> = callbackFlow {
        // Remover o 'launch' aninhado.
        val empresaId = try {
            authManager.awaitEmpresaId()
        } catch (e: IllegalStateException) {
            Log.e("ClienteRepository", "Erro: Empresa ID não encontrado para getClientesCount: ${e.message}")
            trySend(0)
            close(e)
            return@callbackFlow
        }

        val subscription = clientesCollection
            .whereEqualTo("empresaId", empresaId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ClienteRepository", "Erro ao obter contagem de clientes: ${e.message}")
                    close(e)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose {
            Log.d("ClienteRepository", "getClientesCount: Listener removido.")
            subscription.remove()
        }
    }
}