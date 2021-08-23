package com.github.gouravkhunger.jekyllex.ui.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.gouravkhunger.jekyllex.models.CommitModel
import com.github.gouravkhunger.jekyllex.repositories.GithubContentRepository
import com.github.gouravkhunger.jekyllex.util.getMetaDataEndIndex
import kotlinx.coroutines.launch

class EditorViewModel(
    private val repository: GithubContentRepository
) : ViewModel() {

    val scrollDist: MutableLiveData<Int> by lazy { MutableLiveData() }
    val text: MutableLiveData<String> by lazy { MutableLiveData() }
    val postMetaData: MutableLiveData<String> by lazy { MutableLiveData() }
    val originalContent: MutableLiveData<String?> by lazy { MutableLiveData() }
    val isTextUpdated: MutableLiveData<Boolean> by lazy { MutableLiveData(false) }
    val isUploaded: MutableLiveData<Boolean> by lazy { MutableLiveData() }

    fun setScrollDist(newDist: Int) {
        scrollDist.postValue(newDist)
    }

    fun setNewText(newText: String) {
        text.postValue(newText)
        viewModelScope.launch {
            isTextUpdated.postValue(newText != originalContent.value)
        }
    }

    fun changeIsTextUpdated(value: Boolean) {
        isTextUpdated.postValue(value)
    }

    fun saveMetaData(data: String) {
        postMetaData.postValue(data)
    }

    fun getContent(
        repoName: String,
        path: String,
        accessToken: String
    ) = viewModelScope.launch {
        val response = repository.getRawContent(repoName, path, accessToken)
        if (response.isSuccessful) {
            val text = response.body().toString()
            val metaEndIdx = getMetaDataEndIndex(text)
            postMetaData.postValue(text.substring(0, metaEndIdx))
            originalContent.postValue(text.substring(metaEndIdx))
        } else {
            originalContent.postValue(null)
        }
    }

    fun uploadPost(
        commitModel: CommitModel,
        currentRepo: String,
        path: String,
        accessToken: String
    ) = viewModelScope.launch {
        val res = repository.updateFileContent(commitModel, currentRepo, path, accessToken)
        isUploaded.postValue(res.isSuccessful)
    }

}