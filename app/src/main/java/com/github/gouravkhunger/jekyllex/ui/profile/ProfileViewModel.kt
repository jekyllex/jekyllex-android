package com.github.gouravkhunger.jekyllex.ui.profile

import androidx.lifecycle.ViewModel
import com.github.gouravkhunger.jekyllex.repositories.UserRepository

class ProfileViewModel(
    private val repository: UserRepository
) : ViewModel() {

    fun getUserProfile(id: String) = repository.getSavedUser(id)
}