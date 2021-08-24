package com.github.gouravkhunger.jekyllex.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.gouravkhunger.jekyllex.models.repository.RepoModel
import com.github.gouravkhunger.jekyllex.repositories.UserReposRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repoModel: UserReposRepository
) : ViewModel() {

    val userRepos: MutableLiveData<RepoModel> by lazy { MutableLiveData() }

    fun getUserRepositories(accessToken: String) = viewModelScope.launch {
        val response = repoModel.getUserRepositories(accessToken)
        userRepos.postValue(response.body())
    }

}
