package com.clinetar.dnmw.data.note

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

class NoteRepository(private val context: Context) {
    private var database: NoteDatabase? = null
    private val _dbFlow = MutableStateFlow<NoteDatabase?>(null)

    fun getNotes(path: String?, encrypted: Boolean, password: String?): Flow<List<Note>> {
        if (path.isNullOrBlank()) return flowOf(emptyList())
        
        val db = getDatabase(path, encrypted, password)
        return db.noteDao().getAllNotes()
    }

    fun getFavoriteNotes(path: String?, encrypted: Boolean, password: String?): Flow<List<Note>> {
        if (path.isNullOrBlank()) return flowOf(emptyList())

        val db = getDatabase(path, encrypted, password)
        return db.noteDao().getFavoriteNotes()
    }

    fun closeDatabase() {
        database?.close()
        database = null
        _dbFlow.value = null
    }

    private fun getDatabase(path: String, encrypted: Boolean, password: String?): NoteDatabase {
        val currentDb = database
        if (currentDb != null) {
            return currentDb
        }

        val dbFile = File(path)
        
        // Ensure SQLCipher is loaded before any database operation
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            // Already loaded or other issue
        }

        // Check if existing database is compatible with the provided password
        if (dbFile.exists() && encrypted && !password.isNullOrBlank()) {
            try {
                net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                    path, 
                    password, 
                    null, 
                    net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                    null
                ).use { it.close() }
            } catch (e: Exception) {
                // If it's a key mismatch (SQLiteNotADatabaseException), we must reset
                // to avoid crash loops. In a production app, we might prompt the user,
                // but for internal-only keys, we recreate.
                dbFile.delete()
                File("$path-wal").delete()
                File("$path-shm").delete()
            }
        }

        val builder = Room.databaseBuilder(
            context.applicationContext,
            NoteDatabase::class.java,
            dbFile.absolutePath
        )

        if (encrypted && !password.isNullOrBlank()) {
            val factory = SupportOpenHelperFactory(password.toByteArray())
            builder.openHelperFactory(factory)
        }

        val db = builder.fallbackToDestructiveMigration()
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)
            .build()
        database = db
        _dbFlow.value = db
        return db
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
        // Use the files directory for the temp file to avoid cache/permission issues with ATTACH
        val tempFile = File(context.filesDir, "export_temp.db")
        if (tempFile.exists()) tempFile.delete()

        // Use SQLCipher to export/rekey the database
        val database = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
            sourcePath,
            sourcePassword,
            null,
            net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE,
            null
        )

        try {
            val alias = "export_db"
            val key = destPassword ?: ""

            // Log for debugging (remove in production)
            android.util.Log.d("DNMW", "Attaching temp DB: ${tempFile.absolutePath}")

            // Try with simple string concatenation first, ensure path is valid
            database.execSQL("ATTACH DATABASE '${tempFile.absolutePath}' AS $alias KEY '$key'")

            database.rawQuery("SELECT sqlcipher_export('$alias')", null).use { cursor ->
                cursor?.moveToFirst()
            }

            database.execSQL("DETACH DATABASE $alias")
        } catch (e: Exception) {
            android.util.Log.e("DNMW", "Export failed at SQL level: ${e.message}", e)
            throw e
        } finally {
            database.close()
        }

        // Copy temp file to destination URI
        if (tempFile.exists() && tempFile.length() > 0) {
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
        }
        if (tempFile.exists()) tempFile.delete()
    }
}
