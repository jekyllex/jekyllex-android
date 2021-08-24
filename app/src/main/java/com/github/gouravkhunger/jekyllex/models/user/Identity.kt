package com.github.gouravkhunger.jekyllex.models.user

data class Identity(
    val access_token: String,
    val connection: String,
    val isSocial: Boolean,
    val provider: String,
    val user_id: Int
)
