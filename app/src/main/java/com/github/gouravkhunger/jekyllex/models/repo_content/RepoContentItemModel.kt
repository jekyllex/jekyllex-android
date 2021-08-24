package com.github.gouravkhunger.jekyllex.models.repo_content

data class RepoContentItemModel(
    val download_url: String,
    val git_url: String,
    val html_url: String,
    val name: String,
    val path: String,
    val sha: String,
    val size: Long,
    val type: String,
    val url: String
)
