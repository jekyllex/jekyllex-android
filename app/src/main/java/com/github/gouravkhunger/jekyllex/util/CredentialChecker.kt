package com.github.gouravkhunger.jekyllex.util

import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.storage.SecureCredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.github.gouravkhunger.jekyllex.BuildConfig
import com.github.gouravkhunger.jekyllex.R

fun checkCredentials(ctx: Context): Boolean {
    val account = Auth0(
        BuildConfig.Auth0ClientId,
        ctx.getString(R.string.com_auth0_domain)
    )

    val apiClient = AuthenticationAPIClient(account)
    val manager = SecureCredentialsManager(ctx, apiClient, SharedPreferencesStorage(ctx))

    return manager.hasValidCredentials()
}