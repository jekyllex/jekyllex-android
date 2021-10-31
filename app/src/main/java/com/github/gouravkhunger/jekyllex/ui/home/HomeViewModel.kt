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

package com.github.gouravkhunger.jekyllex.ui.home

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.gouravkhunger.jekyllex.models.repository.RepoModel
import com.github.gouravkhunger.jekyllex.repositories.GithubContentRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    // repository which this view model interacts with to get/set data.
    private val repoModel: GithubContentRepository
) : ViewModel() {

    // Live data representing the list of repositories owned by the user
    val userRepos: MutableLiveData<RepoModel> by lazy { MutableLiveData() }

    // Live data representing the current page of repositories user is viewing.
    val hasNext: MutableLiveData<Boolean> by lazy { MutableLiveData(false) }

    private var last = 1

    // function to get the list of repositories owned by the user, from the
    // GitHub api.
    fun getUserRepositories(pg: Int, per_page: Int, accessToken: String) = viewModelScope.launch {
        userRepos.postValue(RepoModel())
        val response = repoModel.getUserRepositories(pg, per_page, accessToken)

        val paginationHeaders = response.headers()["link"]
        val splitList = paginationHeaders?.split(",")

        Log.d("Split list", splitList.toString())

        var found = false

        run loop@{
            splitList?.forEach {
                if ("next" in it) {
                    hasNext.postValue(true)
                    found = true
                    return@loop
                }
            }
        }

        if (!found) hasNext.postValue(false)

        userRepos.postValue(response.body())
    }
}
