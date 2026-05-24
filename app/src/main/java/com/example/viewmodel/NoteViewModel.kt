package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppConfig
import com.example.data.AppDatabase
import com.example.data.Category
import com.example.data.Note
import com.example.data.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    
    // UI status states
    val categories: StateFlow<List<Category>>
    val allNotes: StateFlow<List<Note>>
    
    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered Notes
    val filteredNotes: StateFlow<List<Note>>

    // Flow for PIN setup / secure state
    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isBiometricsEnabled = MutableStateFlow(false)
    val isBiometricsEnabled: StateFlow<Boolean> = _isBiometricsEnabled.asStateFlow()

    // State for dialog / note detail view
    private val _editingNote = MutableStateFlow<Note?>(null)
    val editingNote: StateFlow<Note?> = _editingNote.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NoteRepository(database.noteDao())
        
        categories = repository.allCategories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allNotes = repository.allNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Combine filter logic: selected category + search query + notes list
        filteredNotes = combine(allNotes, _selectedCategoryId, _searchQuery) { notes, categoryId, query ->
            var result = notes
            if (categoryId != null) {
                result = result.filter { it.categoryId == categoryId }
            }
            if (query.isNotBlank()) {
                result = result.filter { 
                    it.title.contains(query, ignoreCase = true) || 
                    it.content.contains(query, ignoreCase = true) 
                }
            }
            result
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Read configuration settings from local secure entities
        viewModelScope.launch {
            checkRegistration()
        }
    }

    private suspend fun checkRegistration() {
        val pinConfig = repository.getConfig("secure_pin")
        val biometricConfig = repository.getConfig("biometric_enabled")
        
        _isRegistered.value = pinConfig != null && pinConfig.configValue.isNotBlank()
        _isBiometricsEnabled.value = biometricConfig?.configValue == "true"
    }

    // Security Management
    fun registerPin(pin: String, enableBiometrics: Boolean) {
        viewModelScope.launch {
            repository.insertConfig(AppConfig("secure_pin", pin))
            repository.insertConfig(AppConfig("biometric_enabled", if (enableBiometrics) "true" else "false"))
            checkRegistration()
            _isAuthenticated.value = true // Automatically log in on setup
        }
    }

    fun authenticatePin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val pinConfig = repository.getConfig("secure_pin")
            val success = pinConfig?.configValue == pin
            if (success) {
                _isAuthenticated.value = true
            }
            onResult(success)
        }
    }

    fun logout() {
        _isAuthenticated.value = false
    }

    fun verifyBiometricSuccess() {
        _isAuthenticated.value = true
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.insertConfig(AppConfig("biometric_enabled", if (enabled) "true" else "false"))
            _isBiometricsEnabled.value = enabled
        }
    }

    // Category Operations
    fun selectCategory(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
    }

    fun setQuery(query: String) {
        _searchQuery.value = query
    }

    fun addCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, colorHex = colorHex, iconName = iconName))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            if (_selectedCategoryId.value == category.id) {
                _selectedCategoryId.value = null
            }
        }
    }

    // Note Operations
    fun saveNote(
        id: Int = 0,
        title: String,
        content: String,
        categoryId: Int?,
        bgColorHex: String,
        audioPath: String?,
        audioDuration: Long,
        isPinned: Boolean = false
    ) {
        viewModelScope.launch {
            val note = Note(
                id = id,
                title = title,
                content = content,
                categoryId = categoryId,
                backgroundColorHex = bgColorHex,
                audioFilePath = audioPath,
                audioDurationMs = audioDuration,
                isPinned = isPinned,
                createdAt = if (id == 0) System.currentTimeMillis() else {
                    // Retain past date if updating
                    repository.getNoteById(id)?.createdAt ?: System.currentTimeMillis()
                }
            )
            repository.insertNote(note)
            _editingNote.value = null
        }
    }

    fun togglePinNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun setEditingNote(note: Note?) {
        _editingNote.value = note
    }
}
