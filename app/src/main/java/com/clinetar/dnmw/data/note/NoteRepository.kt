package com.clinetar.dnmw.data.note

import android.content.Context
import androidx.room.Room
import com.clinetar.dnmw.BuildConfig
import com.clinetar.dnmw.KeyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

class NoteRepository(private val context: Context) {
    private var database: NoteDatabase? = null
    private val _dbFlow = MutableStateFlow<NoteDatabase?>(null)

    fun getNotes(path: String?, encrypted: Boolean, password: String?): Flow<List<Note>> {
        if (path.isNullOrBlank()) return flowOf(emptyList())
        return getDatabase(path, encrypted, password).noteDao().getAllNotes()
    }

    fun getFavoriteNotes(path: String?, encrypted: Boolean, password: String?): Flow<List<Note>> {
        if (path.isNullOrBlank()) return flowOf(emptyList())
        return getDatabase(path, encrypted, password).noteDao().getFavoriteNotes()
    }

    fun closeDatabase() {
        database?.close()
        database = null
        _dbFlow.value = null
    }

    private fun getDatabase(path: String, encrypted: Boolean, password: String?): NoteDatabase {
        database?.let { return it }

        val dbFile = File(path)

        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            // Already loaded
        }

        if (dbFile.exists() && encrypted && !password.isNullOrBlank()) {
            if (!canOpenDatabase(path, password)) {
                when {
                    canOpenDatabase(path, KeyManager.LEGACY_KEY) ->
                        try { reencryptDatabase(path, KeyManager.LEGACY_KEY, password) }
                        catch (e: Exception) { deleteDbFiles(path) }
                    // Empty-key covers unencrypted exports from this app (destPassword = null → key = "")
                    canOpenDatabase(path, "") ->
                        try { reencryptDatabase(path, "", password) }
                        catch (e: Exception) { deleteDbFiles(path) }
                    else -> deleteDbFiles(path)
                }
            }
        }

        val builder = Room.databaseBuilder(
            context.applicationContext,
            NoteDatabase::class.java,
            dbFile.absolutePath
        )

        if (encrypted && !password.isNullOrBlank()) {
            builder.openHelperFactory(SupportOpenHelperFactory(password.toByteArray()))
        }

        val db = builder
            .addMigrations(NoteDatabase.MIGRATION_1_2)
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)
            .build()
        database = db
        _dbFlow.value = db
        return db
    }

    /** Re-encrypts an imported database with the app's Keystore key, trying known fallback keys. */
    fun reencryptImportedDatabase(path: String, importPassword: String?, keystoreKey: String) {
        try { System.loadLibrary("sqlcipher") } catch (e: UnsatisfiedLinkError) { }

        if (canOpenDatabase(path, keystoreKey)) return // already using our key

        val openedWith = when {
            !importPassword.isNullOrBlank() && canOpenDatabase(path, importPassword) -> importPassword
            canOpenDatabase(path, KeyManager.LEGACY_KEY) -> KeyManager.LEGACY_KEY
            canOpenDatabase(path, "") -> ""
            else -> throw IllegalStateException("Cannot open database: incorrect password")
        }

        reencryptDatabase(path, openedWith, keystoreKey)
    }

    private fun canOpenDatabase(path: String, password: String): Boolean {
        return try {
            net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                path, password, null,
                net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY, null
            ).use { true }
        } catch (e: Exception) {
            false
        }
    }

    private fun reencryptDatabase(path: String, oldPassword: String, newPassword: String) {
        val tempFile = File(context.filesDir, "reencrypt_temp.db")
        if (tempFile.exists()) tempFile.delete()
        tempFile.createNewFile()

        val sqlDb = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
            path, oldPassword, null,
            net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE, null
        )
        try {
            sqlDb.execSQL("ATTACH DATABASE '${tempFile.absolutePath}' AS rekey KEY '$newPassword'")
            sqlDb.rawQuery("SELECT sqlcipher_export('rekey')", null).use { it?.moveToFirst() }
            sqlDb.execSQL("DETACH DATABASE rekey")
        } finally {
            sqlDb.close()
        }

        val srcFile = File(path)
        srcFile.delete()
        File("$path-wal").delete()
        File("$path-shm").delete()
        tempFile.renameTo(srcFile)
    }

    private fun deleteDbFiles(path: String) {
        File(path).delete()
        File("$path-wal").delete()
        File("$path-shm").delete()
    }

    suspend fun insertNote(path: String?, encrypted: Boolean, password: String?, note: Note) {
        if (path == null) return
        getDatabase(path, encrypted, password).noteDao().insertNote(note)
    }

    suspend fun deleteNote(path: String?, encrypted: Boolean, password: String?, note: Note) {
        if (path == null) return
        getDatabase(path, encrypted, password).noteDao().deleteNote(note)
    }

    suspend fun updateNote(path: String?, encrypted: Boolean, password: String?, note: Note) {
        if (path == null) return
        getDatabase(path, encrypted, password).noteDao().updateNote(note)
    }

    suspend fun exportDatabase(
        sourcePath: String,
        sourceEncrypted: Boolean,
        sourcePassword: String?,
        destUri: android.net.Uri,
        destPassword: String?
    ) {
        closeDatabase()

        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            if (BuildConfig.DEBUG) android.util.Log.e("DNMW", "Failed to load sqlcipher: ${e.message}", e)
            throw e
        }

        val tempFile = File(context.filesDir, "export_temp.db")
        if (tempFile.exists()) tempFile.delete()
        tempFile.createNewFile()

        val sqlDb = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
            sourcePath, sourcePassword, null,
            net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE, null
        )

        try {
            val alias = "export_db"
            val key = destPassword ?: ""
            sqlDb.execSQL("ATTACH DATABASE '${tempFile.absolutePath}' AS $alias KEY '$key'")
            sqlDb.rawQuery("SELECT sqlcipher_export('$alias')", null).use { it?.moveToFirst() }
            sqlDb.execSQL("DETACH DATABASE $alias")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("DNMW", "Export failed: ${e.message}", e)
            throw e
        } finally {
            sqlDb.close()
        }

        if (tempFile.exists() && tempFile.length() > 0) {
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                tempFile.inputStream().use { input -> input.copyTo(output) }
            }
        }
        if (tempFile.exists()) tempFile.delete()
    }
}
