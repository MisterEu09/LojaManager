package com.example.lojamamager.firebase.repository

import android.content.Context
import android.util.Log
import com.example.lojamamager.data.ItemPedido
import com.example.lojamamager.data.Pedido
import com.example.lojamamager.data.PedidoComCliente
import com.example.lojamamager.data.TopSellingProductData
import com.example.lojamamager.auth.AuthManager
import com.example.lojamamager.data.PedidoWithItems
import com.example.lojamamager.firebase.FirebaseClient
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date // Importar Date para o campo dataCompra/dataEntregaPrevista se for Long

// Assumindo que Pedido, ItemPedido, PedidoComCliente, TopSellingProductData, ClienteRepository, ProdutoRepository existem
// e que suas propriedades de data são do tipo Long (timestamps Unix)

class PedidoRepository(private val context: Context) {

private val authManager: AuthManager by lazy { AuthManager(context) }

private val pedidosCollection: CollectionReference by lazy {
FirebaseClient.getFirestoreInstance().collection("pedidos")
}
private val itensPedidoCollection: CollectionReference by lazy {
FirebaseClient.getFirestoreInstance().collection("itens_pedido")
}

private val clientesRepository: ClienteRepository by lazy { ClienteRepository(context) }
private val produtoRepository: ProdutoRepository by lazy { ProdutoRepository(context) }

private suspend fun getEmpresaIdAtual(): String {
return authManager.awaitEmpresaId()
}

// Método para adicionar/atualizar um pedido.
// Se o pedido.id for nulo, adiciona. Se não, atualiza.
// Retorna o ID do documento ou null em caso de erro.
suspend fun addOrUpdatePedido(pedido: Pedido): String? {
val empresaId = try {
authManager.awaitEmpresaId()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro ao obter empresaId para adicionar/atualizar pedido: ${e.message}")
return null
}
return try {
val pedidoComEmpresaId = pedido.copy(empresaId = empresaId)
val docRef = if (pedidoComEmpresaId.id.isNullOrEmpty()) {
pedidosCollection.add(pedidoComEmpresaId).await()
} else {
pedidosCollection.document(pedidoComEmpresaId.id!!).set(pedidoComEmpresaId).await()
pedidosCollection.document(pedidoComEmpresaId.id!!) // Retorna a referência do documento existente
}
Log.d("PedidoRepository", "Pedido adicionado/atualizado com ID: ${docRef.id}")
docRef.id
} catch (e: Exception) {
Log.e("PedidoRepository", "Erro ao adicionar/atualizar pedido: ${e.message}")
null
}
}

// --- Métodos de CRUD Básicos e Itens de Pedido ---

suspend fun insertPedido(pedido: Pedido): String? {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro ao obter empresaId para inserir pedido: ${e.message}")
return null
}
return try {
val pedidoComEmpresaId = pedido.copy(empresaId = empresaId)
val docRef = pedidosCollection.add(pedidoComEmpresaId).await()
docRef.id
} catch (e: Exception) {
Log.e("PedidoRepository", "Erro ao inserir pedido: ${e.message}")
null
}
}

suspend fun updatePedido(pedido: Pedido): Boolean {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro ao obter empresaId para atualizar pedido: ${e.message}")
return false
}
if (pedido.empresaId != empresaId) {
Log.e("PedidoRepository", "Erro: Acesso negado ou Empresa ID inconsistente ao atualizar pedido.")
return false
}
return try {
pedido.id?.let { id ->
pedidosCollection.document(id).set(pedido).await()
true
} ?: false
} catch (e: Exception) {
Log.e("PedidoRepository", "Erro ao atualizar pedido: ${e.message}")
false
}
}

suspend fun deletePedido(pedidoId: String): Boolean {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro ao obter empresaId para deletar pedido: ${e.message}")
return false
}
return try {
val pedidoToDelete = getPedidoById(pedidoId) // Já usa o empresaId
if (pedidoToDelete?.empresaId != empresaId) {
Log.e("PedidoRepository", "Erro: Pedido não pertence à empresa do usuário logado. Acesso negado.")
return false
}

val batch = FirebaseClient.getFirestoreInstance().batch()
val itemsToDelete = itensPedidoCollection
.whereEqualTo("pedidoId", pedidoId)
.whereEqualTo("empresaId", empresaId) // Garante que deleta apenas itens da empresa
.get().await()

for (doc in itemsToDelete.documents) {
batch.delete(doc.reference)
}
batch.delete(pedidosCollection.document(pedidoId))
batch.commit().await()
true
} catch (e: Exception) {
Log.e("PedidoRepository", "Erro ao deletar pedido: ${e.message}")
false
}
}

suspend fun getPedidoById(pedidoId: String): Pedido? {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro ao obter empresaId para buscar pedido por ID: ${e.message}")
return null
}
return try {
pedidosCollection.document(pedidoId)
.get().await().toObject(Pedido::class.java)?.apply {
id = pedidoId
if (this.empresaId != empresaId) {
Log.w("PedidoRepository", "Pedido $pedidoId encontrado, mas não pertence à empresa $empresaId.")
return null
}
}
} catch (e: Exception) {
Log.e("PedidoRepository", "Erro ao buscar pedido por ID: ${e.message}")
null
}
}

suspend fun insertItemPedido(itemPedido: ItemPedido): String? {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro ao obter empresaId para inserir item de pedido: ${e.message}")
return null
}
return try {
val itemComEmpresaId = itemPedido.copy(empresaId = empresaId)
val docRef = itensPedidoCollection.add(itemComEmpresaId).await()
docRef.id
} catch (e: Exception) {
Log.e("PedidoRepository", "Erro ao inserir item de pedido: ${e.message}")
null
}
}

suspend fun updateItemPedido(itemPedido: ItemPedido): Boolean {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro ao obter empresaId para atualizar item de pedido: ${e.message}")
return false
}
if (itemPedido.empresaId != empresaId) {
Log.e("PedidoRepository", "Erro: Acesso negado ou Empresa ID inconsistente ao atualizar item de pedido.")
return false
}
return try {
itemPedido.id?.let { id ->
itensPedidoCollection.document(id).set(itemPedido).await()
true
} ?: false
} catch (e: Exception) {
Log.e("PedidoRepository", "Erro ao atualizar item de pedido: ${e.message}")
false
}
}

suspend fun deleteItemPedido(itemPedidoId: String): Boolean {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro ao obter empresaId para deletar item de pedido: ${e.message}")
return false
}
return try {
val itemToDelete = itensPedidoCollection.document(itemPedidoId).get().await().toObject(ItemPedido::class.java)
if (itemToDelete?.empresaId != empresaId) { // Verifica se o item pertence à empresa
Log.e("PedidoRepository", "Erro: Item de pedido não pertence à empresa do usuário logado. Acesso negado.")
return false
}
itensPedidoCollection.document(itemPedidoId).delete().await()
true
} catch (e: Exception) {
Log.e("PedidoRepository", "Erro ao deletar item de pedido: ${e.message}")
false
}
}

fun getItensPedidoForPedido(pedidoId: String): Flow<List<ItemPedido>> = callbackFlow {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro: Empresa ID não encontrado para getItensPedidoForPedido: ${e.message}")
trySend(emptyList())
close(e)
return@callbackFlow
}

val subscription = itensPedidoCollection
.whereEqualTo("pedidoId", pedidoId)
.whereEqualTo("empresaId", empresaId)
.addSnapshotListener { snapshot, e ->
if (e != null) {
Log.e("PedidoRepository", "Erro ao observar itens de pedido para $pedidoId: ${e.message}")
close(e)
return@addSnapshotListener
}
if (snapshot != null) {
val itens = snapshot.documents.mapNotNull { doc ->
doc.toObject(ItemPedido::class.java)?.apply { id = doc.id }
}
trySend(itens)
} else {
trySend(emptyList())
}
}
awaitClose {
Log.d("PedidoRepository", "getItensPedidoForPedido: Listener removido.")
subscription.remove()
}
}

suspend fun deleteItensPedidoForPedido(pedidoId: String): Boolean {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro ao obter empresaId para deletar itens de pedido para o pedido: ${e.message}")
return false
}
return try {
val batch = FirebaseClient.getFirestoreInstance().batch()
itensPedidoCollection
.whereEqualTo("pedidoId", pedidoId)
.whereEqualTo("empresaId", empresaId)
.get().await().documents.forEach { doc -> // CORREÇÃO AQUI: 'doc' já é o DocumentSnapshot
batch.delete(doc.reference)
}
batch.commit().await()
true
} catch (e: Exception) {
Log.e("PedidoRepository", "Erro ao deletar itens de pedido para o pedido: ${e.message}")
false
}
}

fun getAllPedidosComCliente(): Flow<List<PedidoComCliente>> = callbackFlow {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro: Empresa ID não encontrado para getAllPedidosComCliente: ${e.message}")
trySend(emptyList())
close(e)
return@callbackFlow
}

val pedidoSubscription = pedidosCollection
.whereEqualTo("empresaId", empresaId)
.orderBy("codigoPedido", Query.Direction.ASCENDING)
.addSnapshotListener { pedidosSnapshot, pedidoError ->
if (pedidoError != null) {
Log.e("PedidoRepository", "Erro ao observar pedidos para getAllPedidosComCliente: ${pedidoError.message}")
close(pedidoError)
return@addSnapshotListener
}

if (pedidosSnapshot != null) {
launch {
val pedidosComClienteList = mutableListOf<PedidoComCliente>()
for (pedidoDoc in pedidosSnapshot.documents) {
val pedido = pedidoDoc.toObject(Pedido::class.java)?.apply { id = pedidoDoc.id } ?: continue
if (pedido.empresaId != empresaId) {
continue
}
val cliente = pedido.clienteId?.let { clientesRepository.getClienteById(it) }
if (cliente != null) {
pedidosComClienteList.add(PedidoComCliente(pedido, cliente))
}
}
trySend(pedidosComClienteList.toList())
}
} else {
trySend(emptyList())
}
}
awaitClose {
Log.d("PedidoRepository", "getAllPedidosComCliente: Listener removido.")
pedidoSubscription.remove()
}
}

fun searchPedidos(query: String?, status: String?, clienteId: String?): Flow<List<PedidoComCliente>> = callbackFlow {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro: Empresa ID não encontrado para searchPedidos: ${e.message}")
trySend(emptyList())
close(e)
return@callbackFlow
}

launch {
var baseQuery: Query = pedidosCollection.whereEqualTo("empresaId", empresaId)
var pedidosComClienteList = emptyList<PedidoComCliente>()

if (!clienteId.isNullOrBlank()) {
baseQuery = baseQuery.whereEqualTo("clienteId", clienteId)
pedidosComClienteList = fetchPedidosAndClientsOnce(baseQuery)
} else if (!query.isNullOrBlank()) {
val clientesFiltradosPorNome = clientesRepository.searchClientes(query, null, null).first()
val idsDosClientesFiltrados = clientesFiltradosPorNome.mapNotNull { it.id }

if (idsDosClientesFiltrados.isNotEmpty()) {
val tempPedidos = mutableListOf<PedidoComCliente>()
// Firebase 'in' clause supports up to 10 values
for (chunk in idsDosClientesFiltrados.chunked(10)) {
var chunkQuery = pedidosCollection
.whereIn("clienteId", chunk)
.whereEqualTo("empresaId", empresaId)
if (!status.isNullOrBlank() && status != "Todos") {
chunkQuery = chunkQuery.whereEqualTo("situacao", status)
}
tempPedidos.addAll(fetchPedidosAndClientsOnce(chunkQuery))
}
pedidosComClienteList = tempPedidos.distinctBy { it.pedido.id }.sortedBy { it.pedido.codigoPedido }
} else if (query.matches("\\d+".toRegex())) {
// Se a query é numérica, tenta buscar por codigoPedido
val codigoPedidoLong = query.toLongOrNull()
if (codigoPedidoLong != null) {
baseQuery = baseQuery
.whereEqualTo("codigoPedido", codigoPedidoLong) // Busca exata por código de pedido
pedidosComClienteList = fetchPedidosAndClientsOnce(baseQuery)
} else {
// Se não é um número válido, retorna lista vazia
trySend(emptyList())
awaitClose { }
return@launch
}
} else {
// Se query não é clienteId, não é numérica, e não encontrou clientes, então lista vazia.
trySend(emptyList())
awaitClose { }
return@launch
}
}

if (!status.isNullOrBlank() && status != "Todos") {
// Aplica o filtro de status à baseQuery original se não foi aplicado antes
// ou se a busca por clienteId/query já preencheu pedidosComClienteList
if (pedidosComClienteList.isEmpty() && clienteId.isNullOrBlank() && query.isNullOrBlank()) {
// Se não há filtros de cliente/query, aplica o status à baseQuery
baseQuery = baseQuery.whereEqualTo("situacao", status)
pedidosComClienteList = fetchPedidosAndClientsOnce(baseQuery)
} else if (pedidosComClienteList.isNotEmpty()) {
// Se já temos uma lista de pedidos (por clienteId ou nome), filtramos por status na memória
pedidosComClienteList = pedidosComClienteList.filter { it.pedido.situacao == status }
}
}

// Se pedidosComClienteList ainda estiver vazia e não houve filtros de cliente/query,
// significa que precisamos buscar todos os pedidos da empresa com o filtro de status (se houver).
if (pedidosComClienteList.isEmpty() && clienteId.isNullOrBlank() && query.isNullOrBlank()) {
pedidosComClienteList = fetchPedidosAndClientsOnce(baseQuery)
}


trySend(pedidosComClienteList)
}
awaitClose {
Log.d("PedidoRepository", "searchPedidos: Flow one-shot finalizado.")
}
}

private suspend fun fetchPedidosAndClientsOnce(queryObj: Query): List<PedidoComCliente> {
val snapshot = queryObj.get().await()
val pedidosComClienteList = mutableListOf<PedidoComCliente>()
val empresaId = getEmpresaIdAtual()
for (pedidoDoc in snapshot.documents) {
val pedido = pedidoDoc.toObject(Pedido::class.java)?.apply { id = pedidoDoc.id } ?: continue
if (pedido.empresaId == empresaId) {
val cliente = pedido.clienteId?.let { clientesRepository.getClienteById(it) }
if (cliente != null) {
pedidosComClienteList.add(PedidoComCliente(pedido, cliente))
}
}
}
return pedidosComClienteList
}

suspend fun insertPedidoWithItems(pedido: Pedido, items: List<ItemPedido>): String? {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro ao obter empresaId para inserir pedido com itens: ${e.message}")
return null
}
return try {
val db = FirebaseClient.getFirestoreInstance()
val batch = db.batch()

val newPedidoRef = pedidosCollection.document()
val pedidoToSave = pedido.copy(id = newPedidoRef.id, empresaId = empresaId)
batch.set(newPedidoRef, pedidoToSave)

for (item in items) {
val newItemRef = itensPedidoCollection.document()
batch.set(newItemRef, item.copy(id = newItemRef.id, pedidoId = pedidoToSave.id!!, empresaId = empresaId))
}

batch.commit().await()
pedidoToSave.id
} catch (e: Exception) {
Log.e("PedidoRepository", "Erro ao inserir pedido com itens: ${e.message}")
null
}
}

suspend fun updatePedidoWithItems(pedido: Pedido, items: List<ItemPedido>): Boolean {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro ao obter empresaId para atualizar pedido com itens: ${e.message}")
return false
}
if (pedido.empresaId != empresaId) {
Log.e("PedidoRepository", "Erro: Acesso negado ou Empresa ID inconsistente ao atualizar pedido com itens.")
return false
}
return try {
val db = FirebaseClient.getFirestoreInstance()
val batch = db.batch()

pedido.id?.let { pedidoId ->
batch.set(pedidosCollection.document(pedidoId), pedido.copy(empresaId = empresaId))

val currentItemsSnapshot = itensPedidoCollection
.whereEqualTo("pedidoId", pedidoId)
.whereEqualTo("empresaId", empresaId)
.get().await()

val existingItemIds = currentItemsSnapshot.documents.map { it.id }.toSet()
val newItemIdsInList = items.mapNotNull { it.id }.toSet()

for (doc in currentItemsSnapshot.documents) {
if (doc.id !in newItemIdsInList) {
batch.delete(doc.reference)
}
}

for (item in items) {
if (item.id.isNullOrEmpty() || item.id !in existingItemIds) {
val newItemRef = itensPedidoCollection.document()
batch.set(newItemRef, item.copy(id = newItemRef.id, pedidoId = pedidoId, empresaId = empresaId))
} else {
batch.set(itensPedidoCollection.document(item.id!!), item.copy(empresaId = empresaId))
}
}
batch.commit().await()
true
} ?: false
} catch (e: Exception) {
Log.e("PedidoRepository", "Erro ao atualizar pedido com itens: ${e.message}")
false
}
}

fun getLastPedido(): Flow<Pedido?> = callbackFlow {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro: Empresa ID não encontrado para getLastPedido: ${e.message}")
trySend(null)
close(e)
return@callbackFlow
}

val subscription = pedidosCollection
.whereEqualTo("empresaId", empresaId)
.orderBy("codigoPedido", Query.Direction.DESCENDING)
.limit(1)
.addSnapshotListener { snapshot, e ->
if (e != null) {
Log.e("PedidoRepository", "Erro ao observar último pedido: ${e.message}")
close(e)
return@addSnapshotListener
}
val lastPedido = snapshot?.documents?.firstOrNull()?.toObject(Pedido::class.java)?.apply {
id = snapshot.documents.firstOrNull()?.id
}
trySend(lastPedido)
}
awaitClose {
Log.d("PedidoRepository", "getLastPedido: Listener removido.")
subscription.remove()
}
}

fun getPedidosWithItemsByDateRange(startDate: Long?, endDate: Long?): Flow<List<PedidoWithItems>> = callbackFlow {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro: Empresa ID não encontrado para getPedidosWithItemsByDateRange: ${e.message}")
trySend(emptyList())
close(e)
return@callbackFlow
}

var firebaseQuery: Query = pedidosCollection.whereEqualTo("empresaId", empresaId)

startDate?.let { firebaseQuery = firebaseQuery.whereGreaterThanOrEqualTo("dataCompra", it) }
endDate?.let { firebaseQuery = firebaseQuery.whereLessThanOrEqualTo("dataCompra", it) }

firebaseQuery = firebaseQuery.orderBy("dataCompra", Query.Direction.DESCENDING)

val pedidoSubscription = firebaseQuery.addSnapshotListener { pedidosSnapshot, pedidoError ->
if (pedidoError != null) {
Log.e("PedidoRepository", "Erro ao observar pedidos para getPedidosWithItemsByDateRange: ${pedidoError.message}")
close(pedidoError)
return@addSnapshotListener
}

if (pedidosSnapshot != null) {
launch {
val pedidosWithItemsList = mutableListOf<PedidoWithItems>()
for (pedidoDoc in pedidosSnapshot.documents) {
val pedido = pedidoDoc.toObject(Pedido::class.java)?.apply { id = pedidoDoc.id } ?: continue
if (pedido.empresaId != empresaId) {
continue
}
val itens = pedido.id?.let { pedidoIdNonNull ->
itensPedidoCollection
.whereEqualTo("pedidoId", pedidoIdNonNull)
.whereEqualTo("empresaId", empresaId)
.get().await().mapNotNull { doc ->
doc.toObject(ItemPedido::class.java)?.apply { id = doc.id }
}
} ?: emptyList()
pedidosWithItemsList.add(PedidoWithItems(pedido, itens))
}
trySend(pedidosWithItemsList.toList())
}
} else {
trySend(emptyList())
}
}
awaitClose {
Log.d("PedidoRepository", "getPedidosWithItemsByDateRange: Listener removido.")
pedidoSubscription.remove()
}
}

fun getTotalPedidosByDateRange(startDate: Long?, endDate: Long?): Flow<Int> = callbackFlow {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro: Empresa ID não encontrado para getTotalPedidosByDateRange: ${e.message}")
trySend(0)
close(e)
return@callbackFlow
}

var firebaseQuery: Query = pedidosCollection.whereEqualTo("empresaId", empresaId)

startDate?.let { firebaseQuery = firebaseQuery.whereGreaterThanOrEqualTo("dataCompra", it) }
endDate?.let { firebaseQuery = firebaseQuery.whereLessThanOrEqualTo("dataCompra", it) }

val subscription = firebaseQuery.addSnapshotListener { snapshot, e ->
if (e != null) {
Log.e("PedidoRepository", "Erro ao obter total de pedidos por data: ${e.message}")
close(e)
return@addSnapshotListener
}
trySend(snapshot?.size() ?: 0)
}
awaitClose {
Log.d("PedidoRepository", "getTotalPedidosByDateRange: Listener removido.")
subscription.remove()
}
}

fun getUpcomingDeliveries(startDate: Long, endDate: Long): Flow<Int> = callbackFlow {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro: Empresa ID não encontrado para getUpcomingDeliveries: ${e.message}")
trySend(0)
close(e)
return@callbackFlow
}

val subscription = pedidosCollection
.whereEqualTo("empresaId", empresaId)
.whereEqualTo("situacao", "Pendente")
.whereGreaterThanOrEqualTo("dataEntregaPrevista", startDate)
.whereLessThanOrEqualTo("dataEntregaPrevista", endDate)
.addSnapshotListener { snapshot, e ->
if (e != null) {
Log.e("PedidoRepository", "Erro ao obter entregas futuras: ${e.message}")
close(e)
return@addSnapshotListener
}
trySend(snapshot?.size() ?: 0)
}
awaitClose {
Log.d("PedidoRepository", "getUpcomingDeliveries: Listener removido.")
subscription.remove()
}
}

fun getTopSellingProducts(limit: Int, startDate: Long?, endDate: Long?): Flow<List<TopSellingProductData>> = callbackFlow {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro: Empresa ID não encontrado para getTopSellingProducts: ${e.message}")
trySend(emptyList())
close(e)
return@callbackFlow
}

val pedidoSubscription = pedidosCollection
.whereEqualTo("empresaId", empresaId)
.addSnapshotListener { pedidosSnapshot, pedidosError ->
if (pedidosError != null) {
Log.e("PedidoRepository", "Erro ao observar pedidos para top produtos: ${pedidosError.message}")
close(pedidosError)
return@addSnapshotListener
}
if (pedidosSnapshot != null) {
launch {
val validPedidoIds = pedidosSnapshot.documents.mapNotNull { doc ->
val pedido = doc.toObject(Pedido::class.java)?.apply { id = doc.id }
if (pedido != null &&
(startDate == null || pedido.dataCompra >= startDate) &&
(endDate == null || pedido.dataCompra <= endDate) &&
pedido.empresaId == empresaId
) {
pedido.id
} else {
null
}
}.toSet()

val allItemsSnapshot = itensPedidoCollection
.whereEqualTo("empresaId", empresaId)
.get().await()

val filteredItems = allItemsSnapshot.documents.mapNotNull { doc ->
doc.toObject(ItemPedido::class.java)?.apply { id = doc.id }
}.filter { item ->
item.pedidoId in validPedidoIds && item.empresaId == empresaId
}

val productSalesMap = mutableMapOf<String, TopSellingProductData>()
for (item in filteredItems) {
val productId = item.produtoId
if (productId != null) {
val productName = produtoRepository.getProdutoByIdSingle(productId)?.nome ?: "Produto Desconhecido"

val currentData = productSalesMap[productId] ?: TopSellingProductData(
productId = productId,
productName = productName,
quantitySold = 0,
totalValue = 0.0
)
productSalesMap[productId] = currentData.copy(
quantitySold = currentData.quantitySold + item.quantidade,
totalValue = currentData.totalValue + (item.quantidade * item.precoUnitario)
)
}
}
val topProducts = productSalesMap.values.sortedByDescending { it.quantitySold }.take(limit)
trySend(topProducts)
}
} else {
trySend(emptyList())
}
}
awaitClose {
Log.d("PedidoRepository", "getTopSellingProducts: Listener removido.")
pedidoSubscription.remove()
}
}

fun getAllItemsPedidosByDateRange(startDate: Long?, endDate: Long?): Flow<List<ItemPedido>> = callbackFlow {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro: Empresa ID não encontrado para getAllItemsPedidosByDateRange: ${e.message}")
trySend(emptyList())
close(e)
return@callbackFlow
}

val pedidosSubscription = pedidosCollection
.whereEqualTo("empresaId", empresaId)
.addSnapshotListener { pedidosSnapshot, pedidosError ->
if (pedidosError != null) {
Log.e("PedidoRepository", "Erro ao observar pedidos para itens por data: ${pedidosError.message}")
close(pedidosError)
return@addSnapshotListener
}

if (pedidosSnapshot != null) {
launch {
val validPedidoIds = pedidosSnapshot.documents.mapNotNull { doc ->
val pedido = doc.toObject(Pedido::class.java)?.apply { id = doc.id }
if (pedido != null &&
(startDate == null || pedido.dataCompra >= startDate) &&
(endDate == null || pedido.dataCompra <= endDate) &&
pedido.empresaId == empresaId
) {
pedido.id
} else {
null
}
}.toSet()

val allItems = mutableListOf<ItemPedido>()

val itemsSnapshot = itensPedidoCollection
.whereEqualTo("empresaId", empresaId)
.get().await()
itemsSnapshot.documents.mapNotNull { doc ->
doc.toObject(ItemPedido::class.java)?.apply { id = doc.id }
}.filter { item ->
item.pedidoId in validPedidoIds && item.empresaId == empresaId
}.let { allItems.addAll(it) }

trySend(allItems)
}
} else {
trySend(emptyList())
}
}
awaitClose {
Log.d("PedidoRepository", "getAllItemsPedidosByDateRange: Listener removido.")
pedidosSubscription.remove()
}
}

fun getPedidosParaAgendaByDateRange(startTime: Long?, endTime: Long?): Flow<List<PedidoComCliente>> = callbackFlow {
val empresaId = try {
getEmpresaIdAtual()
} catch (e: IllegalStateException) {
Log.e("PedidoRepository", "Erro: Empresa ID não encontrado para getPedidosParaAgendaByDateRange: ${e.message}")
trySend(emptyList())
close(e)
return@callbackFlow
}

val agendaQuery = pedidosCollection
.whereEqualTo("empresaId", empresaId)
.whereGreaterThanOrEqualTo("dataEntregaPrevista", startTime ?: 0L)
.whereLessThanOrEqualTo("dataEntregaPrevista", endTime ?: Long.MAX_VALUE)
.orderBy("dataEntregaPrevista", Query.Direction.ASCENDING)
.addSnapshotListener { snapshot, e ->
if (e != null) {
Log.e("PedidoRepository", "Erro ao observar pedidos para agenda: ${e.message}")
close(e)
return@addSnapshotListener
}
if (snapshot != null) {
launch {
val pedidosComClienteList = mutableListOf<PedidoComCliente>()
for (pedidoDoc in snapshot.documents) {
val pedido = pedidoDoc.toObject(Pedido::class.java)?.apply { id = pedidoDoc.id } ?: continue
if (pedido.empresaId != empresaId) {
continue
}
val cliente = pedido.clienteId?.let { clientesRepository.getClienteById(it) }
if (cliente != null) {
pedidosComClienteList.add(PedidoComCliente(pedido, cliente))
}
}
trySend(pedidosComClienteList.toList())
}
} else {
trySend(emptyList())
}
}
awaitClose {
Log.d("PedidoRepository", "getPedidosParaAgendaByDateRange: Listener removido.")
agendaQuery.remove()
}
}

// NOVO MÉTODO PARA EXPORTAÇÃO
suspend fun getPedidosForEmpresa(empresaId: String): List<Pedido> {
return try {
pedidosCollection
.whereEqualTo("empresaId", empresaId)
.get().await().documents.mapNotNull { doc ->
doc.toObject(Pedido::class.java)?.apply { id = doc.id }
}
} catch (e: Exception) {
Log.e("PedidoRepository", "Erro ao obter pedidos para empresa $empresaId: ${e.message}")
emptyList()
}
}
}