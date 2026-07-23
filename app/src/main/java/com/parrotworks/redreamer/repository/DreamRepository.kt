package com.parrotworks.redreamer.repository

import com.parrotworks.redreamer.data.Dream
import com.parrotworks.redreamer.data.DreamDao
import com.parrotworks.redreamer.data.DreamTagCrossRef
import com.parrotworks.redreamer.data.DreamWithTags
import com.parrotworks.redreamer.data.Mood
import com.parrotworks.redreamer.data.Tag
import com.parrotworks.redreamer.data.TagDao
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DreamRepository @Inject constructor(
    private val dreamDao: DreamDao,
    private val tagDao: TagDao,
) {
    fun observeLiveDreams(): Flow<List<DreamWithTags>> = dreamDao.observeLiveDreams()

    fun observeBinnedDreams(): Flow<List<DreamWithTags>> = dreamDao.observeBinnedDreams()

    fun observeDream(id: Long): Flow<DreamWithTags?> = dreamDao.observeDreamWithTags(id)

    fun observeAllTags(): Flow<List<Tag>> = tagDao.observeAllTags()

    /** Creates a dream when [id] is null, otherwise updates it in place without touching [Dream.createdAt]. */
    suspend fun saveDream(
        id: Long?,
        title: String,
        content: String,
        notes: String,
        dreamDate: LocalDate,
        isLucid: Boolean,
        lucidity: Int?,
        clarity: Int,
        isNightmare: Boolean,
        isRecurring: Boolean,
        moods: Set<Mood>,
        tagNames: List<String>,
        existingCreatedAt: Instant? = null,
    ): Long {
        val now = Instant.now()
        val dream = Dream(
            id = id ?: 0,
            title = title,
            content = content,
            notes = notes,
            dreamDate = dreamDate,
            createdAt = existingCreatedAt ?: now,
            updatedAt = now,
            isLucid = isLucid,
            lucidity = if (isLucid) lucidity else null,
            clarity = clarity,
            isNightmare = isNightmare,
            isRecurring = isRecurring,
            moods = moods,
        )

        val dreamId = if (id == null) {
            dreamDao.insertDream(dream)
        } else {
            dreamDao.updateDream(dream)
            id
        }

        val tagIds = tagNames
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .map { name -> resolveTagId(name) }

        dreamDao.clearTagsForDream(dreamId)
        dreamDao.insertCrossRefs(tagIds.map { tagId -> DreamTagCrossRef(dreamId = dreamId, tagId = tagId) })

        return dreamId
    }

    suspend fun softDelete(id: Long) {
        dreamDao.softDelete(id, Instant.now())
    }

    suspend fun softDeleteAll(ids: List<Long>) {
        dreamDao.softDeleteAll(ids, Instant.now())
    }

    suspend fun restore(id: Long) {
        dreamDao.restore(id)
    }

    suspend fun deleteForever(id: Long) {
        dreamDao.deleteForever(id)
    }

    /** Permanently removes bin entries older than [BIN_RETENTION_DAYS]. Call once per app launch. */
    suspend fun purgeExpiredFromBin() {
        val cutoff = Instant.now().minus(BIN_RETENTION_DAYS, ChronoUnit.DAYS)
        dreamDao.purgeDeletedBefore(cutoff)
    }

    /** Finds a tag by name or creates it, guarding against a lost race on the unique index. */
    private suspend fun resolveTagId(name: String): Long {
        tagDao.findByName(name)?.let { return it.id }
        val insertedId = tagDao.insertTag(Tag(name = name))
        if (insertedId != -1L) return insertedId
        return tagDao.findByName(name)?.id
            ?: error("Tag '$name' could not be created or found")
    }

    companion object {
        const val BIN_RETENTION_DAYS = 30L
    }
}
