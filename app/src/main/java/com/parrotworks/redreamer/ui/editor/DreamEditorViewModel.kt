package com.parrotworks.redreamer.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.data.Mood
import com.parrotworks.redreamer.di.ApplicationScope
import com.parrotworks.redreamer.repository.DreamRepository
import com.parrotworks.redreamer.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Serializable so the in-progress draft can be stashed in [SavedStateHandle] and survive the
 * process being killed while the user is away from the app.
 */
data class DreamEditorUiState(
    val title: String = "",
    val content: String = "",
    val notes: String = "",
    val dreamDate: LocalDate = LocalDate.now(),
    val isLucid: Boolean = false,
    val lucidity: Int = 5,
    val clarity: Int = 5,
    val isNightmare: Boolean = false,
    val isRecurring: Boolean = false,
    val moods: Set<Mood> = emptySet(),
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val allTagNames: List<String> = emptyList(),
    val isReady: Boolean = false,
    val isEditingExisting: Boolean = false,
) : Serializable

/**
 * Losing a half-written dream is the worst failure this app can have, so edits are protected twice:
 * a debounced write to the database ~800ms after typing stops, and an immediate mirror of the whole
 * draft into [SavedStateHandle] so even a process death mid-sentence comes back intact.
 */
@HiltViewModel
class DreamEditorViewModel @Inject constructor(
    private val repository: DreamRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val existingDreamId: Long? =
        savedStateHandle.get<Long>(Destinations.ARG_DREAM_ID)?.takeIf { it >= 0 }

    /** True when this editor is composing a brand new dream rather than editing a stored one. */
    private val isNewDream: Boolean = existingDreamId == null

    private var dreamId: Long? = existingDreamId
    private var createdAt: Instant? = null
    private var autosaveJob: Job? = null
    private val persistMutex = Mutex()

    /** Set on every edit, cleared once that edit reaches the database. */
    private var hasUnsavedChanges = false

    private val _uiState = MutableStateFlow(DreamEditorUiState())
    val uiState: StateFlow<DreamEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllTags().collect { tags ->
                _uiState.update { it.copy(allTagNames = tags.map { tag -> tag.name }) }
            }
        }

        val restoredDraft = savedStateHandle.get<DreamEditorUiState>(KEY_DRAFT)
        val id = existingDreamId

        when {
            // A draft survived process death — it is newer than whatever is in the database.
            restoredDraft != null -> {
                dreamId = savedStateHandle.get<Long>(KEY_DRAFT_DREAM_ID)?.takeIf { it >= 0 }
                createdAt = savedStateHandle.get<Long>(KEY_DRAFT_CREATED_AT)
                    ?.takeIf { it > 0 }
                    ?.let(Instant::ofEpochMilli)
                _uiState.value = restoredDraft.copy(isReady = true)
            }

            id == null -> _uiState.update { it.copy(isReady = true) }

            else -> {
                _uiState.update { it.copy(isEditingExisting = true) }
                viewModelScope.launch {
                    val existing = repository.observeDream(id).first()
                    if (existing == null) {
                        _uiState.update { it.copy(isReady = true) }
                    } else {
                        createdAt = existing.dream.createdAt
                        _uiState.update {
                            it.copy(
                                title = existing.dream.title,
                                content = existing.dream.content,
                                notes = existing.dream.notes,
                                dreamDate = existing.dream.dreamDate,
                                isLucid = existing.dream.isLucid,
                                lucidity = existing.dream.lucidity ?: 5,
                                clarity = existing.dream.clarity,
                                isNightmare = existing.dream.isNightmare,
                                isRecurring = existing.dream.isRecurring,
                                moods = existing.dream.moods,
                                tags = existing.tags.map { tag -> tag.name },
                                isReady = true,
                            )
                        }
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) = updateState { it.copy(title = value) }
    fun onContentChange(value: String) = updateState { it.copy(content = value) }
    fun onNotesChange(value: String) = updateState { it.copy(notes = value) }
    fun onDreamDateChange(value: LocalDate) = updateState { it.copy(dreamDate = value) }
    fun onClarityChange(value: Int) = updateState { it.copy(clarity = value) }
    fun onLucidChange(value: Boolean) = updateState { it.copy(isLucid = value) }
    fun onLucidityChange(value: Int) = updateState { it.copy(lucidity = value) }
    fun onNightmareChange(value: Boolean) = updateState { it.copy(isNightmare = value) }
    fun onRecurringChange(value: Boolean) = updateState { it.copy(isRecurring = value) }

    fun onMoodToggle(mood: Mood) = updateState {
        val moods = if (mood in it.moods) it.moods - mood else it.moods + mood
        it.copy(moods = moods)
    }

    /** Half-typed tag text is worth keeping too, but it alone doesn't warrant a database write. */
    fun onTagInputChange(value: String) {
        hasUnsavedChanges = true
        stashDraft(_uiState.updateAndGet { it.copy(tagInput = value) })
    }

    fun onAddTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        updateState { state ->
            if (state.tags.any { it.equals(trimmed, ignoreCase = true) }) {
                state.copy(tagInput = "")
            } else {
                state.copy(tags = state.tags + trimmed, tagInput = "")
            }
        }
    }

    fun onRemoveTag(name: String) = updateState { state ->
        state.copy(tags = state.tags.filterNot { it == name })
    }

    /** Cancels the pending debounce and persists immediately, e.g. when the user taps Save. */
    fun saveNow(onComplete: () -> Unit) {
        autosaveJob?.cancel()
        viewModelScope.launch {
            persist()
            clearDraft()
            onComplete()
        }
    }

    /**
     * Leaving the editor destroys this ViewModel, cancelling any debounced autosave still waiting.
     * Without this, typing and immediately pressing back threw away everything written in the last
     * [AUTOSAVE_DEBOUNCE_MS] — so the final flush runs on a scope that outlives the screen.
     */
    override fun onCleared() {
        super.onCleared()
        if (!hasUnsavedChanges) return
        autosaveJob?.cancel()
        applicationScope.launch { persist() }
    }

    private fun updateState(transform: (DreamEditorUiState) -> DreamEditorUiState) {
        hasUnsavedChanges = true
        stashDraft(_uiState.updateAndGet(transform))
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            persist()
        }
    }

    /**
     * Serialised, and deliberately uncancellable once started. A debounced autosave can still be
     * mid-write when the user taps Save; letting the two overlap meant both read a null [dreamId]
     * and inserted a *second* copy of the same dream.
     */
    private suspend fun persist() = persistMutex.withLock {
        withContext(NonCancellable) {
            val state = _uiState.value
            if (state.title.isBlank() && state.content.isBlank()) {
                // Emptying both fields after an autosave already created the dream used to leave it
                // behind holding the text the user had just deleted. Only discard drafts this editor
                // created — clearing the fields of an existing dream must not delete it.
                val autosavedDraftId = dreamId.takeIf { isNewDream }
                if (autosavedDraftId != null) {
                    repository.discardDream(autosavedDraftId)
                    dreamId = null
                    createdAt = null
                }
                hasUnsavedChanges = false
                return@withContext
            }

            // Pin the creation time on first save; otherwise every later autosave of a new dream
            // would push createdAt forward, which is meant to be immutable and is the list's
            // tiebreak sort key.
            val createdAtToUse = createdAt ?: Instant.now().also { createdAt = it }

            dreamId = repository.saveDream(
                id = dreamId,
                title = state.title,
                content = state.content,
                notes = state.notes,
                dreamDate = state.dreamDate,
                isLucid = state.isLucid,
                lucidity = state.lucidity,
                clarity = state.clarity,
                isNightmare = state.isNightmare,
                isRecurring = state.isRecurring,
                moods = state.moods,
                tagNames = state.tags,
                existingCreatedAt = createdAtToUse,
            )
            stashDraft(state)
            hasUnsavedChanges = false
        }
    }

    private fun stashDraft(state: DreamEditorUiState) {
        savedStateHandle[KEY_DRAFT] = state
        savedStateHandle[KEY_DRAFT_DREAM_ID] = dreamId ?: -1L
        savedStateHandle[KEY_DRAFT_CREATED_AT] = createdAt?.toEpochMilli() ?: -1L
    }

    /** Once the user has deliberately saved, the stashed draft would only cause a stale restore. */
    private fun clearDraft() {
        savedStateHandle.remove<DreamEditorUiState>(KEY_DRAFT)
        savedStateHandle.remove<Long>(KEY_DRAFT_DREAM_ID)
        savedStateHandle.remove<Long>(KEY_DRAFT_CREATED_AT)
    }

    private companion object {
        const val AUTOSAVE_DEBOUNCE_MS = 800L
        const val KEY_DRAFT = "editor_draft"
        const val KEY_DRAFT_DREAM_ID = "editor_draft_dream_id"
        const val KEY_DRAFT_CREATED_AT = "editor_draft_created_at"
    }
}
