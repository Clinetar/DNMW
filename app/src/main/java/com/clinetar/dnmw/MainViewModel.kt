package com.clinetar.dnmw

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clinetar.dnmw.data.note.Note
import com.clinetar.dnmw.data.note.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val themeSettings = ThemeSettings(application)
    private val noteRepository = NoteRepository(application)

    val themeState: StateFlow<AppTheme> = themeSettings.themeStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    val customColorState: StateFlow<CustomColor> = themeSettings.customColorStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomColor.DYNAMIC)

    val pureBlackState: StateFlow<Boolean> = themeSettings.pureBlackStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val databasePathState: StateFlow<String?> = themeSettings.databasePathStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isEncryptedState: StateFlow<Boolean> = themeSettings.isEncryptedStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Loaded asynchronously on IO thread to avoid blocking the main thread during Keystore init.
    private val _internalDbKey = MutableStateFlow<String?>(null)

    val notesState: StateFlow<List<Note>> = combine(databasePathState, _internalDbKey) { path, key ->
        Pair(path, key)
    }.flatMapLatest { (path, key) ->
        if (key != null) noteRepository.getNotes(path, true, key)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteNotesState: StateFlow<List<Note>> = combine(databasePathState, _internalDbKey) { path, key ->
        Pair(path, key)
    }.flatMapLatest { (path, key) ->
        if (key != null) noteRepository.getFavoriteNotes(path, true, key)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _internalDbKey.value = KeyManager.getInternalDbKey(application)
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { themeSettings.setTheme(theme) }
    }

    fun setCustomColor(color: CustomColor) {
        viewModelScope.launch { themeSettings.setCustomColor(color) }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch { themeSettings.setPureBlack(enabled) }
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
        viewModelScope.launch { themeSettings.setIsEncrypted(enabled) }
    }

    fun deleteDatabase() {
        viewModelScope.launch {
            val path = databasePathState.value ?: return@launch
            noteRepository.closeDatabase()
            val file = java.io.File(path)
            if (file.exists()) file.delete()
            java.io.File("$path-wal").delete()
            java.io.File("$path-shm").delete()
            themeSettings.setDatabasePath("")
        }
    }

    fun importDatabase(uri: android.net.Uri, importPassword: String? = null) {
        viewModelScope.launch {
            val key = _internalDbKey.value ?: return@launch
            val context = getApplication<Application>()
            noteRepository.closeDatabase()
            val dbDir = java.io.File(context.filesDir, "databases")
            if (!dbDir.exists()) dbDir.mkdirs()
            val destFile = java.io.File(dbDir, "imported_${System.currentTimeMillis()}.db")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            try {
                noteRepository.reencryptImportedDatabase(destFile.absolutePath, importPassword, key)
                themeSettings.setDatabasePath(destFile.absolutePath)
            } catch (e: Exception) {
                destFile.delete()
                launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Import failed: incorrect password or unsupported file",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun exportDatabase(uri: android.net.Uri, password: String?, isEncrypted: Boolean) {
        viewModelScope.launch {
            val key = _internalDbKey.value ?: return@launch
            val currentPath = databasePathState.value ?: return@launch
            noteRepository.exportDatabase(
                sourcePath = currentPath,
                sourceEncrypted = true,
                sourcePassword = key,
                destUri = uri,
                destPassword = if (isEncrypted) password else null
            )
            themeSettings.setDatabasePath(currentPath)
        }
    }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            val key = _internalDbKey.value ?: return@launch
            noteRepository.insertNote(
                databasePathState.value, true, key,
                Note(title = title, content = content)
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            val key = _internalDbKey.value ?: return@launch
            noteRepository.deleteNote(databasePathState.value, true, key, note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            val key = _internalDbKey.value ?: return@launch
            noteRepository.updateNote(databasePathState.value, true, key, note)
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            val key = _internalDbKey.value ?: return@launch
            noteRepository.updateNote(
                databasePathState.value, true, key,
                note.copy(isFavorite = !note.isFavorite)
            )
        }
    }
}
