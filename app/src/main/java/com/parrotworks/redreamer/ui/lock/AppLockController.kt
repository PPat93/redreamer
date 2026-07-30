package com.parrotworks.redreamer.ui.lock

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds whether the journal is currently unlocked, plus how many app-initiated system dialogs are
 * open.
 *
 * A singleton rather than ViewModel state for two reasons: the unlocked flag should outlive an
 * activity recreation (rotation shouldn't demand a fingerprint), and screens all over the app need
 * to announce excursions without sharing a ViewModel instance. A fresh process starts locked, which
 * is what we want.
 */
@Singleton
class AppLockController @Inject constructor() {

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    /**
     * Counts system UI the app itself opened — file pickers, share sheets, the device-credential
     * keyguard. Each one stops our activity, which would otherwise look identical to the user
     * walking away and trigger a re-lock.
     */
    private var activeSystemFlows = 0

    val isInSystemFlow: Boolean get() = activeSystemFlows > 0

    fun unlock() {
        _unlocked.value = true
    }

    fun relock() {
        _unlocked.value = false
    }

    /** Call immediately before launching system UI, so the resulting stop doesn't re-lock. */
    fun beginSystemFlow() {
        activeSystemFlows++
    }

    fun endSystemFlow() {
        activeSystemFlows = (activeSystemFlows - 1).coerceAtLeast(0)
    }

    /**
     * Once the app is genuinely back in the foreground every excursion is over. Resetting here
     * guarantees a missed [endSystemFlow] can never leave the lock permanently suppressed.
     */
    fun onReturnedToForeground() {
        activeSystemFlows = 0
    }
}
