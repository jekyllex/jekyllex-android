package com.github.gouravkhunger.jekyllex.models

data class CommitModel(
    val message: String,
    val content: String,
    val sha: String?
)
