package com.parrotworks.redreamer.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.data.TagWithUsage
import com.parrotworks.redreamer.repository.DreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TagManagementViewModel @Inject constructor(
    private val repository: DreamRepository,
) : ViewModel() {

    val tags: StateFlow<List<TagWithUsage>> = repository.observeTagsWithUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun renameOrMerge(tagId: Long, newName: String) {
        viewModelScope.launch { repository.renameOrMergeTag(tagId, newName) }
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch { repository.deleteTag(tagId) }
    }
}
