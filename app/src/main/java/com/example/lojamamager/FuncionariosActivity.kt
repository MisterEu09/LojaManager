package com.example.lojamamager

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.data.UserProfile
import com.example.lojamamager.firebase.repository.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await // Importe tasks.await para usar com await() no Firebase Auth

class FuncionariosActivity : BaseActivity() {

    class FuncionariosActivity : BaseActivity() {

        private lateinit var auth: FirebaseAuth
        private lateinit var userProfileRepository: UserProfileRepository

        private lateinit var editTextFuncionarioEmail: EditText
        private lateinit var editTextFuncionarioNome: EditText
        private lateinit var editTextFuncionarioSenha: EditText
        private lateinit var spinnerFuncionarioRole: Spinner
        private lateinit var buttonAddFuncionario: Button
        private lateinit var recyclerViewFuncionarios: RecyclerView
        private lateinit var funcionariosAdapter: FuncionariosAdapter
        private lateinit var imageViewBack: ImageView // *** ADICIONADO ***

        private var currentEmpresaId: String? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_funcionarios)

            auth = FirebaseAuth.getInstance()
            userProfileRepository = UserProfileRepository()

            // Mapeamento das views antigas
            editTextFuncionarioEmail = findViewById(R.id.editTextFuncionarioEmail)
            editTextFuncionarioNome = findViewById(R.id.editTextFuncionarioNome)
            editTextFuncionarioSenha = findViewById(R.id.editTextFuncionarioSenha)
            spinnerFuncionarioRole = findViewById(R.id.spinnerFuncionarioRole)
            buttonAddFuncionario = findViewById(R.id.buttonAddFuncionario)
            recyclerViewFuncionarios = findViewById(R.id.recyclerViewFuncionarios)

            // Inicializando o botão de voltar *** ADICIONADO ***
            imageViewBack = findViewById(R.id.imageViewBackFuncionarios)

            setupRoleSpinner()
            setupRecyclerView()

            // Configurando a ação de voltar ao clicar na seta *** ADICIONADO ***
            imageViewBack.setOnClickListener {
                finish()
            }

            buttonAddFuncionario.setOnClickListener {
                addNewFuncionario()
            }

            loadCurrentUserEmpresaIdAndFuncionarios()
        }

    private fun setupRoleSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.roles_array, // Array que criamos em values/arrays.xml
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerFuncionarioRole.adapter = adapter
        }
    }

    private fun setupRecyclerView() {
        funcionariosAdapter = FuncionariosAdapter()
        recyclerViewFuncionarios.apply {
            layoutManager = LinearLayoutManager(this@FuncionariosActivity)
            adapter = funcionariosAdapter
        }
    }

    private fun loadCurrentUserEmpresaIdAndFuncionarios() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val userProfile = userProfileRepository.getUserProfile(currentUser.uid)
                withContext(Dispatchers.Main) {
                    if (userProfile?.role == "admin" && userProfile.empresaId != null) {
                        currentEmpresaId = userProfile.empresaId
                        loadFuncionarios(currentEmpresaId!!)
                    } else {
                        Toast.makeText(this@FuncionariosActivity, "Você não tem permissão de administrador ou empresa não definida.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }
        } else {
            navigateToLogin()
        }
    }

    private fun loadFuncionarios(empresaId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val funcionarios = userProfileRepository.getUsersByEmpresaId(empresaId)
            withContext(Dispatchers.Main) {
                funcionariosAdapter.updateList(funcionarios)
            }
        }
    }

    private fun addNewFuncionario() {
        val email = editTextFuncionarioEmail.text.toString().trim()
        val nome = editTextFuncionarioNome.text.toString().trim()
        val senha = editTextFuncionarioSenha.text.toString().trim()
        val role = spinnerFuncionarioRole.selectedItem.toString()

        if (email.isEmpty() || nome.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }

        // NOVO: Validação de formato de e-mail antes de enviar para o Firebase
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "O formato do e-mail é inválido. Tente novamente.", Toast.LENGTH_LONG).show()
            return
        }

        if (senha.length < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show()
            return
        }

        buttonAddFuncionario.isEnabled = false
        buttonAddFuncionario.text = "Adicionando..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Criar o usuário no Firebase Authentication (com a senha temporária)
                val authResult = auth.createUserWithEmailAndPassword(email, senha).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null && currentEmpresaId != null) {
                    // 2. Criar o UserProfile para o novo funcionário com o empresaId do admin
                    val newUserProfile = UserProfile(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        nome = nome,
                        empresaId = currentEmpresaId,
                        role = role
                    )
                    val profileCreated = userProfileRepository.createUserProfile(newUserProfile)

                    withContext(Dispatchers.Main) {
                        if (profileCreated) {
                            Toast.makeText(this@FuncionariosActivity, "Funcionário adicionado com sucesso!", Toast.LENGTH_LONG).show()
                            // Limpa os campos
                            editTextFuncionarioEmail.text.clear()
                            editTextFuncionarioNome.text.clear()
                            editTextFuncionarioSenha.text.clear()
                            spinnerFuncionarioRole.setSelection(0)

                            loadFuncionarios(currentEmpresaId!!) // Recarrega a lista
                        } else {
                            Toast.makeText(this@FuncionariosActivity, "Erro ao criar perfil do funcionário. Revertendo...", Toast.LENGTH_LONG).show()
                            firebaseUser.delete().await()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FuncionariosActivity, "Erro: Usuário nulo ou empresa não definida.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = when (e) {
                        is FirebaseAuthWeakPasswordException -> "A senha é muito fraca. Tente uma senha mais forte."
                        is FirebaseAuthInvalidCredentialsException -> "O formato do e-mail é inválido." // Essa mensagem pode ainda aparecer se o formato for muito bizarro
                        is FirebaseAuthUserCollisionException -> "Este e-mail já está em uso."
                        else -> "Falha ao adicionar funcionário: ${e.message}"
                    }
                    Toast.makeText(this@FuncionariosActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    buttonAddFuncionario.isEnabled = true
                    buttonAddFuncionario.text = "Adicionar Novo Funcionário"
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private class FuncionariosAdapter : RecyclerView.Adapter<FuncionariosAdapter.FuncionarioViewHolder>() {

        private val funcionariosList = mutableListOf<UserProfile>()

        fun updateList(newList: List<UserProfile>) {
            funcionariosList.clear()
            funcionariosList.addAll(newList)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FuncionarioViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_funcionario, parent, false)
            return FuncionarioViewHolder(view)
        }

        override fun onBindViewHolder(holder: FuncionarioViewHolder, position: Int) {
            val funcionario = funcionariosList[position]
            holder.bind(funcionario)
        }

        override fun getItemCount(): Int = funcionariosList.size

        class FuncionarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nomeTextView: TextView = itemView.findViewById(R.id.textViewItemFuncionarioNome)
            private val emailTextView: TextView = itemView.findViewById(R.id.textViewItemFuncionarioEmail)
            private val roleTextView: TextView = itemView.findViewById(R.id.textViewItemFuncionarioRole)

            fun bind(funcionario: UserProfile) {
                nomeTextView.text = funcionario.nome
                emailTextView.text = funcionario.email
                roleTextView.text = "Cargo: ${funcionario.role?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
            }
        }
    }
}}