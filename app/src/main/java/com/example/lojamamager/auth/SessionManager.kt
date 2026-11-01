package com.example.lojamamager.auth

import com.example.lojamamager.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    val currentEmpresaId: String?
        get() = _currentUserProfile.value?.empresaId

    val currentUserRole: String?
        get() = _currentUserProfile.value?.role

    fun setSession(userProfile: UserProfile) {
        _currentUserProfile.value = userProfile
    }

    fun clearSession() {
        _currentUserProfile.value = null
    }

    fun isAdmin(): Boolean {
        return currentUserRole == "admin"
    }
}