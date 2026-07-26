package com.parrotworks.redreamer.ui.lock

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager

/**
 * Which authenticators the app lock accepts.
 *
 * Mixing DEVICE_CREDENTIAL with a biometric class is only properly supported from API 30, so older
 * devices fall back to biometrics alone (with a plain cancel button). This also keeps us on the
 * right side of BiometricPrompt's rule that a negative button and DEVICE_CREDENTIAL are mutually
 * exclusive.
 */
internal fun appLockAuthenticators(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    }

/** True when this device can actually satisfy [appLockAuthenticators], so the toggle is worth offering. */
internal fun Context.canUseAppLock(): Boolean =
    BiometricManager.from(this).canAuthenticate(appLockAuthenticators()) == BiometricManager.BIOMETRIC_SUCCESS
