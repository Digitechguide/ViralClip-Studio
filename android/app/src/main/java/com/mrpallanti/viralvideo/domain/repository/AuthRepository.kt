package com.mrpallanti.viralvideo.domain.repository

import com.mrpallanti.viralvideo.domain.model.AuthUser

interface AuthRepository {
    fun getCurrentUser(): AuthUser?
    fun isLoggedIn(): Boolean
    suspend fun login(email: String, password: String): Result<AuthUser>
    fun logout()
}
