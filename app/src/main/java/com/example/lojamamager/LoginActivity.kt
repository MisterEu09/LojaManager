package com.example.lojamamager

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lojamamager.auth.AuthManager// Verifique o pacote, pode ser com.example.lojamamager.auth.AuthManager
import com.example.lojamamager.data.UserProfile
import com.example.lojamamager.firebase.repository.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

private lateinit var auth: FirebaseAuth
private lateinit var emailEditText: EditText
private lateinit var passwordEditText: EditText
private lateinit var loginButton: Button
private lateinit var registerButton: Button // Adicionado

// Nota: authManager e userProfileRepository não são mais estritamente necessários AQUI,
// pois a lógica de registro foi movida para RegisterActivity.
// Mas mantê-los para o onStart() (carregar perfil após login direto) faz sentido.
private lateinit var authManager: AuthManager
private lateinit var userProfileRepository: UserProfileRepository

private val activityScope = CoroutineScope(Dispatchers.Main) // Mantido para o onStart ou futuras lógicas

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContentView(R.layout.activity_login)

auth = FirebaseAuth.getInstance()
authManager = AuthManager(applicationContext)
userProfileRepository = UserProfileRepository()

emailEditText = findViewById(R.id.editTextEmail)
passwordEditText = findViewById(R.id.editTextPassword)
loginButton = findViewById(R.id.buttonLogin)
registerButton = findViewById(R.id.buttonRegister) // Inicialize o botão

loginButton.setOnClickListener {
performLogin()
}

// *** ALTERAÇÃO AQUI: REDIRECIONAR PARA RegisterActivity ***
registerButton.setOnClickListener {
val intent = Intent(this, RegisterActivity::class.java)
startActivity(intent)
}
}

override fun onStart() {
super.onStart()
val currentUser = auth.currentUser
if (currentUser != null) {
navigateToMainActivity()
}
}

private fun performLogin() {
val email = emailEditText.text.toString().trim()
val password = passwordEditText.text.toString().trim()

if (email.isEmpty() || password.isEmpty()) {
Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
return
}

auth.signInWithEmailAndPassword(email, password)
.addOnCompleteListener(this) { task ->
if (task.isSuccessful) {
Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
navigateToMainActivity()
} else {
val errorMessage = when (task.exception) {
is FirebaseAuthInvalidCredentialsException -> "E-mail ou senha inválidos."
else -> "Falha no login: ${task.exception?.message}"
}
Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
}
}
}

private fun navigateToMainActivity() {
val intent = Intent(this, MainActivity::class.java)
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
startActivity(intent)
finish()
}
}