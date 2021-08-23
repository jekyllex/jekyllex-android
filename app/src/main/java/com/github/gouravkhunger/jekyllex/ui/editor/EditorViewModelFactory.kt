package com.github.gouravkhunger.jekyllex.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.gouravkhunger.jekyllex.repositories.GithubContentRepository

class EditorViewModelFactory (
    private val repository: GithubContentRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EditorViewModel(repository) as T
    }

}