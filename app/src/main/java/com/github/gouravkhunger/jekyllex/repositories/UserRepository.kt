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

package com.github.gouravkhunger.jekyllex.repositories

import com.github.gouravkhunger.jekyllex.apis.jekyllex.JekyllExApiInstance
import com.github.gouravkhunger.jekyllex.db.userdb.UserDataBase
import com.github.gouravkhunger.jekyllex.models.user.UserModel

// Android MVVM repository - the source of data for view-models
class UserRepository(
    private val db: UserDataBase
) {
    // Function to get User's data from the JekyllEx api.
    suspend fun getUserData(id: String, accessToken: String) =
        JekyllExApiInstance.api.getUserData(id, accessToken)

    // Function to save a specific user's data in the local database.
    suspend fun saveUser(user: UserModel) =
        db.getUserDao().upsert(user)

    // Function to delete the specified saved user from the local database.
    suspend fun deleteUser(user: UserModel) =
        db.getUserDao().deleteUser(user)

    // Function to get a saved User's data.
    fun getSavedUser(id: String) = db.getUserDao().getUser(id)
}
