package com.github.gouravkhunger.jekyllex.ui.posts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

}
