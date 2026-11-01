package com.example.lojamamager.firebase.repository

import android.util.Log
import com.example.lojamamager.data.UserProfile
import com.example.lojamamager.firebase.FirebaseClient
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.tasks.await

class UserProfileRepository {

    private val usersCollection: CollectionReference by lazy {
        FirebaseClient.getFirestoreInstance().collection("users")
    }

    /**
     * Cria ou atualiza um perfil de usuário no Firestore.
     */
    suspend fun createUserProfile(userProfile: UserProfile): Boolean {
        return try {
            userProfile.uid?.let { uid ->
                usersCollection.document(uid).set(userProfile).await()
                Log.d("UserProfileRepo", "Perfil do usuário '${userProfile.email}' criado/atualizado com UID: $uid")
                true
            } ?: run {
                Log.e("UserProfileRepo", "Erro: UID do perfil do usuário é nulo ao tentar criar/atualizar.")
                false
            }
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "Erro ao criar/atualizar perfil do usuário: ${e.message}")
            false
        }
    }

    /**
     * Obtém um perfil de usuário pelo seu UID.
     */
    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val documentSnapshot = usersCollection.document(uid).get().await()
            val userProfile = documentSnapshot.toObject(UserProfile::class.java)?.apply {
                this.uid = uid // Garante que o UID do documento é setado na data class
            }
            if (userProfile != null) {
                Log.d("UserProfileRepo", "Perfil do usuário '${userProfile.email}' obtido com sucesso. Empresa ID: ${userProfile.empresaId}")
            } else {
                Log.d("UserProfileRepo", "Perfil do usuário com UID '$uid' não encontrado.")
            }
            userProfile
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "Erro ao obter perfil do usuário com UID '$uid': ${e.message}")
            null
        }
    }

    /**
     * Atualiza os dados de um perfil de usuário existente.
     * Reutiliza createUserProfile pois `set()` já faz isso.
     */
    suspend fun updateUserProfile(userProfile: UserProfile): Boolean {
        return createUserProfile(userProfile)
    }

    /**
     * NOVO MÉTODO: Obtém uma lista de UserProfiles que pertencem a uma empresa específica.
     */
    suspend fun getUsersByEmpresaId(empresaId: String): List<UserProfile> {
        return try {
            val querySnapshot = usersCollection.whereEqualTo("empresaId", empresaId).get().await()
            querySnapshot.documents.mapNotNull { document ->
                document.toObject(UserProfile::class.java)?.apply {
                    this.uid = document.id // Garante que o UID é setado no objeto
                }
            }
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "Erro ao obter usuários pela empresaId '$empresaId': ${e.message}")
            emptyList()
        }
    }
}