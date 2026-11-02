package com.mrpallanti.viralvideo.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val status: String,
    val token: String
)
