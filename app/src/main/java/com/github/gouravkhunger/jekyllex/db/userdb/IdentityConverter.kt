package com.github.gouravkhunger.jekyllex.db.userdb

import androidx.room.TypeConverter
import com.github.gouravkhunger.jekyllex.models.user.Identity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class IdentityConverter {

    @TypeConverter
    fun fromIdentityList(countryLang: List<Identity?>?): String? {
        if (countryLang == null) {
            return null
        }
        val gson = Gson()
        val type: Type = object :
            TypeToken<List<Identity?>?>() {}.type
        return gson.toJson(countryLang, type)
    }

    @TypeConverter
    fun toIdentityList(identityString: String?): List<Identity>? {
        if (identityString == null) {
            return null
        }
        val gson = Gson()
        val type: Type = object :
            TypeToken<List<Identity?>?>() {}.type
        return gson.fromJson<List<Identity>>(identityString, type)
    }
}