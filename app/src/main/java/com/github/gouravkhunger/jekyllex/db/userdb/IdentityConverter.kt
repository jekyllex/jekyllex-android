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

package com.github.gouravkhunger.jekyllex.db.userdb

import androidx.room.TypeConverter
import com.github.gouravkhunger.jekyllex.models.user.Identity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

// Custom converter class which configures saving User's Social Identity Providers
// to the local database.
class IdentityConverter {

    // Function to convert JSON to Identity data class for easy storing.
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

    // Function to convert Identity data class to JSON for easy storing.
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
