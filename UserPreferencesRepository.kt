package com.shopizzo.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.dataStore

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = try {
        createEncryptedPrefs(context, masterKey)
    } catch (e: Exception) {
        // If decryption fails (e.g. keystore corrupted), delete the file and try again
        context.deleteSharedPreferences("secure_user_prefs")
        createEncryptedPrefs(context, masterKey)
    }

    private fun createEncryptedPrefs(context: Context, masterKey: MasterKey) = EncryptedSharedPreferences.create(
        context,
        "secure_user_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _biometricEnabled = MutableStateFlow(encryptedPrefs.getBoolean("biometric_enabled", false))
    val biometricEnabledFlow: Flow<Boolean> = _biometricEnabled.asStateFlow()

    companion object {
        val THEME_KEY = stringPreferencesKey("app_theme")
    }

    val themeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "SYSTEM"
    }

    suspend fun saveTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    fun saveBiometricEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _biometricEnabled.value = enabled
    }
}
