package com.example.lojamamager

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lojamamager.data.Empresa
import com.example.lojamamager.data.UserProfile
import com.example.lojamamager.firebase.repository.EmpresaRepository
import com.example.lojamamager.firebase.repository.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegisterActivity : BaseActivity() {

private lateinit var auth: FirebaseAuth
private lateinit var userProfileRepository: UserProfileRepository
private lateinit var empresaRepository: EmpresaRepository

private lateinit var userNameEditText: EditText
private lateinit var emailEditText: EditText
private lateinit var passwordEditText: EditText
private lateinit var empresaNomeEditText: EditText
private lateinit var registerButton: Button
private lateinit var backToLoginTextView: TextView

// Escopo de coroutines para a Activity
private val activityScope = CoroutineScope(Dispatchers.Main)

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContentView(R.layout.activity_register) // Conecta com o novo layout

auth = FirebaseAuth.getInstance()
userProfileRepository = UserProfileRepository()
empresaRepository = EmpresaRepository()

userNameEditText = findViewById(R.id.editTextUserName)
emailEditText = findViewById(R.id.editTextEmailRegister)
passwordEditText = findViewById(R.id.editTextPasswordRegister)
empresaNomeEditText = findViewById(R.id.editTextEmpresaNome)
registerButton = findViewById(R.id.buttonRegisterNewUser)
backToLoginTextView = findViewById(R.id.textViewBackToLogin)

registerButton.setOnClickListener {
performRegistration()
}

backToLoginTextView.setOnClickListener {
navigateToLogin()
}
}

private fun performRegistration() {
val userName = userNameEditText.text.toString().trim()
val email = emailEditText.text.toString().trim()
val password = passwordEditText.text.toString().trim()
val empresaNome = empresaNomeEditText.text.toString().trim()

if (userName.isEmpty() || email.isEmpty() || password.isEmpty() || empresaNome.isEmpty()) {
Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
return
}
if (password.length < 6) {
Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show()
return
}

registerButton.isEnabled = false // Desabilita o botão para evitar cliques múltiplos
registerButton.text = "Registrando..."

activityScope.launch {
try {
// 1. Registrar o usuário no Firebase Authentication
val authResult = auth.createUserWithEmailAndPassword(email, password).await()
val firebaseUser = authResult.user

if (firebaseUser != null) {
// 2. Criar a nova empresa no Firestore
val newEmpresa = Empresa(nome = empresaNome)
val empresaId = empresaRepository.addEmpresa(newEmpresa)

if (empresaId != null) {
// 3. Criar o UserProfile para o novo usuário, associando ao empresaId
val newUserProfile = UserProfile(
uid = firebaseUser.uid,
email = firebaseUser.email,
nome = userName,
empresaId = empresaId, // Define o empresaId gerado
role = "admin" // O primeiro usuário da empresa é o admin
)
val profileCreated = userProfileRepository.createUserProfile(newUserProfile)

if (profileCreated) {
withContext(Dispatchers.Main) {
Toast.makeText(this@RegisterActivity, "Conta e Empresa criadas com sucesso!", Toast.LENGTH_LONG).show()
navigateToMainActivity()
}
} else {
// Se falhar a criação do perfil, tenta deletar a conta Auth e a empresa
withContext(Dispatchers.Main) {
Toast.makeText(this@RegisterActivity, "Erro ao criar perfil de usuário. Limpando dados...", Toast.LENGTH_LONG).show()
firebaseUser.delete().await() // Tenta deletar a conta Auth
empresaRepository.deleteEmpresa(empresaId) // Tenta deletar a empresa
registerButton.isEnabled = true
registerButton.text = "Criar Conta e Empresa"
}
}
} else {
// Falha ao criar a empresa
withContext(Dispatchers.Main) {
Toast.makeText(this@RegisterActivity, "Erro ao criar a empresa. Tente novamente.", Toast.LENGTH_LONG).show()
firebaseUser.delete().await() // Tenta deletar a conta Auth
registerButton.isEnabled = true
registerButton.text = "Criar Conta e Empresa"
}
}
}
} catch (e: Exception) {
withContext(Dispatchers.Main) {
val errorMessage = when (e) {
is FirebaseAuthWeakPasswordException -> "A senha é muito fraca. Tente uma senha mais forte."
is FirebaseAuthInvalidCredentialsException -> "O formato do e-mail é inválido."
is FirebaseAuthUserCollisionException -> "Este e-mail já está em uso."
else -> "Falha no registro: ${e.message}"
}
Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
registerButton.isEnabled = true
registerButton.text = "Criar Conta e Empresa"
}
}
}
}

private fun navigateToMainActivity() {
val intent = Intent(this, MainActivity::class.java)
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
startActivity(intent)
finish()
}

private fun navigateToLogin() {
val intent = Intent(this, LoginActivity::class.java)
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
startActivity(intent)
finish()
}
}