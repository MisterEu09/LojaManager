package com.example.lojamamager

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView // Importar SearchView do androidx.appcompat.widget
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.Categoria
import com.example.lojamamager.firebase.repository.CategoriaRepository // Importar o novo CategoriaRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CategoriaSelectionDialog(
private val onCategorySelected: (Categoria?) -> Unit // Callback para quando uma categoria for selecionada (ou nula para limpar)
) : DialogFragment() {

private lateinit var categoriaRepository: CategoriaRepository
private lateinit var categoriaAdapter: CategoriaAdapter
private lateinit var searchView: SearchView
private lateinit var recyclerView: RecyclerView

override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
categoriaRepository =  CategoriaRepository(requireActivity().applicationContext)

val builder = AlertDialog.Builder(requireActivity())
val inflater = requireActivity().layoutInflater
val view = inflater.inflate(R.layout.dialog_categoria_selection, null)

recyclerView = view.findViewById(R.id.recyclerViewCategoryList)
searchView = view.findViewById(R.id.searchViewCategories)
val buttonAddNewCategory: Button = view.findViewById(R.id.buttonAddNewCategory)
val buttonCategoryBack: Button = view.findViewById(R.id.buttonCategoryBack)
val buttonClearCategory: Button = view.findViewById(R.id.buttonClearCategory)

categoriaAdapter = CategoriaAdapter(
emptyList(),
onItemClick = { categoria ->
onCategorySelected(categoria) // Retorna a categoria selecionada
dismiss() // Fecha o diálogo
},
onMoreOptionsClick = { categoria, anchorView ->
showCategoryOptionsMenu(categoria, anchorView)
}
)
recyclerView.layoutManager = LinearLayoutManager(requireContext())
recyclerView.adapter = categoriaAdapter

// Carrega as categorias iniciais (pode ser todas se a query for vazia)
loadCategorias(null) // Chama com query nula para carregar tudo inicialmente

// Configurar busca
searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
override fun onQueryTextSubmit(query: String?): Boolean {
loadCategorias(query) // Quando o usuário submete (pressiona Enter)
return true
}

override fun onQueryTextChange(newText: String?): Boolean {
loadCategorias(newText) // Em tempo real enquanto o usuário digita
return true
}
})

// Botões de ação
buttonAddNewCategory.setOnClickListener {
AddEditCategoryDialog(onDismiss = {
// Ao adicionar/editar, a lista de categorias deve ser atualizada automaticamente pelo Flow
// não precisamos fazer nada aqui, o Flow já cuida da atualização da lista
// quando os dados no Firestore mudam.
}).show(parentFragmentManager, "AddEditCategoryDialog")
}

buttonCategoryBack.setOnClickListener {
dismiss() // Fecha o diálogo sem selecionar nada
}

buttonClearCategory.setOnClickListener {
onCategorySelected(null) // Retorna nulo para limpar a seleção
dismiss()
}

builder.setView(view)
return builder.create()
}

/**
* Carrega as categorias com base na query.
* Se a query for nula ou vazia, busca todas as categorias.
*/
private fun loadCategorias(query: String?) {
lifecycleScope.launch {
if (query.isNullOrEmpty()) {
categoriaRepository.getAllCategorias().collectLatest { categorias ->
categoriaAdapter.updateCategorias(categorias)
}
} else {
categoriaRepository.searchCategorias(query).collectLatest { categorias ->
categoriaAdapter.updateCategorias(categorias)
}
}
}
}

private fun showCategoryOptionsMenu(categoria: Categoria, anchorView: View) {
val popup = PopupMenu(requireContext(), anchorView)
popup.menuInflater.inflate(R.menu.menu_item_options, popup.menu) // Reutilizamos o menu_item_options

popup.setOnMenuItemClickListener { menuItem ->
when (menuItem.itemId) {
R.id.action_edit -> {
AddEditCategoryDialog(categoria, onDismiss = {
    // Após editar, a lista de categorias deve ser atualizada automaticamente pelo Flow
}).show(parentFragmentManager, "AddEditCategoryDialog")
true
}
R.id.action_delete -> {
confirmDeleteCategory(categoria)
true
}
else -> false
}
}
popup.show()
}

private fun confirmDeleteCategory(categoria: Categoria) {
AlertDialog.Builder(requireContext())
.setTitle("Excluir Categoria")
.setMessage("Tem certeza que deseja excluir '${categoria.nome}'? Isso não removerá a categoria de produtos já associados.")
.setPositiveButton("Sim") { dialog, which ->
lifecycleScope.launch {
categoria.id?.let { id ->
    val success = categoriaRepository.deleteCategoria(id)
    if (success) {
        Toast.makeText(requireContext(), "'${categoria.nome}' excluída!", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(requireContext(), "Erro ao excluir categoria.", Toast.LENGTH_SHORT).show()
    }
} ?: run {
    Toast.makeText(requireContext(), "ID da categoria inválido.", Toast.LENGTH_SHORT).show()
}
}
}
.setNegativeButton("Não", null)
.show()
}
}