package com.example.lojamamager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.auth.AuthManager
import com.example.lojamamager.data.Fornecedor
import com.example.lojamamager.firebase.repository.FornecedorRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.Exception // Adicionando import para Exception

// A classe 'BaseActivity' deve ser a superclasse, se existir. Caso contrário, use AppCompatActivity
class ListaFornecedoresActivity : AppCompatActivity() {

    // --- 1. Declaração de Variáveis ---
    // Usamos 'lateinit' para inicializar as variáveis no onCreate.
    private lateinit var recyclerView: RecyclerView
    private lateinit var fornecedorAdapter: FornecedorAdapter
    private lateinit var searchView: SearchView
    private lateinit var fabAddFornecedor: FloatingActionButton
    private lateinit var imageViewBack: ImageView

    // Repositórios para acesso aos dados do Firebase
    private lateinit var fornecedorRepository: FornecedorRepository
    private lateinit var authManager: AuthManager

    // Variável para controlar o modo de seleção, se aplicável
    private var isSelectionMode: Boolean = false

    // --- 2. Método onCreate() ---
    // Ponto de entrada da Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_fornecedores)

        // Inicialização de variáveis
        isSelectionMode = intent.getBooleanExtra("is_selection_mode", false)

        // Atribui as views do layout às variáveis
        recyclerView = findViewById(R.id.recyclerViewFornecedores)
        searchView = findViewById(R.id.searchViewFornecedores)
        fabAddFornecedor = findViewById(R.id.fabAddFornecedor)
        imageViewBack = findViewById(R.id.imageViewBackFornecedores)

        // Inicializa os repositórios, passando o contexto da aplicação
        fornecedorRepository = FornecedorRepository(applicationContext)
        authManager = AuthManager(applicationContext)

        // Configura o adapter do RecyclerView. O `onClick` define a ação ao clicar em um fornecedor.
        fornecedorAdapter = FornecedorAdapter(emptyList()) { fornecedor ->
            if (isSelectionMode) {
                // Se estiver em modo de seleção, retorna o fornecedor selecionado para a Activity que a chamou.
                val resultIntent = Intent().apply {
                    putExtra("selected_fornecedor", fornecedor)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                // Se não estiver em modo de seleção, abre a tela de detalhes.
                val intent = Intent(this, DetalheFornecedorActivity::class.java)
                intent.putExtra("fornecedor", fornecedor)
                startActivity(intent)
            }
        }
        // Configura o LayoutManager e o Adapter no RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = fornecedorAdapter

        // Inicie o carregamento de fornecedores logo após a configuração do RecyclerView
        loadFornecedores()

        // --- 3. Configuração de Listeners ---
        // Listener para a barra de pesquisa
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Quando o usuário pressiona Enter na pesquisa, filtra a lista.
                filterFornecedores(query.orEmpty())
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // A cada mudança de texto, filtra a lista em tempo real.
                filterFornecedores(newText.orEmpty())
                return true
            }
        })

        // Listener para o botão de adicionar fornecedor
        fabAddFornecedor.setOnClickListener {
            val intent = Intent(this, DetalheFornecedorActivity::class.java)
            startActivity(intent)
        }

        // Listener para o botão de voltar
        imageViewBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // --- 4. Funções de Lógica ---
    /**
     * Carrega a lista de fornecedores do repositório e atualiza o RecyclerView.
     * O uso de lifecycleScope.launch garante que a operação seja segura.
     */
    private fun loadFornecedores() {
        lifecycleScope.launch {
            try {
                // Usa `collectLatest` para receber os dados do Flow. Se um novo valor for emitido,
                // o processamento do valor anterior é cancelado.
                fornecedorRepository.getAllFornecedores().collectLatest { fornecedores ->
                    // Atualiza o adaptador com a nova lista de fornecedores.
                    fornecedorAdapter.updateFornecedores(fornecedores)
                }
            } catch (e: Exception) {
                // Captura qualquer exceção, incluindo a IllegalStateException do AuthManager.
                Log.e("ListaFornecedoresActivity", "Erro ao carregar fornecedores: ${e.message}", e)
                //Toast.makeText(this@ListaFornecedoresActivity, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Filtra a lista de fornecedores com base na consulta de pesquisa.
     * @param query O texto a ser usado para a pesquisa.
     */
    private fun filterFornecedores(query: String) {
        // Usa `lifecycleScope.launch` para garantir o cancelamento automático.
        lifecycleScope.launch {
            try {
                if (query.isEmpty()) {
                    // Se a consulta estiver vazia, carrega todos os fornecedores.
                    fornecedorRepository.getAllFornecedores().collectLatest { fornecedores ->
                        fornecedorAdapter.updateFornecedores(fornecedores)
                    }
                } else {
                    // Se houver uma consulta, realiza a busca no repositório.
                    fornecedorRepository.searchFornecedores(query).collectLatest { fornecedores ->
                        fornecedorAdapter.updateFornecedores(fornecedores)
                    }
                }
            } catch (e: Exception) {
                Log.e("ListaFornecedoresActivity", "Erro ao filtrar fornecedores: ${e.message}", e)
                Toast.makeText(this@ListaFornecedoresActivity, "Erro ao filtrar dados.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}