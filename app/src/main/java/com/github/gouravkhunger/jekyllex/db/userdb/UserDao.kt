package com.github.gouravkhunger.jekyllex.db.userdb

import androidx.lifecycle.LiveData
import androidx.room.*
import com.github.gouravkhunger.jekyllex.models.user.UserModel

@Dao
interface UserDao {

    // upsert-> insert or update
    // insert the value to database if not present
    // update if already present
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserModel): Long

    // get a saved user from the local database
    @Query("SELECT * FROM user WHERE user_id = :id")
    fun getUser(id: String): LiveData<UserModel>

    // delete a user
    @Delete
    suspend fun deleteUser(user: UserModel)

}
