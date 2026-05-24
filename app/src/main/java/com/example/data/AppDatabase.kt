package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Category::class, Note::class, AppConfig::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "secure_notes_db"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Prepopulate categories on database creation
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = database.noteDao()
                        dao.insertCategory(Category(name = "Kişisel", colorHex = "#3B82F6", iconName = "Person"))    // Blue
                        dao.insertCategory(Category(name = "İş", colorHex = "#10B981", iconName = "BusinessCenter"))      // Green
                        dao.insertCategory(Category(name = "Fikirler", colorHex = "#F59E0B", iconName = "Lightbulb")) // Orange/Yellow
                        dao.insertCategory(Category(name = "Önemli", colorHex = "#EF4444", iconName = "PriorityHigh"))  // Red
                    }
                }
            }
        }
    }
}
