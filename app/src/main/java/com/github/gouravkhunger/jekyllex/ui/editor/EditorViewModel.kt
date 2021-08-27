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

package com.github.gouravkhunger.jekyllex.ui.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.gouravkhunger.jekyllex.models.CommitModel
import com.github.gouravkhunger.jekyllex.repositories.GithubContentRepository
import com.github.gouravkhunger.jekyllex.util.getMetaDataEndIndex
import kotlinx.coroutines.launch

class EditorViewModel(
    // repository which this view model interacts with to get/set data.
    private val repository: GithubContentRepository
) : ViewModel() {

    // Observable live data variables.
    val scrollDist: MutableLiveData<Int> by lazy { MutableLiveData() }
    val text: MutableLiveData<String> by lazy { MutableLiveData() }
    val postMetaData: MutableLiveData<String> by lazy { MutableLiveData() }
    val originalContent: MutableLiveData<String?> by lazy { MutableLiveData() }
    val isTextUpdated: MutableLiveData<Boolean> by lazy { MutableLiveData(false) }
    val isUploaded: MutableLiveData<Boolean> by lazy { MutableLiveData() }

    // Function to set the Scroll View scroll distance.
    fun setScrollDist(newDist: Int) {
        scrollDist.postValue(newDist)
    }

    // Function to update the text in the preview pane.
    fun setNewText(newText: String) {
        text.postValue(newText)
        viewModelScope.launch {
            isTextUpdated.postValue(newText != originalContent.value)
        }
    }

    // save Meta-data for the current post
    fun saveMetaData(data: String) {
        postMetaData.postValue(data)
    }

    // Function to get the raw content of a post file from github
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

    // Function to upload the file to github repo.
    fun uploadPost(
        commitModel: CommitModel,
        currentRepo: String,
        path: String,
        accessToken: String
    ) = viewModelScope.launch {
        val res = repository.updateFileContent(commitModel, currentRepo, path, accessToken)
        isUploaded.postValue(res.isSuccessful)
        isTextUpdated.postValue(!res.isSuccessful)
    }
}
