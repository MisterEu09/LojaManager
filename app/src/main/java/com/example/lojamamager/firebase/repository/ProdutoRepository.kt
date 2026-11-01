package com.example.lojamamager.firebase.repository

import android.content.Context
import android.util.Log // Importe Log
import com.example.lojamamager.data.Produto
import com.example.lojamamager.auth.AuthManager
import com.example.lojamamager.firebase.FirebaseClient
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch // Importar launch para usar dentro de callbackFlow
import kotlinx.coroutines.tasks.await
import java.lang.IllegalStateException // Importe IllegalStateException

class ProdutoRepository(private val context: Context) {
private val authManager: AuthManager by lazy { AuthManager(context) }

private val produtosCollection: CollectionReference by lazy {
FirebaseClient.getFirestoreInstance().collection("produtos")
}

// A função getEmpresaIdAtualSuspended() pode ser simplificada
// ou removida, pois awaitEmpresaId já faz o trabalho.
// Vou mantê-la como um wrapper, mas é importante que ela propague a exceção.
private suspend fun getEmpresaIdActual(): String {
return authManager.awaitEmpresaId()
}

// --- Métodos de CRUD Básicos ---
suspend fun addProduto(produto: Produto): String? {
val empresaId = try {
getEmpresaIdActual() // Chame a versão suspensa
} catch (e: IllegalStateException) {
Log.e("ProdutoRepository", "Erro: Empresa ID não encontrado para adicionar produto: ${e.message}")
return null
}
return try {
val produtoComEmpresaId = produto.copy(empresaId = empresaId)
val docRef = produtosCollection.add(produtoComEmpresaId).await()
Log.d("ProdutoRepository", "Produto adicionado com ID: ${docRef.id}")
docRef.id
} catch (e: Exception) {
Log.e("ProdutoRepository", "Erro ao adicionar produto: ${e.message}")
null
}
}

suspend fun updateProduto(produto: Produto): Boolean {
val empresaId = try {
getEmpresaIdActual() // Chame a versão suspensa
} catch (e: IllegalStateException) {
Log.e("ProdutoRepository", "Erro: Empresa ID não encontrado para atualizar produto: ${e.message}")
return false
}
if (produto.empresaId != empresaId) {
Log.e("ProdutoRepository", "Erro: Acesso negado. Produto ID '${produto.id}' não pertence ao empresaId '${empresaId}' do usuário logado.")
return false
}
return try {
produto.id?.let { id ->
produtosCollection.document(id).set(produto).await()
Log.d("ProdutoRepository", "Produto atualizado com ID: $id")
true
} ?: false
} catch (e: Exception) {
Log.e("ProdutoRepository", "Erro ao atualizar produto com ID '${produto.id}': ${e.message}")
false
}
}

suspend fun deleteProduto(produtoId: String): Boolean {
val empresaId = try {
getEmpresaIdActual() // Chame a versão suspensa
} catch (e: IllegalStateException) {
Log.e("ProdutoRepository", "Erro: Empresa ID não encontrado para deletar produto: ${e.message}")
return false
}
return try {
val produtoToDelete = getProdutoById(produtoId) // Usa o getProdutoById que já filtra por empresaId
if (produtoToDelete?.empresaId != empresaId) {
Log.e("ProdutoRepository", "Erro: Acesso negado. Produto com ID '$produtoId' não pertence ao empresaId '${empresaId}' do usuário logado.")
return false
}
produtosCollection.document(produtoId).delete().await()
Log.d("ProdutoRepository", "Produto deletado com ID: $produtoId")
true
} catch (e: Exception) {
Log.e("ProdutoRepository", "Erro ao deletar produto com ID '$produtoId': ${e.message}")
false
}
}

suspend fun getProdutoById(produtoId: String): Produto? {
val empresaId = try {
getEmpresaIdActual() // Chame a versão suspensa
} catch (e: IllegalStateException) {
Log.e("ProdutoRepository", "Erro: Empresa ID não encontrado para obter produto por ID: ${e.message}")
return null
}
return try {
val documentSnapshot = produtosCollection.document(produtoId).get().await()
val produto = documentSnapshot.toObject(Produto::class.java)
if (produto?.empresaId != empresaId) {
Log.w("ProdutoRepository", "Produto com ID '$produtoId' encontrado, mas não pertence ao empresaId '${empresaId}' do usuário logado. Retornando null.")
return null // Garante que o produto pertence à empresa
}
Log.d("ProdutoRepository", "Produto obtido por ID '$produtoId': ${produto?.nome}")
produto
} catch (e: Exception) {
Log.e("ProdutoRepository", "Erro ao obter produto por ID '$produtoId': ${e.message}")
null
}
}

// --- Métodos de Leitura com Flow ---
fun getAllProdutos(): Flow<List<Produto>> = callbackFlow {
// Lançar uma corrotina para obter o empresaId de forma assíncrona
// e somente então configurar o listener do Firestore
val empresaId = try {
authManager.awaitEmpresaId()
} catch (e: IllegalStateException) {
Log.e("ProdutoRepository", "Erro: Empresa ID não disponível para getAllProdutos: ${e.message}")
trySend(emptyList()) // Envia uma lista vazia antes de fechar
close(e) // Encerra o fluxo com erro
return@callbackFlow // Sai da callbackFlow
}

val subscription = produtosCollection
.whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
.orderBy("nome")
.addSnapshotListener { snapshot, e ->
if (e != null) {
Log.e("ProdutoRepository", "Erro ao observar todos os produtos: ${e.message}")
close(e)
return@addSnapshotListener
}
if (snapshot != null) {
val produtos = snapshot.toObjects(Produto::class.java)
Log.d("ProdutoRepository", "getAllProdutos: ${produtos.size} produtos recebidos.")
trySend(produtos)
} else {
trySend(emptyList())
}
}
awaitClose {
Log.d("ProdutoRepository", "getAllProdutos: Listener removido.")
subscription.remove()
}
}

fun searchProdutos(
query: String?,
categoriaId: String?,
marcaId: String?,
fornecedorId: String?,
searchByCodeExact: Boolean = false
): Flow<List<Produto>> = callbackFlow {
val empresaId = try {
authManager.awaitEmpresaId()
} catch (e: IllegalStateException) {
Log.e("ProdutoRepository", "Erro: Empresa ID não disponível para searchProdutos: ${e.message}")
trySend(emptyList())
close(e)
return@callbackFlow
}

var baseQuery: Query = produtosCollection.whereEqualTo("empresaId", empresaId) // Filtro inicial

if (!categoriaId.isNullOrBlank()) {
baseQuery = baseQuery.whereEqualTo("categoriaId", categoriaId)
Log.d("ProdutoRepository", "searchProdutos: Filtrando por categoriaId: $categoriaId")
}
if (!marcaId.isNullOrBlank()) {
baseQuery = baseQuery.whereEqualTo("marcaId", marcaId)
Log.d("ProdutoRepository", "searchProdutos: Filtrando por marcaId: $marcaId")
}
if (!fornecedorId.isNullOrBlank()) {
baseQuery = baseQuery.whereEqualTo("fornecedorId", fornecedorId)
Log.d("ProdutoRepository", "searchProdutos: Filtrando por fornecedorId: $fornecedorId")
}

val finalQuery = if (!query.isNullOrBlank()) {
if (searchByCodeExact) {
Log.d("ProdutoRepository", "searchProdutos: Buscando por código exato: $query")
baseQuery.whereEqualTo("codigo", query).orderBy("codigo")
} else {
Log.d("ProdutoRepository", "searchProdutos: Buscando por nome (início com): $query")
baseQuery
.whereGreaterThanOrEqualTo("nome", query.capitalize()) // capitalize() para buscar por início maiúsculo
.whereLessThanOrEqualTo("nome", query.capitalize() + "\uf8ff")
.orderBy("nome")
}
} else {
Log.d("ProdutoRepository", "searchProdutos: Sem query de nome/código. Ordenando por nome.")
baseQuery.orderBy("nome")
}

val subscription = finalQuery.addSnapshotListener { snapshot, e ->
if (e != null) {
Log.e("ProdutoRepository", "Erro ao observar produtos pesquisados: ${e.message}")
close(e)
return@addSnapshotListener
}
if (snapshot != null) {
val produtos = snapshot.toObjects(Produto::class.java)
Log.d("ProdutoRepository", "searchProdutos: ${produtos.size} produtos encontrados para query '$query'.")
trySend(produtos)
} else {
trySend(emptyList())
}
}
awaitClose {
Log.d("ProdutoRepository", "searchProdutos: Listener removido.")
subscription.remove()
}
}

suspend fun getProdutoByCodigo(codigo: String): Produto? {
val empresaId = try {
getEmpresaIdActual()
} catch (e: IllegalStateException) {
Log.e("ProdutoRepository", "Erro: Empresa ID não disponível para getProdutoByCodigo: ${e.message}")
return null
}
return try {
val snapshot = produtosCollection
.whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
.whereEqualTo("codigo", codigo)
.limit(1)
.get()
.await()
val produto = snapshot.documents.firstOrNull()?.toObject(Produto::class.java)
if (produto != null) {
Log.d("ProdutoRepository", "Produto encontrado por código '$codigo': ${produto.nome}")
} else {
Log.d("ProdutoRepository", "Produto não encontrado por código '$codigo'.")
}
produto
} catch (e: Exception) {
Log.e("ProdutoRepository", "Erro ao obter produto por código '$codigo': ${e.message}")
null
}
}

// --- Métodos de Estoque ---
fun getProdutosEmEstoque(): Flow<List<Produto>> = callbackFlow {
val empresaId = try {
authManager.awaitEmpresaId()
} catch (e: IllegalStateException) {
Log.e("ProdutoRepository", "Erro: Empresa ID não disponível para getProdutosEmEstoque: ${e.message}")
trySend(emptyList())
close(e)
return@callbackFlow
}

val subscription = produtosCollection
.whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
.whereGreaterThan("quantidadeEstoque", 0)
.orderBy("quantidadeEstoque")
.addSnapshotListener { snapshot, e ->
if (e != null) {
Log.e("ProdutoRepository", "Erro ao observar produtos em estoque: ${e.message}")
close(e)
return@addSnapshotListener
}
if (snapshot != null) {
val produtos = snapshot.toObjects(Produto::class.java)
Log.d("ProdutoRepository", "getProdutosEmEstoque: ${produtos.size} produtos em estoque recebidos.")
trySend(produtos)
} else {
trySend(emptyList())
}
}
awaitClose {
Log.d("ProdutoRepository", "getProdutosEmEstoque: Listener removido.")
subscription.remove()
}
}

fun getTotalProdutosEmEstoque(): Flow<Int> = callbackFlow {
val empresaId = try {
authManager.awaitEmpresaId()
} catch (e: IllegalStateException) {
Log.e("ProdutoRepository", "Erro: Empresa ID não disponível para getTotalProdutosEmEstoque: ${e.message}")
trySend(0)
close(e)
return@callbackFlow
}

val subscription = produtosCollection
.whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
.addSnapshotListener { snapshot, e ->
if (e != null) {
Log.e("ProdutoRepository", "Erro ao observar total de produtos em estoque: ${e.message}")
close(e)
return@addSnapshotListener
}
val total = snapshot?.documents?.sumOf { it.toObject(Produto::class.java)?.quantidadeEstoque ?: 0 } ?: 0
Log.d("ProdutoRepository", "getTotalProdutosEmEstoque: Total = $total")
trySend(total)
}
awaitClose {
Log.d("ProdutoRepository", "getTotalProdutosEmEstoque: Listener removido.")
subscription.remove()
}
}

fun getValorTotalEstoque(): Flow<Double> = callbackFlow {
val empresaId = try {
authManager.awaitEmpresaId()
} catch (e: IllegalStateException) {
Log.e("ProdutoRepository", "Erro: Empresa ID não disponível para getValorTotalEstoque: ${e.message}")
trySend(0.0)
close(e)
return@callbackFlow
}

val subscription = produtosCollection
.whereEqualTo("empresaId", empresaId) // Adicionado filtro de empresaId
.addSnapshotListener { snapshot, e ->
if (e != null) {
Log.e("ProdutoRepository", "Erro ao observar valor total do estoque: ${e.message}")
close(e)
return@addSnapshotListener
}
val total = snapshot?.documents?.sumOf { doc ->
val produto = doc.toObject(Produto::class.java)
(produto?.quantidadeEstoque ?: 0) * (produto?.precoCusto ?: 0.0)
} ?: 0.0
Log.d("ProdutoRepository", "getValorTotalEstoque: Total = $total")
trySend(total)
}
awaitClose {
Log.d("ProdutoRepository", "getValorTotalEstoque: Listener removido.")
subscription.remove()
}
}

suspend fun getProdutoByIdSingle(productId: String): Produto? {
val empresaId = try {
getEmpresaIdActual() // Chame a versão suspensa
} catch (e: IllegalStateException) {
Log.e("ProdutoRepository", "Erro: Empresa ID não encontrado para obter produto por ID (single): ${e.message}")
return null
}
return try {
val documentSnapshot = produtosCollection.document(productId).get().await()
val produto = documentSnapshot.toObject(Produto::class.java)
if (produto?.empresaId != empresaId) {
Log.w("ProdutoRepository", "Produto com ID '$productId' encontrado, mas não pertence ao empresaId '${empresaId}' do usuário logado. Retornando null.")
return null // Garante que o produto pertence à empresa
}
Log.d("ProdutoRepository", "Produto obtido por ID (single) '$productId': ${produto?.nome}")
produto
} catch (e: Exception) {
Log.e("ProdutoRepository", "Erro ao obter produto por ID (single) '$productId': ${e.message}")
null
}
}
}