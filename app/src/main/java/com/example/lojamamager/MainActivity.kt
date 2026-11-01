package com.example.lojamamager

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.lojamamager.auth.AuthManager // Importe seu AuthManager
import com.example.lojamamager.data.UserProfile
import com.example.lojamamager.firebase.repository.UserProfileRepository
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var authManager: AuthManager
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var currentUserProfile: UserProfile? = null

    // Escopo de coroutines para a Activity
    private val activityScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        authManager = AuthManager(applicationContext)
        userProfileRepository = UserProfileRepository()

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        // Configurar a Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configurar o ActionBarDrawerToggle para o menu hambúrguer
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)

        // Carregar o perfil do usuário para exibir no cabeçalho do drawer e controlar visibilidade
        loadUserProfile()

        // Configurando os ClickListeners para os CardViews
        findViewById<CardView>(R.id.cardPainelControle).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }
        findViewById<CardView>(R.id.cardPedidos).setOnClickListener {
            val intent = Intent(this, ListaPedidosActivity::class.java)
            startActivity(intent)
        }
        findViewById<CardView>(R.id.cardClientes).setOnClickListener {
            val intent = Intent(this, ListaClientesActivity::class.java)
            startActivity(intent)
        }
        findViewById<CardView>(R.id.cardProdutos).setOnClickListener {
            val intent = Intent(this, ProdutosActivity::class.java)
            startActivity(intent)
        }
        findViewById<CardView>(R.id.cardAgenda).setOnClickListener {
            val intent = Intent(this, AgendaActivity::class.java)
            startActivity(intent)
        }
        findViewById<CardView>(R.id.cardCatalogo).setOnClickListener {
            val intent = Intent(this, CatalogoProdutosActivity::class.java)
            startActivity(intent)
        }
        findViewById<CardView>(R.id.cardFornecedores).setOnClickListener {
            val intent = Intent(this, ListaFornecedoresActivity::class.java)
            startActivity(intent)
        }
        findViewById<CardView>(R.id.cardEstoque).setOnClickListener {
            val intent = Intent(this, EstoqueActivity::class.java)
            startActivity(intent)
        }
        // Dentro do onCreate da sua MainActivity:
        val cardMateriais = findViewById<androidx.cardview.widget.CardView>(R.id.cardMateriais)

        cardMateriais.setOnClickListener {
            val intent = Intent(this, ListaMateriaisActivity::class.java)
            startActivity(intent)
        }
    }


    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navigateToLogin()
        } else {
            // Recarrega o perfil caso o usuário tenha sido alterado ou atualizado
            loadUserProfile()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            activityScope.launch {
                val profile = userProfileRepository.getUserProfile(currentUser.uid)
                withContext(Dispatchers.Main) {
                    currentUserProfile = profile
                    updateDrawerHeader(profile)
                    // Controlar a visibilidade do item "Gerenciar Funcionários"
                    val manageEmployeesItem = navigationView.menu.findItem(R.id.nav_manage_employees)
                    manageEmployeesItem?.isVisible = profile?.role == "admin"
                }
            }
        }
    }

    private fun updateDrawerHeader(profile: UserProfile?) {
        val headerView: View = navigationView.getHeaderView(0)
        val userNameTextView: TextView = headerView.findViewById(R.id.textViewUserName)
        val userEmailTextView: TextView = headerView.findViewById(R.id.textViewUserEmail)

        userNameTextView.text = profile?.nome ?: "Usuário"
        userEmailTextView.text = profile?.email ?: auth.currentUser?.email ?: "Não disponível"
    }


// ... (seu código existente)

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                Toast.makeText(this, "Você está na tela inicial", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_manage_employees -> {
                if (currentUserProfile?.role == "admin") {
                    val intent = Intent(this, FuncionariosActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Você não tem permissão para acessar esta função.", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.nav_my_profile -> {
                val intent = Intent(this, MeuPerfilActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_data_management -> { // NOVO ITEM: Gerenciar Dados
                val intent = Intent(this, DataManagementActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                performLogout()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun performLogout() {
        authManager.logout()
        navigateToLogin()
        Toast.makeText(this, "Logout realizado.", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}