package com.github.gouravkhunger.jekyllex.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.gouravkhunger.jekyllex.repositories.UserRepository

class ProfileViewModelFactory(
    private val repository: UserRepository
) :  ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ProfileViewModel(repository) as T
    }

}
