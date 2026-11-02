package com.mrpallanti.viralvideo.data.local

import android.content.Context
import android.content.SharedPreferences
import com.mrpallanti.viralvideo.domain.model.AuthUser

class AuthSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveUserSession(user: AuthUser) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putInt(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_ROLE, user.role)
            .putString(KEY_USER_STATUS, user.status)
            .putString(KEY_USER_TOKEN, user.token)
            .apply()
    }

    fun getUserSession(): AuthUser? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val token = prefs.getString(KEY_USER_TOKEN, null)
        val email = prefs.getString(KEY_USER_EMAIL, null)

        if (!isLoggedIn || token.isNullOrBlank() || email.isNullOrBlank()) {
            return null
        }

        return AuthUser(
            id = prefs.getInt(KEY_USER_ID, 0),
            name = prefs.getString(KEY_USER_NAME, "") ?: "",
            email = email,
            role = prefs.getString(KEY_USER_ROLE, "user") ?: "user",
            status = prefs.getString(KEY_USER_STATUS, "approved") ?: "approved",
            token = token
        )
    }

    fun isLoggedIn(): Boolean {
        return getUserSession() != null
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "viralclip_auth_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_STATUS = "user_status"
        private const val KEY_USER_TOKEN = "user_token"
    }
}
