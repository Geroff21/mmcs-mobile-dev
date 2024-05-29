package com.example.timeworthapp.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import androidx.room.OnConflictStrategy
import com.example.timeworthapp.model.Note

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)
    @Update
    suspend fun updateNote(note: Note)
    @Delete
    suspend fun deleteNote(note: Note)
    @Query("SELECT * FROM NOTES ORDER BY id DESC")
    fun getAllNotes(): LiveData<List<Note>>
    @Query("SELECT * FROM NOTES WHERE noteTitle LIKE :query OR noteDesc LIKE :query")
    fun searchNote(query: String?): LiveData<List<Note>>
}