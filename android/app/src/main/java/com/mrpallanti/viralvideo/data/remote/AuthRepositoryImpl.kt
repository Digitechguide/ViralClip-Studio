package com.mrpallanti.viralvideo.data.remote

import com.mrpallanti.viralvideo.data.local.AuthSessionManager
import com.mrpallanti.viralvideo.domain.model.AuthUser
import com.mrpallanti.viralvideo.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuthRepositoryImpl(
    private val sessionManager: AuthSessionManager,
    private val client: OkHttpClient = OkHttpClient()
) : AuthRepository {

    override fun getCurrentUser(): AuthUser? {
        return sessionManager.getUserSession()
    }

    override fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    override suspend fun login(email: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("action", "login")
                put("email", email.trim().lowercase())
                put("password", password)
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(AUTH_ENDPOINT)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(responseString).optString("error", "Login failed (HTTP ${response.code})")
                } catch (e: Exception) {
                    "Authentication failed with server code ${response.code}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(responseString)
            if (json.optString("status") == "success") {
                val userObj = json.getJSONObject("user")
                val token = json.getString("token")

                val authUser = AuthUser(
                    id = userObj.optInt("id", 1),
                    name = userObj.optString("name", "Creator"),
                    email = userObj.getString("email"),
                    role = userObj.optString("role", "user"),
                    status = userObj.optString("status", "approved"),
                    token = token
                )

                // Save session in local SharedPreferences
                sessionManager.saveUserSession(authUser)

                Result.success(authUser)
            } else {
                val errMsg = json.optString("error", "Invalid credentials")
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        sessionManager.clearSession()
    }

    companion object {
        private const val AUTH_ENDPOINT = "https://viral.mrpallanti.in/api/auth.php"
    }
}
