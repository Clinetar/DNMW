package com.clinetar.dnmw.data.note

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Note::class], version = 2, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
