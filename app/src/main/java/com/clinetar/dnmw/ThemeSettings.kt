package com.clinetar.dnmw

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

enum class CustomColor(val colorName: String, val hex: Long?) {
    DYNAMIC("Wallpaper", null),
    RED("Red", 0xFFFF0000),
    BLUE("Blue", 0xFF0000FF),
    GREEN("Green", 0xFF00FF00),
    PURPLE("Purple", 0xFFAA00FF),
    ORANGE("Orange", 0xFFFF6D00),
    PINK("Pink", 0xFFFF4081),
    CYAN("Cyan", 0xFF00E5FF)
}

class ThemeSettings(private val context: Context) {
    companion object {
        val THEME_KEY = stringPreferencesKey("theme_setting")
        val COLOR_KEY = stringPreferencesKey("custom_color")
        val DATABASE_PATH_KEY = stringPreferencesKey("database_path")
        val IS_ENCRYPTED_KEY = booleanPreferencesKey("is_encrypted")
    }

    val themeStream: Flow<AppTheme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: AppTheme.SYSTEM.name
            try { AppTheme.valueOf(themeName) } catch (e: Exception) { AppTheme.SYSTEM }
        }

    val customColorStream: Flow<CustomColor> = context.dataStore.data
        .map { preferences ->
            val colorName = preferences[COLOR_KEY] ?: CustomColor.DYNAMIC.name
            try { CustomColor.valueOf(colorName) } catch (e: Exception) { CustomColor.DYNAMIC }
        }

    val databasePathStream: Flow<String?> = context.dataStore.data
        .map { it[DATABASE_PATH_KEY] }

    val isEncryptedStream: Flow<Boolean> = context.dataStore.data
        .map { it[IS_ENCRYPTED_KEY] ?: false }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[THEME_KEY] = theme.name }
    }

    suspend fun setCustomColor(color: CustomColor) {
        context.dataStore.edit { it[COLOR_KEY] = color.name }
    }

    suspend fun setDatabasePath(path: String) {
        context.dataStore.edit { it[DATABASE_PATH_KEY] = path }
    }

    suspend fun setIsEncrypted(enabled: Boolean) {
        context.dataStore.edit { it[IS_ENCRYPTED_KEY] = enabled }
    }
}
