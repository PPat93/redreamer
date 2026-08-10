package com.parrotworks.redreamer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

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
