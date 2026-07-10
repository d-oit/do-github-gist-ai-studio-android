package com.example.data.local.pref

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigPrefs @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("gist_config_prefs", Context.MODE_PRIVATE)

    private val masterKeyAlias by lazy {
        try {
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        } catch (e: Exception) {
            Log.e("ConfigPrefs", "Failed to get/create master key alias", e)
            ""
        }
    }

    private val securePrefs by lazy {
        try {
            if (masterKeyAlias.isNotEmpty()) {
                EncryptedSharedPreferences.create(
                    "secure_gist_config_prefs",
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ConfigPrefs", "Failed to initialize EncryptedSharedPreferences, falling back", e)
            null
        }
    }

    private fun getSecureString(key: String, defaultValue: String = ""): String {
        val sp = securePrefs
        return if (sp != null) {
            sp.getString(key, defaultValue) ?: defaultValue
        } else {
            prefs.getString(key, defaultValue) ?: defaultValue
        }
    }

    private fun setSecureString(key: String, value: String) {
        val sp = securePrefs
        if (sp != null) {
            sp.edit().putString(key, value).apply()
        } else {
            prefs.edit().putString(key, value).apply()
        }
    }

    private fun getSecureInt(key: String, defaultValue: Int = 0): Int {
        val sp = securePrefs
        return if (sp != null) {
            sp.getInt(key, defaultValue)
        } else {
            prefs.getInt(key, defaultValue)
        }
    }

    private fun setSecureInt(key: String, value: Int) {
        val sp = securePrefs
        if (sp != null) {
            sp.edit().putInt(key, value).apply()
        } else {
            prefs.edit().putInt(key, value).apply()
        }
    }

    fun getGithubToken(): String {
        val savedSecure = getSecureString("github_token", "")
        if (savedSecure.isNotEmpty()) {
            return savedSecure
        }
        val savedPlain = prefs.getString("github_token", "") ?: ""
        if (savedPlain.isNotEmpty()) {
            setGithubToken(savedPlain)
            prefs.edit().remove("github_token").apply()
            return savedPlain
        }
        val configPat = try {
            com.example.BuildConfig.GITHUB_PAT
        } catch (e: Exception) {
            ""
        }
        if (configPat.isNotEmpty() && configPat != "YOUR_GITHUB_PERSONAL_ACCESS_TOKEN_HERE") {
            return configPat
        }
        return ""
    }

    fun setGithubToken(token: String) {
        setSecureString("github_token", token)
    }

    fun getOwnerLogin(): String {
        val savedSecure = getSecureString("owner_login", "")
        if (savedSecure.isNotEmpty()) {
            return savedSecure
        }
        val savedPlain = prefs.getString("owner_login", "") ?: ""
        if (savedPlain.isNotEmpty() && savedPlain != "anonymous") {
            setOwnerLogin(savedPlain)
            prefs.edit().remove("owner_login").apply()
            return savedPlain
        }
        return "anonymous"
    }

    fun setOwnerLogin(login: String) {
        setSecureString("owner_login", login)
    }

    fun getOwnerAvatarUrl(): String {
        val savedSecure = getSecureString("owner_avatar_url", "")
        if (savedSecure.isNotEmpty()) {
            return savedSecure
        }
        val savedPlain = prefs.getString("owner_avatar_url", "") ?: ""
        if (savedPlain.isNotEmpty()) {
            setOwnerAvatarUrl(savedPlain)
            prefs.edit().remove("owner_avatar_url").apply()
            return savedPlain
        }
        return ""
    }

    fun setOwnerAvatarUrl(url: String) {
        setSecureString("owner_avatar_url", url)
    }

    fun getOwnerId(): Int {
        val savedSecure = getSecureInt("owner_id", -1)
        if (savedSecure != -1) {
            return savedSecure
        }
        val savedPlain = prefs.getInt("owner_id", -1)
        if (savedPlain != -1) {
            setOwnerId(savedPlain)
            prefs.edit().remove("owner_id").apply()
            return savedPlain
        }
        return 0
    }

    fun setOwnerId(id: Int) {
        setSecureInt("owner_id", id)
    }

    fun getTheme(): String {
        return prefs.getString("app_theme", "light") ?: "light"
    }

    fun setTheme(theme: String) {
        prefs.edit().putString("app_theme", theme).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
        try {
            securePrefs?.edit()?.clear()?.apply()
        } catch (e: Exception) {
            Log.e("ConfigPrefs", "Failed to clear secure preferences", e)
        }
    }
}
