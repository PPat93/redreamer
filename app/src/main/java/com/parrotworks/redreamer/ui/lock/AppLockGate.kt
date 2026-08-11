package com.parrotworks.redreamer.ui.lock

import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parrotworks.redreamer.R

/**
 * Covers [content] with a lock screen until the user authenticates.
 *
 * Deliberately an overlay rather than a replacement: swapping [content] out would tear down the
 * navigation graph and every screen's state, so locking the app mid-edit would discard whatever the
 * user had typed. Keeping it composed underneath means they return exactly where they left off.
 *
 * Authentication is delegated entirely to the OS: biometrics if enrolled, otherwise the device
 * PIN/pattern/password. ReDreamer never stores or verifies a secret of its own.
 */
@Composable
fun AppLockGate(
    viewModel: AppLockViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val lockEnabled by viewModel.lockEnabled.collectAsStateWithLifecycle()
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    val lifecycleOwner = LocalLifecycleOwner.current

    /*
     * If the device no longer has any biometric or screen lock, honouring the stored preference
     * would show a lock screen whose prompt can never succeed — the journal would be permanently
     * unreachable, including the export that would rescue it. Degrading open is the safer failure:
     * the database isn't encrypted anyway, so anyone with this level of device access could read it
     * regardless, whereas bricking costs the user every dream they've written.
     */
    val canAuthenticate = context.canUseAppLock()
    val lockActive = lockEnabled == true && canAuthenticate

    // Re-lock when the user actually leaves — but not for system UI the app opened itself. A file
    // picker, share sheet or the device-credential keyguard all stop our activity while the user
    // never really left, and locking behind them would be both jarring and pointless.
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (!viewModel.isInSystemFlow) viewModel.relock()
                Lifecycle.Event.ON_START -> viewModel.onReturnedToForeground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // While the lock is on, keep entries out of screenshots and the recent-apps thumbnail. This
    // also closes the gap where content would be captured in the instant before the overlay draws.
    DisposableEffect(activity, lockActive) {
        val window = activity?.window
        if (lockActive) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (lockActive) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Rendered before the preference has loaded too — it is only ever *covered*, never discarded.
        content()

        when {
            // Preference still loading: cover without prompting, so entries can't flash on screen
            // before we know whether the journal is supposed to be locked.
            lockEnabled == null -> BlankCover()
            lockActive && !unlocked -> LockOverlay(
                onRequestUnlock = { requestUnlock(activity, viewModel, it) },
            )
        }
    }
}

@Composable
private fun BlankCover() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        color = MaterialTheme.colorScheme.background,
    ) {}
}

@Composable
private fun LockOverlay(onRequestUnlock: (UnlockStrings) -> Unit) {
    val strings = UnlockStrings(
        title = stringResource(R.string.app_lock_prompt_title),
        subtitle = stringResource(R.string.app_lock_prompt_subtitle),
        cancel = stringResource(R.string.action_cancel),
    )

    // Prompt automatically on arrival; the button is the retry path after a cancel or failure.
    LaunchedEffect(Unit) { onRequestUnlock(strings) }

    Surface(
        // Opaque and swallowing all gestures, so the covered content can be neither seen nor touched.
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(R.string.app_lock_locked_message),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            )
            Button(onClick = { onRequestUnlock(strings) }) {
                Text(stringResource(R.string.app_lock_unlock_action))
            }
        }
    }
}

private data class UnlockStrings(val title: String, val subtitle: String, val cancel: String)

private fun requestUnlock(
    activity: FragmentActivity?,
    viewModel: AppLockViewModel,
    strings: UnlockStrings,
) {
    val host = activity ?: return
    viewModel.beginSystemFlow()

    val prompt = BiometricPrompt(
        host,
        ContextCompat.getMainExecutor(host),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                viewModel.endSystemFlow()
                viewModel.onUnlocked()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                viewModel.endSystemFlow()
            }
        },
    )

    val authenticators = appLockAuthenticators()
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(strings.title)
        .setSubtitle(strings.subtitle)
        .setAllowedAuthenticators(authenticators)
        .apply {
            // A negative button is required without DEVICE_CREDENTIAL, and forbidden with it.
            if (authenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL == 0) {
                setNegativeButtonText(strings.cancel)
            }
        }
        .build()

    prompt.authenticate(info)
}

/** LocalContext may be a wrapper rather than the activity itself, so walk the chain. */
private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
