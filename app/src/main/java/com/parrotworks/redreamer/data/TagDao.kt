package com.parrotworks.redreamer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAllTags(): Flow<List<Tag>>

    @Query(
        """
        SELECT tags.id AS id, tags.name AS name, tags.color AS color,
               COUNT(dream_tag_cross_ref.dreamId) AS usageCount
        FROM tags
        LEFT JOIN dream_tag_cross_ref ON tags.id = dream_tag_cross_ref.tagId
        GROUP BY tags.id
        ORDER BY tags.name COLLATE NOCASE ASC
        """,
    )
    fun observeTagsWithUsage(): Flow<List<TagWithUsage>>

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Tag?

    @Query("UPDATE tags SET name = :newName WHERE id = :tagId")
    suspend fun renameTag(tagId: Long, newName: String)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTagById(tagId: Long)

    @Query("SELECT dreamId FROM dream_tag_cross_ref WHERE tagId = :tagId")
    suspend fun getDreamIdsForTag(tagId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(crossRefs: List<DreamTagCrossRef>)

    @Query("DELETE FROM dream_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteCrossRefsForTag(tagId: Long)

    /**
     * Reassigns every dream tagged with [sourceTagId] to [targetTagId] instead, then removes the
     * now-empty source tag. Cross-refs are inserted before the old ones are deleted (with
     * OnConflictStrategy.IGNORE) so a dream already carrying both tags doesn't hit the composite
     * primary key twice.
     */
    @Transaction
    suspend fun mergeTagInto(sourceTagId: Long, targetTagId: Long) {
        val dreamIds = getDreamIdsForTag(sourceTagId)
        insertCrossRefs(dreamIds.map { dreamId -> DreamTagCrossRef(dreamId = dreamId, tagId = targetTagId) })
        deleteCrossRefsForTag(sourceTagId)
        deleteTagById(sourceTagId)
    }
}
