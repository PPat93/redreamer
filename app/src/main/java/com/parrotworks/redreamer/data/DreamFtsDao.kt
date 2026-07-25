package com.parrotworks.redreamer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DreamFtsDao {
    @Insert
    suspend fun insert(entry: DreamFts)

    @Query("UPDATE dream_fts SET title = :title, content = :content, notes = :notes WHERE rowid = :dreamId")
    suspend fun update(dreamId: Long, title: String, content: String, notes: String)

    @Query("DELETE FROM dream_fts WHERE rowid = :dreamId")
    suspend fun deleteByDreamId(dreamId: Long)
}
