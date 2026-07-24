package com.parrotworks.redreamer.ui.navigation

object Destinations {
    const val ARG_DREAM_ID = "dreamId"

    const val HOME = "home"
    const val DREAM_DETAIL = "dreamDetail/{$ARG_DREAM_ID}"
    const val DREAM_EDITOR = "dreamEditor?$ARG_DREAM_ID={$ARG_DREAM_ID}"

    fun dreamDetail(dreamId: Long) = "dreamDetail/$dreamId"

    fun dreamEditorNew() = "dreamEditor"

    fun dreamEditorEdit(dreamId: Long) = "dreamEditor?$ARG_DREAM_ID=$dreamId"
}
