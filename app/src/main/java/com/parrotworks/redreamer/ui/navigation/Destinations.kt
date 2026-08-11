package com.parrotworks.redreamer.ui.navigation

object Destinations {
    const val ARG_DREAM_ID = "dreamId"

    /**
     * Set on the previous back stack entry when a dream is deleted from its detail screen, so the
     * list it returns to can confirm what happened. The detail screen itself can't — it's gone by
     * the time a message would appear.
     */
    const val RESULT_DREAMS_DELETED = "dreamsDeleted"

    const val HOME = "home"
    const val BIN = "bin"
    const val TAG_MANAGEMENT = "tagManagement"
    const val DREAM_DETAIL = "dreamDetail/{$ARG_DREAM_ID}"
    const val DREAM_EDITOR = "dreamEditor?$ARG_DREAM_ID={$ARG_DREAM_ID}"

    fun dreamDetail(dreamId: Long) = "dreamDetail/$dreamId"

    fun dreamEditorNew() = "dreamEditor"

    fun dreamEditorEdit(dreamId: Long) = "dreamEditor?$ARG_DREAM_ID=$dreamId"
}
