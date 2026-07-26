package com.parrotworks.redreamer.ui.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Shows [content] only once the journal is unlocked. When app lock is off this is a pass-through.
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
    val activity = LocalContext.current.findFragmentActivity()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-lock when the app actually leaves the foreground — but not while the OS auth UI is up,
    // since the device-credential path launches the keyguard as its own activity and would
    // otherwise immediately undo a successful unlock.
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !viewModel.isAuthenticating) {
                viewModel.relock()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Still loading the preference — render nothing rather than briefly exposing entries.
    val locked = lockEnabled ?: return

    if (!locked || unlocked) {
        content()
        return
    }

    val promptTitle = stringResource(R.string.app_lock_prompt_title)
    val promptSubtitle = stringResource(R.string.app_lock_prompt_subtitle)
    val cancelLabel = stringResource(R.string.action_cancel)

    val requestUnlock: () -> Unit = requestUnlock@{
        val host = activity ?: return@requestUnlock
        viewModel.isAuthenticating = true
        val prompt = BiometricPrompt(
            host,
            ContextCompat.getMainExecutor(host),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.isAuthenticating = false
                    viewModel.onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    viewModel.isAuthenticating = false
                }
            },
        )
        val authenticators = appLockAuthenticators()
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setSubtitle(promptSubtitle)
            .setAllowedAuthenticators(authenticators)
            .apply {
                // A negative button is required without DEVICE_CREDENTIAL, and forbidden with it.
                if (authenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL == 0) {
                    setNegativeButtonText(cancelLabel)
                }
            }
            .build()
        prompt.authenticate(info)
    }

    // Prompt automatically on arrival; the button is the retry path after a cancel or failure.
    LaunchedEffect(Unit) { requestUnlock() }

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
        Button(onClick = requestUnlock) {
            Text(stringResource(R.string.app_lock_unlock_action))
        }
    }
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
