/*
 * MIT License
 *
 * Copyright (c) 2024 Gourav Khunger
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

package xyz.jekyllex.utils

import android.content.Context
import android.preference.PreferenceManager

class Settings(context: Context) {
    @Suppress("DEPRECATION")
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Suppress("UNCHECKED_CAST")
    fun <T> get(setting: Setting): T {
        return when (val type = setting.defaultValue) {
            is SettingType.FloatValue -> sharedPreferences.getFloat(setting.key, type.value) as T
            is SettingType.StringValue -> sharedPreferences.getString(setting.key, type.value) as T
            is SettingType.BooleanValue -> sharedPreferences.getBoolean(setting.key, type.value) as T
        }
    }
}

sealed class SettingType {
    inline fun <reified T> get(): T {
        return when (this) {
            is FloatValue -> value as T
            is StringValue -> value as T
            is BooleanValue -> value as T
        }
    }

    data class FloatValue(val value: Float) : SettingType()
    data class StringValue(val value: String) : SettingType()
    data class BooleanValue(val value: Boolean) : SettingType()
}

enum class Setting(val key: String, val defaultValue: SettingType) {
    // Git
    GIT_NAME("git_name", SettingType.StringValue("")),
    GIT_EMAIL("git_email", SettingType.StringValue("")),
    GITHUB_PAT("github_pat", SettingType.StringValue("")),
    LOG_PROGRESS("log_progress", SettingType.BooleanValue(true)),

    // General
    TRIM_LOGS("trim_logs", SettingType.BooleanValue(true)),
    DEBOUNCE_DELAY("debounce_delay", SettingType.FloatValue(1f)),
    REDUCE_ANIMATIONS("reduce_animations", SettingType.BooleanValue(false)),

    // Bundler
    LOCAL_GEMS("local_gems", SettingType.BooleanValue(true)),

    // Jekyll
    GUESS_URLS("guess_urls", SettingType.BooleanValue(true)),
    JEKYLL_FLAGS("jekyll_flags", SettingType.StringValue("")),
    SKIP_BUNDLER("skip_bundle", SettingType.BooleanValue(false)),
    LIVERELOAD("enable_livereload", SettingType.BooleanValue(true)),
    PREFIX_BUNDLER("prefix_bundler", SettingType.BooleanValue(true)),
    JEKYLL_ENV("jekyll_env", SettingType.StringValue("development")),
}
