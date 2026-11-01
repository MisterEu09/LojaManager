package com.example.lojamamager.auth

import android.content.Context
import android.util.Log
import com.example.lojamamager.data.UserProfile
import com.example.lojamamager.firebase.repository.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch // Importe este
import kotlinx.coroutines.GlobalScope // Remova este se for usar lifecycleScope ou viewModelScope em outros lugares
import kotlinx.coroutines.Dispatchers // Importe este
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val userProfileRepository: UserProfileRepository by lazy { UserProfileRepository() }

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile

    private val _currentEmpresaId = MutableStateFlow<String?>(null)
    val currentEmpresaId: StateFlow<String?> = _currentEmpresaId
    private val _currentUserRole = MutableStateFlow<String?>(null)
    val currentUserRole: StateFlow<String?> = _currentUserRole

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("AuthManager", "Usuário autenticado: ${user.uid}")
                loadUserProfile(user.uid)
            } else {
                Log.d("AuthManager", "Usuário deslogado.")
                _currentUserProfile.value = null
                _currentEmpresaId.value = null
                _currentUserRole.value = null
            }
        }
    }

    private fun loadUserProfile(uid: String) {
        // Mude GlobalScope para CoroutineScope mais seguro, como um MainScope ou um escopo de ViewModel/Activity
        // Por enquanto, para simplificar o exemplo, mantemos, mas é uma boa prática mudar.
        // Se este AuthManager for um Singleton ou for gerenciado por um LifecycleOwner, use o escopo apropriado.
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val profile = userProfileRepository.getUserProfile(uid)
                _currentUserProfile.value = profile
                _currentEmpresaId.value = profile?.empresaId
                _currentUserRole.value = profile?.role
                Log.d("AuthManager", "Perfil carregado: ${profile?.email}, Empresa ID: ${profile?.empresaId}, Role: ${profile?.role}")

                if (profile == null) {
                    val newUserProfile = UserProfile(
                        uid = uid,
                        email = auth.currentUser?.email,
                        nome = auth.currentUser?.displayName,
                        empresaId = null, // Inicialmente null, pode ser atribuído depois
                        role = "vendedor"
                    )
                    userProfileRepository.createUserProfile(newUserProfile)
                    _currentUserProfile.value = newUserProfile
                    _currentEmpresaId.value = newUserProfile.empresaId
                    _currentUserRole.value = newUserProfile.role
                    Log.d("AuthManager", "Novo perfil de usuário criado (sem empresaId definido ainda): ${newUserProfile.email}")
                }
            } catch (e: Exception) {
                Log.e("AuthManager", "Erro ao carregar perfil do usuário: ${e.message}")
                _currentUserProfile.value = null
                _currentEmpresaId.value = null
                _currentUserRole.value = null
            }
        }
    }

    fun getAuthInstance(): FirebaseAuth {
        return auth
    }

    fun getCurrentUserEmpresaId(): String? {
        return _currentEmpresaId.value
    }

    suspend fun awaitEmpresaId(): String {
        return _currentEmpresaId.filterNotNull().first()
    }

    fun getCurrentUserRole(): String? {
        return _currentUserRole.value
    }

    /**
     * Realiza o logout do usuário atual do Firebase Authentication.
     * Isso irá acionar o AuthStateListener e limpar os dados do perfil localmente.
     */
    fun logout() { // Renomeado para 'logout' para ser mais descritivo e consistente com o uso na MainActivity
        auth.signOut()
        Log.d("AuthManager", "Usuário deslogado via AuthManager.logout()")
    }
}