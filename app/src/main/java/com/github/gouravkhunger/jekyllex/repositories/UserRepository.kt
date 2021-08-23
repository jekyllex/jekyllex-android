package com.github.gouravkhunger.jekyllex.repositories

import com.github.gouravkhunger.jekyllex.apis.jekyllex.JekyllExApiInstance
import com.github.gouravkhunger.jekyllex.db.userdb.UserDataBase
import com.github.gouravkhunger.jekyllex.models.user.UserModel

class UserRepository(
    private val db: UserDataBase
) {

    suspend fun getUserData(id: String, accessToken: String) =
        JekyllExApiInstance.api.getUserData(id, accessToken)

    suspend fun saveUser(user: UserModel) =
        db.getUserDao().upsert(user)

    suspend fun deleteUser(user: UserModel) =
        db.getUserDao().deleteUser(user)

    fun getSavedUser(id: String) = db.getUserDao().getUser(id)

}