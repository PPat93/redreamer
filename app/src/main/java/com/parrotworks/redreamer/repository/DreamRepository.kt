package com.parrotworks.redreamer.repository

import com.parrotworks.redreamer.data.Dream
import com.parrotworks.redreamer.data.DreamDao
import com.parrotworks.redreamer.data.DreamFts
import com.parrotworks.redreamer.data.DreamFtsDao
import com.parrotworks.redreamer.data.DreamTagCrossRef
import com.parrotworks.redreamer.data.DreamWithTags
import com.parrotworks.redreamer.data.Mood
import com.parrotworks.redreamer.data.Tag
import com.parrotworks.redreamer.data.TagDao
import com.parrotworks.redreamer.data.TagWithUsage
import com.parrotworks.redreamer.data.backup.BackupDream
import com.parrotworks.redreamer.data.backup.BackupFile
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class DreamRepository @Inject constructor(
    private val dreamDao: DreamDao,
    private val tagDao: TagDao,
    private val dreamFtsDao: DreamFtsDao,
) {
    fun observeLiveDreams(): Flow<List<DreamWithTags>> = dreamDao.observeLiveDreams()

    fun observeBinnedDreams(): Flow<List<DreamWithTags>> = dreamDao.observeBinnedDreams()

    fun observeBinnedCount(): Flow<Int> = dreamDao.observeBinnedCount()

    fun observeDream(id: Long): Flow<DreamWithTags?> = dreamDao.observeDreamWithTags(id)

    fun observeAllTags(): Flow<List<Tag>> = tagDao.observeAllTags()

    fun observeTagsWithUsage(): Flow<List<TagWithUsage>> = tagDao.observeTagsWithUsage()

    /**
     * Renames a tag, unless [newName] already belongs to a different tag — in that case the two
     * are merged (all of this tag's dreams get reassigned to the existing one, and this tag is
     * removed) instead of creating a duplicate like "flying" / "Flying".
     */
    suspend fun renameOrMergeTag(tagId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val existing = tagDao.findByName(trimmed)
        if (existing != null && existing.id != tagId) {
            tagDao.mergeTagInto(sourceTagId = tagId, targetTagId = existing.id)
        } else {
            tagDao.renameTag(tagId, trimmed)
        }
    }

    suspend fun deleteTag(tagId: Long) {
        tagDao.deleteTagById(tagId)
    }

    /** Creates a standalone tag not yet attached to any dream; no-ops if one with this name (case-insensitive) already exists. */
    suspend fun createTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (tagDao.findByName(trimmed) == null) {
            tagDao.insertTag(Tag(name = trimmed))
        }
    }

    /** Snapshot of every live dream plus all known tag names, ready to serialize. */
    suspend fun exportSnapshot(): BackupFile {
        val dreams = dreamDao.getLiveDreamsOnce()
        val allTags = tagDao.getAllTagsOnce()
        return BackupFile(
            exportedAt = Instant.now().toString(),
            tags = allTags.map { it.name },
            dreams = dreams.map { it.toBackupDream() },
        )
    }

    /**
     * Adds every dream from [backup] as a new entry, preserving its original dates and re-linking
     * tags by name. Existing dreams are left untouched — importing the same file twice therefore
     * duplicates its dreams rather than silently overwriting anything.
     *
     * @return how many dreams were imported.
     */
    suspend fun importBackup(backup: BackupFile): Int {
        backup.tags.forEach { createTag(it) }

        var imported = 0
        backup.dreams.forEach { entry ->
            val dreamDate = runCatching { LocalDate.parse(entry.dreamDate) }.getOrNull() ?: return@forEach
            val createdAt = runCatching { Instant.parse(entry.createdAt) }.getOrNull() ?: Instant.now()
            val moods = entry.moods.mapNotNull { name -> runCatching { Mood.valueOf(name) }.getOrNull() }.toSet()

            saveDream(
                id = null,
                title = entry.title,
                content = entry.content,
                notes = entry.notes,
                dreamDate = dreamDate,
                isLucid = entry.isLucid,
                lucidity = entry.lucidity,
                clarity = entry.clarity,
                isNightmare = entry.isNightmare,
                isRecurring = entry.isRecurring,
                moods = moods,
                tagNames = entry.tags,
                existingCreatedAt = createdAt,
            )
            imported++
        }
        return imported
    }

    private fun DreamWithTags.toBackupDream() = BackupDream(
        title = dream.title,
        content = dream.content,
        notes = dream.notes,
        dreamDate = dream.dreamDate.toString(),
        createdAt = dream.createdAt.toString(),
        updatedAt = dream.updatedAt.toString(),
        isLucid = dream.isLucid,
        lucidity = dream.lucidity,
        clarity = dream.clarity,
        isNightmare = dream.isNightmare,
        isRecurring = dream.isRecurring,
        moods = dream.moods.map { it.name },
        tags = tags.map { it.name },
    )

    /** Searches title/content/notes of live dreams only. Terms are quoted-prefix-matched and ANDed together. */
    fun searchDreams(rawQuery: String): Flow<List<DreamWithTags>> {
        val ftsQuery = FtsQueryBuilder.build(rawQuery) ?: return flowOf(emptyList())
        return dreamDao.searchDreams(ftsQuery)
    }

    /** All figures are derived from live (non-binned) dreams only, recomputed reactively as they change. */
    fun observeStats(): Flow<DreamStats> =
        dreamDao.observeLiveDreams().map { dreams -> DreamStatsCalculator.calculate(dreams) }

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
            val newId = dreamDao.insertDream(dream)
            dreamFtsDao.insert(DreamFts(dreamId = newId, title = title, content = content, notes = notes))
            newId
        } else {
            dreamDao.updateDream(dream)
            dreamFtsDao.update(id, title = title, content = content, notes = notes)
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

    /** Binned dreams drop out of the search index; [restore] rebuilds their entry. */
    suspend fun softDelete(id: Long) {
        dreamDao.softDelete(id, Instant.now())
        dreamFtsDao.deleteByDreamId(id)
    }

    suspend fun softDeleteAll(ids: List<Long>) {
        dreamDao.softDeleteAll(ids, Instant.now())
        ids.forEach { dreamFtsDao.deleteByDreamId(it) }
    }

    suspend fun restore(id: Long) {
        dreamDao.restore(id)
        val restored = dreamDao.observeDreamWithTags(id).first()
        if (restored != null) {
            dreamFtsDao.insert(
                DreamFts(dreamId = id, title = restored.dream.title, content = restored.dream.content, notes = restored.dream.notes),
            )
        }
    }

    suspend fun deleteForever(id: Long) {
        dreamDao.deleteForever(id)
    }

    /** Permanently removes bin entries older than [BIN_RETENTION_DAYS]. Call once per app launch. */
    suspend fun purgeExpiredFromBin() {
        val cutoff = Instant.now().minus(BIN_RETENTION_DAYS, ChronoUnit.DAYS)
        dreamDao.purgeDeletedBefore(cutoff)
    }

    suspend fun emptyBin() {
        dreamDao.deleteAllBinned()
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
