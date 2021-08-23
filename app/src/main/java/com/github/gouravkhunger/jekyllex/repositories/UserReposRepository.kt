package com.github.gouravkhunger.jekyllex.repositories

import com.github.gouravkhunger.jekyllex.apis.github.GithubApiInstance

class UserReposRepository {

    suspend fun getUserRepositories(accessToken: String) =
        GithubApiInstance.api.getUserRepositories(accessToken)

}