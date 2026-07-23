package com.parrotworks.redreamer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface DreamDao {
    @Insert
    suspend fun insertDream(dream: Dream): Long

    @Update
    suspend fun updateDream(dream: Dream)

    @Transaction
    @Query("SELECT * FROM dreams WHERE id = :id")
    fun observeDreamWithTags(id: Long): Flow<DreamWithTags?>

    @Transaction
    @Query("SELECT * FROM dreams WHERE deletedAt IS NULL ORDER BY dreamDate DESC, createdAt DESC")
    fun observeLiveDreams(): Flow<List<DreamWithTags>>

    @Transaction
    @Query("SELECT * FROM dreams WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeBinnedDreams(): Flow<List<DreamWithTags>>

    @Query("UPDATE dreams SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Instant)

    @Query("UPDATE dreams SET deletedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun softDeleteAll(ids: List<Long>, deletedAt: Instant)

    @Query("UPDATE dreams SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM dreams WHERE id = :id")
    suspend fun deleteForever(id: Long)

    @Query("DELETE FROM dreams WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeDeletedBefore(cutoff: Instant)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(crossRefs: List<DreamTagCrossRef>)

    @Query("DELETE FROM dream_tag_cross_ref WHERE dreamId = :dreamId")
    suspend fun clearTagsForDream(dreamId: Long)
}
