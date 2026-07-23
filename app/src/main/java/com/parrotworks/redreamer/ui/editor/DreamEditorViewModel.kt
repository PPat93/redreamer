package com.parrotworks.redreamer.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.data.Mood
import com.parrotworks.redreamer.repository.DreamRepository
import com.parrotworks.redreamer.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
)

/**
 * Debounced autosave means there is no real "cancel": once the user has typed
 * a title or dream text, it is persisted to Room ~800ms after they stop
 * typing, well within the window where dream recall is normally lost.
 */
@HiltViewModel
class DreamEditorViewModel @Inject constructor(
    private val repository: DreamRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val existingDreamId: Long? =
        savedStateHandle.get<Long>(Destinations.ARG_DREAM_ID)?.takeIf { it >= 0 }

    private var dreamId: Long? = existingDreamId
    private var createdAt: Instant? = null
    private var autosaveJob: Job? = null

    private val _uiState = MutableStateFlow(DreamEditorUiState())
    val uiState: StateFlow<DreamEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllTags().collect { tags ->
                _uiState.update { it.copy(allTagNames = tags.map { tag -> tag.name }) }
            }
        }

        val id = existingDreamId
        if (id == null) {
            _uiState.update { it.copy(isReady = true) }
        } else {
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

    fun onTagInputChange(value: String) {
        _uiState.update { it.copy(tagInput = value) }
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
            onComplete()
        }
    }

    private fun updateState(transform: (DreamEditorUiState) -> DreamEditorUiState) {
        _uiState.update(transform)
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            persist()
        }
    }

    private suspend fun persist() {
        val state = _uiState.value
        if (state.title.isBlank() && state.content.isBlank()) return

        val savedId = repository.saveDream(
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
            existingCreatedAt = createdAt,
        )
        dreamId = savedId
    }

    private companion object {
        const val AUTOSAVE_DEBOUNCE_MS = 800L
    }
}
