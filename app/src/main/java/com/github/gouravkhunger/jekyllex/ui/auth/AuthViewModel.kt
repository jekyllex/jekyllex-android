package com.github.gouravkhunger.jekyllex.ui.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.gouravkhunger.jekyllex.models.user.UserModel
import com.github.gouravkhunger.jekyllex.repositories.UserRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: UserRepository
) : ViewModel() {

    val userData : MutableLiveData<UserModel?> by lazy { MutableLiveData() }
    val saved : MutableLiveData<Boolean> by lazy { MutableLiveData() }

    fun getUserData(id: String, accessToken: String) = viewModelScope.launch {
        val response = repository.getUserData(id, accessToken)
        if(response.isSuccessful) {
            userData.postValue(response.body())
        } else {
            userData.postValue(null)
        }
    }

    fun saveUser(user: UserModel) = viewModelScope.launch {
        repository.saveUser(user)
        saved.postValue(true)
    }

}