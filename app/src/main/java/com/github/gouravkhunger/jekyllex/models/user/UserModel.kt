package com.github.gouravkhunger.jekyllex.models.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserModel(
    val bio: String?,
    val blog: String?,
    val company: String?,
    val created_at: String?,
    val email: String?,
    val events_url: String?,
    val followers: Int?,
    val followers_url: String?,
    val following: Int?,
    val following_url: String?,
    val gists_url: String?,
    val gravatar_id: String?,
    val html_url: String?,
    val identities: List<Identity>,
    val last_ip: String?,
    val last_login: String?,
    val location: String?,
    val logins_count: Int?,
    val name: String?,
    val nickname: String?,
    val node_id: String?,
    val organizations_url: String?,
    val picture: String?,
    val public_gists: Int?,
    val public_repos: Int?,
    val received_events_url: String?,
    val repos_url: String?,
    val site_admin: Boolean?,
    val starred_url: String?,
    val subscriptions_url: String?,
    val twitter_username: String?,
    val type: String?,
    val updated_at: String?,
    val url: String?,
    @PrimaryKey
    val user_id: String
)
