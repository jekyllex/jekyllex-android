/*
 * MIT License
 *
 * Copyright (c) 2021 Gourav Khunger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.gouravkhunger.jekyllex.apis.github

import com.github.gouravkhunger.jekyllex.models.CommitModel
import com.github.gouravkhunger.jekyllex.models.repo_content.RepoContentModel
import com.github.gouravkhunger.jekyllex.models.repository.RepoModel
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.PUT

interface GithubApi {
    @GET("/user/repos")
    suspend fun getUserRepositories(
        @Header("Authorization") accessToken: String
    ): Response<RepoModel>

    @GET("/repos/{repoName}/contents/{path}")
    suspend fun getRepoContent(
        @Path("repoName", encoded = true) repoName: String,
        @Path("path", encoded = true) path: String,
        @Header("Authorization") accessToken: String
    ): Response<RepoContentModel>

    @GET("/repos/{repoName}/contents/{path}")
    @Headers(
        "Accept: application/vnd.github.VERSION.raw"
    )
    suspend fun getRawContent(
        @Path("repoName", encoded = true) repoName: String,
        @Path("path", encoded = true) path: String,
        @Header("Authorization") accessToken: String
    ): Response<String>

    @PUT("/repos/{currentRepo}/contents/{path}")
    @Headers(
        "Accept: application/vnd.github.v3+json"
    )
    suspend fun updateFile(
        @Body commitModel: CommitModel,
        @Path("currentRepo", encoded = true) currentRepo: String,
        @Path("path", encoded = true) path: String,
        @Header("Authorization") accessToken: String
    ): Response<Void>

    @HTTP(method = "DELETE", path = "/repos/{currentRepo}/contents/{path}", hasBody = true)
    @Headers(
        "Accept: application/vnd.github.v3+json"
    )
    suspend fun deleteFile(
        @Body commitModel: CommitModel,
        @Path("currentRepo", encoded = true) currentRepo: String,
        @Path("path", encoded = true) path: String,
        @Header("Authorization") accessToken: String
    ): Response<Void>
}
