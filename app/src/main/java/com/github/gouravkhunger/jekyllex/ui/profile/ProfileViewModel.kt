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

package com.github.gouravkhunger.jekyllex.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.gouravkhunger.jekyllex.apis.jekyllex.JekyllExApiInstance
import com.github.gouravkhunger.jekyllex.models.user.UserModel
import com.github.gouravkhunger.jekyllex.repositories.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    // repository which this view model interacts with to get/set data.
    private val repository: UserRepository
) : ViewModel() {

    // Function that gets the user profile from the local room database.
    fun getUserProfile(id: String) = repository.getSavedUser(id)

    // Function to delete the user profile from the database.
    fun deleteUser(user: UserModel) = viewModelScope.launch {
        repository.deleteUser(user)
    }

    // Function to get user profile from the JekyllEx API.
    fun refreshUserProfile(id: String, accessToken: String) = viewModelScope.launch {
        val response = JekyllExApiInstance.api.getUserData(id, accessToken)
        if (response.isSuccessful) {
            repository.saveUser(response.body()!!)
            getUserProfile(id)
        }
    }
}
