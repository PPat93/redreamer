package com.parrotworks.redreamer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Cancels a ViewModel's `viewModelScope`.
 *
 * Tests construct ViewModels directly, so nothing ever clears them — and a ViewModel whose init
 * block collects a Flow forever keeps running on Dispatchers.Main after its test ends. The next
 * class's [MainDispatcherRule] then reassigns Main underneath it, which throws
 * "Dispatchers.Main is used concurrently with setting it" in whichever test happens to be running.
 *
 * `ViewModel.clear()` is internal, so its JVM name is mangled; find it by prefix rather than
 * guessing the suffix.
 */
fun ViewModel.clearForTest() {
    val method = ViewModel::class.java.declaredMethods
        .firstOrNull { it.name == "clear" || it.name.startsWith("clear$") }
        ?: error("Could not find ViewModel.clear() to cancel viewModelScope")
    method.isAccessible = true
    method.invoke(this)
}

/**
 * viewModelScope runs on Dispatchers.Main, which doesn't exist in a JVM test.
 *
 * Defaults to an unconfined test dispatcher so work launched from a ViewModel's init block has
 * already run by the time the test body starts. Pass a real dispatcher instead when the code under
 * test uses `delay` (debounce, say) *and* waits on the database: mixing runTest's virtual clock
 * with real background threads makes those tests flaky.
 */
class MainDispatcherRule(
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
