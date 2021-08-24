package com.github.gouravkhunger.jekyllex.repositories

import com.github.gouravkhunger.jekyllex.apis.github.GithubApiInstance
import com.github.gouravkhunger.jekyllex.models.CommitModel

class GithubContentRepository {

    suspend fun getRepoContent(repoName: String, path: String, accessToken: String) =
        GithubApiInstance.api.getRepoContent(repoName, path, accessToken)

    suspend fun getRawContent(repoName: String, path: String, accessToken: String) =
        GithubApiInstance.api.getRawContent(repoName, path, accessToken)

    suspend fun updateFileContent(
        commitModel: CommitModel,
        currentRepo: String,
        path: String,
        accessToken: String
    ) = GithubApiInstance.api.updateFile(commitModel, currentRepo, path, accessToken)

}
