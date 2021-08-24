package com.github.gouravkhunger.jekyllex.db.userdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.gouravkhunger.jekyllex.models.user.UserModel

// Database configurations
@Database(
    entities = [UserModel::class],
    version = 1
)
@TypeConverters(IdentityConverter::class)
abstract class UserDataBase : RoomDatabase() {

    // DAO object
    abstract fun getUserDao(): UserDao

    companion object {
        @Volatile
        private var instance: UserDataBase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: createDatabase(context).also { instance = it }
        }

        // function to setup Room database
        private fun createDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                UserDataBase::class.java,
                "user.db"
            ).build()

    }

}
