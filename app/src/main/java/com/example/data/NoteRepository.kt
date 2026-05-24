package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allCategories: Flow<List<Category>> = noteDao.getAllCategories()
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun getNotesByCategory(categoryId: Int): Flow<List<Note>> = noteDao.getNotesByCategory(categoryId)

    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)

    suspend fun insertCategory(category: Category): Long = noteDao.insertCategory(category)

    suspend fun deleteCategory(category: Category) = noteDao.deleteCategory(category)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun getConfig(key: String): AppConfig? = noteDao.getConfig(key)

    suspend fun insertConfig(config: AppConfig) = noteDao.insertConfig(config)
}
