package com.clinetar.dnmw

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.SecureRandom

object KeyManager {
    private const val PREFS_NAME = "dnmw_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    // Kept for one-time migration of databases created before the Keystore key existed.
    internal const val LEGACY_KEY = "dnmw_internal_secure_key"

    fun getInternalDbKey(context: Context): String {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val prefs = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return prefs.getString(KEY_DB_PASSPHRASE, null) ?: run {
            val passphrase = generatePassphrase()
            prefs.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()
            passphrase
        }
    }

    private fun generatePassphrase(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
