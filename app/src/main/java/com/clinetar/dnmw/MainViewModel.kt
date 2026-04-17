package com.clinetar.dnmw

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clinetar.dnmw.data.note.Note
import com.clinetar.dnmw.data.note.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val themeSettings = ThemeSettings(application)
    private val noteRepository = NoteRepository(application)

    val themeState: StateFlow<AppTheme> = themeSettings.themeStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    val customColorState: StateFlow<CustomColor> = themeSettings.customColorStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CustomColor.DYNAMIC
        )

    val pureBlackState: StateFlow<Boolean> = themeSettings.pureBlackStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val databasePathState: StateFlow<String?> = themeSettings.databasePathStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isEncryptedState: StateFlow<Boolean> = themeSettings.isEncryptedStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val dbPasswordState: StateFlow<String?> = themeSettings.dbPasswordStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val notesState: StateFlow<List<Note>> = databasePathState.flatMapLatest { path ->
        noteRepository.getNotes(path, true, "dnmw_internal_secure_key")
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteNotesState: StateFlow<List<Note>> = databasePathState.flatMapLatest { path ->
        noteRepository.getFavoriteNotes(path, true, "dnmw_internal_secure_key")
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            themeSettings.setTheme(theme)
        }
    }

    fun setCustomColor(color: CustomColor) {
        viewModelScope.launch {
            themeSettings.setCustomColor(color)
        }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch {
            themeSettings.setPureBlack(enabled)
        }
    }

    fun setDatabasePath(path: String) {
        viewModelScope.launch {
            themeSettings.setDatabasePath(path)
        }
    }

    fun setDatabaseName(name: String) {
        viewModelScope.launch {
            val internalDir = getApplication<Application>().filesDir
            val dbDir = java.io.File(internalDir, "databases")
            if (!dbDir.exists()) dbDir.mkdirs()
            val dbFile = java.io.File(dbDir, if (name.endsWith(".db")) name else "$name.db")
            themeSettings.setDatabasePath(dbFile.absolutePath)
        }
    }

    fun setIsEncrypted(enabled: Boolean) {
        viewModelScope.launch {
            themeSettings.setIsEncrypted(enabled)
        }
    }

    fun setDbPassword(password: String?) {
        viewModelScope.launch {
            themeSettings.setDbPassword(password ?: "")
        }
    }

    fun deleteDatabase() {
        viewModelScope.launch {
            val path = databasePathState.value
            if (path != null) {
                noteRepository.closeDatabase()
                val file = java.io.File(path)
                if (file.exists()) file.delete()
                // Also delete related files if any (e.g. -wal, -shm)
                java.io.File("$path-wal").delete()
                java.io.File("$path-shm").delete()
                themeSettings.setDatabasePath("")
            }
        }
    }

    fun importDatabase(uri: android.net.Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            noteRepository.closeDatabase()
            
            val internalDir = context.filesDir
            val dbDir = java.io.File(internalDir, "databases")
            if (!dbDir.exists()) dbDir.mkdirs()
            
            val fileName = "imported_${System.currentTimeMillis()}.db"
            val destFile = java.io.File(dbDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            themeSettings.setDatabasePath(destFile.absolutePath)
        }
    }

    fun exportDatabase(uri: android.net.Uri, password: String?, isEncrypted: Boolean) {
        viewModelScope.launch {
            val currentPath = databasePathState.value ?: return@launch
            noteRepository.exportDatabase(
                sourcePath = currentPath,
                sourceEncrypted = true,
                sourcePassword = "dnmw_internal_secure_key",
                destUri = uri,
                destPassword = if (isEncrypted) password else null
            )
            // Re-open database for normal operation
            themeSettings.setDatabasePath(currentPath)
        }
    }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            noteRepository.insertNote(
                databasePathState.value,
                true,
                "dnmw_internal_secure_key",
                Note(title = title, content = content)
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.deleteNote(
                databasePathState.value,
                true,
                "dnmw_internal_secure_key",
                note
            )
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            noteRepository.updateNote(
                databasePathState.value,
                true,
                "dnmw_internal_secure_key",
                note.copy(isFavorite = !note.isFavorite)
            )
        }
    }
}
