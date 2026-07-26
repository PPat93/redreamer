package com.parrotworks.redreamer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.parrotworks.redreamer.ui.lock.AppLockGate
import com.parrotworks.redreamer.ui.navigation.ReDreamerNavGraph
import com.parrotworks.redreamer.ui.theme.ReDreamerTheme
import dagger.hilt.android.AndroidEntryPoint

/** A FragmentActivity (rather than plain ComponentActivity) because BiometricPrompt requires one. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReDreamerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppLockGate {
                        ReDreamerNavGraph()
                    }
                }
            }
        }
    }
}
