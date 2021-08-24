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

package com.github.gouravkhunger.jekyllex.ui.posts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.gouravkhunger.jekyllex.models.CommitModel
import com.github.gouravkhunger.jekyllex.models.repo_content.RepoContentItemModel
import com.github.gouravkhunger.jekyllex.repositories.GithubContentRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PostsViewModel(
    private val repository: GithubContentRepository
) : ViewModel() {

    val hasPosts: MutableLiveData<Boolean> by lazy { MutableLiveData() }
    val posts: MutableLiveData<ArrayList<RepoContentItemModel>> by lazy { MutableLiveData() }

    fun getRepoRootContent(
        repoName: String,
        accessToken: String
    ) = viewModelScope.launch {
        val response = repository.getRepoContent(repoName, "", accessToken)
        if (response.isSuccessful) {
            var flag = false
            run loop@{
                response.body()!!.forEach {
                    if (it.name == "_posts") {
                        hasPosts.postValue(true)
                        flag = true
                        return@loop
                    }
                }
            }
            if (!flag) hasPosts.postValue(false)
        } else {
            hasPosts.postValue(false)
        }
    }

    fun getContentFromPath(
        shouldPost: Boolean,
        repoName: String,
        path: String,
        accessToken: String
    ): ArrayList<RepoContentItemModel> {
        val postsArray = arrayListOf<RepoContentItemModel>()
        runBlocking {
            val response = repository.getRepoContent(repoName, path, accessToken)
            if (response.isSuccessful) {
                response.body()!!.forEach {
                    when (it.type) {
                        "file" -> postsArray.add(it)
                        "dir" -> {
                            val nextLevel =
                                getContentFromPath(false, repoName, it.path, accessToken)
                            postsArray.addAll(nextLevel)
                        }
                    }
                }
                if (shouldPost) posts.postValue(postsArray)
            } else {
                hasPosts.postValue(false)
            }
        }
        return postsArray
    }

    fun deletePost(
        commitModel: CommitModel,
        repo: String,
        path: String,
        accessToken: String
    ) = viewModelScope.launch {
        val response = repository.deleteFile(commitModel, repo, path, accessToken)
    }
}
